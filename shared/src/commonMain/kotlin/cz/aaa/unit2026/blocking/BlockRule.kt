package cz.aaa.unit2026.blocking

data class BlockRule(
    val label: String,
    val pattern: String,
    val enabled: Boolean = true,
)

val DEFAULT_BLOCK_RULES = listOf(
    BlockRule("YouTube",     "\\byoutube\\b"),
    BlockRule("Reddit",      "\\breddit\\b"),
    BlockRule("Twitter / X", "\\b(twitter|x\\.com)\\b"),
    BlockRule("Netflix",     "\\bnetflix\\b"),
    BlockRule("Twitch",      "\\btwitch\\b"),
    BlockRule("Instagram",   "\\binstagram\\b"),
    BlockRule("TikTok",      "\\btiktok\\b"),
    BlockRule("Pornhub",     "\\bpornhub\\b"),
    BlockRule("9GAG",        "\\b9gag\\b"),
    BlockRule("Steam",       "^steam"),
    BlockRule("Lutris",      "^lutris"),
)
