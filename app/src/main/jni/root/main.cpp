/*
 * DeepSeek Coder - embedded root helper.
 *
 * Launched by the app through `su`. It attaches to the game via
 * /proc/<pid>/mem, runs the overlays/aim logic and serves compact
 * frames to the app over a loopback-only TCP socket.
 */
#include "Includes/Logger.h"
#include "Includes/Process.h"
#include "Includes/Memory.h"
#include "game/GameManager.h"
#include "hacks/Aimbot.h"
#include "hacks/ESP.h"
#include "hacks/SpeedHack.h"
#include "SocketServer.h"
#include "../common/Protocol.h"

#include <chrono>
#include <csignal>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <memory>
#include <string>
#include <unistd.h>

using namespace std::chrono;

static void sanitizeConfig(Config& c) {
    c.espEnabled = c.espEnabled ? 1 : 0;
    c.aimbotEnabled = c.aimbotEnabled ? 1 : 0;
    c.aimOnFire = c.aimOnFire ? 1 : 0;
    c.aimScoped = c.aimScoped ? 1 : 0;
    c.headLock = c.headLock ? 1 : 0;
    c.silentAim = c.silentAim ? 1 : 0;
    c.box = c.box ? 1 : 0;
    c.health = c.health ? 1 : 0;
    c.distance = c.distance ? 1 : 0;
    c.line = c.line ? 1 : 0;
    c.name = c.name ? 1 : 0;
    c.tracker = c.tracker ? 1 : 0;
    c.hud = c.hud ? 1 : 0;
    c.speedHack = c.speedHack ? 1 : 0;

    if (c.aimFov < 5.0f) c.aimFov = 5.0f;
    if (c.aimFov > 360.0f) c.aimFov = 360.0f;
    if (c.aimBone > 2) c.aimBone = 0;
    if (c.aimDistance < 10.0f) c.aimDistance = 10.0f;
    if (c.aimDistance > 2000.0f) c.aimDistance = 2000.0f;
    if (c.espColor < 0) c.espColor = 0;
    if (c.espColor > 9) c.espColor = 9;
    if (c.thickness < 1) c.thickness = 1;
    if (c.thickness > 8) c.thickness = 8;
}

class Engine {
public:
    explicit Engine(std::string package) : package_(std::move(package)) {}
    ~Engine() { detach(); }

    bool attached() const { return pid_ > 0 && game_ != nullptr; }
    GameManager* game() { return game_.get(); }

    void tick() {
        auto now = steady_clock::now();

        if (pid_ > 0) {
            if (kill(pid_, 0) != 0) {
                detach();
            } else if (now - lastScan_ > seconds(5)) {
                lastScan_ = now;
                if (FindPid(package_.c_str()) != pid_) detach();
            }
        }

        if (pid_ == 0 && now - lastScan_ > seconds(2)) {
            lastScan_ = now;
            pid_t pid = FindPid(package_.c_str());
            if (pid <= 0) return;

            if (OpenMemory(pid) <= 0) return;

            uintptr_t base = FindLibrary("libil2cpp.so", pid);
            if (base == 0) {
                CloseMemory();
                return;
            }

            pid_ = pid;
            base_ = base;
            game_ = std::make_unique<GameManager>(base);
            LOGI("[DC] attached pid=%d base=0x%lx", pid_, (unsigned long)base_);
        }
    }

    void detach() {
        if (pid_ > 0) {
            LOGI("[DC] detached");
            CloseMemory();
        }
        pid_ = 0;
        base_ = 0;
        game_.reset();
    }

private:
    std::string package_;
    pid_t pid_ = 0;
    uintptr_t base_ = 0;
    std::unique_ptr<GameManager> game_;
    steady_clock::time_point lastScan_{};
};

int main(int argc, char* argv[]) {
    signal(SIGPIPE, SIG_IGN);

    int port = 0;
    char token[64] = {0};
    const char* package = "com.dts.freefiremax";

    for (int i = 1; i < argc; i++) {
        if (strncmp(argv[i], "--port=", 7) == 0) {
            port = atoi(argv[i] + 7);
        } else if (strncmp(argv[i], "--token=", 8) == 0) {
            strncpy(token, argv[i] + 8, sizeof(token) - 1);
        } else if (strncmp(argv[i], "--game=", 7) == 0) {
            package = argv[i] + 7;
        }
    }

    if (port <= 0 || token[0] == '\0') {
        LOGE("[DC] missing --port or --token");
        return 1;
    }

    TcpServer server;
    if (!server.Create() || !server.Bind(port) || !server.Listen()) {
        LOGE("[DC] failed to listen on 127.0.0.1:%d (errno=%d)", port, errno);
        return 1;
    }
    LOGI("[DC] helper ready on 127.0.0.1:%d", port);

    // The app normally connects within a second. If it dies before doing so,
    // do not leave a root process behind forever.
    signal(SIGALRM, [](int) { _exit(0); });

    bool authenticated = false;
    while (!authenticated) {
        alarm(60);
        if (!server.Accept()) {
            LOGE("[DC] accept failed");
            return 1;
        }
        alarm(0);
        server.setRecvTimeout(10);

        Handshake hs{};
        if (!server.recvAll(&hs, sizeof(hs))) {
            server.Disconnect();
            continue;
        }

        HandshakeResponse hsr{};
        hsr.magic = DC_MAGIC;

        if (hs.magic != DC_MAGIC || strncmp(hs.token, token, sizeof(token) - 1) != 0) {
            hsr.accepted = 0;
            server.sendAll(&hsr, sizeof(hsr));
            server.Disconnect();
            continue;
        }

        hsr.accepted = 1;
        if (!server.sendAll(&hsr, sizeof(hsr))) {
            server.Disconnect();
            return 1;
        }
        authenticated = true;
    }
    server.setRecvTimeout(0);
    LOGI("[DC] client connected");

    Engine engine(package);
    Aimbot aimbot;
    ESP esp;
    SpeedHack speed;

    while (true) {
        Request req{};
        if (!server.recvAll(&req, sizeof(req))) break;
        if (req.Mode != Mode::EspUpdate) continue;

        Response resp{};
        resp.Success = 1;

        engine.tick();
        if (!engine.attached()) {
            resp.GameFound = 0;
            if (!server.sendAll(&resp, sizeof(resp))) break;
            continue;
        }

        resp.GameFound = 1;
        g_screenWidth = req.ScreenWidth;
        g_screenHeight = req.ScreenHeight;

        Config cfg = req.cfg;
        sanitizeConfig(cfg);

        GameManager* game = engine.game();
        if (game->Update()) {
            bool inMatch = game->IsInMatch();
            resp.InMatch = inMatch ? 1 : 0;

            if (inMatch) {
                aimbot.Run(*game, cfg);
                speed.Apply(*game, cfg);
                esp.BuildData(*game, resp, cfg);
            }
        }

        if (!server.sendAll(&resp, sizeof(resp))) break;
    }

    LOGI("[DC] client disconnected, exiting");
    engine.detach();
    server.Close();
    return 0;
}
