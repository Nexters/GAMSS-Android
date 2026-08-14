plugins {
    alias(libs.plugins.androidAssetPack)
}

// 감정 분류(KoELECTRA INT8) 모델 + tokenizer. base 앱 크기에는 안 잡히지만, 대화 감정 누적이
// 첫 발화부터 바로 이 모델을 필요로 하고 카드 생성이 그 결과에 막혀 있어(하드 블로커) fast-follow 로
// 배포한다 — 설치 완료 직후 자동으로 백그라운드 다운로드가 시작돼, on-demand 보다 훨씬 이른
// 시점(사용자가 채팅을 시작하기 전)부터 리드타임을 번다.
// 실제 파일: src/main/assets/models/emotion_int8.tflite, emotion_tokenizer.json
//
// 이 애셋팩은 Play Console(release buildType) 배포에서만 쓰인다. Play Store 를 거치지 않는 배포
// (debug/firebase buildType)는 이 모델을 아예 여기서 가져가지 않고, data 모듈이 이 assets 디렉터리를
// 자기 debug/firebase sourceSet 에 직접 srcDir 로 얹어 APK 에 번들한다 — data/build.gradle.kts,
// data/model/LocalAssetsModelSource 참고.
assetPack {
    packName.set("emotion_pack")
    dynamicDelivery {
        deliveryType.set("fast-follow")
    }
}
