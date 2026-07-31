plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.chat"
}

dependencies {
    implementation(projects.domain)

    implementation(libs.compose.material.icons.core)
}
