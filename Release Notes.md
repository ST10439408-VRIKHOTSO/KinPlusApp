Kin+ Release Notes

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
