plugins {
    alias(libs.plugins.gamss.android.library)
    alias(libs.plugins.gamss.android.hilt)
}

android {
    namespace = "com.gamss.android.data"

    buildFeatures {
        buildConfig = true
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
