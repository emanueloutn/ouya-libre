param(
 [string]$Sdk = $env:ANDROID_SDK_ROOT,
 [string]$Jdk = $env:JAVA_HOME,
 [string]$BuildTools = '35.0.0',
 [string]$Platform = 'android-35'
)
$ErrorActionPreference = 'Stop'
if (!$Sdk) { $Sdk = $env:ANDROID_HOME }
if (!$Sdk -or !$Jdk) { throw 'Configura ANDROID_SDK_ROOT y JAVA_HOME, o pasa -Sdk y -Jdk.' }
$root = $PSScriptRoot
$build = Join-Path $root 'build'
$classes = Join-Path $build ('classes-' + [Guid]::NewGuid().ToString('N'))
$bt = Join-Path $Sdk "build-tools\$BuildTools"
$jar = Join-Path $Sdk "platforms\$Platform\android.jar"
$env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$env:PATH"
New-Item -ItemType Directory -Force -Path $build,$classes,(Join-Path $root 'assets') | Out-Null
function CheckExit { if ($LASTEXITCODE -ne 0) { throw "Falló herramienta de compilación: $LASTEXITCODE" } }
if (!(Test-Path (Join-Path $root 'assets\cacert.pem'))) { throw 'Falta assets/cacert.pem. Descarga https://curl.se/ca/cacert.pem' }
$sources = Get-ChildItem (Join-Path $root 'src') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }
$tls = Join-Path $root 'libs\public-tls-1.86.jar'
& "$Jdk\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -classpath "$jar;$tls" -d $classes @sources
CheckExit
$classesJar = Join-Path $build 'classes.jar'
& "$Jdk\bin\jar.exe" cf $classesJar -C $classes .
CheckExit
& "$Jdk\bin\java.exe" -Xmx1400m -cp "$bt\lib\d8.jar" com.android.tools.r8.R8 --release --min-api 16 --lib $jar --pg-conf (Join-Path $root 'r8-rules.pro') --output $build $classesJar $tls
CheckExit
$unsigned = Join-Path $build 'unsigned.apk'
& "$bt\aapt.exe" package -f -M (Join-Path $root 'AndroidManifest.xml') -S (Join-Path $root 'res') -A (Join-Path $root 'assets') -I $jar -F $unsigned
CheckExit
Push-Location $build
try { & "$bt\aapt.exe" add $unsigned classes.dex; CheckExit } finally { Pop-Location }
$aligned = Join-Path $build 'aligned.apk'
& "$bt\zipalign.exe" -f 4 $unsigned $aligned
CheckExit
$key = Join-Path $root 'signing\prototype.keystore'
New-Item -ItemType Directory -Force (Join-Path $root 'signing') | Out-Null
if (!(Test-Path $key)) { & "$Jdk\bin\keytool.exe" -genkeypair -keystore $key -storepass android -keypass android -alias ouyalibre -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=OUYA Libre Prototype'; CheckExit }
$apk = Join-Path $build 'OuyaLibre-0.6.1.apk'
& "$Jdk\bin\java.exe" -jar "$bt\lib\apksigner.jar" sign --ks $key --ks-pass pass:android --key-pass pass:android --v1-signing-enabled true --v2-signing-enabled true --out $apk $aligned
CheckExit
& "$Jdk\bin\java.exe" -jar "$bt\lib\apksigner.jar" verify --verbose --min-sdk-version 16 $apk
CheckExit
Write-Output "APK creada: $apk"

