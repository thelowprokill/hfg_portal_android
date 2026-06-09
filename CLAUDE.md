# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew installDebug           # Build and install debug APK on connected device/emulator
./gradlew test                   # Run unit tests
./gradlew testDebugUnitTest      # Run debug unit tests specifically
./gradlew connectedAndroidTest   # Run instrumentation tests (requires device/emulator)
./gradlew lint                   # Run lint checks
./gradlew lintFix                # Auto-fix lint issues
```

## Architecture

**Pattern:** MVVM + Repository, single-module `:app`, Jetpack Compose UI.

**Dependency injection:** Service locator via `HfgApplication` — the Application class holds singletons (database, repositories, remote sources). ViewModels receive these through `ViewModelFactory` subclasses defined per-feature.

**Data flow:** Room DAOs → Repository → ViewModel (StateFlow/Flow) → Compose UI (`.collectAsState()`). Prefer `Flow` at all boundaries; combine flows in ViewModels for compound state.

**Key layers:**
- `data/local/` — Room database (`AppDatabase` v2, destructive migration fallback), DAO, DataStore preferences
- `data/remote/` — Ktor client (`RemoteDataSource`) for the HFG backend + Retrofit (`WeatherApiService`) for weather
- `data/model/` — Room entities: `CarLog`, `Expense`, `Budget`
- `data/repository/` — `AppRepository` (local), `WeatherRepository` (remote)
- `ui/<feature>/` — one directory per screen; each contains a `Screen.kt` composable and a `ViewModel.kt`
- `ui/navigation/` — `Screen` sealed class with route strings, `NavGraph.kt` wires composables to routes
- `worker/` — `MaintenanceReminderWorker` (WorkManager) for scheduled notifications

## Navigation

`NavGraph.kt` uses Jetpack Navigation Compose. Start destination is `dashboard`. Routes are defined as `object` entries in `Screen.kt`. `MainScreen` wraps everything in a `ModalNavigationDrawer`.

## Remote API

`RemoteDataSource` (Ktor) talks to `BuildConfig.BASE_URL` (`https://hullfamilygames.com`). Credentials are stored in `EncryptedSharedPreferences` (AES256-GCM). SSL verification is intentionally disabled via a custom TrustManager (dev/testing setup). Auth is JWT-based via a `POST /apis/call` login call; subsequent calls attach the token as a header.

## Tech Stack Highlights

| Concern | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | androidx.navigation:navigation-compose |
| Local DB | Room 2.7 (KSP codegen) |
| Preferences | DataStore |
| HTTP (backend) | Ktor Client 3.0 |
| HTTP (weather) | Retrofit 2 + Moshi |
| Image loading | Coil 2.7 |
| Background tasks | WorkManager |
| Camera | CameraX 1.5 |
| Permissions | Accompanist Permissions |
| JSON | Moshi (KSP codegen) |

KSP is used for both Room and Moshi — add new entities/adapters to the relevant annotated classes and rebuild to regenerate.

## Versions

- compileSdk / targetSdk: 36 (Android 16)
- minSdk: 30 (Android 11)
- Kotlin: 2.2.10 / AGP: 9.1.1 / Gradle: 9.3.1
- Java source/target compatibility: 11

## Project Status

See `.agent/plan.md` for the phased task breakdown. As of the last commit, Task 1 (data layer + navigation) is complete and Task 2 (car maintenance) is in progress.
