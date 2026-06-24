# Ride Namer 🚴

[![Build](https://github.com/duncanbottrill/karoo-ride-namer/actions/workflows/build.yml/badge.svg)](https://github.com/duncanbottrill/karoo-ride-namer/actions/workflows/build.yml)

A small [Hammerhead Karoo](https://www.hammerhead.io/) extension that gives every finished
ride a funny, random, *ride-specific* name based on its **weather, distance, elevation,
speed, intensity, and where you rode** — and (optionally) renames the matching activity on
Strava.

> Examples it generated in testing:
> - *“The Hills of Box Hill (920m)”* (hilly day, located)
> - *“Tour de Snowdonia”* / *“Mountainous Saga out of Snowdonia”* (big wet mountain day)
> - *“Narnia Outing”* (short, freezing, snowing)
> - *“Pretending to Have a Train”* (flat, suspiciously fast)
> - *“Surface-of-the-Sun Adventure”* (long, 33 °C)

## How it works

The app is a Karoo extension (a background Android service built on the
[`karoo-ext`](https://github.com/hammerheadnav/karoo-ext) SDK, v1.1.9). It:

1. **Watches ride state.** When a ride starts recording it begins accumulating stats and,
   from the first GPS fix, fetches current weather **once** from
   [Open-Meteo](https://open-meteo.com) (free, no API key) and caches it, so losing signal
   mid-ride doesn't change the recorded conditions. It also samples GPS throughout the ride
   (tagged with distance) so it can name the ride by its **midpoint**, not its start —
   handy for point-to-point rides.
2. **Collects the ride's “this ride” aggregate data** as it rides: distance, ascent,
   average/max speed, average/max HR, average power, and Intensity Factor (when a power
   meter + FTP are available).
3. **When the ride ends**, it reverse-geocodes the ride's midpoint into a place name via
   [BigDataCloud](https://www.bigdatacloud.com) (free, no API key; skipped gracefully if
   offline), classifies the ride into buckets (how long / hilly / hard / hot / windy / what
   time of day / where), and runs the **offline name generator**. Two styles are available
   (pick one in the app): **Funny** — silly, random names from curated word banks
   (*“Tour de Snowdonia”*); and **Descriptive** — a plain summary of place, distance, effort
   and weather (*“Morning hard 64 km hilly ride around Box Hill in the rain”*). The generator
   uses no network or AI.
4. **Shows a notification** with the name, saves it to the in-app **history**, and — if
   Strava is connected — **queues a rename** that's applied once the Karoo uploads the
   activity to Strava.

The naming logic lives in
[`name/`](app/src/main/kotlin/com/duncanbottrill/ridenamer/name/) — the fun part to tweak.
Add words to [`WordBanks.kt`](app/src/main/kotlin/com/duncanbottrill/ridenamer/name/WordBanks.kt)
or new templates to
[`RideNameGenerator.kt`](app/src/main/kotlin/com/duncanbottrill/ridenamer/name/RideNameGenerator.kt).

## Building

This is a standard Android (Kotlin / Compose / Gradle) project. There's no Android SDK in
this repo, so build it with **Android Studio** (Ladybug or newer) or the command line.

### Prerequisites

1. **JDK 17** and the **Android SDK** (Android Studio bundles both).
2. A **GitHub Personal Access Token** with the `read:packages` scope — `karoo-ext` is
   published to GitHub Packages, which requires auth even for public packages.

### Steps

1. `cp local.properties.example local.properties` and fill in `sdk.dir`, `gpr.user`, and
   `gpr.key` (see that file for details).
2. Open the project in Android Studio and let it sync (it generates the Gradle wrapper),
   **or** from the command line run `gradle wrapper --gradle-version 8.9` once, then
   `./gradlew :app:assembleDebug`.
3. The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

### Install on the Karoo

The Karoo 3 runs Android and accepts sideloaded APKs over ADB (enable developer
access on the device first — see Hammerhead's developer docs):

```bash
adb connect <karoo-ip>        # or USB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

You can also grab a prebuilt APK from the [Releases](../../releases) page — CI builds and
attaches one whenever a `v*` tag is pushed (e.g. `git tag v1.0 && git push --tags`).

Open **Ride Namer** once from the Karoo app list so the extension registers. After that it
runs in the background; just ride.

## Strava auto-rename (optional)

Renaming the Strava activity uses your own Strava API app, with the **client secret held in
a small backend** (see [`backend/`](backend/)) so it never ships inside the APK. In the app
it's then a one-tap **Connect with Strava** — no credentials to type on the Karoo.

One-time setup:

1. Deploy the backend and set its `STRAVA_CLIENT_ID` / `STRAVA_CLIENT_SECRET` env vars —
   see [backend/README.md](backend/README.md). (Create the Strava API app at
   <https://www.strava.com/settings/api> with **Authorization Callback Domain** = `strava-callback`.)
2. Put your backend's URL in
   [`StravaConfig.kt`](app/src/main/kotlin/com/duncanbottrill/ridenamer/strava/StravaConfig.kt)
   (`STRAVA_BACKEND_URL`) and build/release the app.
3. In Ride Namer, tap **Connect with Strava** → approve in the browser → it redirects back
   (`ridenamer://strava-callback`) and shows "Strava connected ✓". The app sends the OAuth
   code to the backend, which does the secret-requiring exchange and returns only the tokens.

From then on, finished rides are queued for rename. Because the Karoo uploads to Strava a
little after a ride ends, the rename is retried until the activity appears (and you can tap
**Sync now** to force it). Matching is by start time within a 10-minute window, picking the
closest cycling activity.

## Notes & assumptions

- **Units:** stream values from the SDK are treated as metric SI (metres, m/s) and shown in
  km / km/h. Add a unit toggle if you prefer miles.
- **Intensity:** uses Intensity Factor when power + FTP give one; otherwise it estimates
  effort from heart-rate fraction and how hard the terrain/speed pushed back.
- **No custom data fields:** the extension doesn't add in-ride screens — it only reacts to
  the end of a ride. `extension_info.xml` therefore declares no `<DataType>`s.
- **Privacy:** your ride-start coordinates are sent to Open-Meteo (weather) and the ride's
  midpoint coordinates to BigDataCloud (place name); Strava API calls go to Strava; the OAuth
  code/refresh token pass through your own backend (which holds the Strava secret). Tokens are
  stored locally via DataStore; the client secret is never on the device.

## Tests

`./gradlew :app:testDebugUnitTest` runs the generator tests
([`RideNameGeneratorTest`](app/src/test/kotlin/com/duncanbottrill/ridenamer/name/RideNameGeneratorTest.kt)):
determinism per seed, variety across seeds, place-name inclusion, and no crashes across
band combinations.

## License

[Apache License 2.0](LICENSE). Built on the Apache-2.0
[`karoo-ext`](https://github.com/hammerheadnav/karoo-ext) SDK; not affiliated with or
endorsed by Hammerhead/SRAM or Strava.

## Project layout

```
app/src/main/kotlin/com/duncanbottrill/ridenamer/
  RideNamerExtension.kt      # background service: ride lifecycle → name → notify → Strava
  karoo/KarooStreams.kt      # karoo-ext Flow helpers (ride state, data streams, HTTP)
  model/                     # RideStats, WeatherSnapshot
  name/                      # classification + word banks + generator  ← the fun part
  weather/WeatherClient.kt   # Open-Meteo current conditions
  geo/GeocodeClient.kt       # BigDataCloud reverse geocode → place name
  strava/                    # OAuth + activity matching + rename
  data/                      # DataStore: history, pending renames, credentials
  ui/                        # Compose: demo/shuffle, Strava setup, history
```
