package cz.aaa.unit2026.monitoring

/**
 * App category constants — mirrors Android's ApplicationInfo.CATEGORY_* values.
 * null on Desktop.
 */
object AppCategory {
    const val UNDEFINED   = -1
    const val GAME        = 0
    const val AUDIO       = 1
    const val VIDEO       = 2
    const val IMAGE       = 3
    const val SOCIAL      = 4
    const val NEWS        = 5
    const val MAPS        = 6
    const val PRODUCTIVITY = 7
    const val ACCESSIBILITY = 8

    /** Categories that are blocked by default when focus mode starts. */
    val DEFAULT_BLOCKED = setOf(GAME, SOCIAL, VIDEO, NEWS)
}
