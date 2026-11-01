# CHATENDER AI — Android Core

Native Kotlin starter for Samsung Galaxy M14 / Android 15.

## Included
- Dark, compact Android interface.
- Tap-to-talk speech recognition with runtime microphone permission.
- Text-to-speech replies.
- Local commands: help, battery status, time, and opening Wi-Fi settings.
- Unit tests and GitHub Actions debug APK build.

## Deliberately deferred
Always-on wake phrase, cloud AI, and background microphone. These need separate lifecycle, privacy, battery, and security testing. This first build uses explicit tap-to-talk. Protected system actions remain controlled by Android.

## Build
Open in Android Studio with JDK 17, sync Gradle, then run on device.
`gradle testDebugUnitTest` and `gradle assembleDebug`.
APK: `app/build/outputs/apk/debug/app-debug.apk`.

Application ID: `com.chatender.ai`, version 1.0.0.
