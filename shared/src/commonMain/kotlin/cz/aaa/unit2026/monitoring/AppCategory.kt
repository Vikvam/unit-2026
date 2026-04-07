package cz.aaa.unit2026.monitoring

/**
 * App category constants — mirrors Android's ApplicationInfo.CATEGORY_* values.
 */
object AppCategory {
    const val UNDEFINED       = -1
    const val GAME            = 0
    const val AUDIO           = 1
    const val VIDEO           = 2
    const val IMAGE           = 3
    const val SOCIAL          = 4
    const val NEWS            = 5
    const val MAPS            = 6
    const val PRODUCTIVITY    = 7
    const val ACCESSIBILITY   = 8

    /** Categories that are blocked by default when focus mode starts. */
    val DEFAULT_BLOCKED = setOf(GAME, SOCIAL, VIDEO, NEWS)

    /** Display order for category groups (most likely to block first). */
    val DISPLAY_ORDER = listOf(SOCIAL, VIDEO, GAME, NEWS, AUDIO, IMAGE, MAPS, PRODUCTIVITY, ACCESSIBILITY, UNDEFINED)

    fun displayName(category: Int): String = when (category) {
        GAME          -> "Games"
        AUDIO         -> "Audio"
        VIDEO         -> "Video"
        IMAGE         -> "Image"
        SOCIAL        -> "Social"
        NEWS          -> "News"
        MAPS          -> "Maps"
        PRODUCTIVITY  -> "Productivity"
        ACCESSIBILITY -> "Accessibility"
        else          -> "Other"
    }

    /**
     * Resolves the category for a package, using a known-package mapping first,
     * then falling back to the Android-reported category.
     */
    fun resolve(packageName: String, androidCategory: Int = UNDEFINED): Int {
        KNOWN_PACKAGES[packageName]?.let { return it }
        KNOWN_PREFIXES.forEach { (prefix, cat) ->
            if (packageName.startsWith(prefix)) return cat
        }
        return if (androidCategory >= 0) androidCategory else UNDEFINED
    }

    private val KNOWN_PACKAGES = mapOf(
        // Social
        "com.instagram.android" to SOCIAL,
        "com.facebook.katana" to SOCIAL,
        "com.facebook.lite" to SOCIAL,
        "com.facebook.orca" to SOCIAL,
        "com.twitter.android" to SOCIAL,
        "com.x.android" to SOCIAL,
        "com.snapchat.android" to SOCIAL,
        "com.reddit.frontpage" to SOCIAL,
        "com.pinterest" to SOCIAL,
        "com.tumblr" to SOCIAL,
        "com.vkontakte.android" to SOCIAL,
        "com.bereal.android" to SOCIAL,
        "com.linkedin.android" to SOCIAL,
        "com.zhiliaoapp.musically" to SOCIAL, // TikTok
        "com.discord" to SOCIAL,
        "org.telegram.messenger" to SOCIAL,
        "org.thunderdog.challegram" to SOCIAL,
        "com.whatsapp" to SOCIAL,
        "com.viber.voip" to SOCIAL,
        "com.tencent.mm" to SOCIAL, // WeChat

        // Video
        "com.google.android.youtube" to VIDEO,
        "com.netflix.mediaclient" to VIDEO,
        "com.amazon.avod.thirdpartyclient" to VIDEO,
        "com.disney.disneyplus" to VIDEO,
        "tv.twitch.android.app" to VIDEO,
        "com.hbo.hbonow" to VIDEO,
        "com.hulu.plus" to VIDEO,
        "com.paramount.plus" to VIDEO,
        "com.peacocktv.peacockandroid" to VIDEO,
        "com.apple.atve.androidtv.appletv" to VIDEO,

        // Games
        "com.supercell.clashofclans" to GAME,
        "com.supercell.clashroyale" to GAME,
        "com.supercell.brawlstars" to GAME,
        "com.king.candycrushsaga" to GAME,
        "com.king.candycrushsodasaga" to GAME,
        "com.vng.pubgmobile" to GAME,
        "com.tencent.ig" to GAME,
        "com.roblox.client" to GAME,
        "com.epicgames.fortnite" to GAME,
        "com.mojang.minecraftpe" to GAME,
        "com.activision.callofduty.shooter" to GAME,
        "com.garena.game.freefire" to GAME,

        // News
        "com.google.android.apps.magazines" to NEWS, // Google News
        "com.cnn.mobile.android.phone" to NEWS,
        "com.bbc.news" to NEWS,
        "flipboard.app" to NEWS,

        // Audio
        "com.google.android.apps.podcasts" to AUDIO,
        "com.spotify.music" to AUDIO,
        "com.google.android.apps.youtube.music" to AUDIO,
        "com.apple.android.music" to AUDIO,
        "com.soundcloud.android" to AUDIO,

        // Maps
        "com.google.android.apps.maps" to MAPS,
        "com.waze" to MAPS,

        // Productivity
        "com.google.android.gm" to PRODUCTIVITY, // Gmail
        "com.microsoft.office.outlook" to PRODUCTIVITY,
        "com.slack" to PRODUCTIVITY,
        "com.google.android.apps.docs" to PRODUCTIVITY,
        "com.google.android.calendar" to PRODUCTIVITY,
    )

    private val KNOWN_PREFIXES = listOf(
        "com.supercell." to GAME,
        "com.king." to GAME,
        "com.ea." to GAME,
        "com.gameloft." to GAME,
        "com.rovio." to GAME,
    )
}
