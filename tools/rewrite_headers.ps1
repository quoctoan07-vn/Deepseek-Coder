# Rewrites the first comment block of every native file with a descriptive
# DeepSeek Coder header. Run once after importing legacy sources.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$jni = Join-Path $root "app\src\main\jni"

$headers = [ordered]@{
    "root\game\GameManager.h" = @'
/*
 * DeepSeek Coder - game session resolver (root helper).
 *
 * Walks the il2cpp facade chain (GameFacade -> BaseGame -> Match -> LocalPlayer
 * -> Camera) once per frame and caches the view matrix used by ESP and aimbot.
 */
'@
    "root\game\Offsets.h" = @'
/*
 * DeepSeek Coder - IL2CPP offsets for the supported game build.
 *
 * Every value is a field offset inside libil2cpp.so. Re-check these after a
 * Free Fire Max update; the rest of the helper is offset-agnostic.
 */
'@
    "root\game\Player.h" = @'
/*
 * DeepSeek Coder - player object wrapper (root helper).
 *
 * Reads head/foot transforms, health, visibility, death and firing state, the
 * UTF-16 display name, and exposes the head-collider swap used by head lock.
 */
'@
    "root\game\Transform.cpp" = @'
/*
 * DeepSeek Coder - Unity transform math (root helper).
 *
 * Reads the local TRS matrix and walks the parent chain (up to 50 levels) to
 * produce a world-space position for any transform node.
 */
'@
    "root\game\Transform.h" = @'
/*
 * DeepSeek Coder - transform and matrix types (root helper).
 *
 * Shared D3D view-matrix layout plus the TRS node layout returned by the
 * Unity transform hierarchy.
 */
'@
    "root\hacks\Aimbot.h" = @'
/*
 * DeepSeek Coder - aimbot (root helper).
 *
 * Picks the closest valid enemy inside the configured FOV and world distance,
 * then writes a look-rotation quaternion to the local player. Head lock swaps
 * the head collider before the game processes the next shot.
 */
'@
    "root\hacks\ESP.h" = @'
/*
 * DeepSeek Coder - ESP data builder (root helper).
 *
 * Iterates the live player dictionary, filters dead/hidden entries and
 * projects head/foot positions to screen space. Off-screen players can be
 * collapsed into edge trackers instead of being dropped.
 */
'@
    "root\hacks\SilentAim.h" = @'
/*
 * DeepSeek Coder - silent aim (root helper).
 *
 * Keeps a background thread writing the selected target direction into the
 * local aim-info block, so the next shot is redirected without moving the
 * visible camera.
 */
'@
    "root\hacks\SpeedHack.h" = @'
/*
 * DeepSeek Coder - speed hack (root helper).
 *
 * Patches the game timer's delta value (0.033 -> 0.059) while enabled and
 * restores the original value when the toggle is off.
 */
'@
    "root\Includes\Logger.h" = @'
/*
 * DeepSeek Coder - logcat helpers.
 */
'@
    "root\Includes\Memory.h" = @'
/*
 * DeepSeek Coder - process memory primitives (root helper).
 *
 * Opens /proc/<pid>/mem once and exposes typed pread/pwrite helpers used by
 * every game reader in this folder.
 */
'@
    "root\Includes\Process.cpp" = @'
/*
 * DeepSeek Coder - process discovery (root helper).
 *
 * Scans /proc for the game package name and parses /proc/<pid>/maps to find
 * the load base of libil2cpp.so.
 */
'@
    "root\Includes\Process.h" = @'
/*
 * DeepSeek Coder - process discovery declarations (root helper).
 */
'@
    "root\Unity\MathUtils.h" = @'
/*
 * DeepSeek Coder - projection math (root helper).
 *
 * Extracts the camera world position and projects world coordinates to screen
 * space using the in-game view matrix. Screen size is supplied by the app.
 */
'@
    "root\Unity\Quaternion.hpp" = @'
/*
 * DeepSeek Coder - quaternion math (root helper).
 *
 * Self-contained Unity-compatible quaternion type used for look rotation and
 * player rotation writes.
 */
'@
    "root\Unity\Unity.h" = @'
/*
 * DeepSeek Coder - IL2CPP container helpers (root helper).
 *
 * Layouts for monoList, monoDictionary and monoString, plus obscured int/float
 * accessors used by some wrapped game fields.
 */
'@
    "root\Unity\Vector2.hpp" = @'
/*
 * DeepSeek Coder - 2D vector math (root helper).
 */
'@
    "root\Unity\Vector3.hpp" = @'
/*
 * DeepSeek Coder - 3D vector math (root helper).
 *
 * Unity-compatible Vector3 operators, distance and interpolation helpers.
 */
'@
}

foreach ($relative in $headers.Keys) {
    $path = Join-Path $jni $relative
    if (-not (Test-Path $path)) {
        Write-Warning "Missing: $relative"
        continue
    }

    $content = [System.IO.File]::ReadAllText($path)
    $header = $headers[$relative].Trim() + "`n`n"
    $updated = [System.Text.RegularExpressions.Regex]::Replace(
        $content,
        '(?s)\A\s*/\*.*?\*/\s*',
        $header,
        1)

    if ($updated -eq $content) {
        $updated = $header + $content
    }

    [System.IO.File]::WriteAllText($path, $updated, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "Updated $relative"
}
