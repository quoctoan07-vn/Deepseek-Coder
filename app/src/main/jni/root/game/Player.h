/*
 * DeepSeek Coder - player object wrapper (root helper).
 *
 * Reads the current 0x4A0 name, 0x4A8 bot flag and 0x778 avatar manager in one
 * batch. Head world position comes from the 0x6A8 transform node; the root
 * transform at 0x58 is used for the foot/ground position.
 *
 * Health, visibility and death state in this build are stored in the new
 * PRIDataPool replication system, so those checks are intentionally relaxed
 * until the pool reader is added.
 */
#pragma once
#include "../Includes/Memory.h"
#include "../Unity/Vector3.hpp"
#include "../Unity/Quaternion.hpp"
#include "Transform.h"
#include "Offsets.h"
#include <cstring>
#include <cmath>

class Player {
    struct Fields {
        uintptr_t name = 0;
        uintptr_t avatarManager = 0;
        uint8_t isBot = 0;
        int8_t botResolved = -1;
    };

    uintptr_t addr;
    Fields fields;
    bool fieldsLoaded = false;

public:
    Player(uintptr_t address) : addr(address) {}

    uintptr_t GetAddress() const { return addr; }

    Vector3 GetHeadPosition() {
        uintptr_t node = Read<uintptr_t>(addr + Offsets::Player_HeadNode);
        Vector3 pos = GetNodePosition(node);
        if (pos == Vector3::Zero()) {
            Vector3 root = GetFootPosition();
            if (root != Vector3::Zero()) {
                pos = root + Vector3(0.0f, 1.7f, 0.0f);
            }
        }
        return pos;
    }

    Vector3 GetFootPosition() {
        uintptr_t transform = Read<uintptr_t>(addr + Offsets::Player_RootTransform);
        if (!transform) return Vector3::Zero();
        return GetPosition(transform);
    }

    Vector3 GetChestPosition() {
        Vector3 head = GetHeadPosition();
        uintptr_t node = Read<uintptr_t>(addr + Offsets::Player_ChestNode);
        Vector3 chest = GetNodePosition(node);
        if (chest != Vector3::Zero()) return chest;
        if (head != Vector3::Zero()) return head + Vector3(0.0f, -0.50f, 0.0f);
        return Vector3::Zero();
    }

    Vector3 GetHipPosition() {
        Vector3 head = GetHeadPosition();
        uintptr_t node = Read<uintptr_t>(addr + Offsets::Player_HipNode);
        Vector3 hip = GetNodePosition(node);
        if (hip != Vector3::Zero()) return hip;
        if (head != Vector3::Zero()) return head + Vector3(0.0f, -0.95f, 0.0f);
        return Vector3::Zero();
    }

    Vector3 GetNeckPosition() {
        Vector3 head = GetHeadPosition();
        Vector3 chest = GetChestPosition();
        if (head == Vector3::Zero()) return chest;
        if (chest == Vector3::Zero()) return head + Vector3(0.0f, -0.22f, 0.0f);
        return chest + (head - chest) * 0.62f;
    }

    Vector3 GetAimPosition(int bone) {
        Vector3 head = GetHeadPosition();
        if (head == Vector3::Zero()) return GetChestPosition();

        if (bone == 2) return head; // Head

        if (bone == 1) { // Neck
            return head + Vector3(0.0f, -0.22f, 0.0f);
        }

        // Chest: use the real chest node only when it is a plausible distance
        // below the head; otherwise use a stable offset.
        Vector3 chest = GetChestPosition();
        if (chest != Vector3::Zero()) {
            float drop = head.Y - chest.Y;
            if (drop > 0.15f && drop < 1.00f) return chest;
        }
        return head + Vector3(0.0f, -0.55f, 0.0f);
    }

    void SetRotation(Quaternion rot) {
        Write<Quaternion>(addr + Offsets::Player_AimRotation, rot);
        Write<Quaternion>(addr + Offsets::Player_CurrentAimRotation, rot);
    }

    int GetHealth() {
        // Current HP lives behind PRIDataPool in this build.
        return 0;
    }

    bool IsVisible() {
        return F().avatarManager != 0;
    }

    bool IsDead() {
        return false;
    }

    bool IsDying() {
        return false;
    }

    bool ShouldSkip() {
        return F().avatarManager == 0;
    }

    bool IsValidEnemy(uintptr_t self) {
        if (!addr || addr == self) return false;
        if (ShouldSkip()) return false;
        return true;
    }

    bool IsESPTarget(uintptr_t self) {
        if (!addr || addr == self) return false;
        if (ShouldSkip()) return false;
        return true;
    }

    bool IsBot() {
        F();
        if (fields.botResolved >= 0) return fields.botResolved != 0;

        bool bot = fields.isBot != 0;
        if (!bot) {
            uintptr_t namePtr = fields.name;
            if (namePtr < 0x10000000) {
                bot = true;
            } else {
                int len = Read<int>(namePtr + 0x10);
                if (len <= 0) bot = true;
            }
        }

        fields.botResolved = bot ? 1 : 0;
        return bot;
    }

    void GetName(char* dst) {
        memset(dst, 0, 64);
        if (IsBot()) {
            strcpy(dst, "Bot");
            return;
        }
        uintptr_t namePtr = F().name;
        if (namePtr > 0x10000000) getUTF8(dst, namePtr);
        if (dst[0] == '\0') strcpy(dst, "Enemy");
    }

    bool IsSighting() {
        uintptr_t entry = GetPRIEntry(12); // EPRI_PLAYER_SIGHTING_ID
        return entry && Read<uint32_t>(entry + 0x18) != 0;
    }

    uint32_t GetPRISightingValue() {
        uintptr_t entry = GetPRIEntry(12);
        return entry ? Read<uint32_t>(entry + 0x18) : 0;
    }

    uint8_t GetPRIFiringValue() {
        uintptr_t entry = GetPRIEntry(21); // EPRI_PLAYER_FIRING_STATUS
        return entry ? Read<uint8_t>(entry + 0x18) : 0;
    }

    uint32_t GetVisibleFlags() {
        uintptr_t vis = Read<uintptr_t>(addr + Offsets::Player_VisibleFlags);
        if (!vis) return 0;
        return Read<uint32_t>(vis + 0x10); // BitArray.m_Value
    }

    bool IsVisibleByFlag(uint32_t flag) {
        return (GetVisibleFlags() & flag) != 0;
    }

    uintptr_t GetSightingWeapon() {
        return Read<uintptr_t>(addr + Offsets::Player_ActiveSighting);
    }

    float GetRawLastFireBtnDownTime() {
        return Read<float>(addr + Offsets::Player_LastFireBtnDownTime);
    }

    float GetRawLastSmartFireTime() {
        return Read<float>(addr + Offsets::Player_LastSmartFireTime);
    }

    bool IsFiring(float gameTime) {
        (void)gameTime;
        return GetPRIFiringValue() != 0;
    }

    void ApplyHeadCollider() {
        // Head collider layout changed in this build; no-op for now.
    }

private:
    uintptr_t GetPRIEntry(uint32_t varID) {
        uintptr_t pool = Read<uintptr_t>(addr + Offsets::Player_PRIDataPool);
        if (!pool) return 0;

        uintptr_t datas = Read<uintptr_t>(pool + 0x10); // ReplicationDataPool.m_Datas
        if (!datas) return 0;

        int length = Read<int>(datas + 0x18); // IL2CPP array length
        if (length <= 0 || (int)varID >= length) return 0;

        return Read<uintptr_t>(datas + 0x20 + ((uintptr_t)varID << 3));
    }

    Fields& F() {
        if (!fieldsLoaded) {
            fieldsLoaded = true;
            if (addr) {
                MemRange ranges[] = {
                    {addr + Offsets::Player_Name,          &fields.name,          sizeof(fields.name)},
                    {addr + Offsets::Player_AvatarManager, &fields.avatarManager, sizeof(fields.avatarManager)},
                    {addr + Offsets::Player_IsBot,         &fields.isBot,         sizeof(fields.isBot)},
                };
                ReadRanges(ranges, sizeof(ranges) / sizeof(ranges[0]));
            }
        }
        return fields;
    }

    static void getUTF8(char* dst, uintptr_t address) {
        if (address < 0x10000000) { dst[0] = '\0'; return; }

        int stringLen = Read<int>(address + 0x10);
        if (stringLen <= 0 || stringLen > 64) stringLen = 32;

        int bytes = stringLen * 2;
        if (bytes > 128) bytes = 128;

        unsigned char raw[128];
        if (!ReadArray(address + 0x14, raw, (size_t)bytes)) {
            dst[0] = '\0';
            return;
        }

        int j = 0;
        for (int i = 0; i + 1 < bytes; i += 2) {
            unsigned short unicode = (unsigned short)(raw[i] | (raw[i + 1] << 8));
            if (unicode == 0) break;
            if (unicode < 0x80) {
                dst[j++] = (char)unicode;
            } else if (unicode < 0x800) {
                dst[j++] = (char)((unicode >> 6) | 0xC0);
                dst[j++] = (char)((unicode & 0x3F) | 0x80);
            } else {
                dst[j++] = (char)((unicode >> 12) | 0xE0);
                dst[j++] = (char)(((unicode >> 6) & 0x3F) | 0x80);
                dst[j++] = (char)((unicode & 0x3F) | 0x80);
            }
            if (j >= 60) break;
        }
        dst[j] = '\0';
    }
};
