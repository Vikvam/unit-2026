package cz.aaa.unit2026.blocking

data class BlockRule(
    val label: String,
    val pattern: String,
    val enabled: Boolean = true,
)

val DEFAULT_BLOCK_RULES = listOf(
    BlockRule("YouTube",     "youtube\\.com"),
    BlockRule("Reddit",      "reddit\\.com"),
    BlockRule("Twitter / X", "twitter\\.com|\\bx\\.com\\b"),
    BlockRule("Netflix",     "netflix\\.com"),
    BlockRule("Twitch",      "twitch\\.tv"),
    BlockRule("Instagram",   "instagram\\.com"),
    BlockRule("TikTok",      "tiktok\\.com"),
    BlockRule("Pornhub",     "pornhub\\.com"),
    BlockRule("9GAG",        "9gag\\.com"),
    BlockRule("Steam",       "^steam$"),
    BlockRule("Lutris",      "^lutris$"),
)
