plugins {
    alias(libs.plugins.gamss.jvm.library)
}

dependencies {
    // AppResult 가 repository·usecase 반환 타입이라 domain 의 공개 API 표면이다.
    api(projects.core.common)

    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
