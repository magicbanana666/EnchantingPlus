package com.magicbanana666.eplus.enchantment.flimflam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.util.RandomSource;

/**
 * Generates silly item lore and hero names for Flim Flam.
 * <p>
 * Word lists and grammar from {@code LoreGenerator} in
 * <a href="https://github.com/OpenMods/OpenBlocks">OpenBlocks</a> by the OpenMods Team (MIT).
 */
public final class LoreGenerator {

    /** A piece of text that can be generated. */
    @FunctionalInterface
    private interface Gen {
        String generate(RandomSource random, Map<String, String> params);
    }

    private static final Gen HERO = createHeroGenerator();
    private static final Gen LORE = createLoreGenerator(HERO);

    private LoreGenerator() {
    }

    public static String generateLore(RandomSource random, String playerName, String itemName) {
        return finish(LORE.generate(random, Map.of("player", playerName, "item", itemName)));
    }

    public static String generateName(RandomSource random) {
        return finish(HERO.generate(random, Map.of()));
    }

    private static String finish(String text) {
        return capitalize(text.replaceAll("\\s+", " ").trim());
    }

    // ---- Combinators -------------------------------------------------------------------------------------------

    private static Gen of(Object part) {
        if (part instanceof Gen gen) {
            return gen;
        }
        final String text = String.valueOf(part);
        return (random, params) -> text;
    }

    private static Gen[] all(Object... parts) {
        return Arrays.stream(parts).map(LoreGenerator::of).toArray(Gen[]::new);
    }

    /** One of the parts, picked at random. */
    private static Gen alt(Object... parts) {
        final Gen[] gens = all(parts);
        return (random, params) -> gens[random.nextInt(gens.length)].generate(random, params);
    }

    /** All parts, separated by spaces. */
    private static Gen seq(Object... parts) {
        return join(" ", parts);
    }

    /** All parts, glued together. */
    private static Gen word(Object... parts) {
        return join("", parts);
    }

    private static Gen join(String separator, Object... parts) {
        final Gen[] gens = all(parts);
        return (random, params) -> {
            final List<String> out = new ArrayList<>();
            for (final Gen gen : gens) {
                out.add(gen.generate(random, params));
            }
            return String.join(separator, out);
        };
    }

    /** The part, or nothing. */
    private static Gen opt(float chance, Object part) {
        final Gen gen = of(part);
        return (random, params) -> random.nextFloat() < chance ? gen.generate(random, params) : "";
    }

    /** A parameter (EG the player name), or a fallback. */
    private static Gen sub(String key, String fallback) {
        return (random, params) -> params.getOrDefault(key, fallback);
    }

    private static Gen range(int min, int max) {
        return (random, params) -> Integer.toString(min + random.nextInt(max - min));
    }

    private static Gen map(Object part, Function<String, String> mapping) {
        final Gen gen = of(part);
        return (random, params) -> mapping.apply(gen.generate(random, params));
    }

    private static Gen capitalize(Gen gen) {
        return map(gen, LoreGenerator::capitalize);
    }

    private static Gen capitalizeFully(Gen gen) {
        return map(gen, text -> {
            final StringBuilder out = new StringBuilder(text.length());
            boolean start = true;
            for (final char c : text.toCharArray()) {
                out.append(start ? Character.toUpperCase(c) : Character.toLowerCase(c));
                start = Character.isWhitespace(c);
            }
            return out.toString();
        });
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    // ---- Grammar -----------------------------------------------------------------------------------------------

    private static Gen createHeroGenerator() {
        final Gen heroesPrefix = alt("Grunnar", "Hermann", "Sven", "Grarg", "Blarf", "Hans", "Nathan", "Oglaf", "Eric", "Bob", "Banan", "Alaric");
        final Gen heroesPostfix = alt("ish", "ilde", "monkeybutt", "son", "shvili", "berg", "bert", "us");
        final Gen heroName = word(heroesPrefix, opt(0.6f, heroesPostfix));
        final Gen heroOptional = alt("slightly", "sometimes", "mistakenly", "somehow", "part-time");
        final Gen heroAdj = alt("insane", "brave", "smelly", "philosophical", "jumping", "toothless", "burning", "heroic", "shy", "narcoleptic", "manly", "girly", "non-euclidian", "euphoric", "misanthropic", "ambivalent", "fictional", "fetishist");
        final Gen heroClass = alt("babycrusher", "wrestler", "nitpicker", "barber", "anesthesiologist", "sharpshooter", "plumber", "insurance salesman", "clown", "empiricist", "defenestrator", "visigoth", "nipple twister");
        final Gen classicHeroes = capitalizeFully(seq(heroName, "the", seq(opt(0.2f, heroOptional), heroAdj, heroClass, opt(0.2f, word("(lvl. ", range(1, 11), ")")))));

        final Gen firstName = alt("Bill", "Juliet", "Nigel", "Steve", "Parsnip", "Cucumber", "Ludwig", "Markus", "Sven", "Clark", "Carl", "Throatwobbler", "Raymond", "Nancy", "Brian", "Brunhilda", "Richard", "Rupert");
        final Gen lastNameComponent = alt("Smith", "Weston", "Banana", "Drum", "Forklift", "Ampersand", "Fruitbat", "Fhtagn", "Svenson", "Stein", "Gutenabend", "Mangrove", "Bigglesworth", "Larch", "Semicolon", "Wurst", "Nixon", "Baden", "Priapus");
        final Gen lastName = alt(lastNameComponent, word(lastNameComponent, "-", lastNameComponent));
        final Gen pseudonym = alt("Duckie", "Nosepicker", "Snort", "Bomber", "Ouch", "Anvil", "Halfslab", "Radiator", "Barbie", "Biggles", "Income Tax", "Not In Face", "Tea Time", "Twerk", "Mutalisk", "Bueno", "Sixpack", "Yellow Snow");
        final Gen namePrefix = alt("Dr.", "Rev.", "Ms", "Mr", "Prof.", "Hon.", "Sgt.", "Cmdr.", "Sir", "Lady", "Comrade", "His Magnificence", "Her Holiness", "The Right Honourable");
        final Gen middleName = alt("W.", "T.", "F.");
        final Gen nameSuffix = alt("M.Sc", "Ph.D", "OBE", "Jr.", "Sr.", "III", "II", "Esq.");

        final Gen middleStuff = seq(opt(0.1f, middleName), opt(0.6f, word("\"", pseudonym, "\"")));
        final Gen modernHeroes = seq(opt(0.4f, namePrefix), firstName, middleStuff,
                opt(0.3f, alt("von", "de", "van", "van de", "de la")), lastName, opt(0.2f, word(" ", nameSuffix)));

        return alt(classicHeroes, modernHeroes);
    }

    private static Gen createLoreGenerator(Gen heroGenerator) {
        final Gen adj1 = alt("overpowered", "misspelled", "store-brand", "unsettling", "unremarkable", "sleazy", "boring", "golden", "junky", "ergonomic", "low voltage", "many-angled");
        final Gen adj2 = alt("cursed", "legendary", "unique", "penultimate", "awesome", "suboptimal", "mighty", "ridiculously", "slightly");
        final Gen adjs = seq(opt(0.7f, adj2), adj1);
        final Gen parts = alt("codpiece", "loincloth", "tootbrush", "dental floss", "eggbeater", "rubber chicken with a pulley in the middle", "shovel", "hammoc", "panties", "spatula", "fedora");

        final Gen placeAdj = alt("deadly", "dreadful", "boring", "cheap", "backwater", "tax-free", "gluten-free", "dark", "evil", "misunderstood");
        final Gen kingdomAdjective = alt("loathing", "meat", "potatoes", "hydrocarbonates", "sweden", "slighlty unpleasant things", "herpaderp", "sobbing", "knitting");
        final Gen kingdomish = seq(opt(0.4f, placeAdj), alt("kingdom", "cave", "gorge", "convention", "pit", "bazaar", "land"));
        final Gen placeWithAdj = seq(kingdomish, "of", kingdomAdjective, opt(0.2f, seq("and", kingdomAdjective)));
        final Gen mountainName = alt("lard", "butter", "rotten eggs", "brimstone", "newts", "doom", "croc", "flipflop");
        final Gen mountain = seq(opt(0.6f, placeAdj), "Mt.", mountainName);
        final Gen hardcodedPlaces = alt("dalania", "prussia", "foobaria", "hot dog stand", "abyssinia", "zanzibar", "eastasia", "freedonia", "latveria", "woolloomooloo", "breslau", "uzbekistan", "north korea", "lower intestine", "hyperborea");
        final Gen places = capitalizeFully(alt(placeWithAdj, mountain, hardcodedPlaces));

        final Gen otherPeople = alt("youtube personalities", "dwarves", "villagers", "elves", "tax collectors", "quality testers", "boring people");
        final Gen actor = alt(heroGenerator, seq(otherPeople, "of", places));

        final Gen story = alt("that nobody cares about", seq("that previously belonged to", actor));
        final Gen epicLoot = seq(opt(0.5f, adjs), parts, story);

        final Gen created = seq(alt("repurposed from", "originally bundled with", "forged from", "not to be mistaken with"), epicLoot);
        final Gen loaned = seq("loaned to", actor);
        final Gen forgotten = seq("forgotten in", alt("post office", "loo", "deep hole", "hurry"));
        final Gen origin = alt(created, seq(alt("stolen", loaned, "imagined", forgotten, "found behind couch"), "by", heroGenerator));

        final Gen itemModifier = alt("replica of");
        final Gen itemAction = alt("beating", "bleeding", "winds", "things", word(sub("item", "thing"), "ing"), "cooking", "looting", "scrubing", "backpain", "hernia");
        final Gen itemType = alt(sub("item", "gizmo"), alt("gizmo", "thingmajig", "doodad", "tat", "thingie"));
        final Gen item = capitalize(seq(opt(0.9f, adjs), itemType, opt(0.9f, seq("of", itemAction))));
        final Gen fullItem = seq(opt(0.1f, itemModifier), item, opt(0.05f, "(TM)"));

        final Gen taunt = alt("wimp", "noob", "git", "fool", "that scoundrel", "scumbag");
        final Gen playerGet = seq(alt("stolen from", "found in", "bought in", "dug out in", "smuggled from"), places);
        final Gen ownerInfo = seq(playerGet, "by", opt(0.3f, seq(taunt, "named")), sub("player", "Frank"));

        final Gen randomItems = alt("bananas", "grapes", "hairpins", "corks", "shuffling", "squash", "penguins");
        final Gen organizationSpeciality = alt(randomItems, seq(randomItems, "and", randomItems));
        final Gen university = capitalizeFully(seq("university of", alt(hardcodedPlaces, organizationSpeciality)));
        final Gen institute = capitalizeFully(seq(alt("institute", "council", "committee"), "of", organizationSpeciality));
        final Gen foundationFirst = alt("lick", "pick", "poke", "prod", "smell", "ring", "steal", "hug", "kick", "fwap");
        final Gen foundationSecond = alt("fish", "sauce", "leopard", "pick", "smell", "mayonaise", "steal", "grave", "derp");
        final Gen foundation = seq(word(capitalizeFully(foundationFirst), "-a-", capitalizeFully(foundationSecond)), "foundation");
        final Gen organization = alt(university, foundation, institute);

        final Gen restoredInfo = seq("restored by", organization);
        final Gen recent = seq("Recently", alt(ownerInfo, restoredInfo));
        final Gen extra = alt("$1.99 each", "5 quids in plain wrapper", "Accept no substitues", "Made in China", "Batteries not included", "Patent pending");
        return word(fullItem, opt(0.5f, word(", ", origin)), opt(0.5f, word(". ", recent)), opt(0.2f, word(". ", extra)));
    }
}
