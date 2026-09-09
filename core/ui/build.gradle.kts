plugins {
    alias(libs.plugins.gamss.android.library)
    alias(libs.plugins.gamss.android.compose)
}

android {
    namespace = "com.gamss.android.core.ui"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.domain)

    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.material.icons.core)

    // 카드 캡처가 그리기 신호를 Channel 로 주고받는다.
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
