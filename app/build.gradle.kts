import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.gamss.android.application)
    alias(libs.plugins.gamss.android.compose)
    alias(libs.plugins.gamss.android.hilt)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.androidxBaselineProfile)
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
        versionName = "1.0.0"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    // release(Play Console) buildType 은 온디바이스 모델(.tflite/.onnx)을 base 앱에 번들하지 않는다 —
    // Play Asset Delivery(on-demand) 애셋팩으로 분리되어 필요 시점에만 기기로 내려받힌다(다운로드 후
    // 로컬 파일로 추출되므로 APK zip 엔트리가 아니라 noCompress 설정이 필요 없다).
    assetPacks += setOf(":models:emotion-pack", ":models:summary-pack")

    // debug/internal buildType 은 위 애셋팩 대신 같은 파일을 assets 로 직접 번들한다(data 모듈의
    // debug/internal sourceSet 참고) — 이 경우엔 APK zip 엔트리이므로 noCompress 가 필요하다. .onnx 는
    // 명시하지 않으면 압축돼 mmap(assets.openFd)이 실패한다. .tflite 는 AGP 가 기본으로 비압축 처리한다.
    androidResources {
        noCompress += listOf("tflite", "onnx")
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

        // 플러그인이 release 를 복제해 만드는 buildType 은 로컬 설치가 필요해 debug 키로 서명한다.
        configureEach {
            if (name.startsWith("nonMinified") || name.startsWith("benchmark")) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }

        // Firebase App Distribution 전용 buildType. Play Store 를 거치지 않는 설치 경로라 온디바이스
        // 모델을 PAD 대신 APK 에 그대로 번들한다 — data/build.gradle.kts 의 `internal` buildType/
        // sourceSet 참고.
        create("internal") {
            initWith(getByName("release"))
            isDebuggable = true
            versionNameSuffix = "-internal"
            // CI 러너의 debug 키는 빌드마다 SHA-1 이 달라 구글 로그인 OAuth 클라이언트로 등록할 수 없다.
            signingConfig = if (hasCompleteReleaseSigningConfig) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            matchingFallbacks += listOf("release")
        }
    }
}

baselineProfile {
    mergeIntoMain = true
    // 일반 빌드가 기기를 찾으면 안 된다. 갱신은 :app:generateBaselineProfile 로 직접 돌린다.
    automaticGenerationDuringBuild = false
}

dependencies {
    implementation(projects.domain)
    implementation(projects.data)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.feature.home)
    implementation(projects.feature.chat)
    implementation(projects.feature.archive)
    implementation(projects.feature.login)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.setting)
    implementation(projects.feature.carddelete)
    implementation(projects.feature.webview)

    implementation(libs.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.androidx.navigationevent)

    implementation(libs.orbit.core)
    implementation(libs.orbit.viewmodel)
    implementation(libs.orbit.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(projects.baselineprofile)

    testImplementation(libs.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
}
