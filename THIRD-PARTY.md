# Componentes

Bouncy Castle 1.86: módulos bcprov-jdk15to18, bctls-jdk15to18 y bcutil-jdk15to18. Procedencia: [Bouncy Castle](https://www.bouncycastle.org/download/bouncy-castle-java/) y Maven Central. La biblioteca combinada se trasladó de `org.bouncycastle` a `org.ouyalibre.crypto` mediante Jar Jar 1.3, para evitar que Android 4.1 cargue las clases criptográficas antiguas de su propio sistema. R8 elimina los componentes que la aplicación no usa. La licencia se incluye en `LICENSE-BOUNCY-CASTLE.txt`.

Certificados Mozilla: descargados de [curl CA Extract](https://curl.se/docs/caextract.html), archivo [cacert.pem](https://curl.se/ca/cacert.pem). El encabezado del archivo conserva su procedencia. Los certificados públicos no contienen claves privadas.

Android JSON, MediaPlayer, VideoView y demás componentes del sistema se utilizan mediante las API de Android y no se redistribuyen como dependencias de la APK.

Jar Jar y el archivo JSON para pruebas JVM solo se utilizaron como herramientas de construcción o de pruebas. No se incluyen como componentes ejecutables de la APK.
