Kin+ Release Notes

Version 1.3 PoE (final submission): blob storage photos, Android 16 and settings that work

This is the final PoE version. It adds photos to community reports, stored in Azure Blob Storage, moves the app to Android 16 (API 36) so it can be published on Google Play, and fixes the problems we found when we checked the app against the PoE brief.

Added: innovative feature
- Photos on community reports (Community Alerts, FR 26). A report can now carry one photo. The photo is resized on the phone, the Kin+ API gives the app a short lived upload link (a SAS URL that expires after 15 minutes), and the photo goes straight to Azure Blob Storage without passing through our API. The report details show the photo once the report is approved. A report made offline keeps its photo, and the sync worker uploads it when the phone reconnects. If the upload fails, the report is still sent without the photo.

Added
- Editing a Safe Zone. Tapping a saved zone now opens it for editing (name, radius and alerts) instead of an empty form that created a copy.
- Messages after sending a community report: sent for review, sent without the photo, or saved offline.

Changed
- The app targets Android 16 (API 36), which Google Play requires for new apps and updates since 31 August 2026. The Android Gradle plugin is now 8.13.2 and the app compiles against API 36.
- Every screen is drawn edge to edge, with padding for the status bar, the navigation bar and the keyboard, as Android 15 and later require.
- Signing out now clears the cached circles, Safe Zones, locations and queued actions, removes the geofences and the check in timer, and stops this phone receiving the account's notifications. Deleting the account also deletes the Firebase sign in and all Kin+ data on the phone.
- Version 1.3 PoE (version code 5).

Fixed
- The Notifications switch in Settings did nothing. Switching it off now stops push notifications on this phone, even while the app is closed, and switching it on registers the phone again.
- The Units switch in Settings did nothing. Community Alert distances and Safe Zone radii now show in kilometres and metres, or in miles and feet.
- The sharing switch for each circle in Settings always showed as on. It now shows the saved setting, and goes back if the change cannot be saved.
- Error messages on the circle, sign in and profile screens were in English even in isiZulu and Sepedi. They now come from the translated strings. Firebase's English errors are replaced with our own text for a wrong password, an email that is already used, no connection and too many attempts.
- Signing in with an empty email or password showed a technical message. The form is now checked first.
- The check in notifications and the notification channel name now use the language chosen in the app, not only the phone's language.
- Creating a circle or a Safe Zone without a name showed the message meant for a person's first and last name.
- The community report description is optional, as its label says. Before, a report without a description was silently not sent.
- A community report or a Safe Zone could be placed at the Pretoria campus when the phone had no location fix. Kin+ now asks the user to turn on location instead, and does not show distances that were not measured from the user.
- Saving the profile always said it was saved, even when the save failed.

Changes since version 1.2 that were committed before this release
- The Safe Zone editor lets the user choose the circle to notify.
- A queued SOS is marked as sent in Incident History once it syncs.
- Nearby help searches are centred on the user's location.
- The journey ETA is shown as a short local time.
- HTTP traffic is only logged in debug builds, and the sign in token is hidden in the log.

Tests and GitHub
- 46 unit tests, up from 23. The new tests cover community report photos (AlertRepositoryTest, ReportPhotoTest), offline reports with a photo (SyncWorkerTest), sign in errors (AuthErrorTest) and units (DistanceFormatTest).
- The build workflow also uploads the debug APK from every run.
- The release workflow builds the signed APK and AAB when the upload key is saved as repository secrets, and otherwise marks the build as a debug pre-release.
- Logging was added to the main screens and repositories, and every source used in the code is referenced in the comments and listed in the README.


Version 1.2 PoE (Part 3): XML views and release build

This version rebuilds the whole user interface with Android XML layouts, brings back Google sign in, completes the translations and adds a signed release build.

Changed: user interface rebuilt with XML views
- Every screen is now an XML layout shown by a Fragment, with ViewBinding instead of Jetpack Compose. Compose has been removed from the project.
- Screens are connected with the Navigation component (nav_graph.xml) and a BottomNavigationView, with a red SOS tab.
- Lists use RecyclerView with ListAdapter and DiffUtil, so only changed rows are redrawn.
- Each Fragment observes its ViewModel's StateFlow only while the screen is visible (repeatOnLifecycle). The ViewModels, repositories, Room database and API code did not change.
- The calm guardian design from version 1.1 was kept using a Material 3 theme, styles, colours and vector drawables.

Added
- Single sign on with Google (FR 05). A Continue with Google button on the Sign in screen, using the Android Credential Manager and Firebase Authentication. Cancelling the Google picker is not shown as an error.
- Signed release build. The app is signed with a private upload key for Google Play. The key and its passwords are kept outside the repository.

Fixed
- Some screens still showed English text in isiZulu and Sepedi. Both languages now contain every string in the app.
- Community Alert categories now show in the user's language.
- SOS coordinates now show the correct hemisphere (S and E) for South Africa.
- The SOS screen now says it dials 112 emergency dispatch.

Tests
- GitHub Actions builds the app and runs the unit tests on every push.


Version 1.1 PoE: Final polish

This update adds innovative features to our user defined features, refreshes the visual design and fixes a bug found while testing on a phone.

Added: innovative features
- Fake incoming call (Emergency SOS). The phone rings with its own ringtone and vibration and shows a full screen incoming call from a name the user chooses, after a delay of up to one minute. Answering shows a running call timer. It gives the user a believable reason to leave an unsafe situation. Nothing is dialled. Available from the SOS screen and the Fake Call tile on the Safety hub.
- Siren and strobe (Emergency SOS). Plays the alarm sound at full alarm volume and flashes the camera torch to draw attention. The previous volume is restored when it stops.
- I'm OK check in timer (Safe Journey). The user chooses 15, 30, 60 or 120 minutes. When the time runs out a notification asks "Are you OK?". If the user does not tap I'm OK within 5 minutes, Kin+ raises an SOS to all of their circles. It uses WorkManager, so it keeps running after the app is closed, and the SOS is queued if the phone is offline.
- Distance filter and details (Community Alerts). Reports are sorted nearest first, each card shows how far away it is, and the list can be narrowed to 1, 5 or 20 km. The Details button now opens the full report with an Open in Maps shortcut.
- Nearby help (Safety hub). One tap finds the closest police station, hospital, clinic or pharmacy in the phone's maps app.

Changed: design refresh
- New calm guardian look: a warm stone background, white cards with fine borders, deep evergreen as the brand colour, and red kept only for emergencies.
- The Plus Jakarta Sans font (SIL Open Font License) is bundled with the app.
- Pill shaped 56 dp buttons, rounded text fields with an evergreen focus ring, rounded icon badges and a new top bar.
- Sign in and Register use a deep forest header. The SOS button shows a progress ring while it is held.
- Secondary text is darker so it reads more easily on both the background and the cards.

Fixed
- Circle members with no saved name showed a "?" and an empty row. The app now shows the member's name, or "Kin+ member" when none exists, and the API always returns each member's current profile name.
- The Fake Call tile on the Safety hub opened the SOS screen instead of a fake call.
- The Details button on Community Alerts did nothing.

API changes
- Circle details and member locations use each member's live profile name.
- Changing a display name updates it in every circle the user belongs to.
- New members are never stored without a name.

Tests and translations
- New unit tests in AlertDistanceTest.kt for the distance calculation and the check in settings.
- All new text is available in English, isiZulu and Sepedi.


Version 1.0 PoE (Part 2): Portfolio of Evidence

This release builds on the Part 2 prototype and adds the remaining features planned for the final PoE. The list below shows which of our innovative features were added in this version.

Added: PoE features
- Single sign on (SSO). Google sign in using the Android Credential Manager and Firebase authentication.
- Offline mode with sync. Circles, members, Safe Zones and last known locations are saved in Room and remain available without an internet connection. Location updates, SOS alerts, Safe Zone crossings and community reports created while offline are placed in a queue and automatically sent when the connection returns. Client generated IDs are used to prevent duplicate records.
- Real time notifications. Firebase Cloud Messaging sends SOS, Safe Zone and Safe Journey alerts. Tapping a notification opens the related safety screen.
- Multi language interface. The app supports English, isiZulu and Sepedi. Users can change the language in Settings without reinstalling the app.
- Safe Journey (innovative feature). Users can set a destination, expected arrival time and watchers. Watchers are notified when the user arrives. If the arrival is not confirmed after the allowed time, the server sends an alert.
- Community Alerts (innovative feature). Users can report crime, hazards, medical incidents or fires. Reports are checked before they become visible, and the identity of the person who submitted the report is not shown.
- App icon and final assets. Added an adaptive launcher icon using the pin and plus design, including foreground, background and monochrome layers.

Core features
- Email and password registration and login, with credentials handled by Firebase Authentication.
- Kin+ Circles, including creating circles, joining with a code, membership and sharing settings for each circle.
- Emergency SOS with three second press and hold activation and cancellation.
- Safe Zones with entry and exit geofencing.
- Privacy Control Centre with a global pause option and per circle sharing controls.
- Settings and the custom ASP.NET Core REST API with MongoDB Atlas.
- Removed the debug application ID suffix so that one Firebase client configuration can be used for both build types.
- Added the required WorkManager on demand initialisation provider removal for the Hilt worker factory.
