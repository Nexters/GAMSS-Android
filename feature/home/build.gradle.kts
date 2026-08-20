plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.home"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.domain)

    // 캐릭터 패널을 뒤로가기로 닫기 위한 BackHandler. compose-ui 에는 없는 API 다.
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.mockk)

    // orbit-test 가 전이로 가져오지만, 직접 쓰는 API 라 명시한다.
    testImplementation(libs.kotlinx.coroutines.test)
}
