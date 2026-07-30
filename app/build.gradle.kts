plugins {
    alias(libs.plugins.gamss.android.application)
    alias(libs.plugins.gamss.android.compose)
    alias(libs.plugins.gamss.android.hilt)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

android {
    namespace = "com.gamss.android.app"

    defaultConfig {
        applicationId = "com.gamss.android"
        versionCode = 1
        versionName = "0.1.0"

        // 감정 분류 온디바이스 네이티브(DJL 토크나이저 + libc++_shared.so)는 arm64 실기기 대상만 패키징
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    androidResources {
        // .tflite 를 비압축 저장해야 assets.openFd + FileChannel.map(mmap) 가능
        noCompress += "tflite"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(projects.domain)
    implementation(projects.data)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.feature.home)
    implementation(projects.feature.chat)
    implementation(projects.feature.calendar)
    implementation(projects.feature.emotion)

    implementation(libs.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
