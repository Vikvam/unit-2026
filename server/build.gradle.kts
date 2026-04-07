plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    application
}

group = "cz.aaa.unit2026"
version = "1.0.0"
application {
    mainClass.set("cz.aaa.unit2026.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverWebSockets)
    implementation(libs.ktor.serverContentNeg)
    implementation(libs.ktor.serializationJson)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.ktor.clientWebSockets)
    testImplementation(libs.kotlin.testJunit)
}