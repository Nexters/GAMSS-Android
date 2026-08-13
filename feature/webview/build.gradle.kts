plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.webview"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
