<div align="center">

# Kin+

### Stay Connected. Stay Safe.

A South African family and community safety app for Android.

**OPSC6312 Open Source Coding (Intermediate) · Portfolio of Evidence**
Rosebank College (IIE) · Pretoria Campus
Vukosi Rikhotso (ST10439408) & Theo Golele (ST10439863)

</div>

---

## 1. Purpose

Kin+ helps people stay connected with the people they trust and get help quickly in an emergency. Our Part 1 research found that Namola, Life360 and Google Family Link each solve part of the problem, but none combines location circles, a deliberate emergency SOS, geofenced Safe Zones, safe journey monitoring, community safety reporting, offline resilience and a multilingual South African interface in one app. Kin+ does.

## 2. Download

Get the latest installable APK from the [GitHub Releases page](https://github.com/ST10439408-VRIKHOTSO/KinPlusApp/releases/latest).

> The release APK on GitHub is a debug build with a placeholder Firebase config, so Maps needs your own key as described in [Setup](#7-setup).

## 3. Features

| #  | Feature                                       | Requirement | Where in the code                                         |
| -- | --------------------------------------------- | ----------- | --------------------------------------------------------- |
| 1  | Register and log in (Firebase Auth)           | FR 01 to 03 | `auth/AuthManager.kt`, `ui/screens/auth`                  |
| 2  | Single sign on with Google                    | FR 05       | `AuthManager.signInWithGoogle()` (Credential Manager)     |
| 3  | Settings, including language                  | FR 32       | `ui/screens/settings`, `data/local/SettingsStore.kt`      |
| 4  | REST API integration                          | n/a         | `data/remote` (Retrofit) and `data/repository`            |
| 5  | Offline mode with sync                        | FR 28, 29   | `data/local` (Room), `data/sync` (WorkManager)            |
| 6  | Real time notifications                       | FR 30       | `messaging/KinPlusMessagingService.kt` (FCM)              |
| 7  | English, isiZulu and Sepedi                   | FR 31       | `res/values`, `values-zu`, `values-nso`, `LocaleManager`  |
| 8  | **Kin+ Circles**                              | FR 09 to 16 | `ui/screens/circles`                                      |
| 9  | **Emergency SOS**                             | FR 17 to 19 | `ui/screens/sos`                                          |
| 10 | **Safe Zones** with geofencing                | FR 20 to 22 | `ui/screens/zones`, `location/GeofenceManager.kt`         |
| 11 | **Safe Journey**                              | FR 23, 24   | `ui/screens/journey`                                      |
| 12 | **Community Alerts**                          | FR 25 to 27 | `ui/screens/alerts`                                       |
| 13 | **Privacy Control Centre**                    | FR 15, 16   | Safety hub and Profile                                    |

### Added in Version 1.1

| Feature                                   | Part of          | Where in the code                                          |
| ----------------------------------------- | ---------------- | ---------------------------------------------------------- |
| Fake incoming call                        | Emergency SOS    | `ui/components/FakeCall.kt`                                |
| Siren and flashlight strobe               | Emergency SOS    | `ui/components/SirenStrobe.kt`                             |
| "I'm OK" check in timer                   | Safe Journey     | `data/checkin/*`, `ui/screens/journey/CheckInCard.kt`      |
| Distance filter and report details        | Community Alerts | `ui/screens/alerts/CommunityAlertsScreen.kt`               |
| Nearby police, hospitals and clinics      | Safety hub       | `ui/screens/safety/SafetyScreen.kt`                        |

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
* **Only the data we need (POPIA).** Location is used for circles, Safe Zones and SOS. Emergency information and contacts stay on the device. Users can pause sharing at any time and delete their account.

## 6. Architecture

```text
UI (XML layouts + Fragments) -> ViewModels (MVVM) -> Repositories -> Retrofit API client / Room cache
                                                                  -> Firebase Auth, FCM, WorkManager
```

* **Offline first:** reads come from Room, so cached data shows without a connection. Writes that must not be lost (location, SOS, zone crossings, reports) are queued and sent by WorkManager when the connection returns, with a client action ID so the server ignores duplicates.
* **Authentication** is handled by Firebase. The API checks the Firebase ID token on every request and never sees a password.
* **REST API:** our own ASP.NET Core Web API (.NET 8) with MongoDB Atlas and Azure Blob Storage, in the `KinPlusApi` project.

```text
app/src/main/java/za/co/kinplus/app/
├── auth/          Firebase email and password, Google SSO
├── data/          local (Room), remote (Retrofit), repository, sync and checkin (WorkManager)
├── di/            Hilt modules
├── domain/        Domain models
├── location/      Location, geofencing, compass
├── messaging/     Firebase Cloud Messaging
├── ui/            Fragments (one per screen), ViewModels, shared view helpers
└── util/          Connectivity, locale, Resource wrapper
```

**Tech stack:** XML layouts with ViewBinding, Fragments and the Navigation component, Material Components (Material 3), RecyclerView, Hilt, Retrofit and OkHttp, Room, DataStore, WorkManager, Firebase (Auth, Cloud Messaging, Analytics), Play Services Location and Maps. Minimum SDK 26, target SDK 34, Kotlin and Java 17.

## 7. Setup

### Prerequisites

* Android Studio (latest stable) with Android SDK 34
* JDK 17

### `local.properties`

Create `local.properties` in the project root. Never commit this file.

```properties
sdk.dir=/path/to/your/Android/Sdk
KINPLUS_API_BASE_URL=https://your-api-base-url/
# Optional: map tiles will not load without this, but the app still builds
KINPLUS_GOOGLE_MAPS_API_KEY=your-maps-sdk-android-key
```

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

Two GitHub Actions workflows live in `.github/workflows`:

* **`build.yml`** runs on every push and pull request. It sets up JDK 17 and the Android SDK, adds the placeholder Firebase config, runs the unit tests (`testDebugUnitTest`) and builds the debug APK. Test reports are uploaded as an artifact. This proves the app builds and the tests pass on a clean machine, not only on our laptops.
* **`release.yml`** runs when a version tag such as `v1.1` is pushed. It builds the APK and attaches it to a GitHub Release.

Unit tests in `app/src/test`:

* `AuthValidationTest.kt`: email and password rules.
* `SosRepositoryTest.kt`: the offline SOS queue, using MockK.
* `AlertDistanceTest.kt`: the Community Alerts distance calculation and the check in timer settings.

Run them locally with `./gradlew test`.

## 9. Permissions

| Permission | Why |
| ---------- | --- |
| Location (fine, coarse, background) | Sharing location with circles, Safe Zone geofences and SOS location |
| Notifications | SOS, Safe Zone, Safe Journey and check in alerts |
| Internet and network state | Talking to the Kin+ API and detecting offline mode |
| Vibrate | Fake call ringing |
| Receive boot completed | Lets scheduled background work (offline sync and the check in timer) continue after a restart |

## 10. Release notes, AI use and Play Store

* [Release Notes](Release%20Notes.md): every update since the prototype, including which innovative features were added.
* [AI Usage](AI%20Usage.md): how AI tools were used and cited.
* [Play Store](Play%20Store.md): Play Store preparation checklist.
* [Rubric Map](docs/Rubric%20Map.md): where each rubric criterion is met in the code.

## 11. Demonstration video

**Video walkthrough:** [KinPlus Demo Part 3](docs/video/KinPlus%20Demo%20Part%203.mp4) (about 5 minutes, recorded on a Samsung Galaxy A71). Open the link and click **View raw** to download or play it.

## 12. Team

* **Vukosi Rikhotso** (ST10439408): project lead, Android UI, navigation and Firebase integration.
* **Theo Golele** (ST10439863): REST API, MongoDB, Azure services and backend integration.

## 13. Credits

* [Plus Jakarta Sans](https://github.com/tokotype/PlusJakartaSans) by Tokotype, used under the SIL Open Font License 1.1. The license is included at `app/src/main/assets/licenses/PlusJakartaSans OFL.txt`.
* Icons from Material Icons (Apache License 2.0).
