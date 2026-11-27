# Offset map (UnityHUB dump)

Source dump:

```
C:\Users\ADMIN\Downloads\Code\TOOL\UnityHUB\Dump\
  libil2cpp.so
  global-metadata.dat
```

UnityHUB session: AArch64, 43,956 types, 340,202 methods.

## Core chain

| Offset | Dump field | Meaning |
| --- | --- | --- |
| `0xA86D8D8` | `GameFacade` type-info global | holds `Il2CppClass**` |
| `0xB8` | `Il2CppClass.static_fields` | static field block |
| `0x0` | `COW.GameFacade.CurrentGame` | `GCommon.BaseGame*` (actual `COW.MatchGame`) |
| `0x90` | `COW.MatchGame.m_Match` | `COW.GamePlay.JMAGGLCNGIG*` |
| `0xD8` | `COW.MatchGame.m_CameraControllerManager` | `CameraControllerManager*` |
| `0xC0` | `COW.MatchGame.m_ReplicationEntitis` | `Dictionary<uint, ReplicationEntity>` |
| `0xD8` | `JMAGGLCNGIG.MHJCLOPOBAA` | local `Player*` (returned by `IKKLCLKFBHM`) |
| `0xCC` | `JMAGGLCNGIG.LOEPAKMFNJO` | match state enum (`Running = 1`) |
| `0x158` | `JMAGGLCNGIG.LMOIPODCHOL` | `List<Player>` fallback |
| `0x128` / `0x140` | `JMAGGLCNGIG.NOLEDCGOJKC` / `GOFLDILNLBA` | player dictionaries used by minimap |

## Player

| Offset | Dump field | Meaning |
| --- | --- | --- |
| `0x58` | `GCommon.Entity.m_CachedTransform` | root `Transform` |
| `0x4A0` | `Player.OriginalNickName` | display name string |
| `0x4A8` | `Player.IsClientBot` | bot flag |
| `0x698` | `Player.AHNCPOJPPCL` | `FollowCamera` |
| `0x6A8` | `Player.GBKFHDFCPMD` | head `ITransformNode` (`GetHeadTF`) |
| `0x778` | `Player.GGJOLDEEIEN` | `AvatarManager` |
| `0x61C` | `Player.<AKKPEKIHGAH>k__BackingField` | aim quaternion returned by `GetAimRotation()` |

## Camera / time

| Offset | Dump field | Meaning |
| --- | --- | --- |
| `0x30` | `CameraControllerBase.AFMGCAELKBD` | managed `Camera` |
| `0x10` | `UnityEngine.Object.m_CachedPtr` | native camera pointer |
| `0x100` | native camera | view-projection matrix used by WorldToScreen |
| `0x20` | `GCommon.BaseGame.m_SimulationTimer` | `TimeService` |
| `0x2C` | `GCommon.TimeService.m_FixedDeltaTime` | timer delta (speed hack) |

## IL2CPP List

| Offset | Meaning |
| --- | --- |
| `0x10` | `_items` |
| `0x18` | `_size` |
| `0x20` | first element in the backing array |

## Still unmapped

This build moved health/visibility/death into the `IPRIDataPool` replication
system:

- `Player.get_CurHP` / `get_MaxHP` use `Player.m_PRIDataPool` at `0x70`,
  search the property table at `+0xB0`/`+0x12E`, then call a virtual getter.
- Avatar visibility is behind `AvatarManager.IUmaAvatar` at `0x138`.
- The old `AimInfo` block (`0xD20`) is gone; `SilentAim` now uses the same
  `m_CurrentAimRotation` path as the aimbot.
- Head collider swap (`0x658`/`0x78`) no longer matches.

Those features are disabled or relaxed in the current build until the
PRIDataPool reader and avatar visibility chain are implemented.
