# Changelog

All notable changes made after the original course submission.

## [1.1.0] – 2026-11-11 · English translation, fixes and portfolio clean-up

### Translation (no behaviour change)

- All Java comments (224) and XML comments (60) translated from Spanish to English. One unprofessional comment was rewritten.
- Spanish identifiers renamed one-to-one to English:
  - **342 Java names**: 7 classes (e.g. `Centro` → `Campus`, `Parada` → `TransitStop`, `NotificacionClimaReceiver` → `WeatherNotificationReceiver`), methods, fields, parameters and local variables.
  - **257 resource names**: 175 strings, 47 view IDs, 27 drawables, 4 arrays, 2 layouts, 1 style, 1 raw file.

  Source and resource files were renamed to match. The full table is in [docs/DESIGN.md](docs/DESIGN.md#appendix-a--name-equivalence-table).
- **Not changed on purpose**, so that users keep their data and the server keeps working:
  - SharedPreferences keys, SQLite table/column names, the JSON fields of the stop files and the parameters sent to the PHP server.
  - The package and `applicationId` (`com.example.unigo_das`). Changing them would install the app as a different application.
  - The texts shown to the user, which are already translated into 6 languages in `strings.xml`.
- How the translation was checked (before the bug fixes below were applied):
  1. **Java, token by token:** 36,260 tokens in 22 files. Every keyword, operator, number and string literal is identical and in the same order. The only differences are the 1,960 renamed identifier tokens from the table.
  2. **XML, element by element:** 56 files. Same tags, attributes and texts; only the renamed references differ.
  3. **References:** all 609 resource references (`R.<type>.<name>` in Java, `@type/name` in XML) point to an existing resource.
  4. **Name rules:** the table is one-to-one, no new name is a Java keyword, and no new name was already declared or used unqualified in the original code (no accidental shadowing).

  The Android project could not be compiled in the environment used for the translation; see "Pending" below.

### Fixed

| ID | Problem | Effect | Fix |
|----|---------|--------|-----|
| B1 | The code uses `BuildConfig.DIRECTIONS_API_KEY`, but the committed `secrets.properties` only defined `MAPS_API_KEY`. | A fresh clone of the repository did not compile. | `secrets.defaults.properties` with placeholder values for both keys, configured in the `secrets { }` block of `app/build.gradle`. |
| S1 | `secrets.properties`, with a real Google API key, was committed (since commit `e77dc46`). | Anyone with access to the repository could use the key. | The file was removed from Git tracking (`git rm --cached`; the local copy stays) and added to `.gitignore`. **The key must be revoked and replaced in Google Cloud**: it is still in the Git history. |
| F1 | The air-quality map button sent the translated button text ("AIR", "LUFT", …) and `HeatmapActivity` compared it with `"AIRE"`. | In every language except Spanish, the air-quality button opened the **pollen** map. | Fixed constants `HeatmapActivity.MAP_TYPE_AIR` / `MAP_TYPE_POLLEN` / `EXTRA_MAP_TYPE`. |
| F2 | The "daily" weather alarm used `setExactAndAllowWhileIdle`, which fires only once, and nothing scheduled it again. | The notification arrived only once, and the alarm was lost when the phone restarted. | New `WeatherAlarmScheduler` (same scheduling code, moved from `SettingsFragment`). The receiver schedules the next day after each notification. New `BootReceiver` and the `RECEIVE_BOOT_COMPLETED` permission reschedule it after a restart. |
| F3 | Route filters: the departure time was saved only if the date button no longer said "Today". | Choosing only a new time for today was ignored. | The dialog records whether the date or the time was changed. |
| F4 | `ProfileHeaderPreference` cast the Preference context to `Activity`; in a `PreferenceFragmentCompat` it is a `ContextThemeWrapper`. | The `ClassCastException` was swallowed by the `catch`, so the profile photo was probably never shown. | The UI update uses `ImageView.post()`. |
| F5 | Twelve user-visible texts were hardcoded in Spanish in Java (e.g. "Por favor, llena los campos", "¡Usuario registrado!", "Tu ubicación", "Err Red"). | They were shown in Spanish in all 6 languages. | They now come from `strings.xml`: 10 new strings in 6 languages, plus 2 reused ones. The Spanish text is unchanged. |
| F6 | Google's Universal AQI category "Low air quality" was not handled. | The air quality was shown as "unknown". | New string `air_low` and a mapping for "low". |
| F7 | `Log.e(tag, e.getMessage())` with a null message; `requireContext()`/`requireActivity()` called on background threads and in observers after the screen closed. | The app could crash: a `NullPointerException` on the route thread, or an `IllegalStateException` after the user left the screen. | Null-safe logging. The context and Activity are captured before the background work, and the UI is updated only if the fragment is still added. |
| F8 | `onUpgrade` dropped every table. | Every database schema change deleted the users' favourite campuses. | From schema 3 on, only the reference tables (campuses, stops) are rebuilt, because the app fills them again at start-up. Older schemas behave as before. |
| F9 | The 28 campuses were inserted twice per start-up, each in its own open/insert/close on the main thread. The Geocoder ran on the main thread. A new thread was created per route request and never stopped. | Slower start-up, risk of "App not responding", and leaked threads. | One insert in a single transaction; Geocoder on a background thread; one reusable executor per fragment, shut down in `onDestroy()`. |

Comments marked `Fix B1` … `Fix F9` in the code explain each change in place.

### Known issues (reported, not changed)

| ID | Issue |
|----|-------|
| S2 | Login, registration, password change and photo upload use plain **HTTP** to a hard-coded IP (`http://35.233.9.137`, with `usesCleartextTraffic=true`), so passwords travel unencrypted. The server must offer HTTPS first. |
| S3 | Photo upload and deletion identify the user only by email; no session token is sent. Whether this lets someone change another user's photo depends on the PHP server, which is not in this repository. |
| T1 | The "Electric vehicle" setting says it prioritises routes with charging points. It only sets the car's CO₂ in the comparison to 0. |
| T2 | The notifications setting mentions pollen, but the daily notification does not use pollen data. |
| – | Unused code and files kept as they were: 5 Repomix dumps (`repomix-output.xml` ×3, `resumen.xml`, `resumen2.xml`), `AccountDetailFragment` and its layout, `LanguageDialogFragment` (only its interface is used), the local `Usuarios` table with its methods, and the Room and Glide dependencies. |

### Pending (to do in Android Studio)

- Build the project once and run it on a device. The translation was checked token by token, but the APK could not be built in the translation environment.
- Put the real keys in `secrets.properties`: `MAPS_API_KEY` and `DIRECTIONS_API_KEY` (the latter is used for Directions, Air Quality and Pollen).
