package com.nota.notes.data

// P1-B (أكتوبر 2026): قوة كلمة مرور .nota + توليد diceware — منطق خالص بلا اعتماديات (قابل للاختبار).
// قرار القائمة: إنجليزية قصيرة (نمط BIP39) — سهلة النسخ والكتابة وعابرة للغات؛ الكلمات العربية
// multibyte تُربك العد اليدوي عند الاستعادة على جهاز آخر. القائمة 243 كلمة مميزة ≈ 7.9 بت/كلمة،
// 6 كلمات ≈ 47.5 بت entropy — تُشتق عبر PBKDF2 بـ 600k دورة في BackupManager.
// (أُبقيت قائمة 243 عمدًا بدل EFF-7776: الأخيرة ملف كبير جديد بلا حاجة — 47.5 بت كافية للاستخدام الشخصي.)
object BackupPassword {
    // 0 = ضعيفة (<8 أو شائعة/متتالية)، 1 = متوسطة (8-11)، 2 = قوية (12-15 + تنوع)، 3 = ممتازة (16+ أو diceware)
    fun strengthLevel(password: String): Int {
        if (password.length < BackupManager.MIN_PASSWORD_LEN) return 0
        if (!BackupManager.isStrongPassword(password)) return 0
        var classes = 0
        if (password.any { it.isLowerCase() }) classes++
        if (password.any { it.isUpperCase() }) classes++
        if (password.any { it.isDigit() }) classes++
        if (password.any { !it.isLetterOrDigit() }) classes++
        val dicewareLike = password.contains('-') && password.split('-').size >= 4
        return when {
            password.length >= 16 || (dicewareLike && password.length >= 12) -> 3
            password.length >= 12 && classes >= 2 -> 2
            password.length >= 12 || classes >= 3 -> 2
            else -> 1
        }
    }

    // 6 words × log2(243) ≈ 47.5 bits entropy
    // At 10,000 PBKDF2-SHA256(600k) attempts/sec (10 GPUs):
    // 2^47.5 / 10,000 / 3600 / 24 ≈ 3 years
    // Acceptable for personal use. Consider 8 words for paranoid users.
    fun generateDicewarePassword(words: Int = 6): String {
        val rnd = java.security.SecureRandom()
        // بلا تكرار داخل العبارة الواحدة — Set يحجب أي سحب مكرر من SecureRandom.
        val picked = LinkedHashSet<String>()
        val n = words.coerceIn(1, WORDS.size)
        while (picked.size < n) picked += WORDS[rnd.nextInt(WORDS.size)]
        return picked.joinToString("-")
    }

    // 243 كلمة إنجليزية قصيرة مميزة صوتيًا (≈7.9 بت/كلمة)
    private val WORDS = listOf(
        "acorn", "actor", "admit", "adobe", "amber", "angel", "anthem", "apple",
        "april", "arbor", "arrow", "ashen", "atlas", "autumn", "baker", "bamboo",
        "banner", "basin", "beacon", "bench", "berry", "birch", "blade", "blanket",
        "blossom", "border", "boulder", "brave", "bread", "breeze", "bridge", "bright",
        "bronze", "brook", "bundle", "cabin", "cactus", "camera", "candle", "canvas",
        "canyon", "carpet", "carrot", "castle", "cedar", "celery", "chain", "cherry",
        "chime", "cinder", "citrus", "cliff", "clock", "cloud", "clover", "comet",
        "compass", "copper", "coral", "cosmos", "cottage", "cotton", "cougar", "crane",
        "crate", "crescent", "cricket", "crystal", "daisy", "dawn", "delta", "desert",
        "dolphin", "dusk", "eagle", "ember", "engine", "fabric", "falcon", "feather",
        "field", "figure", "flame", "flint", "forest", "forge", "fossil", "frost",
        "galaxy", "garage", "garden", "garlic", "ginger", "glacier", "globe", "granite",
        "grape", "grove", "harbor", "harvest", "hazel", "heron", "honey", "horizon",
        "horse", "igloo", "indigo", "insect", "island", "ivory", "jaguar", "jasper",
        "jungle", "juniper", "kernel", "kite", "lantern", "larch", "laser", "lattice",
        "laurel", "lemon", "lilac", "linen", "lizard", "lunar", "magnet", "mango",
        "maple", "marble", "marsh", "meadow", "melon", "mercury", "meteor", "miller",
        "mint", "mirror", "monsoon", "moss", "mountain", "nectar", "needle", "noble",
        "north", "oasis", "ocean", "olive", "onyx", "opal", "orbit", "orchard",
        "otter", "oxygen", "oyster", "panda", "paper", "parcel", "pebble", "pelican",
        "pepper", "piano", "picker", "pillar", "pilot", "pine", "pioneer", "planet",
        "plaza", "pocket", "poplar", "porch", "prairie", "prism", "puddle", "quartz",
        "radar", "raven", "ridge", "river", "robin", "rocket", "rose", "saddle",
        "saffron", "salmon", "sapphire", "savanna", "scarlet", "sentry", "shadow",
        "sierra", "silver", "slate", "solar", "spark", "spirit", "spring", "staple",
        "statue", "steam", "stone", "storm", "summit", "sunset", "tablet", "talon",
        "tango", "temple", "terra", "thunder", "timber", "topaz", "torch", "tulip",
        "tundra", "tunnel", "turtle", "umbra", "valley", "velvet", "vendor", "villa",
        "violet", "violin", "vista", "walnut", "wander", "willow", "window", "winter",
        "wisdom", "wizard", "wolf", "wren", "yarrow", "yellow", "yogurt", "zenith",
        "zebra", "zephyr", "acorns", "lark"
    )
}
