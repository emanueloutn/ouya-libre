## OuyaLibre 0.8.2 alpha — black-video correction and IPTV recovery

Download **OuyaLibre-0.8.2.apk** and install over the existing app without uninstalling to retain playlists, favorites and history. OUYA / Android 4.1 ARMv7; no external VLC app or always-on PC required.

**Fixes the confirmed visual regression in 0.8.1:** an uninitialized subtitle surface could cover the video while audio continued, briefly revealing the picture on exit. The subtitle surface is now explicitly cleared to transparency at creation and resize and starts at a minimal size. Hardware decoding remains enabled; no external VLC application is required.

Verified visually on a real OUYA with Discovery and a real 1080p video opened through the app's history/source selector. The user confirmed the picture was visible without pressing Back. Decoded-frame counters alone were not used as proof of visible playback.

An A&E observation also ended after a missing HLS fragment (HTTP 404), although the server's live playlist later kept updating. Previously the player stayed at “The stream ended.”

This update retries unexpectedly ended **live IPTV HLS** sessions after 5 and 10 seconds, with a maximum of two automatic retries. A notice remains visible. Leaving the player cancels queued retries; channel hiding and original M3U files are unchanged. Movie/VOD playback is unaffected.

Real encoded TS media served locally was used to verify reconnection, the two-attempt limit, visible recovery controls and cancellation on exit. No simulated search results or bundled channel lists were added.

**Still experimental:** this does not guarantee compatibility with every channel or fix Bluetooth/Wi-Fi interruptions, missing server fragments or all decoder faults. Hardware playback, Spanish/English UI, volume and auto-hiding controls from 0.8.1 are retained.

**Español:** corrige la capa de subtítulos que tapaba la imagen en vídeos e IPTV de 0.8.1. La imagen se comprobó visualmente y el usuario confirmó la corrección. También añade hasta dos reintentos si termina una señal IPTV HLS; al salir se cancelan. Actualiza sin desinstalar para conservar tus datos.

Complete source and bundled dependencies: **OuyaLibre-0.8.2-source.zip**. Verify downloads using **SHA256SUMS**. Repository builds use the unchanged verified LibVLC kit from v0.8.1-alpha. Own code: MIT; dependencies retain their licenses.

Apoyo voluntario / Support: [Ko-fi](https://ko-fi.com/enmanuelyepez) · [PayPal](https://paypal.me/EnmanuelFYepez).
