package com.nota.notes.security

import java.security.SecureRandom

/**
 * مفتاح الاسترداد (مراجعة أكتوبر 2026 — Commit 2).
 *
 * قرار معماري موثق: 12 كلمة من قائمة ثابتة (256 كلمة) بدل BIP39 الكامل —
 * السبب: BIP39 يتطلب اعتماد خارجي + 2048 كلمة + checksum؛ قائمتنا تعطي
 * 12×8 = 96 بت إنتروبي، تُخزَّن كـ hash فقط (PBKDF2-HMAC-SHA256، 310k دورة،
 * نفس PinHash) — لا تُخزَّن الكلمات نفسها أبدًا. 96 بت كافية لأن المهاجم
 * يحتاج الجهاز نفسه + تجاوز درع التخمين (10د سقف) + Keystore للفك الفعلي.
 *
 * UX الإدخال: حقل واحد كبير (12 كلمة مفصولة بمسافة) — قرار موثق: حقل واحد
 * أسهل للصق من مدير كلمات المرور من 12 حقلًا. الفشل يُحتسب في الدرع بعد
 * إدخال 12 كلمة كاملة — لا بعد كل كلمة. لا كشف جزئي: رسالة عامة فقط.
 */
object RecoveryKey {
    // قائمة 256 كلمة إنجليزية قصيرة مميزة (صغيرة ثابتة — بلا اعتماديات)
    private val WORDS = listOf(
        "amber", "anchor", "angel", "antler", "apple", "arrow", "ashen", "atlas",
        "badge", "baker", "bamboo", "banner", "barley", "basin", "beacon", "beard",
        "berry", "birch", "blade", "bloom", "bluff", "boulder", "branch", "brave",
        "bread", "breeze", "bridge", "bright", "bronze", "brook", "brown", "brush",
        "builder", "burst", "cabin", "cactus", "camel", "candle", "canyon", "carpet",
        "castle", "cedar", "chain", "cherry", "chest", "chief", "cinder", "citrus",
        "cliff", "cloak", "cloud", "clover", "cobalt", "comet", "compass", "copper",
        "coral", "cottage", "cotton", "cove", "crane", "crate", "crescent", "cricket",
        "crimson", "crowd", "crown", "crystal", "custom", "dagger", "daisy", "dawn",
        "delta", "desert", "dolphin", "dove", "dragon", "drift", "drum", "dune",
        "eagle", "earth", "ember", "engine", "fabric", "falcon", "feather", "ferry",
        "field", "flame", "flint", "forest", "forge", "fountain", "frost", "galaxy",
        "garnet", "giant", "ginger", "glacier", "glove", "golden", "goose", "granite",
        "grape", "grove", "guard", "guild", "harbor", "harvest", "hazel", "heron",
        "honey", "horizon", "horse", "hound", "hunter", "island", "ivory", "jacket",
        "jasper", "jungle", "karma", "kernel", "kite", "lantern", "larch", "laurel",
        "lava", "ledger", "lemon", "lilac", "linen", "lizard", "lunar", "magnet",
        "mango", "manor", "maple", "marble", "meadow", "melon", "mercury", "mermaid",
        "meteor", "miller", "minnow", "mirror", "monarch", "morning", "moss", "mountain",
        "nectar", "needle", "noble", "north", "nugget", "oasis", "ocean", "olive",
        "onyx", "opal", "orchard", "otter", "owl", "oyster", "paddle", "palace",
        "panda", "panther", "paper", "pebble", "pelican", "pepper", "petal", "phoenix",
        "piano", "pillar", "pine", "pioneer", "planet", "plaza", "poplar", "prairie",
        "prism", "puma", "quartz", "quiver", "rabbit", "radar", "raven", "reef",
        "ribbon", "river", "robin", "rocket", "route", "ruby", "saddle", "saffron",
        "salmon", "sapphire", "savanna", "scarf", "sentry", "shadow", "shuttle", "signal",
        "silver", "slate", "solar", "sparrow", "spear", "spirit", "spring", "stable",
        "stone", "storm", "summit", "swallow", "swan", "sword", "tapestry", "temple",
        "terrace", "thunder", "tiger", "timber", "topaz", "torch", "trail", "travel",
        "tulip", "tunnel", "turtle", "unity", "valley", "velvet", "vendor", "vernal",
        "vessel", "victor", "viking", "violet", "viper", "walnut", "wander", "willow",
        "window", "winter", "wisdom", "wizard", "wolf", "wren", "yellow", "yew",
        "yonder", "zephyr", "zinc", "zebra", "zenith", "zero", "zigzag", "zone"
    )

    init {
        check(WORDS.size == 256) { "wordlist must be 256" }
        check(WORDS.distinct().size == 256) { "wordlist words must be unique" }
    }

    fun generate(): List<String> {
        val rnd = SecureRandom()
        return List(12) { WORDS[rnd.nextInt(256)] }
    }

    fun normalize(input: String): String =
        input.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ")

    fun wordCount(normalized: String): Int =
        if (normalized.isBlank()) 0 else normalized.split(" ").size

    /** 3 فهارس عشوائية مميزة للتحقق من الحفظ */
    fun quizIndices(): List<Int> {
        val rnd = SecureRandom()
        return (0 until 12).shuffled(rnd).take(3).sorted()
    }
}
