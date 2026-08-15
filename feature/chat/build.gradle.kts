plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.chat"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.domain)

    implementation(libs.compose.material.icons.core)

    // 선택 모드를 뒤로가기로 빠져나가기 위한 BackHandler. compose-ui 에는 없는 API 다.
    implementation(libs.androidx.activity.compose)

    // orbit-test 가 전이로 가져오지만, 직접 쓰는 API 라 명시한다.
    testImplementation(libs.kotlinx.coroutines.test)
}
