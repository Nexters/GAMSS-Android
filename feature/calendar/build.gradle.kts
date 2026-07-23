plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.calendar"
}

dependencies {
    implementation(projects.core.ui)
}
