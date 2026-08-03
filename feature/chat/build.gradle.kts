plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.chat"
}

dependencies {
    implementation(projects.domain)

    implementation(libs.compose.material.icons.core)

    // orbit-test 가 전이로 가져오지만, 직접 쓰는 API 라 명시한다.
    testImplementation(libs.kotlinx.coroutines.test)
}
