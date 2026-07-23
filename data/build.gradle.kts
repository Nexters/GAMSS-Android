import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    check(localPropertiesFile.exists()) {
        "local.properties 파일이 없습니다. 프로젝트 루트에 파일을 생성해 주세요."
    }
    localPropertiesFile.inputStream().use(::load)
}

val baseUrl = checkNotNull(localProperties.getProperty("BASE_URL")) {
    "local.properties에 BASE_URL을 설정해 주세요."
}

plugins {
    alias(libs.plugins.gamss.android.library)
    alias(libs.plugins.gamss.android.hilt)
}

android {
    namespace = "com.gamss.android.data"

    buildFeatures {
        buildConfig = true
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

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
}
