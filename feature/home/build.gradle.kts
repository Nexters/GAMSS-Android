plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.home"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.domain)

    testImplementation(libs.mockk)

    // orbit-test 가 전이로 가져오지만, 직접 쓰는 API 라 명시한다.
    testImplementation(libs.kotlinx.coroutines.test)
}
