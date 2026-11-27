/*
 * DeepSeek Coder - ESP data builder (root helper).
 *
 * Iterates the live player dictionary, filters dead/hidden entries and
 * projects head/foot positions to screen space. Off-screen players can be
 * collapsed into edge trackers instead of being dropped. Player pointers are
 * read in batches and each player's fields are fetched in one syscall.
 */
#pragma once
#include "../game/GameManager.h"
#include "../game/Player.h"
#include "../game/PlayerList.h"
#include "../Unity/MathUtils.h"
#include "../../common/Protocol.h"
#include <cmath>

class ESP {
public:
    void BuildData(GameManager& game, Response& resp, Config& cfg) {
        resp.PlayerCount = 0;
        resp.BotCount = 0;
        if (!cfg.espEnabled || !game.IsInMatch()) return;

        uintptr_t localPlayer = game.GetLocalPlayerAddr();
        D3DMatrix matrix = game.GetViewMatrix();

        uintptr_t players[200];
        int count = ReadAllPlayers(game.GetBaseGame(), game.GetMatchAddr(), players, 200);
        if (count <= 0) return;
        uintptr_t values = (uintptr_t)players;

        Player local(localPlayer);
        Vector3 myPos = local.GetHeadPosition();
        if (myPos == Vector3::Zero()) {
            myPos = GetCameraWorldPos(game.GetViewOnlyMatrix());
        }

        float sw = (float)g_screenWidth;
        float sh = (float)g_screenHeight;

        ForEachPlayer(values, count, [&](Player enemy) {
            if (!enemy.IsESPTarget(localPlayer)) return true;

            Vector3 headPos = enemy.GetHeadPosition();
            if (headPos == Vector3::Zero()) return true;

            Vector3 footPos = enemy.GetFootPosition();
            if (footPos == Vector3::Zero()) return true;

            Vector3 headScreen = WorldToScreen(matrix, headPos);
            Vector3 footScreen = WorldToScreen(matrix, footPos);
            bool behindCamera = (headScreen.X < 0);

            bool offScreen = behindCamera ||
                (IsOffScreen(headScreen, sw, sh) && IsOffScreen(footScreen, sw, sh));

            if (offScreen) {
                if (!cfg.tracker) return true;
                headScreen = GetTrackerPosition(matrix, headPos, sw, sh);
            }

            auto* data = &resp.Players[resp.PlayerCount];
            data->headX = headScreen.X;
            data->headY = headScreen.Y;
            data->toeX = footScreen.X;
            data->toeY = footScreen.Y;
            data->IsDying = enemy.IsDying() ? 1 : 0;
            data->IsBot = enemy.IsBot() ? 1 : 0;
            data->Distance = Vector3::Distance(myPos, headPos);
            data->Health = enemy.GetHealth();
            if (data->IsBot) resp.BotCount++;
            enemy.GetName(data->PlayerName);

            resp.PlayerCount++;
            return resp.PlayerCount < DC_MAX_PLAYERS;
        });

#ifdef DC_DEBUG_SELF
        if (localPlayer && resp.PlayerCount < DC_MAX_PLAYERS) {
            Vector3 h = local.GetHeadPosition();
            Vector3 f = local.GetFootPosition();
            if (h != Vector3::Zero() && f != Vector3::Zero()) {
                Vector3 hs = WorldToScreen(matrix, h);
                Vector3 fs = WorldToScreen(matrix, f);
                auto* d = &resp.Players[resp.PlayerCount++];
                d->headX = hs.X;
                d->headY = hs.Y;
                d->toeX = fs.X;
                d->toeY = fs.Y;
                d->Distance = 0;
                d->Health = 100;
                d->IsDying = 0;
                d->IsBot = 1;
                strcpy(d->PlayerName, "SELF");
            }
        }
#endif
    }

private:
    static bool IsOffScreen(Vector3 screen, float sw, float sh) {
        return screen.X < 0 || screen.X > sw || screen.Y < 0 || screen.Y > sh;
    }

    static Vector3 GetTrackerPosition(D3DMatrix& matrix, Vector3 worldPos, float sw, float sh) {
        float x = worldPos.X * matrix._11 + worldPos.Y * matrix._21
                + worldPos.Z * matrix._31 + matrix._41;
        float y = worldPos.X * matrix._12 + worldPos.Y * matrix._22
                + worldPos.Z * matrix._32 + matrix._42;
        float w = worldPos.X * matrix._14 + worldPos.Y * matrix._24
                + worldPos.Z * matrix._34 + matrix._44;

        if (w < 0) { x = -x; y = -y; }

        float len = sqrtf(x * x + y * y);
        if (len < 0.001f) len = 0.001f;

        float halfW = sw / 2.0f;
        float halfH = sh / 2.0f;
        float push = fmaxf(sw, sh) * 2.0f;

        return Vector3(halfW + (x / len) * push, halfH - (y / len) * push, 0);
    }
};
