import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.gamss.android.application)
    alias(libs.plugins.gamss.android.compose)
    alias(libs.plugins.gamss.android.hilt)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

/**
 * 네 값이 모두 있을 때만 서명한다. 키스토어가 없는 환경(CI·다른 개발자)에서도 빌드는 돌아야 한다.
 * 값은 local.properties 에만 두고 저장소에 올리지 않는다.
 */
val releaseKeystore = listOf(
    "RELEASE_STORE_FILE",
    "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS",
    "RELEASE_KEY_PASSWORD",
).associateWith { localProperties.getProperty(it).orEmpty().trim() }
    .takeIf { values -> values.none { it.value.isEmpty() } }

android {
    namespace = "com.gamss.android.app"

    buildFeatures {
        resValues = true
        buildConfig = true
    }

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

    signingConfigs {
        releaseKeystore?.let { keystore ->
            create("release") {
                storeFile = file(keystore.getValue("RELEASE_STORE_FILE"))
                storePassword = keystore.getValue("RELEASE_STORE_PASSWORD")
                keyAlias = keystore.getValue("RELEASE_KEY_ALIAS")
                keyPassword = keystore.getValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "GAMSS Dev")
            buildConfigField("boolean", "INTERNAL_TOOLS", "true")
        }
        /**
         * 팀에 돌리는 내부 배포본. 아직 debug 로만 보이는 화면을 열어 두되,
         * 릴리즈 키로 서명하는 바이너리라 디버깅은 막는다.
         */
        create("internal") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            // applicationId 를 release 와 같이 둬야 이미 등록된 릴리즈 키 지문으로 소셜 로그인이 된다.
            versionNameSuffix = "-internal"
            resValue("string", "app_name", "GAMSS Internal")
            buildConfigField("boolean", "INTERNAL_TOOLS", "true")
            isDebuggable = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField("boolean", "INTERNAL_TOOLS", "false")
            signingConfigs.findByName("release")?.let { signingConfig = it }
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

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
