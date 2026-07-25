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

fun resolveBaseUrl(key: String): String =
    checkNotNull(localProperties.getProperty(key)) {
        "Missing $key in local.properties"
    }.trim()

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

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)

    implementation(libs.kotlinx.coroutines.android)

    // 온디바이스 감정 분류(KoELECTRA INT8) — LiteRT 추론 + DJL WordPiece 토크나이저
    implementation(libs.litert)
    implementation(platform(libs.djl.bom))
    implementation(libs.djl.huggingface.tokenizers)
    runtimeOnly(libs.djl.android.tokenizer.native)
}
