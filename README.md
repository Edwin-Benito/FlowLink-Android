# FlowLink Android

Aplicación de mensajería enfocada en privacidad con cifrado de extremo a extremo y traducción segura de mensajes. El proyecto está construido con Kotlin y Jetpack Compose, e integra servicios de Firebase para autenticación, almacenamiento y notificaciones.

## Funcionalidades principales
- Registro e inicio de sesión con email, Google y Facebook.
- Chats en tiempo real con indicadores de escritura y confirmaciones de lectura.
- Cifrado de extremo a extremo usando Google Tink.
- Traducción automática de mensajes con Google Cloud Translation API.
- Envío de imágenes, respuestas a mensajes y mensajes destacados.
- Mensajes temporales (desaparición programada).
- Notificaciones push con Firebase Cloud Messaging.
- Perfiles de usuario y búsqueda de contactos.
- Modo de mantenimiento remoto mediante Firebase Remote Config.

## Stack tecnológico
- Kotlin + Jetpack Compose
- AndroidX Navigation Compose
- Firebase:
  - Auth
  - Firestore
  - Realtime Database
  - Storage
  - Messaging
  - Remote Config
  - App Check
  - Crashlytics
- Google Sign-In y Facebook Login
- Google Cloud Translation API
- Google Tink (cifrado)

## Requisitos
- Android Studio (recomendado)
- JDK 11
- Android SDK 29+
- Proyecto de Firebase configurado

## Configuración inicial
1. Clona el repositorio.
2. Crea `local.properties` a partir de `local.properties.example` y completa tus claves:
   - `TRANSLATION_API_KEY`
   - `FACEBOOK_APP_ID`
   - `FACEBOOK_CLIENT_TOKEN`
3. Descarga tu `google-services.json` desde Firebase Console y colócalo en `app/google-services.json`.
4. (Opcional) Configura la app de Facebook con el paquete y los SHA-1 correctos.

Para más detalles sobre secretos y seguridad, revisa `docs/SECURITY_SETUP.txt`.

## Ejecución
Puedes ejecutar la app desde Android Studio o con Gradle:

```bash
./gradlew assembleDebug
```

Para instalar en un dispositivo/emulador:

```bash
./gradlew installDebug
```

## Notas de seguridad
- No subas `google-services.json` ni `local.properties` al repositorio.
- Rota claves expuestas y usa variables de entorno en CI/CD.

## Estado del proyecto
En desarrollo activo.
