package cz.aaa.unit2026.blocking

/**
 * Default set of app package IDs to block during focus sessions.
 * Android package names only — Desktop uses resourceClass names (lowercase exe).
 */
val DEFAULT_BLACKLIST: Set<String> = setOf(

    // Social media
    "com.instagram.android",
    "com.facebook.katana",
    "com.facebook.lite",
    "com.twitter.android",          // Twitter / X
    "com.x.android",                // X (new package)
    "com.zhiliaoapp.musically",     // TikTok
    "com.snapchat.android",
    "com.reddit.frontpage",
    "com.pinterest",
    "com.tumblr",
    "com.vkontakte.android",
    "com.bereal.android",
    "com.linkedin.android",

    // Video & streaming
    "com.google.android.youtube",
    "com.netflix.mediaclient",
    "com.amazon.avod.thirdpartyclient",
    "com.disney.disneyplus",
    "tv.twitch.android.app",
    "com.hbo.hbonow",
    "com.hulu.plus",
    "com.paramount.plus",
    "com.peacocktv.peacockandroid",
    "com.apple.atve.androidtv.appletv",

    // Games
    "com.supercell.clashofclans",
    "com.supercell.clashroyale",
    "com.king.candycrushsaga",
    "com.king.candycrushsodasaga",
    "com.vng.pubgmobile",
    "com.tencent.ig",               // PUBG (alternative)
    "com.roblox.client",
    "com.epicgames.fortnite",
    "com.mojang.minecraftpe",
    "com.activision.callofduty.shooter",
    "com.garena.game.freefire",

    // Adult content
    "com.pornhub.android",
    "com.xvideos.android",
    "com.xnxx.android",

    // Audio / Podcasts
    "com.google.android.apps.podcasts",

    // Desktop (resourceClass)
    "steam",
    "lutris",
)
