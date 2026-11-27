$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
if (-not (Test-Path $sdk)) {
    throw "Android SDK not found. Set ANDROID_HOME or edit local.properties."
}

$ndkRoot = Join-Path $sdk "ndk"
if (-not (Test-Path $ndkRoot)) {
    throw "Android NDK not found under $ndkRoot"
}

$ndk = Get-ChildItem $ndkRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1
if (-not $ndk) {
    throw "No NDK version installed under $ndkRoot"
}

$ndkBuild = Join-Path $ndk.FullName "ndk-build.cmd"
if (-not (Test-Path $ndkBuild)) {
    throw "ndk-build.cmd not found in $($ndk.FullName)"
}

$jniProject = Join-Path $root "app\src\main"
$out = Join-Path $root "build\native"
$obj = Join-Path $out "obj"
$libs = Join-Path $out "libs"
New-Item -ItemType Directory -Force -Path $obj, $libs | Out-Null

Write-Host "Using NDK: $($ndk.Name)"
& $ndkBuild -C $jniProject NDK_PROJECT_PATH=. NDK_OUT=$obj NDK_LIBS_OUT=$libs
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

foreach ($abi in @("arm64-v8a", "x86_64")) {
    $srcApp = Join-Path $libs "$abi\libdccore.so"
    $srcRoot = Join-Path $libs "$abi\dcroot"
    if (-not (Test-Path $srcApp)) { throw "Missing $srcApp" }
    if (-not (Test-Path $srcRoot)) { throw "Missing $srcRoot" }

    $dstDir = Join-Path $root "app\src\main\jniLibs\$abi"
    New-Item -ItemType Directory -Force -Path $dstDir | Out-Null
    Copy-Item $srcApp (Join-Path $dstDir "libdccore.so") -Force
    Copy-Item $srcRoot (Join-Path $dstDir "libdcroot.so") -Force
    Write-Host "Packaged native libs for $abi"
}
