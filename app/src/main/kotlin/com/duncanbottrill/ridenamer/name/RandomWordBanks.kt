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
 * Size matters more than it looks. A one-word name draws from this list directly, so the
 * pool size *is* the number of possible names, and repeats arrive on the birthday-paradox
 * curve — roughly at the square root of the pool. At 121 words a one-word name repeated
 * every ~13 rides; the test now floors the pool at 500 to keep that honest.
 *
 * Add whatever you like. Longer list, better names.
 */
object RandomWordBanks {

    val words: List<String> = listOf(
        // Things you can hold
        "Velvet", "Lantern", "Anvil", "Kettle", "Compass", "Marble", "Ribbon", "Trumpet",
        "Domino", "Satchel", "Zipper", "Piano", "Mirror", "Candle", "Hammock", "Pebble",
        "Wrench", "Teapot", "Balloon", "Kazoo", "Umbrella", "Telescope", "Accordion",
        "Bucket", "Ladder", "Cushion", "Bracket", "Trinket", "Locket", "Goblet", "Chalice",
        "Tripod", "Spindle", "Bobbin", "Thimble", "Trowel", "Mallet", "Chisel", "Cleaver",
        "Skillet", "Cauldron", "Flask", "Decanter", "Tureen", "Colander", "Whisk", "Ladle",
        "Corkscrew", "Paperclip", "Inkwell", "Quill", "Parchment", "Tinderbox", "Bellows",
        // Creatures
        "Cobra", "Otter", "Magpie", "Walrus", "Gecko", "Puffin", "Badger", "Falcon",
        "Lobster", "Mongoose", "Heron", "Weasel", "Bison", "Newt", "Albatross", "Wombat",
        "Pelican", "Ferret", "Marmot", "Osprey", "Lemur", "Tapir", "Okapi", "Narwhal",
        "Manatee", "Axolotl", "Pangolin", "Armadillo", "Porcupine", "Chinchilla", "Meerkat",
        "Capybara", "Wallaby", "Kestrel", "Buzzard", "Curlew", "Lapwing", "Godwit",
        "Sandpiper", "Cormorant", "Stoat", "Vole", "Shrew", "Dormouse", "Hedgehog",
        "Salamander", "Terrapin", "Barnacle", "Limpet", "Cuttlefish", "Starling", "Chaffinch",
        "Wagtail", "Jackdaw", "Guillemot", "Razorbill", "Ocelot", "Platypus", "Echidna",
        // Edible
        "Marmalade", "Biscuit", "Pickle", "Tangerine", "Custard", "Waffle", "Pretzel",
        "Nutmeg", "Rhubarb", "Truffle", "Gherkin", "Crumpet", "Parsnip", "Toffee",
        "Liquorice", "Dumpling", "Paprika", "Sherbet", "Marzipan", "Nougat", "Praline",
        "Brioche", "Baguette", "Focaccia", "Ciabatta", "Pancake", "Flapjack", "Scone",
        "Muffin", "Strudel", "Eclair", "Meringue", "Trifle", "Sorbet", "Gelato", "Chutney",
        "Mustard", "Wasabi", "Cardamom", "Cinnamon", "Turmeric", "Oregano", "Rosemary",
        "Marjoram", "Coriander", "Fennel", "Chicory", "Radish", "Artichoke", "Aubergine",
        "Courgette", "Beetroot", "Shallot", "Tamarind", "Pomegranate", "Persimmon",
        // Weather and sky
        "Thunder", "Cyclone", "Monsoon", "Eclipse", "Comet", "Meteor", "Tempest",
        "Avalanche", "Mirage", "Aurora", "Blizzard", "Squall", "Typhoon", "Zephyr",
        "Sirocco", "Nebula", "Quasar", "Pulsar", "Galaxy", "Cosmos", "Solstice", "Equinox",
        "Horizon", "Cirrus", "Cumulus", "Stratus", "Hailstone", "Snowdrift", "Sunbeam",
        "Moonbeam", "Starfall", "Skyline",
        // Places and landscape
        "Fjord", "Canyon", "Tundra", "Lagoon", "Meadow", "Quarry", "Isthmus", "Delta",
        "Prairie", "Grotto", "Bazaar", "Harbour", "Atoll", "Archipelago", "Peninsula",
        "Plateau", "Savannah", "Steppe", "Oasis", "Dune", "Crevasse", "Ravine", "Gorge",
        "Gully", "Thicket", "Copse", "Glade", "Marsh", "Fenland", "Moorland", "Heath",
        "Hollow", "Knoll", "Ridge", "Summit", "Crag", "Scree", "Cairn", "Causeway",
        "Viaduct", "Aqueduct", "Belfry", "Cloister", "Citadel", "Rampart", "Turret",
        "Alcove", "Veranda", "Catacomb", "Cellar", "Attic", "Cupola",
        // Abstract
        "Paradox", "Riddle", "Rumour", "Whisper", "Fable", "Omen", "Echo", "Fiasco",
        "Quandary", "Serenade", "Jubilee", "Escapade", "Kerfuffle", "Debacle", "Reverie",
        "Gambit", "Requiem", "Nonsense", "Alibi", "Verdict", "Epiphany", "Nostalgia",
        "Serendipity", "Zeitgeist", "Vertigo", "Limbo", "Tangent", "Vortex", "Spiral",
        "Cipher", "Enigma", "Conundrum", "Anomaly", "Interlude", "Prologue", "Epilogue",
        "Sonnet", "Ballad", "Limerick", "Anthem", "Chorus", "Cadence", "Crescendo",
        "Staccato", "Tremolo", "Rhapsody", "Lullaby", "Overture", "Fanfare", "Mayhem",
        "Aftermath", "Momentum", "Inertia", "Parallax", "Symmetry", "Entropy",
        // Machines and instruments
        "Obelisk", "Gargoyle", "Windmill", "Turbine", "Zeppelin", "Catapult", "Periscope",
        "Harpsichord", "Xylophone", "Sundial", "Kaleidoscope", "Bagpipe", "Chandelier",
        "Guillotine", "Portcullis", "Tambourine", "Wheelbarrow", "Trebuchet", "Ballista",
        "Gyroscope", "Metronome", "Barometer", "Thermostat", "Sextant", "Astrolabe",
        "Abacus", "Typewriter", "Gramophone", "Phonograph", "Calliope", "Ocarina",
        "Bassoon", "Oboe", "Clarinet", "Trombone", "Tuba", "Banjo", "Ukulele", "Mandolin",
        "Sitar", "Marimba", "Timpani", "Glockenspiel", "Theremin", "Concertina", "Piccolo",
        "Cornet", "Cymbal", "Harmonium", "Funicular", "Escalator", "Dynamo",
        // Plants
        "Cactus", "Fern", "Thistle", "Bramble", "Nettle", "Clover", "Daisy", "Foxglove",
        "Bluebell", "Primrose", "Buttercup", "Dandelion", "Hyacinth", "Marigold", "Petunia",
        "Begonia", "Orchid", "Lupin", "Delphinium", "Hollyhock", "Snapdragon", "Wisteria",
        "Jasmine", "Lavender", "Mistletoe", "Holly", "Willow", "Alder", "Birch", "Rowan",
        "Hawthorn", "Sycamore", "Juniper", "Cypress", "Redwood", "Baobab", "Mangrove",
        "Bulrush", "Sequoia", "Bracken", "Toadstool", "Foxtail",
        // Mythic
        "Griffin", "Phoenix", "Kraken", "Basilisk", "Chimera", "Minotaur", "Centaur",
        "Sphinx", "Hydra", "Cyclops", "Golem", "Banshee", "Wraith", "Spectre",
        "Poltergeist", "Gremlin", "Goblin", "Troll", "Ogre", "Pixie", "Sprite", "Nymph",
        "Dryad", "Siren", "Valkyrie", "Titan", "Oracle", "Prophet", "Alchemy", "Talisman",
        "Amulet", "Rune", "Sigil", "Grimoire", "Elixir", "Potion", "Incantation",
        "Prophecy", "Labyrinth", "Leviathan",
        // Worn
        "Poncho", "Kimono", "Kaftan", "Sarong", "Cardigan", "Waistcoat", "Dungarees",
        "Galoshes", "Slipper", "Mitten", "Muffler", "Cravat", "Bowtie", "Fedora", "Trilby",
        "Beret", "Bonnet", "Tiara", "Monocle", "Spectacles", "Bandana", "Turban", "Anorak",
        "Cagoule", "Gaiters", "Brogues", "Clogs", "Sandal", "Tunic", "Doublet",
        // Sounds and motion
        "Clatter", "Rustle", "Murmur", "Babble", "Chatter", "Giggle", "Snicker", "Guffaw",
        "Hiccup", "Sneeze", "Yawn", "Wobble", "Waddle", "Tumble", "Stumble", "Fumble",
        "Jumble", "Rumble", "Grumble", "Mumble", "Bumble", "Scramble", "Squabble",
        "Wriggle", "Jiggle", "Wiggle", "Twiddle", "Fiddle", "Doodle", "Kerplunk",
        // Whimsy
        "Bagatelle", "Bibelot", "Doodad", "Gizmo", "Widget", "Contraption", "Apparatus",
        "Rigmarole", "Hullabaloo", "Brouhaha", "Palaver", "Shenanigan", "Malarkey",
        "Balderdash", "Poppycock", "Codswallop", "Twaddle", "Gobbledegook", "Flummox",
        "Bamboozle", "Skedaddle", "Whatnot", "Thingamajig", "Bricabrac", "Knickknack",
        "Curio", "Oddity", "Whimsy", "Rumpus", "Caper",
    )
}
