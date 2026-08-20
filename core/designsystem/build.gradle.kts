plugins {
    alias(libs.plugins.gamss.android.library)
    alias(libs.plugins.gamss.android.compose)
}

android {
    namespace = "com.gamss.android.core.designsystem"
}

dependencies {
    implementation(libs.lottie.compose)
}