pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "GAMSS-Android"

include(":app")

// 콜드스타트 Baseline Profile 을 실기기에서 수집하는 계측 모듈. 앱에 실리지 않는다.
include(":baselineprofile")

include(":domain")
include(":data")

// Play Asset Delivery(on-demand) 로 배포되는 대용량 온디바이스 모델 전용 애셋팩.
// 코드 없이 assets 만 담으며, app 모듈의 assetPacks 에 등록되어 필요 시점에 개별 다운로드된다.
include(":models:emotion-pack")
include(":models:summary-pack")

include(":core:common")
include(":core:ui")
include(":feature:home")
include(":feature:chat")
include(":feature:archive")
include(":feature:login")
include(":feature:setting")
include(":feature:carddelete")
include(":feature:webview")
include(":core:designsystem")
include(":feature:onboarding")
