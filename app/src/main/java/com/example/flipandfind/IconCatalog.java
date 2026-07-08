package com.example.flipandfind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Card labels used by the matching game. */
public final class IconCatalog {
    public static final int MAX_PAIR_COUNT = 50;

    public enum Category {
        RANDOM("Random", "🎲"),
        FACES("Faces", "😀"),
        ANIMALS("Animals", "🐼"),
        FOOD("Food", "🍓"),
        NATURE("Nature", "🌻"),
        ACTIVITIES("Activities", "⚽"),
        TRAVEL("Travel", "🚗"),
        OBJECTS("Objects", "💡"),
        FLAGS("Flags", "🇮🇳"),
        SYMBOLS("Symbols", "★"),
        WORDS("Words", "📝"),
        BLANK("Blank", "🎨"),
        // New categories are appended so every previously shipped ordinal stays stable.
        RUBICS("Rubics", "▦"),
        NUMBERS("Numbers", "123");

        private final String displayName;
        private final String sample;

        Category(String displayName, String sample) {
            this.displayName = displayName;
            this.sample = sample;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSample() {
            return sample;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /*
     * These sets deliberately favor the original Unicode 6-era emoji repertoire.
     * In particular they avoid skin-tone modifiers, ZWJ compositions, and recent
     * additions that commonly render as missing-glyph boxes on older Android.
     */
    private static final Map<Category, List<String>> POOLS = createPools();
    private static final Map<Category, List<List<String>>> TRICKY_GROUPS =
        createTrickyGroups();
    private static final Map<Category, List<String>> TRICKY_TRIPLES =
        createTrickyTriples();
    private static final List<Category> EMOJI_CATEGORIES = Collections.unmodifiableList(
        Arrays.asList(
            Category.FACES,
            Category.ANIMALS,
            Category.FOOD,
            Category.NATURE,
            Category.ACTIVITIES,
            Category.TRAVEL,
            Category.OBJECTS,
            Category.FLAGS,
            Category.SYMBOLS
        )
    );

    private IconCatalog() {}

    /** Returns {@code pairCount} distinct labels in deterministic, seed-controlled order. */
    public static String[] iconsFor(Category category, int pairCount, long seed) {
        validateRequest(category, pairCount);
        if (category == Category.BLANK) {
            return blankLabels(pairCount);
        }
        List<String> shuffled = new ArrayList<>(POOLS.get(category));
        Collections.shuffle(shuffled, new Random(seed));
        return shuffled.subList(0, pairCount).toArray(new String[0]);
    }

    /**
     * Convenience overload for callers that expose a Tricky-mode switch.
     * Different pair IDs still receive different labels; each card pair should
     * continue to render the same returned label twice.
     */
    public static String[] iconsFor(
        Category category,
        int pairCount,
        long seed,
        boolean tricky
    ) {
        return tricky
            ? trickyIconsFor(category, pairCount, seed)
            : iconsFor(category, pairCount, seed);
    }

    /**
     * Returns distinct but intentionally easy-to-confuse labels.
     *
     * Results are arranged in confusable groups of two before the game shuffles
     * card positions. RANDOM chooses one emoji family, and never chooses words.
     */
    public static String[] trickyIconsFor(Category category, int pairCount, long seed) {
        validateRequest(category, pairCount);
        if (category == Category.BLANK) {
            return blankLabels(pairCount);
        }

        Category selectedCategory = category;
        long selectionSeed = seed;
        if (category == Category.RANDOM) {
            Random categoryRandom = new Random(seed ^ 0x62A9D9ED799705F5L);
            selectedCategory = EMOJI_CATEGORIES.get(
                categoryRandom.nextInt(EMOJI_CATEGORIES.size())
            );
            selectionSeed ^= 0x9E3779B97F4A7C15L;
        }

        List<List<String>> groups = new ArrayList<>(TRICKY_GROUPS.get(selectedCategory));
        Random random = new Random(selectionSeed);
        List<String> triple = null;
        int pairedLabelCount = pairCount;
        if (pairCount >= 3 && pairCount % 2 == 1) {
            triple = new ArrayList<>(TRICKY_TRIPLES.get(selectedCategory));
            pairedLabelCount = pairCount - triple.size();
            for (int index = groups.size() - 1; index >= 0; index--) {
                List<String> group = groups.get(index);
                if (triple.contains(group.get(0)) || triple.contains(group.get(1))) {
                    groups.remove(index);
                }
            }
            Collections.shuffle(triple, random);
        }
        Collections.shuffle(groups, random);

        List<String> labels = new ArrayList<>(pairCount);
        if (pairedLabelCount > 0) {
            for (List<String> group : groups) {
                int first = random.nextBoolean() ? 0 : 1;
                labels.add(group.get(first));
                if (labels.size() == pairedLabelCount) {
                    break;
                }
                labels.add(group.get(1 - first));
                if (labels.size() == pairedLabelCount) {
                    break;
                }
            }
        }
        if (triple != null) {
            labels.addAll(triple);
        }
        return labels.toArray(new String[0]);
    }

    /** Returns the number of distinct labels available in a category. */
    public static int availableIconCount(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Icon category is required");
        }
        return POOLS.get(category).size();
    }

    private static void validateRequest(Category category, int pairCount) {
        if (category == null) {
            throw new IllegalArgumentException("Icon category is required");
        }
        if (pairCount < 1 || pairCount > MAX_PAIR_COUNT) {
            throw new IllegalArgumentException("Pair count must be between 1 and 50");
        }
    }

    private static String[] blankLabels(int pairCount) {
        String[] labels = new String[pairCount];
        Arrays.fill(labels, "");
        return labels;
    }

    private static Map<Category, List<String>> createPools() {
        EnumMap<Category, List<String>> pools = new EnumMap<>(Category.class);

        pools.put(
            Category.FACES,
            pool(
                "😀", "😃", "😄", "😁", "😆", "😅", "😂", "😊", "😇", "😉",
                "😌", "😍", "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜",
                "😎", "😏", "😒", "😞", "😔", "😟", "😕", "😣", "😖", "😫",
                "😩", "😢", "😭", "😤", "😠", "😡", "😱", "😨", "😰", "😥",
                "😐", "😑", "😶", "😮", "😯", "😲", "😳", "😵", "😴", "😪"
            )
        );
        pools.put(
            Category.ANIMALS,
            pool(
                "🐶", "🐱", "🐭", "🐹", "🐰", "🐻", "🐼", "🐨", "🐯", "🐮",
                "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🐺",
                "🐗", "🐴", "🐝", "🐛", "🐌", "🐞", "🐜", "🐢", "🐍", "🐙",
                "🐠", "🐟", "🐡", "🐬", "🐳", "🐋", "🐊", "🐆", "🐅", "🐃",
                "🐂", "🐄", "🐎", "🐖", "🐏", "🐑", "🐐", "🐓", "🐕", "🐩"
            )
        );
        pools.put(
            Category.FOOD,
            pool(
                "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍈",
                "🍒", "🍑", "🍍", "🍅", "🍆", "🌽", "🍄", "🌰", "🍞", "🍖",
                "🍗", "🍔", "🍟", "🍕", "🍝", "🍛", "🍱", "🍣", "🍥", "🍙",
                "🍘", "🍚", "🍜", "🍲", "🍢", "🍡", "🍳", "🍦", "🍧", "🍰",
                "🎂", "🍮", "🍭", "🍬", "🍫", "☕", "🍵", "🍶", "🍷", "🍺"
            )
        );
        pools.put(
            Category.NATURE,
            pool(
                "🌱", "🌲", "🌳", "🌴", "🌵", "🌾", "🌿", "🍀", "🍁", "🍂",
                "🍃", "🌺", "🌻", "🌹", "🌷", "🌼", "🌸", "💐", "🌎", "🌍",
                "🌏", "🌕", "🌖", "🌗", "🌘", "🌑", "🌒", "🌓", "🌔", "⭐",
                "🌟", "✨", "⚡", "🔥", "💧", "🌊", "🌈", "☀", "☁", "☂",
                "☃", "⛄", "❄", "🌀", "🌄", "🌅", "🌇", "🌆", "🌃", "🌌"
            )
        );
        pools.put(
            Category.ACTIVITIES,
            pool(
                "⚽", "🏀", "🏈", "⚾", "🎾", "🏉", "🎱", "🎯", "🎳", "🎮",
                "🎲", "🎨", "🎤", "🎧", "🎼", "🎹", "🎷", "🎺", "🎸", "🎻",
                "🏆", "🏁", "🏊", "🏄", "🎿", "🏂", "🏇", "🚴", "🚵", "🎣",
                "♠", "♥", "♦", "♣", "🀄", "🎴", "🃏", "🎭", "🎬", "🎪",
                "🎵", "🎶", "🎰", "🚣", "🏃", "🚶", "💃", "⛳", "⛸", "♟"
            )
        );
        pools.put(
            Category.TRAVEL,
            pool(
                "🚗", "🚕", "🚙", "🚌", "🚎", "🚓", "🚑", "🚒", "🚐", "🚚",
                "🚛", "🚜", "🚲", "🚨", "🚔", "🚍", "🚘", "🚖", "🚡", "🚠",
                "🚟", "🚃", "🚋", "🚞", "🚝", "🚄", "🚅", "🚈", "🚂", "✈",
                "🚀", "🚁", "⛵", "🚤", "🚢", "🚉", "🚏", "🗻", "🗼", "🗽",
                "🚆", "🚇", "🚊", "🚣", "🚥", "🚦", "⛽", "♨", "🏠", "🏡"
            )
        );
        pools.put(
            Category.OBJECTS,
            pool(
                "⌚", "📱", "💻", "💽", "💾", "💿", "📀", "📷", "📹", "🎥",
                "📞", "☎", "📺", "📻", "⏰", "⌛", "⏳", "💡", "🔦", "💵",
                "💴", "💶", "💷", "💰", "💳", "💎", "🔧", "🔨", "🔩", "🔑",
                "✉", "📨", "📦", "📌", "📎", "✂", "🔒", "🔓", "🔔", "🔍",
                "🔐", "🔏", "🔎", "🔬", "🔭", "📡", "💊", "💉", "🚪", "🚿"
            )
        );
        pools.put(
            Category.FLAGS,
            pool(
                "🇮🇳", "🇺🇸", "🇬🇧", "🇨🇦", "🇦🇺", "🇯🇵", "🇨🇳", "🇫🇷", "🇩🇪", "🇮🇹",
                "🇪🇸", "🇧🇷", "🇦🇷", "🇲🇽", "🇰🇷", "🇿🇦", "🇪🇬", "🇳🇬", "🇰🇪", "🇸🇦",
                "🇦🇪", "🇸🇬", "🇲🇾", "🇮🇩", "🇹🇭", "🇻🇳", "🇵🇭", "🇳🇿", "🇳🇱", "🇧🇪",
                "🇨🇭", "🇦🇹", "🇸🇪", "🇳🇴", "🇩🇰", "🇫🇮", "🇮🇪", "🇵🇹", "🇬🇷", "🇹🇷",
                "🇳🇪", "🇵🇱", "🇷🇺", "🇷🇴", "🇹🇩", "🇲🇨", "🇨🇴", "🇪🇨", "🇲🇱", "🇬🇳"
            )
        );
        pools.put(
            Category.SYMBOLS,
            pool(
                "❤", "💛", "💚", "💙", "💜", "💔", "💕", "💞", "💓", "💗",
                "💖", "💘", "💝", "💟", "☮", "✝", "☪", "☸", "✡", "☯",
                "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑",
                "♒", "♓", "⭕", "❌", "❗", "❓", "‼", "⁉", "✅", "❎",
                "⬆", "⬇", "⬅", "➡", "↗", "↘", "↙", "↖", "🔴", "🔵"
            )
        );
        pools.put(
            Category.WORDS,
            wordPool(
                "TIGER", "APPLE", "RIVER", "HOUSE", "CLOUD", "MUSIC", "CHAIR", "TRAIN", "BEACH", "LIGHT",
                "DREAM", "STONE", "PLANT", "SMILE", "BREAD", "HORSE", "OCEAN", "GRAPE", "CLOCK", "GREEN",
                "STARS", "WATER", "FLAME", "SHELL", "BRUSH", "CROWN", "HEART", "SPOON", "PANDA", "PIZZA",
                "ROBOT", "MANGO", "LEMON", "NIGHT", "EARTH", "HAPPY", "MAGIC", "SNAKE", "WHALE", "ZEBRA",
                "BIRD", "MOUSE", "TOWER", "CANDY", "HONEY", "STORM", "FIELD", "DANCE", "PEACH", "QUEEN"
            )
        );

        pools.put(
            Category.BLANK,
            Collections.unmodifiableList(new ArrayList<>(Collections.nCopies(MAX_PAIR_COUNT, "")))
        );
        pools.put(Category.RUBICS, RubicsFaceCatalog.normalPatterns());
        pools.put(Category.NUMBERS, NumberCatalog.normalNumbers());

        // RANDOM is intentionally emoji-only. Custom-drawn faces, words, and blank cards are
        // never merged into it.
        Set<String> randomIcons = new LinkedHashSet<>();
        for (Category category : Category.values()) {
            if (category != Category.RANDOM
                && category != Category.WORDS
                && category != Category.BLANK
                && category != Category.RUBICS
                && category != Category.NUMBERS) {
                randomIcons.addAll(pools.get(category));
            }
        }
        pools.put(
            Category.RANDOM,
            Collections.unmodifiableList(new ArrayList<>(randomIcons))
        );
        return Collections.unmodifiableMap(pools);
    }

    private static Map<Category, List<List<String>>> createTrickyGroups() {
        EnumMap<Category, List<List<String>>> groups = new EnumMap<>(Category.class);
        groups.put(Category.FACES, pairs(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "😭", "😊", "😇",
            "😉", "😌", "😍", "😘", "😗", "😙", "😚", "😋", "😛", "😝",
            "😜", "😏", "😒", "😞", "😔", "😟", "😕", "😣", "😠", "😡",
            "😎", "😐", "😖", "😫", "😩", "😢", "😤", "😱", "😨", "😰",
            "😥", "😪", "😑", "😶", "😮", "😯", "😲", "😳", "😵", "😴"
        ));
        groups.put(Category.ANIMALS, pairs(
            "🐶", "🐺", "🐱", "🐯", "🐭", "🐹", "🐰", "🐼", "🐻", "🐨",
            "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥",
            "🐴", "🐗", "🐝", "🐛", "🐌", "🐞", "🐢", "🐍", "🐠", "🐟",
            "🐜", "🐃", "🐙", "🐡", "🐬", "🐳", "🐋", "🐊", "🐆", "🐅",
            "🐂", "🐄", "🐎", "🐖", "🐏", "🐑", "🐐", "🐓", "🐕", "🐩"
        ));
        groups.put(Category.FOOD, pairs(
            "🍏", "🍎", "🍊", "🍋", "🍇", "🍓", "🍈", "🍉", "🍒", "🍑",
            "🍍", "🌽", "🍅", "🍆", "🍄", "🌰", "🍞", "🍰", "🍖", "🍗",
            "🍔", "🍟", "🍕", "🍝", "🍛", "🍲", "🍱", "🍣", "🍙", "🍘",
            "🍐", "🍌", "🍥", "🍚", "🍜", "🍢", "🍡", "🍳", "🍦", "🍧",
            "🎂", "🍮", "🍭", "🍬", "🍫", "☕", "🍵", "🍶", "🍷", "🍺"
        ));
        groups.put(Category.NATURE, pairs(
            "🌱", "🌿", "🌲", "🌳", "🌴", "🌵", "🍀", "🍁", "🍂", "🍃",
            "🌺", "🌻", "🌹", "🌷", "🌼", "🌸", "🌎", "🌍", "🌏", "🌕",
            "🌖", "🌗", "🌘", "🌑", "🌒", "🌓", "🌔", "⭐", "🌟", "✨",
            "🌾", "💐", "⚡", "🔥", "💧", "🌊", "🌈", "☀", "☁", "☂",
            "☃", "⛄", "❄", "🌀", "🌄", "🌅", "🌇", "🌆", "🌃", "🌌"
        ));
        groups.put(Category.ACTIVITIES, pairs(
            "⚽", "🏀", "🏈", "⚾", "🎾", "🏉", "🎱", "🎯", "🎳", "🎮",
            "🎲", "🎴", "🎨", "🎭", "🎤", "🎧", "🎼", "🎹", "🎷", "🎺",
            "🎸", "🎻", "🏆", "🏁", "🏊", "🏄", "🎿", "🏂", "🚴", "🚵",
            "🏇", "🎣", "♠", "♥", "♦", "♣", "🀄", "🃏", "🎬", "🎪",
            "🎵", "🎶", "🎰", "🚣", "🏃", "🚶", "💃", "⛳", "⛸", "♟"
        ));
        groups.put(Category.TRAVEL, pairs(
            "🚗", "🚕", "🚙", "🚌", "🚎", "🚓", "🚑", "🚒", "🚐", "🚚",
            "🚛", "🚜", "🚲", "🚨", "🚔", "🚍", "🚘", "🚖", "🚡", "🚠",
            "🚟", "🚃", "🚋", "🚞", "🚝", "🚄", "🚅", "🚈", "🚂", "🚉",
            "✈", "🚀", "🚁", "⛵", "🚤", "🚢", "🚏", "🗻", "🗼", "🗽",
            "🚆", "🚇", "🚊", "🚣", "🚥", "🚦", "⛽", "♨", "🏠", "🏡"
        ));
        groups.put(Category.OBJECTS, pairs(
            "💽", "💾", "💿", "📀", "📷", "📹", "🎥", "📺", "📞", "☎",
            "⌚", "⏰", "⌛", "⏳", "💵", "💴", "💶", "💷", "🔒", "🔓",
            "🔧", "🔨", "📌", "📎", "✉", "📨", "🔦", "💡", "🔔", "🔍",
            "📱", "💻", "📻", "💰", "💳", "💎", "🔩", "🔑", "📦", "✂",
            "🔐", "🔏", "🔎", "🔬", "🔭", "📡", "💊", "💉", "🚪", "🚿"
        ));
        groups.put(Category.FLAGS, pairs(
            "🇮🇳", "🇳🇪", "🇮🇩", "🇵🇱", "🇮🇪", "🇮🇹", "🇫🇷", "🇷🇺", "🇩🇪", "🇧🇪",
            "🇷🇴", "🇹🇩", "🇲🇨", "🇸🇬", "🇦🇺", "🇳🇿", "🇨🇴", "🇪🇨", "🇲🇱", "🇬🇳",
            "🇨🇮", "🇲🇽", "🇳🇱", "🇱🇺", "🇳🇴", "🇮🇸", "🇸🇪", "🇫🇮", "🇯🇵", "🇧🇩",
            "🇺🇸", "🇬🇧", "🇨🇦", "🇦🇹", "🇨🇳", "🇻🇳", "🇪🇸", "🇵🇹", "🇧🇷", "🇦🇷",
            "🇰🇷", "🇨🇭", "🇿🇦", "🇰🇪", "🇪🇬", "🇸🇦", "🇳🇬", "🇲🇾", "🇦🇪", "🇹🇭"
        ));
        groups.put(Category.SYMBOLS, pairs(
            "❤", "💔", "💛", "💚", "💙", "💜", "💕", "💞", "💓", "💗",
            "💖", "💘", "💝", "💟", "☮", "☯", "✝", "☪", "☸", "✡",
            "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "❗", "❓",
            "♐", "♑", "♒", "♓", "⭕", "❌", "‼", "⁉", "✅", "❎",
            "⬆", "⬇", "⬅", "➡", "↗", "↘", "↙", "↖", "🔴", "🔵"
        ));
        groups.put(Category.WORDS, wordPairs(
            "DEER", "DEAR", "BEAR", "BARE", "PAIR", "PEAR", "MAIL", "MALE", "SEA", "SEE",
            "SUN", "SON", "ONE", "WON", "TWO", "TOO", "BLUE", "BLEW", "READ", "REED",
            "MEET", "MEAT", "ROAD", "RODE", "TAIL", "TALE", "HERE", "HEAR", "RIGHT", "WRITE",
            "PEACE", "PIECE", "WEEK", "WEAK", "SALE", "SAIL", "PLAIN", "PLANE", "HOLE", "WHOLE",
            "BREAK", "BRAKE", "FAIR", "FARE", "CELL", "SELL", "DYE", "DIE", "NEW", "KNEW"
        ));
        groups.put(Category.RUBICS, RubicsFaceCatalog.trickyGroups());
        groups.put(Category.NUMBERS, NumberCatalog.trickyGroups());
        return Collections.unmodifiableMap(groups);
    }

    private static Map<Category, List<String>> createTrickyTriples() {
        EnumMap<Category, List<String>> triples = new EnumMap<>(Category.class);
        triples.put(Category.FACES, triple("😀", "😃", "😄"));
        triples.put(Category.ANIMALS, triple("🐤", "🐣", "🐥"));
        triples.put(Category.FOOD, triple("🍙", "🍘", "🍚"));
        triples.put(Category.NATURE, triple("🌎", "🌍", "🌏"));
        triples.put(Category.ACTIVITIES, triple("⚽", "🏀", "⚾"));
        triples.put(Category.TRAVEL, triple("🚗", "🚕", "🚙"));
        triples.put(Category.OBJECTS, triple("💽", "💾", "💿"));
        triples.put(Category.FLAGS, triple("🇳🇱", "🇱🇺", "🇷🇺"));
        triples.put(Category.SYMBOLS, triple("💓", "💗", "💖"));
        triples.put(Category.WORDS, wordTriple("TO", "TWO", "TOO"));
        triples.put(Category.RUBICS, RubicsFaceCatalog.trickyTriple());
        triples.put(Category.NUMBERS, NumberCatalog.trickyTriple());
        return Collections.unmodifiableMap(triples);
    }

    private static List<String> triple(String first, String second, String third) {
        return checkedTriple(false, first, second, third);
    }

    private static List<String> wordTriple(String first, String second, String third) {
        return checkedTriple(true, first, second, third);
    }

    private static List<String> checkedTriple(
        boolean words,
        String first,
        String second,
        String third
    ) {
        List<String> labels = Arrays.asList(first, second, third);
        if (new LinkedHashSet<>(labels).size() != 3) {
            throw new IllegalStateException("Tricky triples must be distinct");
        }
        if (words) {
            assertShortWords(labels);
        }
        return Collections.unmodifiableList(labels);
    }

    private static List<String> pool(String... labels) {
        return checkedPool(false, labels);
    }

    private static List<String> wordPool(String... labels) {
        return checkedPool(true, labels);
    }

    private static List<String> checkedPool(boolean words, String... labels) {
        List<String> list = Arrays.asList(labels);
        Set<String> distinct = new LinkedHashSet<>(list);
        if (distinct.size() != list.size()) {
            throw new IllegalStateException("A card category contains a duplicate label");
        }
        if (distinct.size() < MAX_PAIR_COUNT) {
            throw new IllegalStateException(
                "A card category needs at least " + MAX_PAIR_COUNT + " labels"
            );
        }
        if (words) {
            assertShortWords(distinct);
        }
        return Collections.unmodifiableList(new ArrayList<>(distinct));
    }

    private static List<List<String>> pairs(String... labels) {
        return checkedPairs(false, labels);
    }

    private static List<List<String>> wordPairs(String... labels) {
        return checkedPairs(true, labels);
    }

    private static List<List<String>> checkedPairs(boolean words, String... labels) {
        if (labels.length < MAX_PAIR_COUNT || labels.length % 2 != 0) {
            throw new IllegalStateException("Tricky labels must contain complete pairs");
        }
        Set<String> distinct = new LinkedHashSet<>(Arrays.asList(labels));
        if (distinct.size() != labels.length) {
            throw new IllegalStateException("Tricky labels must be distinct");
        }
        if (words) {
            assertShortWords(distinct);
        }

        List<List<String>> groups = new ArrayList<>();
        for (int i = 0; i < labels.length; i += 2) {
            groups.add(Collections.unmodifiableList(Arrays.asList(labels[i], labels[i + 1])));
        }
        return Collections.unmodifiableList(groups);
    }

    private static void assertShortWords(Iterable<String> words) {
        for (String word : words) {
            if (!word.matches("[A-Z]{1,5}")) {
                throw new IllegalStateException("Words must contain at most five letters: " + word);
            }
        }
    }
}
