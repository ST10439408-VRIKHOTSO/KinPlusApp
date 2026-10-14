# Play Store Preparation

This checklist tracks what is needed to publish Kin+ on the Google Play Store, and where each item lives.

## Already in place

* **Package name:** `za.co.kinplus.app`
* **Version:** set in `app/build.gradle.kts` (`versionCode` and `versionName`). Increase `versionCode` for every upload.
* **Release signing:** `app/build.gradle.kts` reads the upload key from `keystore.properties` in the project root when that file exists. The file and the keystore are ignored by git.
* **Code shrinking:** release builds use R8 (`isMinifyEnabled` and `isShrinkResources`) with `proguard-rules.pro`.
* **App icon:** adaptive launcher icon with foreground, background and monochrome layers in `res/mipmap-anydpi-v26`.
* **Target SDK 34**, as Google Play requires.

## Create the signed release (AAB)

1. In Android Studio choose **Build > Generate Signed App Bundle or APK**, pick **Android App Bundle**, and create or select the upload keystore. Keep the keystore and its passwords safe and out of GitHub.
2. Create `keystore.properties` in the project root:

   ```properties
   storeFile=/path/to/kinplus-upload.jks
   storePassword=your-store-password
   keyAlias=kinplus
   keyPassword=your-key-password
   ```

3. Run `./gradlew bundleRelease`. The signed bundle is at `app/build/outputs/bundle/release/app-release.aab`.

## Store listing checklist

| Item | Status |
| ---- | ------ |
| App name, short description (80 characters) and full description | Done, see `docs/STORE_LISTING.md` |
| High resolution icon, 512 x 512 PNG | To do |
| Feature graphic, 1024 x 500 | To do |
| At least 2 phone screenshots from a real device (see `docs/screenshots`) | Done |
| Privacy policy URL (required because Kin+ uses location) | Done, see `docs/privacy-policy.md` |
| Data safety form: location, name, email, device ID, push token | Answers ready in `docs/STORE_LISTING.md` |
| Location permissions declaration (background location for Safe Zones) | Text ready in `docs/STORE_LISTING.md` |
| Content rating questionnaire | Answers ready in `docs/STORE_LISTING.md` |
| Target audience and app category (Lifestyle or Tools) | Done: Lifestyle, 13 and over |
| Signed AAB built (`app-release.aab`) | Done |
| Upload the AAB to an internal testing track | Blocked: needs a paid developer account (USD 25) |
| Screenshot of the Play Console signup for the PoE evidence | Done, see `docs/screenshots/play-console-signup.png` |

Update the Status column as each item is done.
