/*
 * DeepSeek Coder - game session resolver (root helper).
 *
 * Resolves the current game object from the GameFacade static fields, then
 * follows MatchGame -> Match -> LocalPlayer -> FollowCamera to cache the view
 * matrix. The match pointer is also exposed so the ESP/Aimbot can read the
 * current List<Player> directly.
 */
#pragma once
#include "Player.h"
#include "Transform.h"
#include "Offsets.h"

class GameManager {
    uintptr_t libBase;
    uintptr_t localPlayerAddr;
    uintptr_t baseGameAddr;
    uintptr_t matchAddr;
    D3DMatrix viewMatrix;
    D3DMatrix viewOnlyMatrix;
    bool inMatch;

public:
    GameManager(uintptr_t base)
        : libBase(base), localPlayerAddr(0), baseGameAddr(0), matchAddr(0),
          viewMatrix{}, viewOnlyMatrix{}, inMatch(false) {}

    bool Update() {
        inMatch = false;

        auto fail = [](const char* step, uintptr_t value) {
            static int dbg = 0;
            if (dbg < 12) {
                dbg++;
                LOGI("[DC] chain fail at %s value=0x%lx", step, (unsigned long)value);
            }
            return false;
        };

        uintptr_t typeInfoPtr = Read<uintptr_t>(libBase + Offsets::GameFacadeTypeInfo);
        if (!typeInfoPtr) return fail("typeInfoPtr", typeInfoPtr);

        uintptr_t klass = Read<uintptr_t>(typeInfoPtr);
        if (!klass) return fail("klass", klass);

        uintptr_t staticFields = Read<uintptr_t>(klass + Offsets::Class_StaticFields);
        if (!staticFields) return fail("staticFields", staticFields);

        baseGameAddr = Read<uintptr_t>(staticFields + Offsets::GameFacade_CurrentGame);
        if (!baseGameAddr) return fail("currentGame", baseGameAddr);

        matchAddr = Read<uintptr_t>(baseGameAddr + Offsets::MatchGame_Match);
        if (!matchAddr) return fail("match", matchAddr);

        localPlayerAddr = Read<uintptr_t>(matchAddr + Offsets::Match_LocalPlayer);

        uintptr_t cameraManager = Read<uintptr_t>(baseGameAddr + Offsets::MatchGame_CameraManager);
        if (!cameraManager) return fail("cameraManager", cameraManager);

        uintptr_t camera = Read<uintptr_t>(cameraManager + 0x20);
        if (!camera) return fail("camera", camera);

        uintptr_t nativeCamera = Read<uintptr_t>(camera + Offsets::Object_CachedPtr);
        if (!nativeCamera) return fail("nativeCamera", nativeCamera);

        viewMatrix = Read<D3DMatrix>(nativeCamera + Offsets::Camera_Matrix);
        viewOnlyMatrix = Read<D3DMatrix>(nativeCamera + Offsets::Camera_ViewMatrix);

        static int okDbg = 0;
        if (okDbg < 2) {
            okDbg++;
            LOGI("[DC] update ok cam=0x%lx m11=%.3f m14=%.3f m41=%.3f m44=%.3f",
                 (unsigned long)nativeCamera,
                 viewMatrix._11, viewMatrix._14, viewMatrix._41, viewMatrix._44);
        }

        inMatch = true;
        return true;
    }

    bool IsInMatch() { return inMatch; }

    bool IsMatchRunning() {
        if (!matchAddr) return false;
        return Read<int>(matchAddr + Offsets::Match_State) == 1;
    }

    uintptr_t GetLocalPlayerAddr() { return localPlayerAddr; }
    uintptr_t GetBaseGame() { return baseGameAddr; }
    uintptr_t GetMatchAddr() { return matchAddr; }
    D3DMatrix GetViewMatrix() { return viewMatrix; }    D3DMatrix GetViewOnlyMatrix() { return viewOnlyMatrix; }

    float GetGameTime() {
        if (!baseGameAddr) return 0.0f;
        uintptr_t timer = Read<uintptr_t>(baseGameAddr + Offsets::BaseGame_Timer);
        if (!timer) return 0.0f;
        return Read<float>(timer + 0x10); // TimeService.m_GameTime
    }
};
