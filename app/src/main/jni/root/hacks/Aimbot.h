/*
 * DeepSeek Coder - aimbot (root helper).
 *
 * Picks the closest valid enemy inside the configured FOV and world distance,
 * then writes a look-rotation quaternion to the local player. Head lock swaps
 * the head collider before the game processes the next shot.
 */

#pragma once
#include "../game/GameManager.h"
#include "../game/Player.h"
#include "../game/PlayerList.h"
#include "../Unity/MathUtils.h"
#include "../../common/Protocol.h"
#include <cmath>
#include <chrono>

class Aimbot {
public:
    void Run(GameManager& game, Config& cfg) {
        if (!cfg.aimbotEnabled || !game.IsInMatch()) return;

        uintptr_t localPlayer = game.GetLocalPlayerAddr();
        Player local(localPlayer);

        if (!localPlayer) return;

        uint8_t fireState = local.GetPRIFiringValue();
        auto now = std::chrono::steady_clock::now();
        if (fireState != 0) {
            lastFireSeen_ = now;
            hasFireSeen_ = true;
        }
        bool firingRecent = hasFireSeen_ &&
                (now - lastFireSeen_) < std::chrono::milliseconds(400);

        static int gateDbg = 0;
        if ((gateDbg++ % 60) == 0) {
            float gt = game.GetGameTime();
            Vector3 lh = local.GetHeadPosition();
            Vector3 la = local.GetAimPosition(cfg.aimBone);
            LOGI("[DC] aim gate local=0x%lx sight=0x%lx priSight=%u priFire=%u recent=%d scoped=%d fire=%d bone=%d headY=%.2f aimY=%.2f gt=%.2f",
                 (unsigned long)localPlayer,
                 (unsigned long)local.GetSightingWeapon(),
                 local.GetPRISightingValue(),
                 (unsigned)fireState,
                 firingRecent ? 1 : 0,
                 cfg.aimScoped ? 1 : 0,
                 cfg.aimOnFire ? 1 : 0,
                 (int)cfg.aimBone,
                 lh.Y, la.Y,
                 gt);
        }

        if (!cfg.aimOnFire && !cfg.aimScoped) return; // pick at least one trigger
        bool fireOk = cfg.aimOnFire && firingRecent;
        bool scopeOk = cfg.aimScoped && local.IsSighting();
        if (!fireOk && !scopeOk) return;

        uintptr_t players[200];
        int count = ReadAllPlayers(game.GetBaseGame(), game.GetMatchAddr(), players, 200);
        if (count <= 0) return;
        uintptr_t values = (uintptr_t)players;

        D3DMatrix matrix = game.GetViewMatrix();
        Vector3 camPos = GetCameraWorldPos(game.GetViewOnlyMatrix());
        Vector3 headPos = local.GetHeadPosition();
        if (camPos == Vector3::Zero()) camPos = headPos;
        if (headPos == Vector3::Zero()) return;

        float cx = g_screenWidth / 2.0f;
        float cy = g_screenHeight / 2.0f;

        uintptr_t bestAddr = 0;
        float bestDist = 99999.0f;
        Vector3 bestPos = Vector3::Zero();
        uint32_t bestVis = 0;

        uintptr_t lockedAddr = 0;
        float lockedDist = 99999.0f;
        Vector3 lockedPos = Vector3::Zero();
        uint32_t lockedVis = 0;

        int validCount = 0;
        int zeroHead = 0;
        int outDistance = 0;
        int outFov = 0;
        int hidden = 0;

        ForEachPlayer(values, count, [&](Player enemy) {
            if (!enemy.IsValidEnemy(localPlayer)) return true;

            validCount++;
            if (cfg.headLock) enemy.ApplyHeadCollider();

            uint32_t vis = enemy.GetVisibleFlags();
            if (vis != 0 && (vis & Offsets::ISVISIBLE_DynamicPVS) == 0) {
                hidden++;
                return true;
            }

            Vector3 pos = enemy.GetAimPosition(cfg.aimBone);
            if (pos == Vector3::Zero()) { zeroHead++; return true; }

            float worldDist = Vector3::Distance(headPos, pos);
            if (worldDist > cfg.aimDistance) { outDistance++; return true; }

            Vector3 sp = WorldToScreen(matrix, pos);
            if (sp.X < 0.0f || sp.Y < 0.0f
                    || sp.X > (float)g_screenWidth || sp.Y > (float)g_screenHeight) {
                outFov++;
                return true;
            }

            float screenDist = sqrtf(
                    (sp.X - cx) * (sp.X - cx) + (sp.Y - cy) * (sp.Y - cy));
            if (cfg.aimFov < 360.0f && screenDist > cfg.aimFov) {
                outFov++;
                return true;
            }

            if (enemy.GetAddress() == lockedTarget_) {
                lockedAddr = enemy.GetAddress();
                lockedDist = screenDist;
                lockedPos = pos;
                lockedVis = vis;
            }

            if (screenDist < bestDist) {
                bestDist = screenDist;
                bestAddr = enemy.GetAddress();
                bestPos = pos;
                bestVis = vis;
            }
            return true;
        });

        // Small hysteresis: keep the current target until another enemy is
        // clearly closer to the crosshair. Prevents flickering between two
        // enemies that are almost equally close to screen center.
        if (lockedAddr != 0 && lockedDist <= bestDist + 40.0f) {
            bestAddr = lockedAddr;
            bestPos = lockedPos;
            bestDist = lockedDist;
            bestVis = lockedVis;
        }
        lockedTarget_ = bestAddr;

        if ((gateDbg % 60) == 1) {
            Vector3 lc = local.GetChestPosition();
            Vector3 lhip = local.GetHipPosition();
            LOGI("[DC] aim scan count=%d valid=%d hidden=%d zeroHead=%d outDist=%d outFov=%d best=%d bone=%d fov=%.0f dist=%.0f head=(%.1f,%.1f,%.1f) chest=(%.1f,%.1f,%.1f)",
                 count, validCount, hidden, zeroHead, outDistance, outFov,
                 bestAddr != 0 ? 1 : 0,
                 (int)cfg.aimBone, cfg.aimFov, cfg.aimDistance,
                 headPos.X, headPos.Y, headPos.Z,
                 lc.X, lc.Y, lc.Z);
        }

        if (bestAddr != 0) {
            // The weapon sits on the right shoulder. Aiming from the head
            // leaves a parallel offset that shows up on the right edge of the
            // enemy box; shift the rotation origin to the shoulder to cancel it.
            Vector3 flat(bestPos.X - headPos.X, 0.0f, bestPos.Z - headPos.Z);
            Vector3 right = Vector3::Cross(Vector3(0, 1, 0), flat);
            float rightLen = sqrtf(right.X * right.X + right.Y * right.Y + right.Z * right.Z);

            Vector3 aimOrigin = headPos;
            if (rightLen > 0.01f) {
                aimOrigin = headPos + right * (0.30f / rightLen);
            }
            if (aimOrigin == Vector3::Zero()) aimOrigin = camPos;

            Quaternion rot = Quaternion::LookRotation(bestPos - aimOrigin, Vector3(0, 1, 0));
            local.SetRotation(rot);

            static int dbg = 0;
            if (dbg < 5) {
                dbg++;
                LOGI("[DC] aim target=0x%lx bone=%d vis=0x%x screenDist=%.1f pos=(%.1f,%.1f,%.1f) origin=(%.1f,%.1f,%.1f) cam=(%.1f,%.1f,%.1f)",
                     (unsigned long)bestAddr, (int)cfg.aimBone, bestVis, bestDist,
                     bestPos.X, bestPos.Y, bestPos.Z,
                     aimOrigin.X, aimOrigin.Y, aimOrigin.Z,
                     camPos.X, camPos.Y, camPos.Z);
            }
        }
    }

private:
    uintptr_t lockedTarget_ = 0;
    std::chrono::steady_clock::time_point lastFireSeen_{};
    bool hasFireSeen_ = false;
};
