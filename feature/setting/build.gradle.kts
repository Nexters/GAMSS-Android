plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.setting"
}

dependencies {
    implementation(projects.core.ui)

    implementation(libs.calendar.compose) {
        exclude(group = "androidx.compose.ui", module = "ui-tooling")
    }
    implementation(libs.compose.material.icons.core)
}