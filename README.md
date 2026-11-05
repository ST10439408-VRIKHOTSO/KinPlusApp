<div align="center">

# Kin+

### Stay Connected. Stay Safe.

A South African family and community safety app for Android.

**OPSC6312 Open Source Coding (Intermediate) · Portfolio of Evidence**
Rosebank College (IIE) · Pretoria Campus
Vukosi Rikhotso (ST10439408) & Theo Golele (ST10439863)

[![Build & test](https://github.com/ST10439408-VRIKHOTSO/KinPlusApp/actions/workflows/build.yml/badge.svg)](https://github.com/ST10439408-VRIKHOTSO/KinPlusApp/actions/workflows/build.yml)

</div>

---

## 1. Purpose

Kin+ helps people stay connected with the people they trust and get help quickly in an emergency. Our Part 1 research found that Namola, Life360 and Google Family Link each solve part of the problem, but none combines location circles, a deliberate emergency SOS, geofenced Safe Zones, safe journey monitoring, community safety reporting, offline resilience and a multilingual South African interface in one app. Kin+ does.

## 2. Download

Get the installable APK from the [GitHub Releases page](https://github.com/ST10439408-VRIKHOTSO/KinPlusApp/releases).

> The PoE releases (`v1.2-poe` onwards) have the signed release APK attached. Download it on an Android phone running Android 8 or newer and open it to install. You may need to allow installs from your browser first. The final PoE commit is tagged `final-poe`.

## 3. Features

| #  | Feature                                       | Requirement | Where in the code                                         |
| -- | --------------------------------------------- | ----------- | --------------------------------------------------------- |
| 1  | Register and log in (Firebase Auth)           | FR 01 to 03 | `auth/AuthManager.kt`, `ui/screens/auth`                  |
| 2  | Single sign on with Google                    | FR 05       | `AuthManager.signInWithGoogle()` (Credential Manager)     |
| 3  | Settings: notifications, language, units, sharing | FR 32   | `ui/screens/settings`, `data/local/SettingsStore.kt`, `messaging/PushRegistration.kt` |
| 4  | REST API integration                          | n/a         | `data/remote` (Retrofit) and `data/repository`            |
| 5  | Offline mode with sync                        | FR 28, 29   | `data/local` (Room), `data/sync` (WorkManager)            |
| 6  | Real time notifications                       | FR 30       | `messaging/KinPlusMessagingService.kt` (FCM)              |
| 7  | English, isiZulu and Sepedi                   | FR 31       | `res/values`, `values-zu`, `values-nso`, `LocaleManager`  |
| 8  | **Kin+ Circles**                              | FR 09 to 16 | `ui/screens/circles`                                      |
| 9  | **Emergency SOS**                             | FR 17 to 19 | `ui/screens/sos`                                          |
| 10 | **Safe Zones** with geofencing                | FR 20 to 22 | `ui/screens/zones`, `location/GeofenceManager.kt`         |
| 11 | **Safe Journey**                              | FR 23, 24   | `ui/screens/journey`                                      |
| 12 | **Community Alerts** with photos in Azure Blob Storage | FR 25 to 27 | `ui/screens/alerts`, `data/repository/ReportPhotoRepository.kt` |
| 13 | **Privacy Control Centre**                    | FR 15, 16   | Safety hub, Profile and Settings                          |

### Added in Version 1.1

| Feature                                   | Part of          | Where in the code                                          |
| ----------------------------------------- | ---------------- | ---------------------------------------------------------- |
| Fake incoming call                        | Emergency SOS    | `ui/components/FakeCall.kt`                                |
| Siren and flashlight strobe               | Emergency SOS    | `ui/components/SirenStrobe.kt`                             |
| "I'm OK" check in timer                   | Safe Journey     | `data/checkin/*`, `ui/screens/journey/CheckInCard.kt`      |
| Distance filter and report details        | Community Alerts | `ui/screens/alerts/CommunityAlertsFragment.kt`             |
| Nearby police, hospitals and clinics      | Safety hub       | `ui/screens/safety/SafetyFragment.kt`, `NearbySearch.kt`   |

### Added in Version 1.3 (final PoE)

| Feature                                   | Part of          | Where in the code                                          |
| ----------------------------------------- | ---------------- | ---------------------------------------------------------- |
| Photos on community reports, stored in Azure Blob Storage | Community Alerts | `data/repository/ReportPhotoRepository.kt`, `AlertRepository.kt` |
| Editing a saved Safe Zone                 | Safe Zones       | `ui/screens/zones/SafeZoneEditorViewModel.kt`              |
| Units (km and m, or mi and ft)            | Settings         | `ui/common/DistanceFormat.kt`                              |
| Notifications switch that registers or removes the push token | Settings | `messaging/PushRegistration.kt`                  |
| Android 16 (API 36) target and edge to edge layout | Whole app | `app/build.gradle.kts`, `MainActivity.kt`                 |

Full details are in [Release Notes](Release%20Notes.md).

## 4. Screenshots

| Sign in | Map | Circles | Safe Zones |
| ------- | --- | ------- | ---------- |
| <img src="docs/screenshots/01%20Login.png" width="200"> | <img src="docs/screenshots/03%20Map.png" width="200"> | <img src="docs/screenshots/04%20Circles.png" width="200"> | <img src="docs/screenshots/10%20Safe%20Zones.png" width="200"> |

| Emergency SOS | Fake call | Safety hub | isiZulu |
| ------------- | --------- | ---------- | ------- |
| <img src="docs/screenshots/06%20Emergency%20SOS.png" width="200"> | <img src="docs/screenshots/09%20Fake%20Call%20Incoming.png" width="200"> | <img src="docs/screenshots/11%20Safety%20Hub.png" width="200"> | <img src="docs/screenshots/14%20Settings%20isiZulu.png" width="200"> |

More screenshots are in [docs/screenshots](docs/screenshots).

## 5. Design considerations

* **Calm, not alarming.** A safety app should feel steady and reassuring. Kin+ uses a warm stone background, white cards with fine borders and deep evergreen as the single brand colour.
* **Red means emergency.** Red is used only for SOS, danger and destructive actions, so it always stands out when it matters.
* **One handed use under stress.** Buttons are at least 48 dp tall, main actions are 56 dp, and the SOS button is large and needs a 3 second hold so it is never pressed by accident.
* **Readable everywhere.** The Plus Jakarta Sans font stays clear at small sizes, and secondary text is dark enough to read on both the background and the cards.
* **Consistent.** Every colour, font and shape is defined once in the XML resources (`res/values/colors.xml`, `themes.xml` and `styles.xml`), and shared pieces such as the top bar, list rows and cards are reusable layouts, so all screens look and behave the same.
* **Edge to edge.** The app draws behind the status and navigation bars, as Android 15 and later require, and `MainActivity` pads the content so nothing sits under the bars or the keyboard.
* **Handles bad input.** Every form is checked before anything is sent, and every error is shown as a plain message in the user's language instead of a crash or a technical message.
* **Only the data we need (POPIA).** Location is used for circles, Safe Zones and SOS. Emergency information and contacts stay on the device. Users can pause sharing at any time, and deleting the account removes their data from the server and from the phone.

## 6. Architecture

```text
UI (XML layouts + Fragments) -> ViewModels (MVVM) -> Repositories -> Retrofit API client / Room cache
                                                                  -> Firebase Auth, FCM, WorkManager
                                                                  -> Azure Blob Storage (report photos)
```

* **Offline first:** reads come from Room, so cached data shows without a connection. Writes that must not be lost (location, SOS, zone crossings, reports and their photos) are queued and sent by WorkManager when the connection returns, with a client action ID so the server ignores duplicates.
* **Authentication and password encryption:** Firebase Authentication handles email and password sign in and Google single sign on. The password goes only to Firebase, over HTTPS, and Firebase stores only a salted scrypt hash of it. Kin+ never stores the password on the phone, writes it to the log or sends it to our API. The API checks the Firebase ID token on every request.
* **REST API:** our own ASP.NET Core Web API (.NET 8), hosted on Azure App Service, with MongoDB Atlas (a NoSQL database) for users, circles, locations, zones, journeys, SOS events and reports.
* **Blob storage:** a report photo is resized on the phone, the API issues a write only SAS link that expires after 15 minutes (`POST api/v1/alerts/upload-url`), and the phone uploads the photo straight to Azure Blob Storage. Only the photo's URL is saved with the report in MongoDB.

```text
app/src/main/java/za/co/kinplus/app/
├── auth/          Firebase email and password, Google SSO
├── data/          local (Room, DataStore), remote (Retrofit), repository, sync and checkin (WorkManager)
├── di/            Hilt modules
├── domain/        Domain models
├── location/      Location, geofencing, compass
├── messaging/     Firebase Cloud Messaging and the notifications switch
├── ui/            Fragments (one per screen), ViewModels, shared view helpers
└── util/          Connectivity, locale, Resource wrapper
```

**Tech stack:** XML layouts with ViewBinding, Fragments and the Navigation component, Material Components (Material 3), RecyclerView, Hilt, Retrofit and OkHttp, Room, DataStore, WorkManager, Firebase (Auth, Cloud Messaging, Analytics), Play Services Location and Maps, Azure Blob Storage. Android Gradle plugin 8.13.2, minimum SDK 26, compile and target SDK 36, Kotlin and Java 17.

**Logging:** every repository, worker and main ViewModel logs what it is doing with `android.util.Log` (for example `SosViewModel`, `SyncWorker`, `PushRegistration` and `ReportPhotoRepository`). Filter Logcat by these tags to follow a feature. Personal details, passwords and tokens are never logged, and HTTP logging is switched on in debug builds only.

## 7. Setup

### Prerequisites

* Android Studio (latest stable) with the Android SDK 36 platform
* JDK 17

### `local.properties`

Create `local.properties` in the project root. Never commit this file.

```properties
sdk.dir=/path/to/your/Android/Sdk
KINPLUS_API_BASE_URL=https://your-api-base-url/
# Optional: map tiles will not load without this, but the app still builds
KINPLUS_GOOGLE_MAPS_API_KEY=your-maps-sdk-android-key
```

Without `KINPLUS_API_BASE_URL` the app uses our live Azure API.

Get a Maps SDK for Android key from the [Google Cloud Console](https://console.cloud.google.com/google/maps-apis/credentials).

### Firebase

1. In the [Firebase Console](https://console.firebase.google.com/), register an Android app with the package name `za.co.kinplus.app`.
2. Download `google-services.json` into `app/`. A placeholder is provided at `app/google-services.json.template`.
3. `app/google-services.json` is ignored by git and must stay out of version control.

### Build and run

```bash
./gradlew assembleDebug
```

Or open the project in Android Studio and run the `app` configuration on a phone.

## 8. GitHub and automated testing

The project has been on GitHub since the prototype, with regular commits for every feature and fix. The final PoE commit is tagged `final-poe`, and each version is tagged too (`v1.0.0` to `v1.2-poe`).

Two GitHub Actions workflows live in `.github/workflows`. They are based on the guides given in the PoE brief (Amir, 2026; IMAD5112, 2025).

* **`build.yml`** runs on every push and pull request. It sets up JDK 17 and the Android SDK, adds the placeholder Firebase config, runs the unit tests (`testDebugUnitTest`) and builds the debug APK. The test report and the APK are uploaded as artifacts. This proves the app builds and the tests pass on a clean machine, not only on our laptops. The badge at the top of this page shows the result of the latest run.
* **`release.yml`** runs when a version tag such as `v1.3-poe` is pushed. With the upload key saved as repository secrets it builds the signed APK and AAB and attaches them to a GitHub Release. Without the secrets it attaches a debug APK and marks the release as a pre-release.

The 46 unit tests in `app/src/test` cover the main functionality:

| Test class | What it checks |
| ---------- | -------------- |
| `AuthValidationTest` | Email, password and phone number rules |
| `AuthErrorTest` | Empty sign in fields are caught, and Firebase errors map to translated messages |
| `SosRepositoryTest` | An SOS raised offline is queued and logged, using MockK |
| `SyncWorkerTest` | Queued actions are replayed on reconnect, and a report made offline uploads its photo first |
| `AlertRepositoryTest` | Community reports with photos: uploaded, sent without the photo if the upload fails, or queued offline |
| `ReportPhotoTest` | Photos are sampled down before upload and only loaded from Azure Blob Storage |
| `AlertDistanceTest` | The Community Alerts distance calculation and the check in timer settings |
| `DistanceFormatTest` | Kilometre and mile conversions for the Units setting |
| `EtaFormatTest` | The Safe Journey arrival time format |
| `NearbySearchTest` | The nearby police, hospital and clinic search links |

Run them locally with `./gradlew test`.

## 9. Permissions

| Permission | Why |
| ---------- | --- |
| Location (fine, coarse, background) | Sharing location with circles, Safe Zone geofences and SOS location |
| Notifications | SOS, Safe Zone, Safe Journey and check in alerts |
| Internet and network state | Talking to the Kin+ API and detecting offline mode |
| Vibrate | Fake call ringing |
| Receive boot completed | Lets scheduled background work (offline sync and the check in timer) continue after a restart |

Photos for community reports are chosen with the Android photo picker, which needs no storage permission.

## 10. Release notes, AI use and Play Store

* [Release Notes](Release%20Notes.md): every update since the prototype, including which innovative features were added in each version.
* [AI Usage](AI%20Usage.md): how AI tools were used and cited.
* [Play Store](Play%20Store.md): Play Store preparation checklist, signing and the target API level.
* [Store Listing](docs/Store%20Listing.md) and [Privacy Policy](docs/Privacy%20Policy.md): the Play Console listing, data safety answers and privacy policy.
* [Rubric Map](docs/Rubric%20Map.md): where each rubric criterion is met in the code.

## 11. Demonstration video

**Video walkthrough on YouTube:** https://youtu.be/xNa7XxI52-g

A copy of the video is also in this repository: [KinPlus Demo Part 3](docs/video/KinPlus%20Demo%20Part%203.mp4).

## 12. Team

* **Vukosi Rikhotso** (ST10439408): project lead, Android UI, navigation and Firebase integration.
* **Theo Golele** (ST10439863): REST API, MongoDB, Azure services and backend integration.

## 13. Credits

* [Plus Jakarta Sans](https://github.com/tokotype/PlusJakartaSans) by Tokotype, used under the SIL Open Font License 1.1. The license is included at `app/src/main/assets/licenses/PlusJakartaSans OFL.txt`.
* Icons from Material Icons (Apache License 2.0).

## 14. References

Every source below is cited in a code comment next to the code that uses it.

* Amir, F. (2026) Automated build Android app with GitHub Action. Available at: https://github.com/marketplace/actions/automated-build-android-app-with-github-action [Accessed 5 November 2026].
* Android Developers (2026) About Sign in with Google. Available at: https://developer.android.com/identity/sign-in/credential-manager-siwg [Accessed 5 November 2026].
* Android Developers (2026) CameraManager. Available at: https://developer.android.com/reference/android/hardware/camera2/CameraManager [Accessed 5 November 2026].
* Android Developers (2026) Create and monitor geofences. Available at: https://developer.android.com/develop/sensors-and-location/location/geofencing [Accessed 5 November 2026].
* Android Developers (2026) DataStore. Available at: https://developer.android.com/topic/libraries/architecture/datastore [Accessed 5 November 2026].
* Android Developers (2026) Dependency injection with Hilt. Available at: https://developer.android.com/training/dependency-injection/hilt-android [Accessed 5 November 2026].
* Android Developers (2026) Display content edge-to-edge in views. Available at: https://developer.android.com/develop/ui/views/layout/edge-to-edge [Accessed 5 November 2026].
* Android Developers (2026) Get the last known location. Available at: https://developer.android.com/develop/sensors-and-location/location/retrieve-current [Accessed 5 November 2026].
* Android Developers (2026) Loading Large Bitmaps Efficiently. Available at: https://developer.android.com/topic/performance/graphics/load-bitmap [Accessed 5 November 2026].
* Android Developers (2026) Meet Google Play's target API level requirement. Available at: https://developer.android.com/google/play/requirements/target-sdk [Accessed 5 November 2026].
* Android Developers (2026) Navigation. Available at: https://developer.android.com/guide/navigation [Accessed 5 November 2026].
* Android Developers (2026) Photo picker. Available at: https://developer.android.com/training/data-storage/shared/photopicker [Accessed 5 November 2026].
* Android Developers (2026) Read network state. Available at: https://developer.android.com/training/basics/network-ops/reading-network-state [Accessed 5 November 2026].
* Android Developers (2026) RingtoneManager. Available at: https://developer.android.com/reference/android/media/RingtoneManager [Accessed 5 November 2026].
* Android Developers (2026) Save data in a local database using Room. Available at: https://developer.android.com/training/data-storage/room [Accessed 5 November 2026].
* Android Developers (2026) Sign your app. Available at: https://developer.android.com/studio/publish/app-signing [Accessed 5 November 2026].
* Android Developers (2026) Support different languages and cultures. Available at: https://developer.android.com/training/basics/supporting-devices/languages [Accessed 5 November 2026].
* Android Developers (2026) Task scheduling. Available at: https://developer.android.com/topic/libraries/architecture/workmanager [Accessed 5 November 2026].
* Android Developers (2026) Use Kotlin coroutines with lifecycle-aware components. Available at: https://developer.android.com/topic/libraries/architecture/coroutines [Accessed 5 November 2026].
* Firebase (2026) Authenticate with Firebase using Password-Based Accounts on Android. Available at: https://firebase.google.com/docs/auth/android/password-auth [Accessed 5 November 2026].
* Firebase (2026) Best practices for FCM registration management. Available at: https://firebase.google.com/docs/cloud-messaging/manage-tokens [Accessed 5 November 2026].
* Firebase (2026) FirebaseAuthException. Available at: https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuthException [Accessed 5 November 2026].
* Firebase (2026) Receive messages in Android apps. Available at: https://firebase.google.com/docs/cloud-messaging/android/receive [Accessed 5 November 2026].
* IMAD5112 (2025) Github-actions: build.yml. Available at: https://github.com/IMAD5112/Github-actions/blob/main/.github/workflows/build.yml [Accessed 5 November 2026].
* Microsoft (2023) Put Blob (REST API). Available at: https://learn.microsoft.com/en-us/rest/api/storageservices/put-blob [Accessed 5 November 2026].
* Microsoft (2026) Create a service SAS. Available at: https://learn.microsoft.com/en-us/rest/api/storageservices/create-service-sas [Accessed 5 November 2026].
* Veness, C. (2022) Calculate distance and bearing between two Latitude/Longitude points using haversine formula in JavaScript. Available at: https://www.movable-type.co.uk/scripts/latlong.html [Accessed 5 November 2026].
