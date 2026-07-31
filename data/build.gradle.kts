import java.util.Properties

plugins {
    alias(libs.plugins.gamss.android.library)
    alias(libs.plugins.gamss.android.hilt)
    alias(libs.plugins.kotlinSerialization)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    check(localPropertiesFile.exists()) {
        "local.properties 가 없습니다. 프로젝트 루트에 만들고 DEV_BASE_URL·PROD_BASE_URL 을 채우세요."
    }
    localPropertiesFile.inputStream().use(::load)
}

fun localProperty(key: String): String = localProperties.getProperty(key).orEmpty().trim()

/** 주소는 기본값으로 떨어뜨리지 않는다 — 빠진 채로 빌드되면 잘못된 서버를 가리킨 앱이 나온다. */
fun resolveBaseUrl(key: String): String {
    val value = localProperty(key)
    check(value.isNotEmpty()) { "local.properties 에 $key 이 없습니다. 예: https://dev-api.gamss.kr/" }
    // Retrofit 은 '/' 로 끝나지 않는 baseUrl 을 거부한다.
    return value.removeSuffix("/") + "/"
}

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val devBaseUrl = resolveBaseUrl("DEV_BASE_URL")
val prodBaseUrl = resolveBaseUrl("PROD_BASE_URL")

// TODO(google-login 머지 시 삭제): 저장된 토큰을 쓰는 TokenInterceptor 로 대체한다.
val devAccessToken = localProperty("DEV_ACCESS_TOKEN")

android {
    namespace = "com.gamss.android.data"

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", devBaseUrl.asBuildConfigString())
            buildConfigField("String", "DEV_ACCESS_TOKEN", devAccessToken.asBuildConfigString())
        }
        release {
            buildConfigField("String", "BASE_URL", prodBaseUrl.asBuildConfigString())
            buildConfigField("String", "DEV_ACCESS_TOKEN", "\"\"")
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

    implementation(libs.kotlinx.coroutines.android)

    // 온디바이스 감정 분류(KoELECTRA INT8) — LiteRT 추론 + DJL WordPiece 토크나이저
    implementation(libs.litert)
    implementation(platform(libs.djl.bom))
    implementation(libs.djl.huggingface.tokenizers)
    runtimeOnly(libs.djl.android.tokenizer.native)

    // 온디바이스 원문 요약(kobart INT8) — ONNX Runtime Mobile (토크나이저는 DJL 재사용)
    implementation(libs.onnxruntime.android)

    testImplementation(libs.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test:runner:1.6.2")
}
