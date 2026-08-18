plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.calendar"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.domain)

    implementation(libs.calendar.compose) {
        exclude(group = "androidx.compose.ui", module = "ui-tooling")
    }
    implementation(libs.compose.material.icons.core)

    // 카드 공유 검증용 디버그 액티비티(src/debug)에서만 쓴다.
    debugImplementation(libs.androidx.activity.compose)
}
