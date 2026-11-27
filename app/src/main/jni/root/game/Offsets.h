/*
 * DeepSeek Coder - IL2CPP offsets for the supported game build.
 *
 * Source: UnityHUB dump in TOOL/UnityHUB/Dump (latest libil2cpp.so +
 * global-metadata.dat). Field offsets are read directly from the dump:
 *   - COW.GameFacade.CurrentGame   (static field, block offset 0x0)
 *   - COW.MatchGame.m_Match        (0x90)
 *   - COW.GamePlay.JMAGGLCNGIG     (local player 0xD8, state 0xCC, list 0x158)
 *   - COW.GamePlay.Player          (root 0x58, head node 0x6A8, camera 0x698)
 *
 * Re-check these after every Free Fire Max update.
 */
#pragma once
#include <cstdint>

namespace Offsets {
    // GameFacade static field block. 0xA86D8D8 holds a pointer to the
    // Il2CppClass*, whose static_fields pointer is at +0xB8.
    constexpr uintptr_t GameFacadeTypeInfo     = 0xA86D8D8;
    constexpr uintptr_t Class_StaticFields     = 0xB8;
    constexpr uintptr_t GameFacade_CurrentGame = 0x0;   // BaseGame* (MatchGame)

    // COW.MatchGame
    constexpr uintptr_t MatchGame_Match        = 0x90;  // JMAGGLCNGIG*
    constexpr uintptr_t MatchGame_CameraManager = 0xD8; // CameraControllerManager*
    constexpr uintptr_t MatchGame_ReplicationEntities = 0xC0; // Dictionary<uint, ReplicationEntity>

    // COW.GamePlay.JMAGGLCNGIG (match)
    constexpr uintptr_t Match_LocalPlayer      = 0xD8;  // Player*
    constexpr uintptr_t Match_State            = 0xCC;  // Running = 1
    constexpr uintptr_t Match_PlayerList       = 0x158; // List<Player>
    constexpr uintptr_t Match_PlayerDict1      = 0x128; // Dictionary<BEADLMGGGGL, Player>
    constexpr uintptr_t Match_PlayerDict2      = 0x140; // Dictionary<BEADLMGGGGL, Player>

    // COW.GamePlay.Player
    constexpr uintptr_t Player_RootTransform   = 0x58;  // Entity.m_CachedTransform
    constexpr uintptr_t Player_HeadNode        = 0x6A8; // GetHeadTF()
    constexpr uintptr_t Player_HipNode         = 0x6B0; // GetHipTF()
    constexpr uintptr_t Player_ChestNode       = 0x6B8; // GetChestPos()
    constexpr uintptr_t Player_FollowCamera    = 0x698;
    constexpr uintptr_t Player_Name            = 0x4A0; // OriginalNickName
    constexpr uintptr_t Player_IsBot           = 0x4A8; // IsClientBot
    constexpr uintptr_t Player_AimRotation     = 0x61C; // <AKKPEKIHGAH>k__BackingField (GetAimRotation)
    constexpr uintptr_t Player_CurrentAimRotation = 0x1AA4; // m_CurrentAimRotation
    constexpr uintptr_t Player_VisibleFlags    = 0xAD8; // BitArrayBoolean NBIIBJBALJD
    constexpr uintptr_t Player_PRIDataPool     = 0x70;  // IPRIDataPool
    constexpr uintptr_t Player_AvatarManager   = 0x778;
    constexpr uintptr_t Player_ActiveSighting  = 0x608; // HBIBDMMOOOK* while scoped
    constexpr uintptr_t Player_LastFireBtnDownTime = 0x1854;
    constexpr uintptr_t Player_LastSmartFireTime   = 0xDF8;

    // CameraControllerBase -> UnityEngine.Camera -> native Camera
    constexpr uintptr_t Camera_TargetCamera    = 0x30;
    constexpr uintptr_t Object_CachedPtr       = 0x10;
    constexpr uintptr_t Camera_ViewMatrix      = 0x80;
    constexpr uintptr_t Camera_Matrix          = 0x100;

    // GCommon.BaseGame -> GCommon.TimeService
    constexpr uintptr_t BaseGame_Timer         = 0x20;
    constexpr uintptr_t Timer_Delta            = 0x2C;

    // IL2CPP List<T> layout (64-bit)
    constexpr uintptr_t List_Items             = 0x10;
    constexpr uintptr_t List_Size              = 0x18;
    constexpr uintptr_t Array_Vector           = 0x20;

    // System.Collections.Generic.Dictionary<TKey,TValue> layout.
    // BEADLMGGGGL is 0x18 bytes at runtime, so an Entry is 0x28 bytes and
    // the value starts at entry+0x20 (hash 0x0, next 0x4, key 0x8..0x1F).
    constexpr uintptr_t Dict_Entries           = 0x18;
    constexpr uintptr_t Dict_Count             = 0x20;
    constexpr uintptr_t Dict_Entry_Size        = 0x28;
    constexpr uintptr_t Dict_Entry_Value       = 0x20;

    // Dictionary<uint, ReplicationEntity>: key is 4 bytes, so Entry is
    // 0x18 bytes and the value starts at entry+0x10.
    constexpr uintptr_t RDict_Entry_Size       = 0x18;
    constexpr uintptr_t RDict_Entry_Value      = 0x10;

    // COW.GamePlay.Player visibility bitmask (BitArrayBoolean).
    constexpr uint32_t ISVISIBLE_ALIVE         = 0x20;     // 32
    constexpr uint32_t ISVISIBLE_DynamicPVS    = 0x100000; // 1048576
}
