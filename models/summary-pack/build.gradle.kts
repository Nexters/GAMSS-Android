plugins {
    alias(libs.plugins.androidAssetPack)
}

// 원문 요약(kobart INT8) 인코더/디코더 모델 + tokenizer. on-demand 로 배포되어 base 앱 크기에 잡히지 않는다.
// 실제 파일: src/main/assets/models/kobart_encoder_int8.onnx, kobart_decoder_int8.onnx, kobart_tokenizer.json
//
// 이 애셋팩은 Play Console(release buildType) 배포에서만 쓰인다. models/emotion-pack/build.gradle.kts
// 상단 주석 참고 — Play Store를 거치지 않는 배포(debug/internal buildType)는 data 모듈이 이 assets
// 디렉터리를 직접 srcDir 로 얹어 APK 에 번들한다.
assetPack {
    packName.set("summary_pack")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
