package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.WeatherCondition

/**
 * Word lists keyed by the ride's character. The generator stitches these into
 * names. Add your own — every list is fair game and longer lists = more variety.
 */
object WordBanks {

    val distanceNoun = mapOf(
        DistanceBand.SPIN to listOf("Spin", "Loop", "Pootle", "Lap", "Leg-Loosener", "Coffee Run", "Wiggle"),
        DistanceBand.SHORT to listOf("Jaunt", "Outing", "Spin", "Nip", "Quickie", "Dash"),
        DistanceBand.MEDIUM to listOf("Ride", "Tour", "Cruise", "Roll", "Romp", "Excursion"),
        DistanceBand.LONG to listOf("Adventure", "Expedition", "Voyage", "Odyssey", "Haul", "Saga"),
        DistanceBand.EPIC to listOf("Epic", "Odyssey", "Pilgrimage", "Grand Tour", "Death March", "Saga", "Marathon"),
    )

    val climbAdj = mapOf(
        ClimbBand.FLAT to listOf("Pancake-Flat", "Billiard-Table", "Sea-Level", "Horizontal", "Gravity-Optional", "Featureless"),
        ClimbBand.ROLLING to listOf("Rolling", "Lumpy", "Undulating", "Wavy", "Speed-Bump"),
        ClimbBand.HILLY to listOf("Hilly", "Leg-Sapping", "Up-and-Down", "Quad-Burning", "Punchy"),
        ClimbBand.MOUNTAINOUS to listOf("Vertical", "Mountainous", "Lung-Busting", "Alpine", "Goat-Track", "Everest-Adjacent"),
    )

    val climbPhrase = mapOf(
        ClimbBand.FLAT to listOf("not a hill in sight", "gravity took the day off", "flatter than a forgotten pancake"),
        ClimbBand.ROLLING to listOf("just enough lumps to notice", "death by a thousand bumps", "rolling like the credits"),
        ClimbBand.HILLY to listOf("the hills had opinions", "every road went up, somehow", "legs filed a complaint"),
        ClimbBand.MOUNTAINOUS to listOf("the road pointed at the sky", "Sir Edmund would be proud", "gravity sent an invoice"),
    )

    val intensityAdj = mapOf(
        IntensityBand.CHILL to listOf("Leisurely", "Casual", "Café-Pace", "Recovery", "Z2 Therapy", "Gentle"),
        IntensityBand.STEADY to listOf("Steady", "Tempo", "Honest", "No-Nonsense", "Workmanlike"),
        IntensityBand.HARD to listOf("Spicy", "Hard", "Sweaty", "Gut-Check", "Threshold"),
        IntensityBand.SAVAGE to listOf("Savage", "Eyeballs-Out", "Full-Gas", "Brutal", "Soul-Crushing", "Maximal"),
    )

    val intensityNoun = mapOf(
        IntensityBand.CHILL to listOf("Recovery Spin", "Coffee Ride", "Soul Cruise", "Active Recovery", "Vibes Session"),
        IntensityBand.STEADY to listOf("Tempo Grind", "Steady State", "Diesel Effort", "Honest Day's Work"),
        IntensityBand.HARD to listOf("Sufferfest", "Threshold Party", "Sweat Lodge", "Gut-Check"),
        IntensityBand.SAVAGE to listOf("Sufferfest", "Pain Cave Express", "Lactate Bath", "World of Hurt", "Death March"),
    )

    val weatherAdj = mapOf(
        WeatherCondition.CLEAR to listOf("Sun-Drenched", "Bluebird", "Golden", "Glorious", "Postcard"),
        WeatherCondition.CLOUDY to listOf("Grey", "Overcast", "Moody", "Gloomy", "Flat-Light"),
        WeatherCondition.FOG to listOf("Foggy", "Misty", "Murky", "Pea-Soup", "Ghostly"),
        WeatherCondition.DRIZZLE to listOf("Drizzly", "Damp", "Soggy", "Mizzly", "Clammy"),
        WeatherCondition.RAIN to listOf("Soggy", "Drowned", "Rain-Lashed", "Sodden", "Biblical-Rain"),
        WeatherCondition.SNOW to listOf("Snowy", "Frostbitten", "Arctic", "Frozen", "Narnia"),
        WeatherCondition.THUNDER to listOf("Storm-Chased", "Thunderstruck", "Electric", "Apocalyptic"),
        WeatherCondition.UNKNOWN to listOf("Weather-Beaten", "Atmospheric", "Meteorological"),
    )

    val weatherPhrase = mapOf(
        WeatherCondition.CLEAR to listOf("not a cloud dared show up", "sunshine tax fully paid", "tan lines incoming"),
        WeatherCondition.CLOUDY to listOf("the sun ghosted us", "fifty shades of grey sky", "vitamin D not included"),
        WeatherCondition.FOG to listOf("couldn't see the next corner", "riding inside a cloud", "spooky season energy"),
        WeatherCondition.DRIZZLE to listOf("that fine rain that soaks you through", "permanently damp socks", "neither dry nor wet"),
        WeatherCondition.RAIN to listOf("rain found every gap in the kit", "bilge pumps recommended", "the bike needs a snorkel"),
        WeatherCondition.SNOW to listOf("frostbite was a real option", "the bottles froze solid", "winter is not messing about"),
        WeatherCondition.THUNDER to listOf("outrunning the lightning", "the sky was furious", "weather warnings ignored"),
        WeatherCondition.UNKNOWN to listOf("the sky did something", "weather happened", "four seasons in one ride"),
    )

    val tempPhrase = mapOf(
        TempBand.FREEZING to listOf("Toes Optional", "Brass Monkeys", "Sub-Zero Heroics", "Numb-Fingered"),
        TempBand.COLD to listOf("Knee-Warmer Weather", "Crisp", "Bracing", "Two-Glove"),
        TempBand.MILD to listOf("Goldilocks Conditions", "Just Right", "Shorts-Maybe"),
        TempBand.WARM to listOf("Bottle-Drainer", "Sun's Out", "Warm and Willing"),
        TempBand.SCORCHING to listOf("Surface-of-the-Sun", "Heatstroke Special", "Tarmac-Melting", "Furnace"),
    )

    val timeAdj = mapOf(
        TimeBand.DAWN to listOf("Dawn Patrol", "Stupid-O'Clock", "Sunrise", "Pre-Dawn", "Early-Bird"),
        TimeBand.MORNING to listOf("Morning", "Breakfast", "A.M.", "Pre-Work"),
        TimeBand.MIDDAY to listOf("Midday", "Lunchtime", "High-Noon", "Solar-Maximum"),
        TimeBand.AFTERNOON to listOf("Afternoon", "Post-Lunch", "Golden-Hour"),
        TimeBand.EVENING to listOf("Evening", "After-Work", "Twilight", "Sundown"),
        TimeBand.NIGHT to listOf("Midnight", "Nocturnal", "Headlight", "Vampire-Shift", "After-Dark"),
    )

    /** Connectors for slotting a place name into a title. */
    val placeConnector = listOf("around", "round", "through", "across", "out of", "near")

    /** Generic crowd-pleasers, used to round out a name or stand alone. */
    val flourish = listOf(
        "Type 2 Fun", "Strava Bait", "No Regrets", "Legs Were Optional", "Send It",
        "The Reckoning", "Worth It", "Questionable Decisions", "Character Building",
        "The Long Way Home", "Because Cake", "Earning the Pastry", "Watt a Day",
    )
}
