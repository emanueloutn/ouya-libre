## OuyaLibre 0.9.1 alpha — búsqueda ampliada y archivos públicos de Drive

**OUYA / Android 4.1 / ARMv7.** Descarga **OuyaLibre-0.9.1.apk** e instálala encima de la versión anterior, sin desinstalar, para conservar listas M3U, favoritos e historial. Funciona en la consola sin PC encendida ni VLC externo.

### Novedades

- Abre archivos públicos individuales de Google Drive mediante **Abrir enlace público**. Conserva la clave de recurso del enlace y acepta MP4/M4V/3GP entregados sin sesión. No utiliza cuenta de Google, claves API, servicios de pago ni servidor propio.
- Reconoce enlaces de Drive encontrados en la web. Añade una búsqueda de enlaces indexados mediante Bing, activable/desactivable en **Ajustes → Motores de búsqueda → Google Drive**.
- Incluye las mejoras de búsqueda de 0.9.0: SepiaSearch para descubrir vídeos en distintos servidores PeerTube, consultas alternativas acotadas, hasta 24 candidatos comprobados por fuente/página y validación alternada entre fuentes.
- Corrige la deduplicación que agrupaba vídeos distintos del mismo servidor PeerTube.
- La comprobación HLS prueba alternativas y segmentos actuales en directo, y respeta la URL final del manifiesto para sus enlaces relativos.

### Límites claros

**Drive no admite carpetas en esta versión.** Se rechazan páginas que requieren sesión, permisos, confirmación o cuyo acceso está limitado. Se reproduce el archivo original: no se convierte vídeo ni se garantiza cualquier códec/resolución. La búsqueda depende de enlaces publicados e indexados y del acceso a los buscadores; no es una búsqueda de todo Drive. No se confirmó ningún resultado nuevo de Drive para «Indiana Jones latino».

No se ha integrado un puente SearXNG. Google puede exigir una comprobación de acceso; Brave/DuckDuckGo y otros servicios pueden limitar solicitudes. La disponibilidad de una página no confirma que contenga la película completa ni su doblaje.

### Verificación en una OUYA real

- Drive: **19 comprobaciones** de enlaces, resourcekey, filtrado, acceso público real, rechazo de sesión/confirmación y transferencia por rangos.
- Reproductor: **7 comprobaciones** con un MP4 público de demostración; reproducción en el motor incorporado, avance, imagen real, audio decodificado, salto de posición y conservación de las listas IPTV. Se observaron 195 fotogramas y 768 bloques de audio decodificados durante una prueba breve; no equivale a una prueba auditiva prolongada de Bluetooth.
- Búsqueda: «Big Buck Bunny» produjo 43 enlaces comprobados en dos minutos entre Archive, OK.ru y varios servidores PeerTube. Son enlaces/versiones/extractos del mismo título, no 43 películas distintas. «Dexter latino» no produjo episodios verificables de la serie en español en las fuentes probadas.
- El código del motor de vídeo y PlayerActivity conserva exactamente los hashes de 0.8.2; se mantienen la corrección de imagen negra y los reintentos de HLS en directo.

**English:** adds anonymous individual Google Drive file playback and indexed-link discovery, plus bounded search expansion/global PeerTube discovery from 0.9.0. Drive folders are not supported. No account, paid API or always-on PC is required. Real OUYA checks covered media bytes, access rejection, range requests, native picture/audio decoding and seeking; search coverage remains dependent on public sources and search-engine access. Install over the previous app to retain local data.

**Assets:** APK, complete source/dependency ZIP and **SHA256SUMS**. Own code MIT; dependency licenses retained. Private signing keys, user playlists, account data and test video files are not distributed.

Apoyo voluntario / Support: [Ko-fi](https://ko-fi.com/enmanuelyepez) · [PayPal](https://paypal.me/EnmanuelFYepez).
