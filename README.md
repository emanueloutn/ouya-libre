# OuyaLibre 0.8.1 alpha — OUYA / Android 4.1

[Download APK / Descargar APK](https://github.com/emanueloutn/ouya-libre/releases/tag/v0.8.1-alpha) · [Ko-fi](https://ko-fi.com/enmanuelyepez) · [PayPal](https://paypal.me/EnmanuelFYepez)

Free native video player and public-source search app for OUYA, Android 4.1 / API 16 and ARMv7. Runs on the console without an always-on PC. No app advertising, subscriptions, Google Play Services or external VLC installation required.

## What's included

- Embedded legacy LibVLC engine with hardware video decoding and direct rendering when supported; Android system player remains an alternative.
- Import M3U files or URLs. Each playlist retains its own channel listing, groups, favorites and hidden-channel markers. No playlists or channels are bundled.
- Original M3U files stay intact. Restore hidden channels under **TV / IPTV → your playlist → Hidden channels**.
- Spanish and English interface, OUYA system keyboard option, favorites, history and resume.
- Controller navigation, multimedia volume, and playback controls that hide after five seconds. Press a controller key to reveal them; Y toggles visibility.
- Public MP4/HLS playback and searches across public catalogs and web sources. Access restrictions, logins, paywalls and CAPTCHA checks are not bypassed.

## Install / Actualizar

Download `OuyaLibre-0.8.1.apk` from Releases and check `SHA256SUMS`. Install it over the previous OuyaLibre version using the same package/signature; do not uninstall first if you want to retain local data. The app appears in **MAKE → Software**.

Descarga el APK de Releases e instálalo encima de la versión anterior. Conserva las listas, favoritos e historial. No incluye listas IPTV: importa tus propios archivos o enlaces en **TV / IPTV**.

## Tested and limitations

Tested on a real OUYA: local H.264 720p/1080p playback, MPEG Layer II audio in a real TS sample, controller/volume behavior, and three consecutive playback restarts. One 30-second live-channel observation recorded 884 displayed frames and no reported frame loss. These are short tests, not a guarantee for every stream or frame rate.

Some channels can start with audio and a black screen. Discovery was reproduced with this symptom, then displayed video in a fresh OuyaLibre session. The cause of every intermittent decoder/stream transition failure is not established. Playlist resolution labels can be inaccurate: one channel labeled 1080p delivered a 544×480 interlaced sample. Reopening may help, but is not a guaranteed fix.

Wi-Fi/Bluetooth interruptions remain under investigation. Server timeouts, missing HLS segments, unsupported codecs and changing search-engine access can still affect operation. Google may require JavaScript or an access check, and enabled search sources do not guarantee usable results. Audio language metadata does not reliably distinguish Latin American Spanish.

See [verification](VERIFICATION.md) and [hardware integration](HARDWARE-OUYA.md).

## Build from source

Windows, JDK 17 or 21, Android SDK platform 35/build-tools 35.0.0:

```powershell
.\build.ps1 -Sdk E:\SDK -Jdk 'C:\Program Files\Android\Android Studio\jbr'
```

The first build downloads the pinned LibVLC kit from this release if native libraries are absent, verifies its SHA-256, and unpacks the binaries, bindings, license notices and upstream Android source archive. The complete source ZIP attached to the release already contains those files and builds without that download. The APK is written in the parent directory. A new local signing key is generated under `signing/`; it cannot update the maintainer-signed APK. The maintainer's private signing key is not distributed.

Own application code: MIT. Dependencies retain their original licenses; see [THIRD-PARTY.md](THIRD-PARTY.md) and `third-party/`. The separate VLC application's GPL interface is not incorporated.

## Apoyo voluntario / Support

Donations help maintain the project and do not unlock features: [Ko-fi](https://ko-fi.com/enmanuelyepez) · [PayPal](https://paypal.me/EnmanuelFYepez).
