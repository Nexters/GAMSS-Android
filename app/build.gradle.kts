import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

    buildFeatures {
        resValues = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.gamss.android"
        // CD에서 fastlane이 -PversionCode= 로 CI 빌드 번호(GITHUB_RUN_NUMBER 기반)를 주입한다.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = "0.1.0"

        // 감정 분류 온디바이스 네이티브(DJL 토크나이저 + libc++_shared.so)는 arm64 실기기 대상만 패키징
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    androidResources {
        // 모델을 비압축 저장해야 assets.openFd + FileChannel.map(mmap) 로 힙 복사 없이 로드 가능.
        // (int8 모델은 고엔트로피라 비압축 저장에 따른 APK 크기 증가가 미미하다.)
        noCompress += "tflite"
        noCompress += "onnx"
    }

    packaging {
        resources {
            // DJL 토크나이저가 데스크톱 바이너리까지 배포한다. 안드로이드는 lib/arm64-v8a 만 쓴다.
            excludes += setOf(
                "native/lib/win-x86_64/**",
                "native/lib/osx-aarch64/**",
                "native/lib/osx-x86_64/**",
                "native/lib/linux-x86_64/**",
                "com/sun/jna/aix-ppc/**",
                "com/sun/jna/aix-ppc64/**",
                "com/sun/jna/win32-x86/**",
                "com/sun/jna/win32-x86-64/**",
                "com/sun/jna/darwin-aarch64/**",
                "com/sun/jna/darwin-x86-64/**",
                "META-INF/INDEX.LIST",
            )
        }
    }

    val releaseKeystorePath = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull
    val releaseKeystorePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").orNull
    val releaseKeyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
    val hasCompleteReleaseSigningConfig = listOf(
        releaseKeystorePath,
        releaseKeystorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

    signingConfigs {
        if (hasCompleteReleaseSigningConfig) {
            val keystorePath = releaseKeystorePath.orEmpty()
            val keystorePassword = releaseKeystorePassword.orEmpty()
            val signingKeyAlias = releaseKeyAlias.orEmpty()
            val signingKeyPassword = releaseKeyPassword.orEmpty()
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "GAMSS Dev")
        }
        getByName("release") {
            isMinifyEnabled = false
            if (hasCompleteReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    implementation(projects.feature.login)

    implementation(libs.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    implementation(libs.orbit.core)
    implementation(libs.orbit.viewmodel)
    implementation(libs.orbit.compose)

    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
