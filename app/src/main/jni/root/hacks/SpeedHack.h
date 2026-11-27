/*
 * DeepSeek Coder - speed hack (root helper).
 *
 * Patches the game timer's delta value (0.033 -> 0.059) while enabled and
 * restores the original value when the toggle is off.
 */

#pragma once
#include "../game/GameManager.h"
#include "../game/Offsets.h"
#include "../../common/Protocol.h"

class SpeedHack {
public:
    void Apply(GameManager& game, Config& cfg) {
        if (!game.IsInMatch()) return;

        uintptr_t baseGame = game.GetBaseGame();
        if (!baseGame) return;

        uintptr_t timer = Read<uintptr_t>(baseGame + Offsets::BaseGame_Timer);
        if (!timer) return;

        if (cfg.speedHack) {
            Write<float>(timer + Offsets::Timer_Delta, 0.059f);
        } else {
            Write<float>(timer + Offsets::Timer_Delta, 0.033f);
        }
    }
};
