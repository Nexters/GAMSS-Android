plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.game"
}

dependencies {
    implementation(projects.domain)

    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
