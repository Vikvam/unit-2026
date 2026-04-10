plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// Force kotlinx-datetime to 0.6.2 across every subproject. supabase-kt 3.1.4
// was compiled against `kotlinx.datetime.serializers.InstantIso8601Serializer`,
// which was removed in kotlinx-datetime 0.7.x (the kotlin.time.Instant
// migration). Compose Material3 1.10.0-alpha05 pulls 0.7.1 transitively, and
// Gradle's default resolution bumps supabase-kt's 0.6.2 → 0.7.1, producing a
// runtime ClassNotFoundException at UserSession deserialization.
// Revisit on supabase-kt 3.2.x stable — see BEFORE_LAUNCH.md.
allprojects {
    configurations.configureEach {
        resolutionStrategy {
            force("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
        }
    }
}