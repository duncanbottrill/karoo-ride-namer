# Minimal name style — design

**Date:** 2026-08-29
**Status:** Approved, ready for implementation planning

## Summary

Add a third naming style, **Minimal**, alongside Funny and Descriptive. It names a ride
with one to three single words drawn from the ride's weather, time of day and speed:

```
1 word  → Amber
2 words → Amber Sprint
3 words → Amber Dawn Sprint
```

After selecting Minimal the user picks how many words they want. The setting persists,
defaults to 3, and applies to every subsequent ride.

## Goals

- A terse, evocative alternative to the two existing styles.
- Word count is the user's choice: 1, 2 or 3.
- The chosen count is always honoured, including when weather data is missing.
- Deterministic for a given seed, like the existing generators, so it is unit-testable.

## Non-goals

- No change to how the two existing styles behave or are generated.
- No change to history storage, the Strava rename path, or the backend.
- No shared template engine across the three styles. Each generator stays independent.

## Word recipe

Three slots, always in this order:

| Position | Slot | Derived from |
|---|---|---|
| 1 | Colour | Weather condition + temperature band |
| 2 | Time | Time-of-day band |
| 3 | Speed | Average speed, adjusted for terrain |

Truncation drops the **middle** slot first, then the tail:

| Count | Slots used | Example |
|---|---|---|
| 3 | Colour + Time + Speed | `Slate Midnight Crawl` |
| 2 | Colour + Speed | `Slate Crawl` |
| 1 | Colour | `Slate` |

The colour is therefore always present — it is the slot that carries the ride's character
most distinctively, and it survives even at one word.

### Single-token rule

**Every word-bank entry is a single token containing no spaces.** This is what makes
"3 words" mean exactly three words. Hyphenated entries are permitted; spaced entries such
as `Pale Gold` are not. A unit test enforces this across all four banks so a later
contributor cannot silently break the count guarantee.

### Adjacent-duplicate rule

When the colour and time slots would produce the same word (reachable via the fallback
palette, e.g. `Midnight Midnight Crawl`), the generator re-draws the colour from its bank
until it differs. If every entry in the bank collides, it uses the first entry regardless.
Bank contents are also curated to avoid overlaps between the colour and time banks in the
first place; the guard exists so the invariant does not depend on that curation staying
correct.

## Classification changes

`RideClassification` exposes speed only as `fast: Boolean`, which is too coarse for a word
bank. Add:

```kotlin
enum class SpeedBand { CRAWL, AMBLE, CRUISE, SWIFT, FLYING }
```

computed as a ratio of the terrain-adjusted `fastThreshold` already present in
`RideClassification.of` (MOUNTAINOUS 20.0, HILLY 24.0, ROLLING 27.0, FLAT 30.0):

| `avgSpeedKmh / fastThreshold` | Band |
|---|---|
| `< 0.65` | CRAWL |
| `< 0.85` | AMBLE |
| `< 1.00` | CRUISE |
| `< 1.15` | SWIFT |
| `>= 1.15` | FLYING |

Reusing the existing threshold means 24 km/h up a mountain still reads as fast. The
existing `fast: Boolean` stays as-is — `RideNameGenerator` depends on it. Nothing else in
`RideClassification` changes.

## New file: `name/MinimalWordBanks.kt`

Four banks. Illustrative contents; exact wording is an implementation detail, but the
single-token rule and the colour/time non-overlap are not.

**Colour, by condition and temperature.** Temperature only shifts `CLEAR` and `CLOUDY`,
where it is the dominant signal; for precipitation and fog the condition dominates.

| Condition | Temp | Words |
|---|---|---|
| CLEAR | FREEZING, COLD | Frost, Pewter, Silver, Crystal |
| CLEAR | MILD | Gold, Wheat, Lemon, Straw |
| CLEAR | WARM, SCORCHING | Amber, Ochre, Brass, Ember, Scorched |
| CLOUDY | FREEZING, COLD | Zinc, Steel, Dove |
| CLOUDY | MILD, WARM, SCORCHING | Pewter, Putty, Oyster, Chalk |
| FOG | any | Ash, Smoke, Milk, Shroud |
| DRIZZLE | any | Drab, Moss, Sage, Verdigris |
| RAIN | any | Slate, Gunmetal, Storm, Lead |
| SNOW | any | Bone, Alabaster, Ice, Glacier, Porcelain |
| THUNDER | any | Ink, Bruise, Charcoal, Sable |

**Colour fallback, by time band.** Used when weather is unavailable — see below.

| Time band | Words |
|---|---|
| DAWN | Rose, Blush, Coral, Peach |
| MORNING | Cyan, Dew, Mint, Eggshell |
| MIDDAY | Azure, Cobalt, Cerulean |
| AFTERNOON | Gold, Wheat, Sand, Honey |
| EVENING | Umber, Rust, Copper, Sienna |
| NIGHT | Indigo, Obsidian, Onyx, Starlight |

**Time, by time band.**

| Time band | Words |
|---|---|
| DAWN | Dawn, Daybreak, Sunrise, Aurora |
| MORNING | Morning, Matins, Forenoon, Sunup |
| MIDDAY | Noon, Midday, Zenith, Meridian |
| AFTERNOON | Afternoon, Nones, Waning |
| EVENING | Evening, Dusk, Vespers, Sundown, Gloaming |
| NIGHT | Midnight, Nocturne, Nightfall, Witching |

**Speed, by speed band.**

| Speed band | Words |
|---|---|
| CRAWL | Crawl, Trudge, Plod, Slog, Dawdle |
| AMBLE | Amble, Pootle, Saunter, Drift |
| CRUISE | Cruise, Tempo, Steady, Glide, Rhythm |
| SWIFT | Sprint, Surge, Charge, Hustle, Chase |
| FLYING | Blitz, Bolt, Rocket, Flight, Warp |

## New file: `name/MinimalNameGenerator.kt`

```kotlin
object MinimalNameGenerator {
    fun generate(stats: RideStats, wordCount: Int = 3, seed: Long? = null): String
}
```

Mirrors `DescriptiveNameGenerator`: seeded `Random` when `seed` is non-null, otherwise
`Random.Default`; derives a `RideClassification`; picks one entry per required slot;
joins with a single space.

`wordCount` is coerced into `1..3` on entry, so a bad value degrades to a valid name
rather than throwing inside the naming path.

### Missing weather

The colour slot falls back to the time-of-day palette when **either** `stats.weather` is
null **or** the condition is `WeatherCondition.UNKNOWN`. Both cases are treated
identically. The requested word count is always honoured — the name never silently
shortens because the weather fetch failed.

## Plumbing

**`NameStyle`** gains a third entry:

```kotlin
MINIMAL("Minimal", "A colour, a time, a pace — in as few words as you like")
```

`fromName` is unchanged: an unrecognised stored value still falls back to `FUNNY`.

**`generateRideName`** gains a defaulted parameter, so existing call sites and the other
two generators are untouched:

```kotlin
fun generateRideName(
    stats: RideStats,
    style: NameStyle,
    wordCount: Int = 3,
    seed: Long? = null,
): String
```

`wordCount` is ignored by `FUNNY` and `DESCRIPTIVE`.

**`RideNamerStore`** gains a word-count setting following the existing `nameStyle`
pattern, using an `intPreferencesKey`:

```kotlin
val minimalWordCount: Flow<Int>          // default 3, coerced into 1..3 on read
suspend fun setMinimalWordCount(count: Int)
```

Clamping on read means a corrupt or out-of-range stored value cannot break ride naming.
Existing installs have no value for this key and get the default of 3.

**`RideNamerExtension`** reads the count alongside the style at
`RideNamerExtension.kt:107` and passes it through. History entries and the pending-rename
queue are unaffected — they only ever handle the finished string.

## UI

**`StyleCard`** renders a third button automatically, since it iterates `NameStyle.entries`.
When `MINIMAL` is the current style, a row of three buttons labelled `1`, `2`, `3` appears
below the blurb, using the same filled-when-selected / outlined-otherwise treatment as the
style buttons. Selecting one writes through to the store on `Dispatchers.IO`, matching how
the style selection is already persisted.

**`DemoCard`** takes the word count as a parameter and includes it in the `remember` key
alongside style and seed, so switching between 1, 2 and 3 updates the sample immediately
rather than waiting for a Shuffle.

## Testing

New `MinimalNameGeneratorTest`, alongside the existing generator tests:

- A count of 1, 2 and 3 produces exactly that many space-separated tokens.
- The same seed produces the same name; different seeds produce varied names.
- `wordCount` values of 0 and 4 are coerced to 1 and 3 rather than throwing.
- Null weather produces a full-length name using the fallback palette.
- An `UNKNOWN` WMO code behaves identically to null weather.
- The colour and time words are never identical in a 3-word name.
- Every entry in all four banks is a single token with no whitespace.

Extend the existing classification coverage with `SpeedBand` boundary cases, including
that a mountainous ride at 24 km/h classifies faster than a flat ride at the same speed.

## Out of scope

- Retroactively renaming rides already in history.
- Per-ride word-count overrides. The setting is global.
- Exposing the generated colour as an actual UI colour.
