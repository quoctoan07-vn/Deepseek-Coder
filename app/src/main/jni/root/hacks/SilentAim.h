/*
 * DeepSeek Coder - secondary aim (root helper).
 *
 * The old 0xD20 AimInfo block no longer exists in this build. This module now
 * uses the same aim-rotation write path as the main aimbot, with its own FOV
 * and distance settings. It is a visible snap until the new weapon aim-assist
 * interface is mapped.
 */
#pragma once
#include "../game/GameManager.h"
#include "../game/Player.h"
#include "../game/PlayerList.h"
#include "../Unity/MathUtils.h"
#include "../../common/Protocol.h"
#include <cmath>

class SilentAim {
public:
    void Run(GameManager& game, Config& cfg) {
        if (!cfg.silentAim || cfg.aimbotEnabled || !game.IsInMatch()) return;

        uintptr_t localAddr = game.GetLocalPlayerAddr();
        Player local(localAddr);

        if (!localAddr) return;
        if (!cfg.aimOnFire && !cfg.aimScoped) return;
        bool fireOk = cfg.aimOnFire && local.IsFiring(game.GetGameTime());
        bool scopeOk = cfg.aimScoped && local.IsSighting();
        if (!fireOk && !scopeOk) return;

        D3DMatrix matrix = game.GetViewMatrix();
        Vector3 camPos = GetCameraWorldPos(game.GetViewOnlyMatrix());
        Vector3 headPos = local.GetHeadPosition();
        if (camPos == Vector3::Zero()) camPos = headPos;
        if (headPos == Vector3::Zero()) return;

        uintptr_t players[200];
        int count = ReadAllPlayers(game.GetBaseGame(), game.GetMatchAddr(), players, 200);
        if (count <= 0) return;
        uintptr_t values = (uintptr_t)players;

        float bestDist = 99999.0f;
        Vector3 bestPos = Vector3::Zero();
        float cx = g_screenWidth / 2.0f;
        float cy = g_screenHeight / 2.0f;

        ForEachPlayer(values, count, [&](Player enemy) {
            if (!enemy.IsValidEnemy(localAddr)) return true;

            uint32_t vis = enemy.GetVisibleFlags();
            if (vis != 0 && (vis & Offsets::ISVISIBLE_DynamicPVS) == 0) return true;

            Vector3 pos = enemy.GetAimPosition(cfg.aimBone);
            if (pos == Vector3::Zero()) return true;

            float worldDist = Vector3::Distance(headPos, pos);
            if (worldDist > cfg.aimDistance) return true;

            Vector3 screenPos = WorldToScreen(matrix, pos);
            if (screenPos.X < 0.0f || screenPos.Y < 0.0f
                    || screenPos.X > (float)g_screenWidth
                    || screenPos.Y > (float)g_screenHeight) return true;

            float dx = screenPos.X - cx;
            float dy = screenPos.Y - cy;
            float screenDist = sqrtf(dx * dx + dy * dy);
            if (cfg.aimFov < 360.0f && screenDist > cfg.aimFov) return true;

            if (screenDist < bestDist) {
                bestDist = screenDist;
                bestPos = pos;
            }
            return true;
        });

        if (bestPos != Vector3::Zero()) {
            Quaternion rot = Quaternion::LookRotation(bestPos - headPos, Vector3(0, 1, 0));
            local.SetRotation(rot);
        }
    }

    void Stop() {}
};
