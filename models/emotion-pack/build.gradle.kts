plugins {
    alias(libs.plugins.androidAssetPack)
}

// 감정 분류(KoELECTRA INT8) 모델 + tokenizer. base 앱 크기에는 안 잡히지만, 대화 감정 누적이
// 첫 발화부터 바로 이 모델을 필요로 하고 카드 생성이 그 결과에 막혀 있어(하드 블로커) fast-follow 로
// 배포한다 — 설치 완료 직후 자동으로 백그라운드 다운로드가 시작돼, on-demand 보다 훨씬 이른
// 시점(사용자가 채팅을 시작하기 전)부터 리드타임을 번다.
// 실제 파일: src/main/assets/models/emotion_int8.tflite, emotion_tokenizer.json
//
// -PinstallTimeModels=true 를 주면 install-time 으로 강제 전환된다. Play Asset Delivery(on-demand/
// fast-follow)는 Play Store 설치 경로에서만 채워지므로, Play Store를 거치지 않는 배포(Firebase App
// Distribution 등)용 빌드에서 이 모델이 필요할 때 CD 에서 이 플래그로 켠다 — AAB 안에 모델이 그대로
// 박혀 용량은 커지지만(전체 ~400MB) 어떤 설치 경로에서도 바로 동작한다.
assetPack {
    packName.set("emotion_pack")
    dynamicDelivery {
        deliveryType.set(if (project.hasProperty("installTimeModels")) "install-time" else "fast-follow")
    }
}
