# Play Store Preparation

This checklist tracks what is needed to publish Kin+ on the Google Play Store, and where each item lives.

## Already in place

* **Package name:** `za.co.kinplus.app`
* **Version:** set in `app/build.gradle.kts` (`versionCode` 5, `versionName` 1.3-poe). Increase `versionCode` for every upload.
* **Target SDK 36 (Android 16).** Since 31 August 2026 Google Play only accepts new apps and updates that target API 36 or higher (Android Developers, 2026). Kin+ compiles against and targets API 36, and draws edge to edge with system bar padding, which Android 15 and later require.
* **Release signing:** `app/build.gradle.kts` reads the upload key from `keystore.properties` in the project root when that file exists. `keystore.properties`, `*.jks` and `*.keystore` are listed in `.gitignore`, so the key and its passwords never reach GitHub.
* **Code shrinking:** release builds use R8 (`isMinifyEnabled` and `isShrinkResources`) with `proguard-rules.pro`.
* **App icon:** adaptive launcher icon with foreground, background and monochrome layers in `res/mipmap-anydpi-v26`.
* **Release workflow:** `.github/workflows/release.yml` builds the signed APK and AAB on GitHub when a `v` tag is pushed and the upload key is saved as repository secrets (see below).

## Create the signed release (AAB and APK)

1. In Android Studio choose **Build > Generate Signed App Bundle or APK**, pick **Android App Bundle**, and create or select the upload keystore. Keep the keystore and its passwords safe and out of GitHub.
2. Create `keystore.properties` in the project root:

   ```properties
   storeFile=/path/to/kinplus-upload.jks
   storePassword=your-store-password
   keyAlias=kinplus
   keyPassword=your-key-password
   ```

3. Run `./gradlew bundleRelease assembleRelease`. The signed bundle is at `app/build/outputs/bundle/release/app-release.aab` and the signed APK at `app/build/outputs/apk/release/app-release.apk`.

### Or let GitHub Actions sign it

Add these repository secrets (Settings > Secrets and variables > Actions), then push a tag such as `v1.3-poe`:

| Secret | Value |
| ------ | ----- |
| `KINPLUS_UPLOAD_KEYSTORE_B64` | The `.jks` file encoded with `base64` |
| `KINPLUS_KEYSTORE_PASSWORD` | The keystore password |
| `KINPLUS_KEY_ALIAS` | The key alias, for example `kinplus` |
| `KINPLUS_KEY_PASSWORD` | The key password |

Without these secrets the workflow still runs, but it attaches a debug APK named `-debug` and marks the release as a pre-release.

## Store listing checklist

| Item | Status |
| ---- | ------ |
| App name, short description (80 characters) and full description | Done, see `docs/Store Listing.md` |
| High resolution icon, 512 x 512 PNG | Done, `docs/store/Icon 512.png` |
| Feature graphic, 1024 x 500 | Done, `docs/store/Feature Graphic 1024x500.png` |
| At least 2 phone screenshots from a real device (see `docs/screenshots`) | Done |
| Privacy policy URL (required because Kin+ uses location) | Done, see `docs/Privacy Policy.md` |
| Data safety form: location, name, email, community report photos, device ID, push token | Answers ready in `docs/Store Listing.md` |
| Location permissions declaration (background location for Safe Zones) | Text ready in `docs/Store Listing.md` |
| Content rating questionnaire | Answers ready in `docs/Store Listing.md` |
| Target audience and app category (Lifestyle or Tools) | Done: Lifestyle, 13 and over |
| Targets API 36, as Google Play requires | Done, `app/build.gradle.kts` |
| Signed AAB and APK for version 1.2 | Done, signed APK attached to the `v1.2-poe` release |
| Signed AAB and APK for version 1.3 (final PoE) | To do: build with the upload key and attach to the `v1.3-poe` release |
| Upload the AAB to an internal testing track | Blocked: needs a paid developer account (USD 25) |
| Screenshot of the Play Console signup for the PoE evidence | Done, see `docs/screenshots/Play Console Signup.png` |

Update the Status column as each item is done.

## Reference

Android Developers (2026) Meet Google Play's target API level requirement. Available at: https://developer.android.com/google/play/requirements/target-sdk [Accessed 5 November 2026].
