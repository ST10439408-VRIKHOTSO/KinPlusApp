# KinPlus (Kin+)

An Android app for family/circle safety: real-time location sharing, safe zones with geofencing, SOS alerts, journey monitoring, and community alerts — with offline support and background sync.

## Tech stack

- **UI**: Jetpack Compose, Material 3
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp
- **Local storage**: Room, DataStore, EncryptedSharedPreferences (security-crypto)
- **Background work**: WorkManager (offline sync queue)
- **Firebase**: Auth, Cloud Messaging, Analytics
- **Location**: Play Services Location, Maps Compose, geofencing
- **Auth**: Google Sign-In via Credential Manager

Minimum SDK 26 (Android 8.0), target/compile SDK 34, Kotlin, Java 17.

## Project structure

```
app/src/main/java/za/co/kinplus/app/
├── auth/          # Auth manager
├── data/          # local (Room), remote (Retrofit), repository, sync (WorkManager)
├── di/            # Hilt modules
├── domain/        # Domain models
├── location/      # Location, geofencing, compass providers
├── messaging/      # FCM service
├── ui/            # Compose screens, navigation, theme, shared components
└── util/          # Connectivity, locale, Resource wrapper
```

## Setup

### 1. Prerequisites

- Android Studio (latest stable) with Android SDK 34
- JDK 17

### 2. `local.properties`

Create `local.properties` in the project root (never commit this file) with:

```properties
sdk.dir=/path/to/your/Android/Sdk

KINPLUS_API_BASE_URL=https://your-api-base-url/

# Optional — Google Sign-In won't work without it
KINPLUS_GOOGLE_WEB_CLIENT_ID=your-oauth-web-client-id

# Optional — map tiles won't load without it, but the app still builds
KINPLUS_GOOGLE_MAPS_API_KEY=your-maps-sdk-android-key
```

Get a Maps SDK for Android key from the [Google Cloud Console](https://console.cloud.google.com/google/maps-apis/credentials) (has a free monthly quota).

### 3. Firebase config

Firebase Auth and Cloud Messaging require a real `google-services.json`:

1. Create/open your project in the [Firebase Console](https://console.firebase.google.com/), with an Android app registered under package name `za.co.kinplus.app`.
2. Download `google-services.json` and place it at `app/google-services.json`.

A placeholder is checked in at `app/google-services.json.template` — copy it and replace with your real values, or download the real file directly; either way, `app/google-services.json` itself stays out of version control (see `.gitignore`).

### 4. Build & run

```bash
./gradlew assembleDebug
```

Or open the project in Android Studio and run the `app` configuration.

## Testing

```bash
./gradlew test              # unit tests
./gradlew connectedAndroidTest   # instrumented tests (needs a device/emulator)
```
