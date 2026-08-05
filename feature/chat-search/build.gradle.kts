plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.chatSearch"
}

dependencies {
    implementation(projects.domain)
    implementation(libs.androidx.paging.compose)
}
