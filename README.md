# CHATENDER AI — Android Core

Native Kotlin starter for Samsung Galaxy M14 / Android 15.

## Included
- Dark, compact Android interface.
- Tap-to-talk speech recognition with runtime microphone permission.
- Text-to-speech replies.
- Local commands: help, battery status, time, and opening Wi-Fi, Bluetooth, and device settings.
- Local private memory for the most recent command, with clear controls.
- OpenRouter chat connection using HTTPS.
- API key encrypted with an AES-GCM key stored in Android Keystore.
- Configurable OpenRouter model (default: `openrouter/free`).
- Unit tests and GitHub Actions debug APK build.

## Connect the AI provider
1. Install the debug APK on your Android device.
2. Open **AI Provider Settings**.
3. Create an OpenRouter API key in your own OpenRouter account and paste it into the key field.
4. Keep the model as `openrouter/free` or enter a model supported by your OpenRouter account.
5. Save. Then send a text message or speak a question.

The key is encrypted on-device. It is not hardcoded in the source, printed to logs, or uploaded to this repository. Network requests go to OpenRouter over HTTPS. Provider usage, rate limits, and any charges are controlled by OpenRouter and the selected model.

## Deliberately deferred
Always-on wake phrase, background microphone, and broad phone control need separate lifecycle, privacy, battery, and security testing. This build uses explicit tap-to-talk. Protected system actions remain controlled by Android.


## Multi-role AI and credential storage
- Independent coding, memory, diagnostics, and general model profiles are defined in AiRoleRouter.
- SecureProviderKeyStore stores separate provider credentials using AES-GCM and an Android Keystore master key.
- The current live chat transport remains OpenRouter. Provider profiles are configuration scaffolding until each direct provider adapter is implemented and verified.
- Voice ID configured for the planned voice adapter: e13fa398a7f445a685316a3de6089ce7. The current build still uses Android system text-to-speech.

## Build
Open in Android Studio with JDK 17, sync Gradle, then run on device.

`gradle testDebugUnitTest` and `gradle assembleDebug`.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Application ID: `com.chatender.ai`, version 1.0.0.
