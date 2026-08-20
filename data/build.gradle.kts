import java.util.Properties

plugins {
    alias(libs.plugins.gamss.android.library)
    alias(libs.plugins.gamss.android.hilt)
    alias(libs.plugins.gamss.android.room)
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
        // app 의 `internal` buildType(Firebase App Distribution 배포)과 짝을 맞춘다. app 은 이
        // buildType 에 matchingFallbacks("release")를 두지만, 그건 internal variant 가 아예 없는
        // 라이브러리에만 적용된다 — 이 모듈처럼 진짜 internal variant 를 선언하면 그게 우선한다.
        // BASE_URL 은 release 와 동일(운영 서버 대상 QA 배포).
        create("internal") {
            initWith(getByName("release"))
        }
    }

    // 온디바이스 모델(.tflite/.onnx)은 release(Play Console)에서는 Play Asset Delivery 로 내려받지만,
    // debug/internal(Play Store 를 거치지 않는 설치 경로)에서는 AssetPackManager 가 동작하지 않아
    // APK 에 그대로 번들한다. 파일을 복사하지 않고 애셋팩 모듈의 assets 를 그대로 srcDir 로 참조해
    // 단일 소스를 유지한다 — data/model/LocalAssetsModelSource, di/ModelAssetSourceModule 참고.
    sourceSets {
        listOf("debug", "internal").forEach { buildTypeName ->
            getByName(buildTypeName) {
                assets.srcDirs(
                    "../models/emotion-pack/src/main/assets",
                    "../models/summary-pack/src/main/assets",
                )
            }
        }
    }

    androidResources {
        // assets.openFd() + mmap 으로 로드하려면(LocalAssetsModelSource) APK 안에 비압축으로
        // 들어있어야 한다. release 는 이 assets 를 안 쓰지만(애셋팩으로 분리) 무해하다.
        noCompress += listOf("tflite", "onnx")
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
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.paging.common)

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
    androidTestImplementation("androidx.test:runner:1.6.2")

    androidTestImplementation(libs.androidx.test.runner)
}
