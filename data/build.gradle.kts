import java.util.Properties

plugins {
    alias(libs.plugins.gamss.android.library)
    alias(libs.plugins.gamss.android.hilt)
    alias(libs.plugins.kotlinSerialization)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    check(localPropertiesFile.exists()) {
        "local.properties not found. Create it in the project root."
    }
    localPropertiesFile.inputStream().use(::load)
}

fun resolveBaseUrl(key: String): String {
    val value = localProperties.getProperty(key)
    require(value.isNotBlank()) { "Missing $key in local.properties" }
    // Retrofit 은 '/' 로 끝나지 않는 baseUrl 을 거부한다. 런타임이 아니라 빌드에서 걸러낸다.
    return value.trim().removeSuffix("/") + "/"
}

val devBaseUrl = resolveBaseUrl("DEV_BASE_URL")
val prodBaseUrl = resolveBaseUrl("PROD_BASE_URL")

android {
    namespace = "com.gamss.android.data"

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$devBaseUrl\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"$prodBaseUrl\"")
        }
    }

}

dependencies {
    implementation(projects.domain)
    implementation(projects.core.common)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.tink.android)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.config)
    implementation(libs.kotlinx.coroutines.play.services)

    // 온디바이스 감정 분류(KoELECTRA INT8). LiteRT 추론, 토크나이저는 순수 Kotlin.
    implementation(libs.litert) {
        // 모델은 Play Asset Delivery(범용 on-demand 애셋팩)로 내려받는다.
        // LiteRT 자체 모델 배포 API(ai-delivery)는 쓰지 않는다 — 콜드스타트마다 WorkManager 를
        // 초기화시켜서 제외하고, 대신 asset-delivery 로 직접 다운로드 상태를 제어한다.
        exclude(group = "com.google.android.play", module = "ai-delivery")
    }

    // 온디바이스 원문 요약(kobart INT8). ONNX Runtime Mobile, 토크나이저는 순수 Kotlin.
    implementation(libs.onnxruntime.android)

    // 감정/요약 모델(.tflite, .onnx)을 담은 on-demand 애셋팩(:models:emotion-pack, :models:summary-pack)을
    // 런타임에 요청·추적·로컬 경로 조회하는 데 사용.
    implementation(libs.play.asset.delivery)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
