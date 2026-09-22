# KinPlus (Kin+)

An Android app for family and circle safety, real time location sharing, Safe Zones with geofencing, SOS alerts, journey monitoring and community alerts. The app also supports offline use and background sync.

## Tech stack

* **UI:** Jetpack Compose, Material 3
* **DI:** Hilt
* **Networking:** Retrofit + OkHttp
* **Local storage:** Room, DataStore, EncryptedSharedPreferences (security crypto)
* **Background work:** WorkManager (offline sync queue)
* **Firebase:** Auth, Cloud Messaging, Analytics
* **Location:** Play Services Location, Maps Compose, geofencing
* **Auth:** Google Sign In using Credential Manager

Minimum SDK 26 (Android 8.0), target and compile SDK 34, Kotlin and Java 17.

## Project structure

```text
app/src/main/java/za/co/kinplus/app/
├── auth/          # Auth manager
├── data/          # local (Room), remote (Retrofit), repository, sync (WorkManager)
├── di/            # Hilt modules
├── domain/        # Domain models
├── location/      # Location, geofencing, compass providers
├── messaging/     # FCM service
├── ui/            # Compose screens, navigation, theme, shared components
└── util/          # Connectivity, locale, Resource wrapper
```

## Setup

### 1. Prerequisites

* Android Studio (latest stable) with Android SDK 34
* JDK 17

### 2. `local.properties`

Create `local.properties` in the project root. Never commit this file. Add the following:

```properties
sdk.dir=/path/to/your/Android/Sdk

KINPLUS_API_BASE_URL=https://your-api-base-url/

# Optional: Google Sign In will not work without this
KINPLUS_GOOGLE_WEB_CLIENT_ID=your-oauth-web-client-id

# Optional: Map tiles will not load without this, but the app will still build
KINPLUS_GOOGLE_MAPS_API_KEY=your-maps-sdk-android-key
```

Get a Maps SDK for Android key from the [Google Cloud Console](https://console.cloud.google.com/google/maps-apis/credentials). It has a free monthly quota.

### 3. Firebase config

Firebase Authentication and Cloud Messaging require a real `google-services.json` file.

1. Create or open your project in the [Firebase Console](https://console.firebase.google.com/) and register an Android app using the package name `za.co.kinplus.app`.
2. Download `google-services.json` and place it at `app/google-services.json`.

A placeholder is included at `app/google-services.json.template`. Copy it and replace the placeholder values with your real values, or download the real file directly.

The file `app/google-services.json` must remain out of version control. This is already configured in `.gitignore`.

### 4. Build and run

```bash
./gradlew assembleDebug
```

Alternatively, open the project in Android Studio and run the `app` configuration.

## Testing

```bash
./gradlew test
```

Runs the unit tests.

```bash
./gradlew connectedAndroidTest
```

Runs the instrumented tests. A device or emulator is required.
