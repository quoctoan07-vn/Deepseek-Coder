/*
 * DeepSeek Coder - JNI bridge between the app UI and the embedded root helper.
 */
#include <jni.h>
#include <mutex>
#include <string>
#include <cstring>
#include "SocketClient.h"
#include "ESPRenderer.h"
#include "UiStrings.h"
#include "../common/Protocol.h"
#include "../root/Includes/Logger.h"

static SocketClient g_socket;
static std::mutex g_mutex;
static Config g_cfg;
static ESPRenderer g_renderer;

static int g_gameFound = 0;
static int g_inMatch = 0;
static int g_players = 0;
static int g_bots = 0;

static Config defaultConfig() {
    Config c{};
    c.espColor = 0;
    c.thickness = 2;
    c.aimFov = 360.0f;
    c.aimDistance = 500.0f;
    return c;
}

static std::string jstringToString(JNIEnv* env, jstring value) {
    if (!value) return std::string();
    const char* raw = env->GetStringUTFChars(value, nullptr);
    std::string out(raw ? raw : "");
    if (raw) env->ReleaseStringUTFChars(value, raw);
    return out;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_deepseek_coder_NativeBridge_nativeConnect(JNIEnv* env, jclass, jstring host, jint port, jstring token) {
    std::string h = jstringToString(env, host);
    std::string t = jstringToString(env, token);

    std::lock_guard<std::mutex> lock(g_mutex);
    bool ok = g_socket.connectTo(h.c_str(), (int)port, t.c_str());
    if (!ok) {
        g_gameFound = 0;
        g_inMatch = 0;
        g_players = 0;
        g_bots = 0;
    }
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_deepseek_coder_NativeBridge_nativeDisconnect(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_socket.closeSocket();
    g_gameFound = 0;
    g_inMatch = 0;
    g_players = 0;
    g_bots = 0;
}

JNIEXPORT jboolean JNICALL
Java_com_deepseek_coder_NativeBridge_nativeIsConnected(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_socket.isConnected() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_deepseek_coder_NativeBridge_nativeLabel(JNIEnv* env, jclass, jint id) {
    return env->NewStringUTF(UiStrings::get((int)id));
}

JNIEXPORT void JNICALL
Java_com_deepseek_coder_NativeBridge_nativeSetToggle(JNIEnv*, jclass, jint mode, jboolean on) {
    std::lock_guard<std::mutex> lock(g_mutex);
    uint8_t v = on ? 1 : 0;

    switch (mode) {
        case Mode::Esp:       g_cfg.espEnabled = v; break;
        case Mode::Aimbot:    g_cfg.aimbotEnabled = v; break;
        case Mode::AimOnFire: g_cfg.aimOnFire = v; break;
        case Mode::AimScoped: g_cfg.aimScoped = v; break;
        case Mode::BoxEsp:    g_cfg.box = v; break;
        case Mode::HealthBar: g_cfg.health = v; break;
        case Mode::DistEsp:   g_cfg.distance = v; break;
        case Mode::LineEsp:   g_cfg.line = v; break;
        case Mode::NameEsp:   g_cfg.name = v; break;
        case Mode::OffScreen: g_cfg.tracker = v; break;
        case Mode::HudInfo:   g_cfg.hud = v; break;
        case Mode::HeadLock:  g_cfg.headLock = v; break;
        case Mode::SpeedHack: g_cfg.speedHack = v; break;
        case Mode::AimSilent: g_cfg.silentAim = v; break;
        default: break;
    }
}

JNIEXPORT void JNICALL
Java_com_deepseek_coder_NativeBridge_nativeSetValue(JNIEnv*, jclass, jint mode, jint value) {
    std::lock_guard<std::mutex> lock(g_mutex);
    switch (mode) {
        case Mode::EspColor:  g_cfg.espColor = value; break;
        case Mode::Thickness: g_cfg.thickness = value; break;
        case Mode::AimFov:    g_cfg.aimFov = (float)value; break;
        case Mode::AimDist:   g_cfg.aimDistance = (float)value; break;
        case Mode::AimBone:   g_cfg.aimBone = (uint8_t)value; break;
        default: break;
    }
}

JNIEXPORT jintArray JNICALL
Java_com_deepseek_coder_NativeBridge_nativeGetStatus(JNIEnv* env, jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    jint values[5] = {
        g_socket.isConnected() ? 1 : 0,
        g_gameFound,
        g_inMatch,
        g_players,
        g_bots
    };
    jintArray arr = env->NewIntArray(5);
    if (arr) env->SetIntArrayRegion(arr, 0, 5, values);
    return arr;
}

JNIEXPORT void JNICALL
Java_com_deepseek_coder_NativeBridge_nativeDraw(JNIEnv* env, jclass, jobject espView, jobject canvas, jint w, jint h) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_socket.isConnected()) {
        g_gameFound = 0;
        g_inMatch = 0;
        g_players = 0;
        g_bots = 0;
        return;
    }

    Request req{};
    req.Mode = Mode::EspUpdate;
    req.ScreenWidth = w;
    req.ScreenHeight = h;
    req.cfg = g_cfg;

    Response resp{};
    if (!g_socket.request(req, resp)) {
        g_gameFound = 0;
        g_inMatch = 0;
        g_players = 0;
        g_bots = 0;
        return;
    }

    g_gameFound = resp.GameFound;
    g_inMatch = resp.InMatch;
    g_players = resp.PlayerCount;
    g_bots = resp.BotCount;

    if (resp.GameFound && resp.InMatch) {
        g_renderer.draw(env, espView, canvas, w, h, resp, g_cfg);
    }
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM*, void*) {
    g_cfg = defaultConfig();
    return JNI_VERSION_1_6;
}

} // extern "C"
