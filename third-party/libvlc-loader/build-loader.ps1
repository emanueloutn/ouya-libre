param([Parameter(Mandatory=$true)][string]$Zig)
$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$env:ZIG_GLOBAL_CACHE_DIR=Join-Path $root 'build\zig-global-cache'
$env:ZIG_LOCAL_CACHE_DIR=Join-Path $root 'build\zig-local-cache'
& $Zig cc -target arm-linux-android -mcpu=cortex_a9 -mfloat-abi=softfp -fPIC -shared -nostdlib -fno-stack-protector -fno-builtin '-Wl,--hash-style=sysv' '-Wl,-soname,libjniloader.so' (Join-Path $PSScriptRoot 'loader.c') (Join-Path $root 'native\lib\armeabi-v7a\libvlcjni.so') -o (Join-Path $root 'native\lib\armeabi-v7a\libjniloader.so')
if($LASTEXITCODE -ne 0){throw 'JNI bootstrap build failed'}
