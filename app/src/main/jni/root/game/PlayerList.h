/*
 * DeepSeek Coder - player enumeration (root helper).
 *
 * Sources, in order:
 *   1. MatchGame.m_ReplicationEntitis (0xC0) - Dictionary<uint, ReplicationEntity>
 *      Players are ReplicationEntity subclasses, so this is the full live list.
 *   2. JMAGGLCNGIG.NOLEDCGOJKC / GOFLDILNLBA (0x128 / 0x140) - player dictionaries.
 *   3. JMAGGLCNGIG.LMOIPODCHOL (0x158) - List<Player> fallback.
 *
 * Every candidate is validated by reading its IL2CPP class pointer, then 16
 * pointers are read per process_vm_readv call.
 */
#pragma once
#include <algorithm>
#include "Player.h"
#include "../Includes/Memory.h"

inline bool IsPlayerClass(uintptr_t klass) {
    uintptr_t namePtr = Read<uintptr_t>(klass + 0x10);
    if (!namePtr) return false;

    char name[48] = {0};
    if (!ReadArray(namePtr, name, sizeof(name) - 1)) return false;
    return strstr(name, "Player") != nullptr;
}

inline void AddPlayer(uintptr_t* out, int& count, int max, uintptr_t player) {
    if (!player || player < 0x10000000 || count >= max) return;

    uintptr_t klass = Read<uintptr_t>(player);
    if (!klass || klass < 0x10000000) return;
    if (!IsPlayerClass(klass)) return;

    for (int i = 0; i < count; i++) {
        if (out[i] == player) return;
    }
    out[count++] = player;
}

inline void AddDictionary(uintptr_t dict, uintptr_t entrySize, uintptr_t valueOffset,
                          uintptr_t* out, int& count, int max) {
    if (!dict) return;

    uintptr_t entries = Read<uintptr_t>(dict + Offsets::Dict_Entries);
    int size = Read<int>(dict + Offsets::Dict_Count);
    if (!entries || size <= 0 || size > 1000) return;

    uintptr_t vector = entries + Offsets::Array_Vector;

    constexpr int kBatch = 8;
    uintptr_t values[kBatch];
    for (int base = 0; base < size && count < max; base += kBatch) {
        int n = std::min(kBatch, size - base);

        MemRange ranges[kBatch];
        for (int i = 0; i < n; i++) {
            ranges[i].address = vector
                    + (uintptr_t)(base + i) * entrySize
                    + valueOffset;
            ranges[i].buffer = &values[i];
            ranges[i].size = sizeof(uintptr_t);
        }

        if (!ReadRanges(ranges, (size_t)n)) {
            for (int i = 0; i < n; i++) {
                values[i] = Read<uintptr_t>(ranges[i].address);
            }
        }

        for (int i = 0; i < n; i++) {
            AddPlayer(out, count, max, values[i]);
        }
    }
}

inline int ReadAllPlayers(uintptr_t matchGame, uintptr_t match, uintptr_t* out, int max) {
    int count = 0;

    // 1) All replicated entities (players are a subclass).
    AddDictionary(Read<uintptr_t>(matchGame + Offsets::MatchGame_ReplicationEntities),
                  Offsets::RDict_Entry_Size, Offsets::RDict_Entry_Value,
                  out, count, max);

    // 2) Player-only dictionaries used by the minimap.
    AddDictionary(Read<uintptr_t>(match + Offsets::Match_PlayerDict1),
                  Offsets::Dict_Entry_Size, Offsets::Dict_Entry_Value,
                  out, count, max);
    AddDictionary(Read<uintptr_t>(match + Offsets::Match_PlayerDict2),
                  Offsets::Dict_Entry_Size, Offsets::Dict_Entry_Value,
                  out, count, max);

    // 3) List<Player> fallback.
    if (count == 0) {
        uintptr_t list = Read<uintptr_t>(match + Offsets::Match_PlayerList);
        if (list) {
            uintptr_t items = Read<uintptr_t>(list + Offsets::List_Items);
            int size = Read<int>(list + Offsets::List_Size);
            if (items && size > 0 && size <= 200) {
                uintptr_t vector = items + Offsets::Array_Vector;
                int n = std::min(size, max);
                for (int i = 0; i < n; i++) {
                    AddPlayer(out, count, max,
                              Read<uintptr_t>(vector + (uintptr_t)i * sizeof(uintptr_t)));
                }
            }
        }
    }

    return count;
}
template <typename Fn>
inline void ForEachPlayer(uintptr_t values, int count, Fn&& fn) {
    if (!values || count <= 0) return;

    const uintptr_t* pointers = reinterpret_cast<const uintptr_t*>(values);
    for (int i = 0; i < count; i++) {
        if (!fn(Player(pointers[i]))) return;
    }
}
