plugins {
    alias(libs.plugins.gamss.android.library)
    alias(libs.plugins.gamss.android.compose)
}

android {
    namespace = "com.gamss.android.core.ui"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.material.icons.core)

    testImplementation(libs.junit)
}
