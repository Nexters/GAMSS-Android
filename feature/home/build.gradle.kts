plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.home"
}

dependencies {
    implementation(projects.core.ui)
}
