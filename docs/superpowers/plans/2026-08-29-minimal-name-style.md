# Minimal Name Style Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a third naming style, "Minimal", that names a ride with 1, 2 or 3 single words drawn from weather colour, time of day and speed.

**Architecture:** A new `MinimalNameGenerator` sits beside the two existing generators, backed by its own `MinimalWordBanks` file. `RideClassification` gains a `SpeedBand` so speed can drive a word bank rather than a boolean. The word count is a persisted user setting threaded from `RideNamerStore` through `generateRideName` to the generator; the UI exposes it as a 1/2/3 selector shown only when Minimal is the active style.

**Tech Stack:** Kotlin, Android (compileSdk 35), Jetpack Compose Material3, DataStore Preferences, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-08-29-minimal-name-style-design.md`

## Global Constraints

- **Every word-bank entry is a single token with no whitespace.** Hyphens are allowed; spaces are not. This is what makes the user's word-count choice literally true, and a test enforces it.
- **Default word count is 3**, exposed as `MinimalNameGenerator.DEFAULT_WORDS`. Never hardcode `3` elsewhere.
- **Valid word counts are 1..3**, exposed as `MIN_WORDS` / `MAX_WORDS`. Out-of-range values are coerced, never thrown on — this code runs in the ride-finished path where a crash loses the user's ride name.
- **The colour slot is always present.** Truncation drops the time word first, then the speed word.
- **Generators are deterministic for a given seed**, matching `RideNameGenerator` and `DescriptiveNameGenerator`.
- **No new dependencies.** JUnit 4 is already the only test dependency.
- **UI style:** the selected option is a filled `Button`, unselected options are `OutlinedButton`. This matches the existing `StyleCard`.

## Verification approach — read this before starting

**This machine cannot run the test suite.** There is no Android SDK (`ANDROID_HOME` unset, nothing at `~/Library/Android/sdk`), no `local.properties`, and the karoo-ext dependency lives on GitHub Packages behind a token. `./gradlew test` will fail at SDK resolution.

Verification therefore happens on **GitHub Actions**, which runs `./gradlew :app:assembleRelease testDebugUnitTest` on a push to any branch.

Consequences for how you work:

- Each task writes its test **first**, then the implementation, as **two separate commits**. The history shows the TDD order and stays bisectable even though the red phase is not observed locally.
- **Be honest about this:** you are not watching tests fail before you make them pass. That is a real weakening of TDD, accepted deliberately because the alternative is a several-minute CI round trip per step. Compensate by making each test assert something specific enough that it could only pass for the right reason.
- There are **two CI checkpoints** (after Task 4 and after Task 7). Do not skip them, and do not proceed past a red checkpoint.
- **Pushing is an outward-facing action. Ask the user before every push.**

---

### Task 0: Branch

**Files:** none

- [ ] **Step 1: Confirm a clean tree on `main`**

```bash
cd ~/github/karoo-ride-namer && git status -sb
```

Expected: `## main...origin/main` and no modified files. If there are uncommitted changes, stop and ask the user what to do with them.

- [ ] **Step 2: Create the feature branch**

```bash
cd ~/github/karoo-ride-namer && git checkout -b feat/minimal-name-style
```

Expected: `Switched to a new branch 'feat/minimal-name-style'`

---

### Task 1: SpeedBand in RideClassification

Speed is currently only a `fast: Boolean`, too coarse to key a word bank. Add a five-way band computed as a ratio of the terrain-adjusted threshold that already exists in this file, so 24 km/h up a mountain still reads as fast.

**Files:**
- Modify: `app/src/main/kotlin/com/duncanbottrill/ridenamer/name/RideClassification.kt`
- Test: `app/src/test/kotlin/com/duncanbottrill/ridenamer/name/RideClassificationTest.kt` (create)

**Interfaces:**
- Consumes: `RideStats`, `ClimbBand` (existing).
- Produces: `enum class SpeedBand { CRAWL, AMBLE, CRUISE, SWIFT, FLYING }` and a new `speed: SpeedBand` property on `RideClassification`, used by Task 3.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/duncanbottrill/ridenamer/name/RideClassificationTest.kt`:

```kotlin
package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import org.junit.Assert.assertEquals
import org.junit.Test

class RideClassificationTest {

    /** A 50 km ride whose elevation picks the climb band and whose speed we vary. */
    private fun ride(avgSpeedKmh: Double, elevationGainM: Double) = RideStats(
        distanceKm = 50.0,
        elevationGainM = elevationGainM,
        durationSec = 7200.0,
        avgSpeedKmh = avgSpeedKmh,
        maxSpeedKmh = avgSpeedKmh + 20.0,
        startEpochMs = 1_700_000_000_000L,
        endEpochMs = 1_700_000_000_000L,
    )

    private val flat = 100.0        // 2 m/km  -> ClimbBand.FLAT,        threshold 30.0
    private val mountainous = 1_000.0 // 20 m/km -> ClimbBand.MOUNTAINOUS, threshold 20.0

    @Test
    fun `speed bands span the flat threshold`() {
        assertEquals(SpeedBand.CRAWL, RideClassification.of(ride(15.0, flat)).speed)   // ratio 0.50
        assertEquals(SpeedBand.AMBLE, RideClassification.of(ride(24.0, flat)).speed)   // ratio 0.80
        assertEquals(SpeedBand.CRUISE, RideClassification.of(ride(28.0, flat)).speed)  // ratio 0.93
        assertEquals(SpeedBand.SWIFT, RideClassification.of(ride(30.0, flat)).speed)   // ratio 1.00
        assertEquals(SpeedBand.FLYING, RideClassification.of(ride(40.0, flat)).speed)  // ratio 1.33
    }

    @Test
    fun `the same speed rates faster on a mountainous ride`() {
        // 24 km/h is a gentle amble on the flat and flying up a mountain.
        assertEquals(SpeedBand.AMBLE, RideClassification.of(ride(24.0, flat)).speed)
        assertEquals(SpeedBand.FLYING, RideClassification.of(ride(24.0, mountainous)).speed)
    }

    @Test
    fun `speed band boundaries are inclusive at the lower edge`() {
        // 0.65 exactly -> AMBLE, not CRAWL (30.0 * 0.65 = 19.5)
        assertEquals(SpeedBand.AMBLE, RideClassification.of(ride(19.5, flat)).speed)
        // 1.15 exactly -> FLYING, not SWIFT (30.0 * 1.15 = 34.5)
        assertEquals(SpeedBand.FLYING, RideClassification.of(ride(34.5, flat)).speed)
    }

    @Test
    fun `the existing fast flag is unchanged`() {
        assertEquals(true, RideClassification.of(ride(30.0, flat)).fast)
        assertEquals(false, RideClassification.of(ride(29.0, flat)).fast)
    }
}
```

- [ ] **Step 2: Commit the test**

```bash
cd ~/github/karoo-ride-namer
git add app/src/test/kotlin/com/duncanbottrill/ridenamer/name/RideClassificationTest.kt
git commit -m "test: SpeedBand classification with terrain-adjusted thresholds"
```

- [ ] **Step 3: Add the enum**

In `RideClassification.kt`, add to the enum block near the top of the file, after the existing `TimeBand` line:

```kotlin
enum class SpeedBand { CRAWL, AMBLE, CRUISE, SWIFT, FLYING }
```

- [ ] **Step 4: Add the property to the data class**

Add `speed` as the last constructor parameter of `RideClassification`, after `val fast: Boolean,`:

```kotlin
    val fast: Boolean,
    val speed: SpeedBand,
) {
```

- [ ] **Step 5: Extract the threshold and classify speed**

In `RideClassification.of`, replace this block:

```kotlin
            // "Fast" is relative to terrain: 28+ on the flat, less if it was a climb-fest.
            val fastThreshold = when (climb) {
                ClimbBand.MOUNTAINOUS -> 20.0
                ClimbBand.HILLY -> 24.0
                ClimbBand.ROLLING -> 27.0
                ClimbBand.FLAT -> 30.0
            }
            val fast = stats.avgSpeedKmh >= fastThreshold
```

with:

```kotlin
            // "Fast" is relative to terrain: 28+ on the flat, less if it was a climb-fest.
            val fastThreshold = fastThresholdFor(climb)
            val fast = stats.avgSpeedKmh >= fastThreshold
            val speed = classifySpeed(stats.avgSpeedKmh, fastThreshold)
```

and update the return statement from:

```kotlin
            return RideClassification(
                distance, climb, intensity, temp,
                stats.weather?.condition, windy, time, fast,
            )
```

to:

```kotlin
            return RideClassification(
                distance, climb, intensity, temp,
                stats.weather?.condition, windy, time, fast, speed,
            )
```

- [ ] **Step 6: Add the two private helpers**

In the same `companion object`, immediately before `private fun classifyIntensity(`:

```kotlin
        private fun fastThresholdFor(climb: ClimbBand): Double = when (climb) {
            ClimbBand.MOUNTAINOUS -> 20.0
            ClimbBand.HILLY -> 24.0
            ClimbBand.ROLLING -> 27.0
            ClimbBand.FLAT -> 30.0
        }

        /** Where the ride sat relative to "fast for this terrain" — 1.0 is exactly on the threshold. */
        private fun classifySpeed(avgSpeedKmh: Double, fastThreshold: Double): SpeedBand {
            val ratio = avgSpeedKmh / fastThreshold
            return when {
                ratio < 0.65 -> SpeedBand.CRAWL
                ratio < 0.85 -> SpeedBand.AMBLE
                ratio < 1.00 -> SpeedBand.CRUISE
                ratio < 1.15 -> SpeedBand.SWIFT
                else -> SpeedBand.FLYING
            }
        }
```

- [ ] **Step 7: Check for other construction sites**

```bash
cd ~/github/karoo-ride-namer && grep -rn "RideClassification(" app/src --include='*.kt'
```

Expected: exactly one hit, the `return RideClassification(` you just edited. If there are others, add `speed` to them too — the new parameter has no default, so any missed site is a compile error.

- [ ] **Step 8: Commit**

```bash
cd ~/github/karoo-ride-namer
git add app/src/main/kotlin/com/duncanbottrill/ridenamer/name/RideClassification.kt
git commit -m "feat: add SpeedBand to RideClassification"
```

---

### Task 2: Minimal word banks

**Files:**
- Create: `app/src/main/kotlin/com/duncanbottrill/ridenamer/name/MinimalWordBanks.kt`
- Test: `app/src/test/kotlin/com/duncanbottrill/ridenamer/name/MinimalWordBanksTest.kt` (create)

**Interfaces:**
- Consumes: `WeatherCondition`, `TempBand`, `TimeBand`, `SpeedBand` (Task 1).
- Produces, all used by Task 3:
  - `MinimalWordBanks.colours(condition: WeatherCondition, temp: TempBand?): List<String>?` — **returns null for `WeatherCondition.UNKNOWN`**, which is how the generator learns to use the fallback palette.
  - `MinimalWordBanks.skyColours: Map<TimeBand, List<String>>`
  - `MinimalWordBanks.times: Map<TimeBand, List<String>>`
  - `MinimalWordBanks.speeds: Map<SpeedBand, List<String>>`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/duncanbottrill/ridenamer/name/MinimalWordBanksTest.kt`:

```kotlin
package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.WeatherCondition
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalWordBanksTest {

    /** Every colour bank reachable through the public API, including the null-temp case. */
    private fun allColourBanks(): List<List<String>> {
        val temps: List<TempBand?> = TempBand.entries + listOf(null)
        val weather = WeatherCondition.entries
            .flatMap { condition -> temps.map { temp -> MinimalWordBanks.colours(condition, temp) } }
            .filterNotNull()
        return weather + MinimalWordBanks.skyColours.values
    }

    private fun allBanks(): List<List<String>> =
        allColourBanks() + MinimalWordBanks.times.values + MinimalWordBanks.speeds.values

    @Test
    fun `every word is a single token`() {
        allBanks().flatten().forEach { word ->
            assertTrue("'$word' must not contain whitespace", word.none { it.isWhitespace() })
            assertTrue("'$word' must not be blank", word.isNotBlank())
        }
    }

    @Test
    fun `no bank is empty`() {
        // A generator picking at random from an empty list would throw mid-ride.
        allBanks().forEach { assertTrue("bank must not be empty", it.isNotEmpty()) }
    }

    @Test
    fun `every band has a bank`() {
        TimeBand.entries.forEach {
            assertNotNull("no sky colours for $it", MinimalWordBanks.skyColours[it])
            assertNotNull("no time words for $it", MinimalWordBanks.times[it])
        }
        SpeedBand.entries.forEach { assertNotNull("no speed words for $it", MinimalWordBanks.speeds[it]) }
    }

    @Test
    fun `unknown weather has no colour bank`() {
        TempBand.entries.forEach {
            assertNull(MinimalWordBanks.colours(WeatherCondition.UNKNOWN, it))
        }
        assertNull(MinimalWordBanks.colours(WeatherCondition.UNKNOWN, null))
    }

    @Test
    fun `known weather always has a colour bank`() {
        val known = WeatherCondition.entries - WeatherCondition.UNKNOWN
        val temps: List<TempBand?> = TempBand.entries + listOf(null)
        known.forEach { condition ->
            temps.forEach { temp ->
                assertNotNull("$condition / $temp has no colours", MinimalWordBanks.colours(condition, temp))
            }
        }
    }

    @Test
    fun `colour words never collide with time words`() {
        // Guards against "Midnight Midnight Crawl". The generator also re-draws, but the
        // banks should not rely on that.
        val colours = allColourBanks().flatten().map { it.lowercase() }.toSet()
        val times = MinimalWordBanks.times.values.flatten().map { it.lowercase() }.toSet()
        val overlap = colours intersect times
        assertTrue("colour and time banks overlap: $overlap", overlap.isEmpty())
    }
}
```

- [ ] **Step 2: Commit the test**

```bash
cd ~/github/karoo-ride-namer
git add app/src/test/kotlin/com/duncanbottrill/ridenamer/name/MinimalWordBanksTest.kt
git commit -m "test: Minimal word bank invariants"
```

- [ ] **Step 3: Create the word banks**

Create `app/src/main/kotlin/com/duncanbottrill/ridenamer/name/MinimalWordBanks.kt`:

```kotlin
package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.WeatherCondition

/**
 * Word lists for the Minimal style: one colour, one time-of-day word, one speed word.
 *
 * Every entry MUST be a single token with no whitespace — that is what makes the user's
 * "1, 2 or 3 words" choice literally true. Colour words must also never collide with time
 * words, or a three-word name could read "Midnight Midnight Crawl". MinimalWordBanksTest
 * enforces both rules, so add new words freely and let the test keep you honest.
 */
object MinimalWordBanks {

    /**
     * Colours for a known sky. Temperature only shifts the dry conditions, where it is the
     * dominant signal; when it is raining or snowing the condition speaks for itself.
     *
     * Returns null for [WeatherCondition.UNKNOWN] — the caller falls back to [skyColours].
     */
    fun colours(condition: WeatherCondition, temp: TempBand?): List<String>? = when (condition) {
        WeatherCondition.CLEAR -> when (temp) {
            TempBand.FREEZING, TempBand.COLD -> listOf("Frost", "Pewter", "Silver", "Crystal")
            TempBand.WARM, TempBand.SCORCHING -> listOf("Amber", "Ochre", "Brass", "Ember", "Scorched")
            TempBand.MILD, null -> listOf("Gold", "Wheat", "Lemon", "Straw")
        }
        WeatherCondition.CLOUDY -> when (temp) {
            TempBand.FREEZING, TempBand.COLD -> listOf("Zinc", "Steel", "Dove")
            else -> listOf("Pewter", "Putty", "Oyster", "Chalk")
        }
        WeatherCondition.FOG -> listOf("Ash", "Smoke", "Milk", "Shroud")
        WeatherCondition.DRIZZLE -> listOf("Drab", "Moss", "Sage", "Verdigris")
        WeatherCondition.RAIN -> listOf("Slate", "Gunmetal", "Storm", "Lead")
        WeatherCondition.SNOW -> listOf("Bone", "Alabaster", "Ice", "Glacier", "Porcelain")
        WeatherCondition.THUNDER -> listOf("Ink", "Bruise", "Charcoal", "Sable")
        WeatherCondition.UNKNOWN -> null
    }

    /**
     * Colours for when the weather fetch failed or returned a code we don't recognise —
     * the colour the sky plausibly was at that hour. Deliberately shares no words with
     * [times].
     */
    val skyColours: Map<TimeBand, List<String>> = mapOf(
        TimeBand.DAWN to listOf("Rose", "Blush", "Coral", "Peach"),
        TimeBand.MORNING to listOf("Cyan", "Dew", "Mint", "Eggshell"),
        TimeBand.MIDDAY to listOf("Azure", "Cobalt", "Cerulean"),
        TimeBand.AFTERNOON to listOf("Gold", "Wheat", "Sand", "Honey"),
        TimeBand.EVENING to listOf("Umber", "Rust", "Copper", "Sienna"),
        TimeBand.NIGHT to listOf("Indigo", "Obsidian", "Onyx", "Starlight"),
    )

    val times: Map<TimeBand, List<String>> = mapOf(
        TimeBand.DAWN to listOf("Dawn", "Daybreak", "Sunrise", "Aurora"),
        TimeBand.MORNING to listOf("Morning", "Matins", "Forenoon", "Sunup"),
        TimeBand.MIDDAY to listOf("Noon", "Midday", "Zenith", "Meridian"),
        TimeBand.AFTERNOON to listOf("Afternoon", "Nones", "Waning"),
        TimeBand.EVENING to listOf("Evening", "Dusk", "Vespers", "Sundown", "Gloaming"),
        TimeBand.NIGHT to listOf("Midnight", "Nocturne", "Nightfall", "Witching"),
    )

    val speeds: Map<SpeedBand, List<String>> = mapOf(
        SpeedBand.CRAWL to listOf("Crawl", "Trudge", "Plod", "Slog", "Dawdle"),
        SpeedBand.AMBLE to listOf("Amble", "Pootle", "Saunter", "Drift"),
        SpeedBand.CRUISE to listOf("Cruise", "Tempo", "Steady", "Glide", "Rhythm"),
        SpeedBand.SWIFT to listOf("Sprint", "Surge", "Charge", "Hustle", "Chase"),
        SpeedBand.FLYING to listOf("Blitz", "Bolt", "Rocket", "Flight", "Warp"),
    )
}
```

- [ ] **Step 4: Commit**

```bash
cd ~/github/karoo-ride-namer
git add app/src/main/kotlin/com/duncanbottrill/ridenamer/name/MinimalWordBanks.kt
git commit -m "feat: add Minimal style word banks"
```

---

### Task 3: MinimalNameGenerator

**Files:**
- Create: `app/src/main/kotlin/com/duncanbottrill/ridenamer/name/MinimalNameGenerator.kt`
- Test: `app/src/test/kotlin/com/duncanbottrill/ridenamer/name/MinimalNameGeneratorTest.kt` (create)

**Interfaces:**
- Consumes: `RideClassification` with `speed` (Task 1), `MinimalWordBanks` (Task 2).
- Produces, used by Tasks 4, 5 and 7:
  - `MinimalNameGenerator.generate(stats: RideStats, wordCount: Int = DEFAULT_WORDS, seed: Long? = null): String`
  - `MinimalNameGenerator.MIN_WORDS = 1`, `MAX_WORDS = 3`, `DEFAULT_WORDS = 3`

**Note on time zones:** `RideClassification.of` uses `ZoneId.systemDefault()`, so a fixed epoch maps to different `TimeBand`s on different machines. Tests must never hardcode an expected band — derive it with `RideClassification.of(ride).time` and assert membership in that band's bank.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/duncanbottrill/ridenamer/name/MinimalNameGeneratorTest.kt`:

```kotlin
package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalNameGeneratorTest {

    private val ride = RideStats(
        distanceKm = 64.0,
        elevationGainM = 920.0,
        durationSec = 9600.0,
        avgSpeedKmh = 24.0,
        maxSpeedKmh = 52.0,
        avgHr = 150.0,
        maxHr = 185.0,
        startEpochMs = 1_700_000_000_000L,
        endEpochMs = 1_700_000_000_000L,
        weather = WeatherSnapshot(9.0, 12.0, 61), // rain, cold
        placeName = "Box Hill",
    )

    private fun words(name: String) = name.split(" ")

    @Test
    fun `word count is honoured exactly`() {
        assertEquals(1, words(MinimalNameGenerator.generate(ride, 1, seed = 1L)).size)
        assertEquals(2, words(MinimalNameGenerator.generate(ride, 2, seed = 1L)).size)
        assertEquals(3, words(MinimalNameGenerator.generate(ride, 3, seed = 1L)).size)
    }

    @Test
    fun `out of range word counts are coerced, not thrown`() {
        assertEquals(1, words(MinimalNameGenerator.generate(ride, 0, seed = 1L)).size)
        assertEquals(1, words(MinimalNameGenerator.generate(ride, -5, seed = 1L)).size)
        assertEquals(3, words(MinimalNameGenerator.generate(ride, 4, seed = 1L)).size)
        assertEquals(3, words(MinimalNameGenerator.generate(ride, 99, seed = 1L)).size)
    }

    @Test
    fun `shorter names are prefixes of the colour and keep the speed word`() {
        val one = MinimalNameGenerator.generate(ride, 1, seed = 7L)
        val two = MinimalNameGenerator.generate(ride, 2, seed = 7L)
        val three = MinimalNameGenerator.generate(ride, 3, seed = 7L)
        // Colour is slot 1 in every length.
        assertEquals(words(one)[0], words(two)[0])
        assertEquals(words(one)[0], words(three)[0])
        // Speed is always last; the time word is what gets dropped.
        assertEquals(words(two)[1], words(three)[2])
    }

    @Test
    fun `is deterministic per seed`() {
        assertEquals(
            MinimalNameGenerator.generate(ride, 3, seed = 5L),
            MinimalNameGenerator.generate(ride, 3, seed = 5L),
        )
    }

    @Test
    fun `different seeds vary the name`() {
        val names = (1L..40L).map { MinimalNameGenerator.generate(ride, 3, seed = it) }.toSet()
        assertTrue("expected variety, got $names", names.size > 1)
    }

    @Test
    fun `words come from the banks for this ride`() {
        val c = RideClassification.of(ride)
        val name = words(MinimalNameGenerator.generate(ride, 3, seed = 3L))
        assertTrue(name[0], MinimalWordBanks.colours(c.condition!!, c.temp)!!.contains(name[0]))
        assertTrue(name[1], MinimalWordBanks.times.getValue(c.time).contains(name[1]))
        assertTrue(name[2], MinimalWordBanks.speeds.getValue(c.speed).contains(name[2]))
    }

    @Test
    fun `missing weather still produces a full length name from the sky palette`() {
        val bare = ride.copy(weather = null)
        val c = RideClassification.of(bare)
        val name = words(MinimalNameGenerator.generate(bare, 3, seed = 2L))
        assertEquals(3, name.size)
        assertTrue(name[0], MinimalWordBanks.skyColours.getValue(c.time).contains(name[0]))
    }

    @Test
    fun `an unrecognised weather code behaves like missing weather`() {
        // WMO code 4 is not mapped, so WeatherCondition.fromWmo returns UNKNOWN.
        val unknown = ride.copy(weather = WeatherSnapshot(9.0, 12.0, 4))
        val c = RideClassification.of(unknown)
        val name = words(MinimalNameGenerator.generate(unknown, 3, seed = 2L))
        assertEquals(3, name.size)
        assertTrue(name[0], MinimalWordBanks.skyColours.getValue(c.time).contains(name[0]))
    }

    @Test
    fun `colour and time words are never identical`() {
        listOf(ride, ride.copy(weather = null)).forEach { stats ->
            (1L..60L).forEach { seed ->
                val name = words(MinimalNameGenerator.generate(stats, 3, seed = seed))
                assertNotEquals(name.joinToString(" "), name[0], name[1])
            }
        }
    }

    @Test
    fun `never produces a blank name`() {
        (1L..30L).forEach { seed ->
            (1..3).forEach { count ->
                assertTrue(MinimalNameGenerator.generate(ride, count, seed = seed).isNotBlank())
            }
        }
    }
}
```

- [ ] **Step 2: Commit the test**

```bash
cd ~/github/karoo-ride-namer
git add app/src/test/kotlin/com/duncanbottrill/ridenamer/name/MinimalNameGeneratorTest.kt
git commit -m "test: MinimalNameGenerator word counts and fallbacks"
```

- [ ] **Step 3: Write the generator**

Create `app/src/main/kotlin/com/duncanbottrill/ridenamer/name/MinimalNameGenerator.kt`:

```kotlin
package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import kotlin.random.Random

/**
 * Terse names built from three single words — a weather colour, a time of day and a pace:
 *
 *     1 -> "Slate"    2 -> "Slate Crawl"    3 -> "Slate Midnight Crawl"
 *
 * The colour always survives, so even a one-word name carries the ride's most distinctive
 * signal; the time word is the first thing dropped. Deterministic for a given seed, like
 * the other generators.
 */
object MinimalNameGenerator {

    const val MIN_WORDS = 1
    const val MAX_WORDS = 3
    const val DEFAULT_WORDS = 3

    fun generate(stats: RideStats, wordCount: Int = DEFAULT_WORDS, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random.Default
        val words = wordCount.coerceIn(MIN_WORDS, MAX_WORDS)
        val c = RideClassification.of(stats)

        val timeWord = MinimalWordBanks.times.getValue(c.time).random(rng)
        // No weather (or a WMO code we don't recognise) falls back to the sky's colour at
        // that hour, so the user's chosen word count is honoured either way.
        val colourBank = c.condition?.let { MinimalWordBanks.colours(it, c.temp) }
            ?: MinimalWordBanks.skyColours.getValue(c.time)
        val colourWord = pickDistinct(colourBank, avoid = timeWord, rng = rng)
        val speedWord = MinimalWordBanks.speeds.getValue(c.speed).random(rng)

        return when (words) {
            1 -> colourWord
            2 -> "$colourWord $speedWord"
            else -> "$colourWord $timeWord $speedWord"
        }
    }

    /** Stops a name reading "Midnight Midnight Crawl" if a bank ever overlaps the time words. */
    private fun pickDistinct(bank: List<String>, avoid: String, rng: Random): String {
        val options = bank.filter { !it.equals(avoid, ignoreCase = true) }
        return if (options.isEmpty()) bank.first() else options.random(rng)
    }
}
```

- [ ] **Step 4: Commit**

```bash
cd ~/github/karoo-ride-namer
git add app/src/main/kotlin/com/duncanbottrill/ridenamer/name/MinimalNameGenerator.kt
git commit -m "feat: add MinimalNameGenerator"
```

---

### Task 4: NameStyle entry and dispatcher

**Files:**
- Modify: `app/src/main/kotlin/com/duncanbottrill/ridenamer/name/NameStyles.kt`
- Modify: `app/src/main/kotlin/com/duncanbottrill/ridenamer/ui/RideNamerApp.kt:141` (call-site fix — see the warning below)
- Test: `app/src/test/kotlin/com/duncanbottrill/ridenamer/name/DescriptiveNameGeneratorTest.kt` (extend)

**Interfaces:**
- Consumes: `MinimalNameGenerator` (Task 3).
- Produces: `NameStyle.MINIMAL`, and `generateRideName(stats, style, wordCount, seed)` — used by Tasks 6 and 7.

> **Warning — silent breakage.** `generateRideName` gains a third positional parameter *before* `seed`. `RideNamerApp.kt:141` currently calls it positionally as `generateRideName(SAMPLE_RIDE_STATS, style, seed)`, which would compile fine and bind the seed to `wordCount`. Step 4 of this task fixes it. Do not skip that step.

- [ ] **Step 1: Write the failing test**

Append these two tests inside the existing `DescriptiveNameGeneratorTest` class in
`app/src/test/kotlin/com/duncanbottrill/ridenamer/name/DescriptiveNameGeneratorTest.kt`, just before the closing brace:

```kotlin
    @Test
    fun `dispatcher routes to the minimal style with its word count`() {
        assertEquals(
            MinimalNameGenerator.generate(ride, 2, 9L),
            generateRideName(ride, NameStyle.MINIMAL, wordCount = 2, seed = 9L),
        )
    }

    @Test
    fun `word count is ignored by the other styles`() {
        assertEquals(
            generateRideName(ride, NameStyle.FUNNY, wordCount = 1, seed = 4L),
            generateRideName(ride, NameStyle.FUNNY, wordCount = 3, seed = 4L),
        )
        assertEquals(
            generateRideName(ride, NameStyle.DESCRIPTIVE, wordCount = 1, seed = 4L),
            generateRideName(ride, NameStyle.DESCRIPTIVE, wordCount = 3, seed = 4L),
        )
    }
```

- [ ] **Step 2: Commit the test**

```bash
cd ~/github/karoo-ride-namer
git add app/src/test/kotlin/com/duncanbottrill/ridenamer/name/DescriptiveNameGeneratorTest.kt
git commit -m "test: dispatcher routes the Minimal style"
```

- [ ] **Step 3: Add the style and thread the word count**

In `NameStyles.kt`, change the enum to:

```kotlin
/** The naming styles the user can choose between. */
enum class NameStyle(val label: String, val blurb: String) {
    FUNNY("Funny", "Silly, random names for a laugh"),
    DESCRIPTIVE("Descriptive", "Plain facts: place, distance, effort, weather"),
    MINIMAL("Minimal", "A colour, a time, a pace — in as few words as you like"),
    ;
```

and change `generateRideName` to:

```kotlin
/**
 * Single entry point: generates a ride name in the chosen [style].
 *
 * [wordCount] applies to [NameStyle.MINIMAL] only; the other styles ignore it.
 */
fun generateRideName(
    stats: RideStats,
    style: NameStyle,
    wordCount: Int = MinimalNameGenerator.DEFAULT_WORDS,
    seed: Long? = null,
): String = when (style) {
    NameStyle.FUNNY -> RideNameGenerator.generate(stats, seed)
    NameStyle.DESCRIPTIVE -> DescriptiveNameGenerator.generate(stats, seed)
    NameStyle.MINIMAL -> MinimalNameGenerator.generate(stats, wordCount, seed)
}
```

Note the doc comment above the enum changes from "The two naming styles" to "The naming styles".

- [ ] **Step 4: Fix the positional call site**

In `app/src/main/kotlin/com/duncanbottrill/ridenamer/ui/RideNamerApp.kt`, inside `DemoCard`, change:

```kotlin
    val sample = remember(style, seed) { generateRideName(SAMPLE_RIDE_STATS, style, seed) }
```

to:

```kotlin
    val sample = remember(style, seed) { generateRideName(SAMPLE_RIDE_STATS, style, seed = seed) }
```

This is a holding fix that keeps behaviour identical; Task 7 replaces this line properly.

- [ ] **Step 5: Verify no other positional call sites exist**

```bash
cd ~/github/karoo-ride-namer && grep -rn "generateRideName(" app/src --include='*.kt'
```

Expected: three hits — the declaration in `NameStyles.kt`, the `DemoCard` line you just fixed, and `RideNamerExtension.kt:108` (which already uses `seed = ...` and is safe). Plus the test-file hits, all of which use named arguments.

- [ ] **Step 6: Commit**

```bash
cd ~/github/karoo-ride-namer
git add app/src/main/kotlin/com/duncanbottrill/ridenamer/name/NameStyles.kt \
        app/src/main/kotlin/com/duncanbottrill/ridenamer/ui/RideNamerApp.kt
git commit -m "feat: add MINIMAL name style and word count parameter"
```

---

### CI Checkpoint 1 — all logic and tests

Everything testable is now written. This is the first real verification.

- [ ] **Step 1: Ask the user before pushing**

Pushing is outward-facing. Ask: *"Ready to push `feat/minimal-name-style` to run the tests on CI?"* Wait for a yes.

- [ ] **Step 2: Push**

```bash
cd ~/github/karoo-ride-namer && git push -u origin feat/minimal-name-style
```

- [ ] **Step 3: Watch the run**

```bash
cd ~/github/karoo-ride-namer && gh run watch --exit-status
```

Expected: the `Build` workflow completes green. It runs `./gradlew :app:assembleRelease testDebugUnitTest`, so this covers both compilation and the unit tests.

- [ ] **Step 4: If it fails, read the log before changing anything**

```bash
cd ~/github/karoo-ride-namer && gh run view --log-failed
```

Fix, commit, push, and re-watch. Do not start Task 5 until this is green.

---

### Task 5: Persist the word count

**Files:**
- Modify: `app/src/main/kotlin/com/duncanbottrill/ridenamer/data/RideNamerStore.kt`

**Interfaces:**
- Consumes: `MinimalNameGenerator.MIN_WORDS/MAX_WORDS/DEFAULT_WORDS` (Task 3).
- Produces: `RideNamerStore.minimalWordCount: Flow<Int>` and `suspend fun setMinimalWordCount(count: Int)` — used by Tasks 6 and 7.

**No unit test.** `RideNamerStore` wraps Android DataStore and needs an instrumented test to exercise; there is no existing test for it and adding an instrumentation harness is out of scope for this feature. The clamping logic it relies on is already covered by `MinimalNameGeneratorTest.out of range word counts are coerced`. Verification is Checkpoint 2 plus the manual check in Task 8.

- [ ] **Step 1: Add the import**

In `RideNamerStore.kt`, add alongside the existing `androidx.datastore.preferences.core` imports:

```kotlin
import androidx.datastore.preferences.core.intPreferencesKey
```

and alongside the existing `NameStyle` import:

```kotlin
import com.duncanbottrill.ridenamer.name.MinimalNameGenerator
```

- [ ] **Step 2: Add the preference key**

Below `private val keyNameStyle = stringPreferencesKey("name_style")`:

```kotlin
    private val keyMinimalWordCount = intPreferencesKey("minimal_word_count")
```

- [ ] **Step 3: Add the flow and setter**

In the `// --- Name style ---` section, after `setNameStyle`:

```kotlin
    /**
     * How many words the Minimal style uses. Clamped on read as well as write so a value
     * written by an older or buggier build can't break naming at the end of a ride.
     */
    val minimalWordCount: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[keyMinimalWordCount] ?: MinimalNameGenerator.DEFAULT_WORDS).clampWords()
    }

    suspend fun setMinimalWordCount(count: Int) {
        context.dataStore.edit { it[keyMinimalWordCount] = count.clampWords() }
    }

    private fun Int.clampWords() =
        coerceIn(MinimalNameGenerator.MIN_WORDS, MinimalNameGenerator.MAX_WORDS)
```

- [ ] **Step 4: Commit**

```bash
cd ~/github/karoo-ride-namer
git add app/src/main/kotlin/com/duncanbottrill/ridenamer/data/RideNamerStore.kt
git commit -m "feat: persist the Minimal style word count"
```

---

### Task 6: Wire the extension

**Files:**
- Modify: `app/src/main/kotlin/com/duncanbottrill/ridenamer/RideNamerExtension.kt` (around line 107)

**Interfaces:**
- Consumes: `RideNamerStore.minimalWordCount` (Task 5), `generateRideName(..., wordCount, ...)` (Task 4).
- Produces: nothing new.

- [ ] **Step 1: Read the word count and pass it through**

Replace:

```kotlin
        val style = store.nameStyle.first()
        val name = generateRideName(stats, style, seed = stats.endEpochMs)
        Log.i(TAG, "Generated name ($style): $name")
```

with:

```kotlin
        val style = store.nameStyle.first()
        val wordCount = store.minimalWordCount.first()
        val name = generateRideName(stats, style, wordCount, seed = stats.endEpochMs)
        Log.i(TAG, "Generated name ($style): $name")
```

- [ ] **Step 2: Commit**

```bash
cd ~/github/karoo-ride-namer
git add app/src/main/kotlin/com/duncanbottrill/ridenamer/RideNamerExtension.kt
git commit -m "feat: use the stored word count when naming a finished ride"
```

---

### Task 7: Word count selector in the UI

**Files:**
- Modify: `app/src/main/kotlin/com/duncanbottrill/ridenamer/ui/RideNamerApp.kt`

**Interfaces:**
- Consumes: `RideNamerStore.minimalWordCount` / `setMinimalWordCount` (Task 5), `NameStyle.MINIMAL` and `generateRideName` (Task 4), `MinimalNameGenerator` constants (Task 3).
- Produces: nothing new.

- [ ] **Step 1: Add the import**

Alongside the other `com.duncanbottrill.ridenamer.name` imports:

```kotlin
import com.duncanbottrill.ridenamer.name.MinimalNameGenerator
```

`Row` and `Arrangement` are already imported; no other import changes are needed.

- [ ] **Step 2: Collect the word count and pass it down**

In `RideNamerApp`, after the existing `val style by ...` line:

```kotlin
                val wordCount by store.minimalWordCount
                    .collectAsState(initial = MinimalNameGenerator.DEFAULT_WORDS)
```

Then replace the two `item { ... }` blocks for the style and demo cards:

```kotlin
                    item {
                        StyleCard(
                            current = style,
                            wordCount = wordCount,
                            onSelect = { picked ->
                                scope.launch(Dispatchers.IO) { store.setNameStyle(picked) }
                            },
                            onWordCount = { count ->
                                scope.launch(Dispatchers.IO) { store.setMinimalWordCount(count) }
                            },
                        )
                    }
                    item { DemoCard(style, wordCount) }
```

- [ ] **Step 3: Replace StyleCard**

Replace the whole `StyleCard` composable with:

```kotlin
@Composable
private fun StyleCard(
    current: NameStyle,
    wordCount: Int,
    onSelect: (NameStyle) -> Unit,
    onWordCount: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Name style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            NameStyle.entries.forEach { option ->
                val selected = option == current
                val modifier = Modifier.fillMaxWidth()
                if (selected) {
                    Button(onClick = { onSelect(option) }, modifier = modifier) { Text(option.label) }
                } else {
                    OutlinedButton(onClick = { onSelect(option) }, modifier = modifier) { Text(option.label) }
                }
            }
            Text(
                current.blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (current == NameStyle.MINIMAL) {
                Text("How many words?", style = MaterialTheme.typography.labelMedium, color = Accent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (MinimalNameGenerator.MIN_WORDS..MinimalNameGenerator.MAX_WORDS).forEach { count ->
                        val modifier = Modifier.weight(1f)
                        if (count == wordCount) {
                            Button(onClick = { onWordCount(count) }, modifier = modifier) { Text("$count") }
                        } else {
                            OutlinedButton(onClick = { onWordCount(count) }, modifier = modifier) { Text("$count") }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Update DemoCard**

Replace the first two lines of the `DemoCard` composable:

```kotlin
@Composable
private fun DemoCard(style: NameStyle) {
    var seed by remember { mutableStateOf(System.nanoTime()) }
    val sample = remember(style, seed) { generateRideName(SAMPLE_RIDE_STATS, style, seed = seed) }
```

with:

```kotlin
@Composable
private fun DemoCard(style: NameStyle, wordCount: Int) {
    var seed by remember { mutableStateOf(System.nanoTime()) }
    val sample = remember(style, wordCount, seed) {
        generateRideName(SAMPLE_RIDE_STATS, style, wordCount, seed)
    }
```

Including `wordCount` in the `remember` key is what makes the sample update the instant the user taps 1, 2 or 3, rather than waiting for a Shuffle.

- [ ] **Step 5: Commit**

```bash
cd ~/github/karoo-ride-namer
git add app/src/main/kotlin/com/duncanbottrill/ridenamer/ui/RideNamerApp.kt
git commit -m "feat: word count selector for the Minimal style"
```

---

### CI Checkpoint 2 — full build

- [ ] **Step 1: Ask the user before pushing**

Ask: *"Ready to push the UI and wiring commits for the second CI run?"* Wait for a yes.

- [ ] **Step 2: Push**

```bash
cd ~/github/karoo-ride-namer && git push
```

- [ ] **Step 3: Watch the run**

```bash
cd ~/github/karoo-ride-namer && gh run watch --exit-status
```

Expected: green. `assembleRelease` compiles the Compose UI, so this is where a `StyleCard` or `DemoCard` mistake surfaces.

- [ ] **Step 4: If it fails**

```bash
cd ~/github/karoo-ride-namer && gh run view --log-failed
```

Fix, commit, push, re-watch.

---

### Task 8: README and handoff

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Check how styles are described**

```bash
cd ~/github/karoo-ride-namer && grep -n -i "funny\|descriptive\|style" README.md
```

- [ ] **Step 2: Document the new style**

Add Minimal wherever the other two styles are listed, matching the surrounding tone and formatting. Cover: what it produces, that the user picks 1–3 words, that the default is 3, and give a worked example such as `Slate Midnight Crawl`. If the README does not list the styles at all, skip this task and say so rather than inventing a section.

- [ ] **Step 3: Commit and push**

```bash
cd ~/github/karoo-ride-namer
git add README.md
git commit -m "docs: describe the Minimal name style"
git push
```

- [ ] **Step 4: Report to the user**

Summarise: what was built, the CI run result with its URL, and the two things that still need a human because there is no local Android SDK or device:

1. **Sideload and check the UI** — the 1/2/3 selector only appears when Minimal is selected, the sample updates on tap, and the setting survives an app restart. `RideNamerStore` has no automated coverage, so this is the only check on persistence.
2. **Finish a real ride** and confirm the generated name has the expected number of words.

Then ask whether they want a PR opened or the branch merged to `main`.

---

## Notes for the implementer

- **Do not push without asking.** Both checkpoints and Task 8 push to a shared remote.
- **The repo's default branch is `main`** and this work happens on `feat/minimal-name-style`. Do not commit to `main`.
- **`SAMPLE_RIDE_STATS`** (62 km, 940 m, 26.5 km/h, rain at 7 °C) classifies as HILLY / SWIFT / RAIN-cold, so the in-app sample draws from the rain colours and the swift speeds — e.g. `Slate Dusk Sprint`. If the sample looks wrong in the emulator, that is the expected shape.
- **Adding words later** is safe: the bank tests enforce the single-token and no-collision rules, so a bad addition fails CI rather than shipping a two-word "word".
