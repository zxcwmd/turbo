# omnisound — Android SoundCloud client

Native Android application scaffold built with Kotlin, Jetpack Compose, Room and Media3. The current milestone is an interactive UI + local recommendation prototype. It does **not** pretend to stream demo tracks: the sample catalogue is local demo data.

## Build without Android Studio

Android Studio is not required. You can build the APK either on GitHub Actions (no Android SDK installation on your computer) or locally with command-line tools.

### Option A — GitHub Actions (easiest)

1. Create a GitHub repository and upload this project, including the `.github/workflows/android.yml` file.
2. Push the files to the `main` branch (or `master`). The workflow starts automatically. You can also start it manually from the repository's **Actions** tab using **Build omnisound APK** → **Run workflow**.
3. When the workflow finishes, open its run and download the `omnisound-debug-apk` artifact.
4. Extract the artifact. The APK inside is `app-debug.apk`; install it on your Android device.

This workflow installs JDK 17, Android SDK Platform 35, Build Tools 35.0.0 and Gradle 8.9 on the GitHub-hosted runner, then runs `gradle assembleDebug`.

### Option B — build on your computer from the command line

Requirements: JDK 17, Android SDK Command-line Tools, Android SDK Platform 35 and Build Tools 35.0.0. No Android Studio is needed.

On Windows PowerShell, after installing the command-line SDK and setting `ANDROID_HOME` to its folder:

```powershell
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
# Install Gradle 8.9 separately and add it to PATH, then from this folder:
gradle assembleDebug
```

On Linux/macOS, the equivalent command is:

```bash
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
gradle assembleDebug
```

The resulting debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Included in this source milestone

- Monochrome Compose interface: Home, Radio, Search, Library and Now Playing.
- Local recommendation engine (`RecommendationEngine`) with taste/genre/tag affinity, seed-track similarity, tempo proximity and anti-repeat weighting.
- Room schema for on-device listening events.
- Media3 playback controller that accepts a real stream URL from the SoundCloud API.
- Deep-link callback URI: `omnisound://soundcloud/callback`.
- GitHub Actions workflow for building the APK without Android Studio.

## SoundCloud API integration — important

This milestone is not yet a release-ready SoundCloud client. It does not include a working live API login/search repository or a production background playback service. SoundCloud API access and OAuth requirements must be followed; do not embed confidential application secrets in a distributable APK. Some tracks may be unavailable for external playback due to permissions, territory or platform restrictions.

Before enabling live SoundCloud search/account actions:

- Register an application using the official SoundCloud developer process.
- Review the current API terms and playback/branding requirements.
- Decide how to handle confidential OAuth credentials. A local-only app cannot safely hide a bundled client secret; a token broker backend is recommended for a distributable app.
- Store user tokens encrypted at rest and refresh rotating refresh tokens carefully.
- Only play tracks whose API access state permits playback.

Official docs: https://developers.soundcloud.com/docs

## Project structure

- `domain/Track.kt` — track and listening event models
- `domain/RecommendationEngine.kt` — local ranking algorithm
- `data/ListeningDatabase.kt` — Room persistence schema
- `player/PlaybackController.kt` — Media3 ExoPlayer wrapper
- `ui/theme/Theme.kt` — monochrome design system
- `MainActivity.kt` — Compose screens and navigation

## Current limitations

Live API repository/auth flow, Room database wiring, background MediaSession service, artwork loading, real queue/progress integration, and automated tests remain to be implemented. The APK is produced by the GitHub Actions workflow or a local command-line Android SDK build; it is not included in the source archive.
