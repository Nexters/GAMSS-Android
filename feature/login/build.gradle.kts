import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.login"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.domain)

    implementation(libs.google.googleid)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)

    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
