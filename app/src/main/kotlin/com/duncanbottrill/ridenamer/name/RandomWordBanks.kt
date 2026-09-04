package com.duncanbottrill.ridenamer.name

/**
 * The word pool for the Random style. Unlike every other bank in this package these words
 * mean nothing — they are not keyed off the weather, the clock or the ride. That is the
 * whole point: "Velvet Marmalade Thunder" is funny because it is unearned.
 *
 * Two rules, enforced by RandomWordBanksTest:
 *
 *  1. Every entry is a single token with no whitespace, so the user's "1, 2 or 3 words"
 *     choice is literally true.
 *  2. No duplicates, so a bigger pool always means more variety rather than a heavier thumb
 *     on one word.
 *
 * Add whatever you like. Longer list, better names.
 */
object RandomWordBanks {

    val words: List<String> = listOf(
        // Things you can hold
        "Velvet", "Lantern", "Anvil", "Kettle", "Compass", "Marble", "Ribbon", "Trumpet",
        "Domino", "Satchel", "Zipper", "Piano", "Mirror", "Candle", "Hammock", "Pebble",
        "Wrench", "Teapot", "Balloon", "Kazoo", "Umbrella", "Telescope", "Accordion",
        // Creatures
        "Cobra", "Otter", "Magpie", "Walrus", "Gecko", "Puffin", "Badger", "Falcon",
        "Lobster", "Mongoose", "Heron", "Weasel", "Bison", "Newt", "Albatross", "Wombat",
        "Pelican", "Ferret", "Marmot", "Osprey",
        // Edible
        "Marmalade", "Biscuit", "Pickle", "Tangerine", "Custard", "Waffle", "Pretzel",
        "Nutmeg", "Rhubarb", "Truffle", "Gherkin", "Crumpet", "Parsnip", "Toffee",
        "Liquorice", "Dumpling", "Paprika", "Sherbet",
        // Weather and sky
        "Thunder", "Cyclone", "Monsoon", "Eclipse", "Comet", "Meteor", "Tempest",
        "Avalanche", "Mirage", "Aurora",
        // Places and landscape
        "Fjord", "Canyon", "Tundra", "Lagoon", "Meadow", "Quarry", "Isthmus", "Delta",
        "Prairie", "Grotto", "Bazaar", "Harbour",
        // Abstract
        "Paradox", "Riddle", "Rumour", "Whisper", "Fable", "Omen", "Echo", "Fiasco",
        "Quandary", "Serenade", "Jubilee", "Escapade", "Kerfuffle", "Debacle", "Reverie",
        "Gambit", "Requiem", "Nonsense", "Alibi", "Verdict",
        // Odds and ends
        "Cactus", "Obelisk", "Gargoyle", "Windmill", "Turbine", "Zeppelin", "Catapult",
        "Periscope", "Harpsichord", "Xylophone", "Sundial", "Kaleidoscope", "Bagpipe",
        "Chandelier", "Guillotine", "Portcullis", "Tambourine", "Wheelbarrow",
    )
}
