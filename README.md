# DeepSeek Coder

Offline, single-APK Free Fire Max toolkit. One app, embedded root helper, no
PHP panel, no license key and no external server.

## Features

- **Enable aim** master toggle with a screen-space FOV ring.
- **Aim when scoped** and **Aim when firing** triggers. Enable one or both
  (either condition triggers aim).
- **Aim bone selector**: Chest (default), Neck, Head.
- Target selection picks the on-screen enemy closest to the crosshair, with a
  small hysteresis so it does not flicker between two targets.
- **ESP**: player line, box, name, distance, off-screen tracker and HUD info.
- **Speed hack** (risk).
- **Reset guest account** through the game's own `localConfig.json` /
  `GarenaMSDKMgr.ResetGuest()` path.
- **Reset session** resets all toggles, stops the helper and reconnects it.
- All toggles default to OFF.

## Requirements

- Rooted Android device or rooted emulator (MuMu / LDPlayer).
- Android 7.0+ (minSdk 24), arm64-v8a or x86_64.
- Free Fire Max (`com.dts.freefiremax`).
- Overlay permission.

## Build

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleRelease
```

`buildNative` runs `build-native.ps1`, which locates the installed NDK, builds
`libdccore.so` and the `dcroot` helper, and copies them into
`app/src/main/jniLibs/` (generated, not committed).

APK output:

```
app/build/outputs/apk/release/app-release.apk
```

Install:

```powershell
adb install -r app/build/outputs/apk/release/app-release.apk
```

The local `deepseek.keystore` and `local.properties` are intentionally excluded
from git. Add your own keystore or use a debug build if you do not have the
original signing key.

## Architecture

```
MainActivity
  └─ OverlayService (foreground)
       ├─ ESPView                  transparent Canvas overlay
       ├─ FloatingMenu             DeepSeek-style floating panel
       └─ libdccore.so  ── TCP 127.0.0.1 ──  libdcroot.so (root)
                                                └─ /proc/<pid>/mem
                                                     └─ Free Fire Max
```

1. `MainActivity` checks root and overlay permission.
2. `OverlayService` copies `libdcroot.so` to `/data/local/tmp/dc_root`.
3. The helper is launched with a random loopback port and per-session token.
4. The app sends the full config with every frame and receives one `Response`
   containing up to 100 projected players.
5. `ESPView` draws the overlay; the root helper reads/writes game memory.

If Free Fire is not running, the helper keeps scanning `/proc`. If the game
restarts, the helper re-attaches automatically.

## Memory access

The helper prefers `process_vm_readv` / `process_vm_writev` and falls back to
`pread64` / `pwrite64` on `/proc/<pid>/mem` when the kernel, seccomp or SELinux
blocks the syscall. Scattered player reads are batched into one syscall.

## Native layout

| File | Role |
| --- | --- |
| `common/Protocol.h` | fixed-size wire format shared by app and helper |
| `client/NativeBridge.cpp` | JNI bridge, config, status, draw call |
| `client/UiStrings.h` | all menu/button strings (native) |
| `client/SocketClient.h` | loopback TCP client + token handshake |
| `client/ESPRenderer.h` | Canvas renderer with DeepSeek palette |
| `root/main.cpp` | helper entry point and frame loop |
| `root/SocketServer.h` | loopback TCP server |
| `root/game/Offsets.h` | IL2CPP offsets for the supported build |
| `root/game/Player.h` | player wrapper, bones, visibility flags, aim rotation |
| `root/hacks/Aimbot.h` | target selection, FOV, bone, wall visibility |
| `root/hacks/ESP.h` | ESP data builder |
| `root/hacks/SpeedHack.h` | movement speed |

## Notes

- Offsets are mapped from the latest UnityHUB dump (AArch64, 2026-11). See
  `OFFSETS.md` for the field table. Re-check them after every game update.
- Player enumeration reads `MatchGame.m_ReplicationEntitis` (`0xC0`) plus the
  player dictionaries (`0x128` / `0x140`), then filters objects whose IL2CPP
  class name contains `Player`.
- The helper is an executable renamed to `libdcroot.so` so Android extracts it
  with the APK; it is copied to `/data/local/tmp` before execution.
- The DeepSeek logo is MIT-licensed; see `NOTICE`. DeepSeek is a trademark of
  DeepSeek and this project is not affiliated with them.
