plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.carddelete"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.domain)

    testImplementation(libs.mockk)
}
