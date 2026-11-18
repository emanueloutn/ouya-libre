$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$expected='a70d8b7ec2007b4b998ec47fab10881505938d16f4fbf9358839b79f559ef561'
$archive=Join-Path $root 'build\LibVLC-3.0.13-ouya-armv7.zip'
New-Item -ItemType Directory -Force (Split-Path $archive -Parent) | Out-Null
if(!(Test-Path $archive)) {
 [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12
 Invoke-WebRequest -UseBasicParsing -Uri 'https://github.com/emanueloutn/ouya-libre/releases/download/v0.8.1-alpha/LibVLC-3.0.13-ouya-armv7.zip' -OutFile $archive
}
if((Get-FileHash $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected){throw 'LibVLC archive checksum mismatch'}
Expand-Archive -LiteralPath $archive -DestinationPath $root -Force
Write-Output 'Verified LibVLC binaries, bindings, notices and upstream Android source archive installed.'
