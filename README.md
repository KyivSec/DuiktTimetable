# DUІКТ Timetable

Android timetable app for students of the State University of Information and Communication Technologies (ДУІКТ). It is built with Kotlin, Jetpack Compose, Material 3, Room, OkHttp, jsoup, Kotlin serialization, DataStore, and coroutines.

The app supports Android 8.0 and newer (`minSdk 26`).

## Build

Requirements:

- Android Studio with Android SDK 37
- JDK 17 or newer
- Internet access for the initial Gradle dependency download

From the project root, build an installable debug APK:

```bash
./gradlew assembleDebug
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Build the signed release APK and Play-uploadable Android App Bundle with:

```bash
./gradlew assembleRelease bundleRelease
```

The artifacts are written to:

```text
app/build/outputs/apk/release/app-release.apk
app/build/outputs/bundle/release/app-release.aab
```

Release signing reads the local `keystore.properties` file and uses `keystore/duikt-release.jks`. Both are excluded from Git. Back up both files securely; the keystore is the permanent update identity for installations signed directly with it. Upload the AAB through Google Play and enroll in Play App Signing for Play distribution.

Run local unit tests with:

```bash
./gradlew testDebugUnitTest
```

## Continuous integration

Every push to `main` runs the unit tests and builds signed APK and AAB release artifacts with GitHub Actions. The workflow is also available through **Actions → Android release build → Run workflow**. Successful artifacts are retained for 30 days on the workflow run. The APK is named `DuiktTimetable-{version}-{dd.mm.yyyy}.apk` using the Kyiv calendar date and is sent to the configured Telegram channel after a successful build.

Release credentials are stored as encrypted GitHub Actions secrets named `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`; signing material is never committed.

Telegram delivery uses the encrypted GitHub Actions secrets `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID`. The bot must be an administrator of the target channel with permission to post messages.

## Schedule parsing

Data comes directly from `https://e-rozklad.duikt.edu.ua/time-table/group`; no intermediary server is used.

The client first requests the group timetable page and reads the Yii CSRF token and session cookies. Faculty, course, group, and date selections are then submitted as URL-encoded `TimeTableForm[...]` fields. Directory requests use `type=0`; schedule requests use `type=1` because that response contains structured lesson data.

The response parser:

1. Uses jsoup to validate the timetable form and read faculty, course, group, and semester values.
2. Locates the script containing `var events =`.
3. Extracts the complete JSON object or array with a quote-aware balanced-bracket scanner.
4. Decodes events with Kotlin serialization while ignoring unknown fields.
5. Maps dates, times, subjects, lesson types, rooms, teachers, notices, online links, and source metadata into app models.
6. Sanitizes embedded information HTML and accepts only HTTP or HTTPS links.
7. Generates deterministic SHA-256 lesson identifiers and stores fetched ranges transactionally in Room.

On the first launch, the app opens an empty institute/course/group selector and does not request a schedule until the user explicitly chooses all three values. Cached days are displayed immediately on later launches. The app then refreshes the current semester once for the persisted selected group. Selecting another group performs one update for that explicit selection. Day/week swipes, mode changes, item expansion, and returning to today read Room only and never trigger network requests. Successful empty dates are cached as valid coverage, and failed requests do not erase existing data.
