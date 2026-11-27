plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.store"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.domain)

    // 결제 플로우를 실행할 Activity 참조(LocalActivity)를 위해 필요하다.
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
