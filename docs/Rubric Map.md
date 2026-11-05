# Rubric to Code Map

A guide for the marker showing where each rubric criterion is met in the final PoE. Since Part 3 the interface is built with XML layouts, Fragments and ViewBinding. All Kotlin paths below are under `app/src/main/java/za/co/kinplus/app/`.

## PoE: Final Submission

| Criterion | Where it lives |
| --------- | -------------- |
| App runs on a mobile device | Tested on a Samsung Galaxy A71 (Android 13). Targets Android 16 (API 36) and draws edge to edge for Android 15 and later. `MainActivity.kt` hosts every screen |
| User interface (XML) | `app/src/main/res/layout` (one XML layout per screen), `res/navigation/nav_graph.xml`, `res/values/themes.xml` and `styles.xml`, `ui/common` |
| Register and log in, password encrypted | `auth/AuthManager.kt` (Firebase Authentication stores only a salted scrypt hash), `ui/screens/auth/LoginViewModel.kt` and `RegisterViewModel.kt` (input checked first) |
| Feature: SSO login | `auth/AuthManager.kt` `signInWithGoogle()` (Credential Manager and Firebase), `ui/screens/auth/LoginFragment.kt` |
| Feature: Settings | `ui/screens/settings/SettingsFragment.kt` and `SettingsViewModel.kt`: notifications (`messaging/PushRegistration.kt`), language, units (`ui/common/DistanceFormat.kt`), global and per circle sharing |
| Creation and use of the REST API | `KinPlusApi` (ASP.NET Core on Azure App Service, MongoDB Atlas, Firebase token validation) |
| Integration of the REST API | `data/remote/KinPlusApi.kt`, `data/remote/AuthInterceptor.kt`, `data/repository` |
| NoSQL database (store and read) | MongoDB Atlas through the API: for example `CircleRepository.kt` (create, join, read circles) and `AlertRepository.kt` (send and read reports) |
| Blob storage | `data/repository/ReportPhotoRepository.kt`: a SAS link from `POST api/v1/alerts/upload-url`, then a PUT straight to Azure Blob Storage; photos shown in Community Alerts |
| Feature: Offline mode with sync | `data/local` (Room), `data/sync/OfflineQueue.kt` and `SyncWorker.kt` (also uploads photos of reports made offline) |
| Feature: Real time notifications | `messaging/KinPlusMessagingService.kt` (Firebase Cloud Messaging), switched on and off by `messaging/PushRegistration.kt` |
| Feature: Multi language | `res/values`, `res/values-zu`, `res/values-nso` (441 strings each), `util/LocaleManager.kt` (also used for notifications and error messages) |
| User Defined 1: Circles | `ui/screens/circles`, API `CirclesController.cs` |
| User Defined 2: Emergency SOS | `ui/screens/sos/SosFragment.kt`, fake call `ui/components/FakeCall.kt`, siren and strobe `ui/components/SirenStrobe.kt`, API `SosController.cs` |
| User Defined 3: Safe Zones | `ui/screens/zones` (create and edit), `location/GeofenceManager.kt`, API `ZonesController.cs` |
| User Defined 4: Safe Journey | `ui/screens/journey`, I'm OK check in timer `data/checkin` and `ui/screens/journey/CheckInCard.kt`, API `JourneysController.cs` |
| User Defined 5: Community Alerts | `ui/screens/alerts` (distance filter, details and report photos), API `AlertsController.cs` |
| Safety hub and nearby help | `ui/screens/safety/SafetyFragment.kt`, `ui/screens/safety/IncidentHistoryFragment.kt` |
| App prepared for Play Store | `Play Store.md`, `docs/Store Listing.md`, `docs/Privacy Policy.md`, `docs/store`, release signing and target SDK 36 in `app/build.gradle.kts` |
| GitHub, README and automated testing | `README.md`, `.github/workflows/build.yml` and `release.yml`, 46 unit tests in `app/src/test`, final commit tagged `final-poe` |
| Comments, references and logging | Requirement IDs and Harvard references in code comments (listed in README section 14), `android.util.Log` in every repository, worker and main ViewModel |
| Release notes | `Release Notes.md` (Version 1.3 covers the final PoE and names the innovative feature added) |
| AI use write up | `AI Usage.md` |
| Demonstration video | Link in `README.md` section 11 |
| Screenshots | `docs/screenshots` |

## Requirement traceability: Part 1B

Functional requirements FR 01 to FR 32 and non functional requirements NFR 01 to NFR 08 are referenced in code comments next to the class or method that implements each one. Search the code for a requirement ID, such as `FR-17`, to find it.
