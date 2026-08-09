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

    androidResources {
        // .onnx 를 비압축 저장해야 assets.openFd + mmap 로드 가능(androidTest APK 는 이 모듈 설정을 따른다).
        // .tflite 는 AGP 가 기본으로 비압축 처리한다.
        noCompress += "onnx"
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
    implementation(libs.kotlinx.coroutines.play.services)

    // 온디바이스 감정 분류(KoELECTRA INT8) — LiteRT 추론 + DJL WordPiece 토크나이저
    implementation(libs.litert) {
        // 모델을 assets 에 직접 넣으므로 AI Pack 배포가 필요 없다.
        // 이게 끌고 오는 WorkManager 가 콜드스타트마다 초기화된다.
        exclude(group = "com.google.android.play", module = "ai-delivery")
    }
    implementation(platform(libs.djl.bom))
    implementation(libs.djl.huggingface.tokenizers)
    runtimeOnly(libs.djl.android.tokenizer.native)

    // 온디바이스 원문 요약(kobart INT8) — ONNX Runtime Mobile (토크나이저는 DJL 재사용)
    implementation(libs.onnxruntime.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
