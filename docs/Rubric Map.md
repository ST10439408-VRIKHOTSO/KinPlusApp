# Rubric to Code Map

A guide for the marker showing where each rubric criterion is met in the final PoE. Since Part 3 the interface is built with XML layouts, Fragments and ViewBinding. All Kotlin paths below are under `app/src/main/java/za/co/kinplus/app/`.

## PoE: Final Submission

| Criterion | Where it lives |
| --------- | -------------- |
| App runs on a mobile device | Signed release build, tested on a Samsung Galaxy A71. `MainActivity.kt` hosts every screen |
| User interface (XML) | `app/src/main/res/layout` (one XML layout per screen), `res/navigation/nav_graph.xml`, `res/values/themes.xml` and `styles.xml`, `ui/common` |
| Feature: SSO login | `auth/AuthManager.kt` `signInWithGoogle()` (Credential Manager and Firebase), `ui/screens/auth/LoginFragment.kt` |
| Feature: Settings | `ui/screens/settings/SettingsFragment.kt`, `data/local/SettingsStore.kt` |
| Creation and use of the REST API | `KinPlusApi` (ASP.NET Core, MongoDB, Firebase token validation) |
| Integration of the REST API | `data/remote/KinPlusApi.kt`, `data/repository` |
| Feature: Offline mode with sync | `data/local` (Room), `data/sync/OfflineQueue.kt` and `SyncWorker.kt` |
| Feature: Real time notifications | `messaging/KinPlusMessagingService.kt` (Firebase Cloud Messaging) |
| Feature: Multi language | `res/values`, `res/values-zu`, `res/values-nso`, `util/LocaleManager.kt` |
| User Defined 1: Circles | `ui/screens/circles`, API `CirclesController.cs` |
| User Defined 2: Emergency SOS | `ui/screens/sos/SosFragment.kt`, fake call `ui/components/FakeCall.kt`, siren and strobe `ui/components/SirenStrobe.kt`, API `SosController.cs` |
| User Defined 3: Safe Zones | `ui/screens/zones`, `location/GeofenceManager.kt`, API `ZonesController.cs` |
| User Defined 4: Safe Journey | `ui/screens/journey`, I'm OK check in timer `data/checkin` and `ui/screens/journey/CheckInCard.kt`, API `JourneysController.cs` |
| User Defined 5: Community Alerts | `ui/screens/alerts` (distance filter and details), API `AlertsController.cs` |
| Safety hub and nearby help | `ui/screens/safety/SafetyFragment.kt`, `ui/screens/safety/IncidentHistoryFragment.kt` |
| App prepared for Play Store | `Play Store.md`, `docs/Store Listing.md`, `docs/Privacy Policy.md`, `docs/store`, release signing in `app/build.gradle.kts` |
| GitHub, README and automated testing | `README.md`, `.github/workflows/build.yml`, unit tests in `app/src/test` |
| Release notes | `Release Notes.md` (Version 1.2 covers Part 3) |
| AI use write up | `AI Usage.md` |
| Demonstration video | Link in `README.md` section 11 |
| Screenshots | `docs/screenshots` |

## Requirement traceability: Part 1B

Functional requirements FR 01 to FR 32 and non functional requirements NFR 01 to NFR 08 are referenced in code comments next to the class or method that implements each one. Search the code for a requirement ID, such as `FR-17`, to find it.
