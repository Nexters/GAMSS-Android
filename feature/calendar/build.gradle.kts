plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.archive"
}

dependencies {
    implementation(projects.core.ui)
    implementation(libs.compose.material.icons.core)
}
