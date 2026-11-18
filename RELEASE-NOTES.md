## OuyaLibre 0.8.1 alpha — hardware video and independent IPTV playback

**For OUYA / Android 4.1 ARMv7.** Download `OuyaLibre-0.8.1.apk` and install over your existing version to retain playlists, favorites and history. No VLC app or always-on PC is required.

- Embedded LibVLC with hardware video decoding and direct rendering when compatible; fixes surface handling and repeated layout updates.
- Separate M3U playlists, groups, favorites and local hidden-channel markers; original M3U files remain unchanged. No channel lists are bundled.
- Spanish/English interface, OUYA system keyboard option, volume buttons and controls that auto-hide during video and IPTV playback. Y toggles controls.
- Real-device checks covered local H.264 720p/1080p, MP2 audio, controls and three playback restarts. A short live test recorded 884 displayed frames in 30 seconds with no reported frame loss.

**Experimental:** some streams can start with audio but no image; Discovery worked after opening a fresh session, but intermittent stream/decoder transitions remain under investigation. Bluetooth/Wi-Fi cuts, source failures and search access limits are not claimed fixed. Resolution labels in M3U files may be inaccurate.

`OuyaLibre-0.8.1-source.zip` contains the complete app source, bundled libraries, notices and upstream Android source archive, excluding private signing keys. `LibVLC-3.0.13-ouya-armv7.zip` is the verified native/source kit used by repository builds. Check all downloads against `SHA256SUMS`.

**Español:** reproducción acelerada integrada, listas M3U independientes, interfaz español/inglés, volumen y controles ocultables. Actualiza sin desinstalar. Sigue siendo una versión experimental; no se promete compatibilidad o fluidez para todos los canales.

Apoyo voluntario / Support: [Ko-fi](https://ko-fi.com/enmanuelyepez) · [PayPal](https://paypal.me/EnmanuelFYepez).
