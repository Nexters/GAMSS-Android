plugins {
    alias(libs.plugins.gamss.android.feature)
}

android {
    namespace = "com.gamss.android.feature.billing"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.domain)
    implementation(libs.billing.ktx)

    // LocalActivity 로 launchBillingFlow 에 넘길 Activity 를 얻기 위해 필요하다.
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
