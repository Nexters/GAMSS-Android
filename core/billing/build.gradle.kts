plugins {
    alias(libs.plugins.gamss.android.library)
    alias(libs.plugins.gamss.android.hilt)
}

android {
    namespace = "com.gamss.android.core.billing"
}

dependencies {
    implementation(projects.core.common)

    // BillingResult/ProductDetails가 BillingRepository의 공개 시그니처에 그대로 노출되므로 api로 둔다.
    api(libs.billing.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
