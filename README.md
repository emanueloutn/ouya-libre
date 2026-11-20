# OuyaLibre 0.8.2 alpha — OUYA / Android 4.1

[Download APK / Descargar APK](https://github.com/emanueloutn/ouya-libre/releases/tag/v0.8.2-alpha) · [Ko-fi](https://ko-fi.com/enmanuelyepez) · [PayPal](https://paypal.me/EnmanuelFYepez)

Free native video player and public-source search app for OUYA, Android 4.1 / API 16 and ARMv7. Runs on the console without an always-on PC. No app advertising, subscriptions, Google Play Services or external VLC installation required.

## Live HLS recovery in 0.8.2

If an IPTV HLS stream ends unexpectedly, OuyaLibre shows a recovery notice and retries after 5 and 10 seconds, for a maximum of two attempts. Leaving the player cancels delayed retries. An unexpected end does not hide the channel or edit the original M3U. Normal movie/VOD playback is not automatically restarted.

This addresses the case where A&E reported an ended stream while its server playlist later continued updating. It does not repair missing server fragments or guarantee continuous playback.

## Black-picture correction in 0.8.2

Version 0.8.1 could render video underneath an uninitialized subtitle surface: audio played while the screen stayed black, sometimes revealing a frame when leaving the player. Version 0.8.2 explicitly clears that surface to transparency when created or resized and starts it at a minimal size. Hardware video decoding stays enabled.

The failure was reproduced on a real OUYA, with decoded/displayed counters advancing under a black picture. Clearing only the subtitle surface exposed the running video without restarting the stream. The corrected app was then checked visually on Discovery and a real 1080p video opened through the app's history/source selector; the user confirmed the picture was visible without pressing Back.

## What's included

- Embedded legacy LibVLC engine with hardware video decoding and direct rendering when supported; Android system player remains an alternative.
- Import M3U files or URLs. Each playlist retains its own channel listing, groups, favorites and hidden-channel markers. No playlists or channels are bundled.
- Original M3U files stay intact. Restore hidden channels under **TV / IPTV → your playlist → Hidden channels**.
- Spanish and English interface, OUYA system keyboard option, favorites, history and resume.
- Controller navigation, multimedia volume, and playback controls that hide after five seconds. Press a controller key to reveal them; Y toggles visibility.
- Public MP4/HLS playback and searches across public catalogs and web sources. Access restrictions, logins, paywalls and CAPTCHA checks are not bypassed.

## Install / Actualizar

Download `OuyaLibre-0.8.2.apk` from Releases and check `SHA256SUMS`. Install it over the previous OuyaLibre version using the same package/signature; do not uninstall first if you want to retain local data. The app appears in **MAKE → Software**.

Descarga el APK de Releases e instálalo encima de la versión anterior. Conserva las listas, favoritos e historial. No incluye listas IPTV: importa tus propios archivos o enlaces en **TV / IPTV**.

## Tested and limitations

Tested on a real OUYA: local H.264 720p/1080p playback, MPEG Layer II audio in a real TS sample, controller/volume behavior, and three consecutive playback restarts. One 30-second live-channel observation recorded 884 displayed frames and no reported frame loss. These are short tests, not a guarantee for every stream or frame rate.

The confirmed subtitle-surface regression from 0.8.1 is corrected as described above. This does not guarantee compatibility with every stream or codec. Playlist resolution labels can be inaccurate: one channel labeled 1080p delivered a 544×480 interlaced sample.

Wi-Fi/Bluetooth interruptions remain under investigation. Server timeouts, missing HLS segments, unsupported codecs and changing search-engine access can still affect operation. Google may require JavaScript or an access check, and enabled search sources do not guarantee usable results. Audio language metadata does not reliably distinguish Latin American Spanish.

See [verification](VERIFICATION.md) and [hardware integration](HARDWARE-OUYA.md).

## Build from source

Windows, JDK 17 or 21, Android SDK platform 35/build-tools 35.0.0:

```powershell
.\build.ps1 -Sdk E:\SDK -Jdk 'C:\Program Files\Android\Android Studio\jbr'
```

The first build downloads the unchanged pinned LibVLC kit from release v0.8.1-alpha if native libraries are absent, verifies its SHA-256, and unpacks the binaries, bindings, license notices and upstream Android source archive. The complete source ZIP attached to the release already contains those files and builds without that download. The APK is written in the parent directory. A new local signing key is generated under `signing/`; it cannot update the maintainer-signed APK. The maintainer's private signing key is not distributed.

Own application code: MIT. Dependencies retain their original licenses; see [THIRD-PARTY.md](THIRD-PARTY.md) and `third-party/`. The separate VLC application's GPL interface is not incorporated.

## Apoyo voluntario / Support

Donations help maintain the project and do not unlock features: [Ko-fi](https://ko-fi.com/enmanuelyepez) · [PayPal](https://paypal.me/EnmanuelFYepez).
