plugins {
    alias(libs.plugins.androidAssetPack)
}

// 원문 요약(kobart INT8) 인코더/디코더 모델 + tokenizer. on-demand 로 배포되어 base 앱 크기에 잡히지 않는다.
// 실제 파일: src/main/assets/models/kobart_encoder_int8.onnx, kobart_decoder_int8.onnx, kobart_tokenizer.json
//
// -PinstallTimeModels=true 를 주면 install-time 으로 강제 전환된다. models/emotion-pack/build.gradle.kts
// 상단 주석 참고 — Play Store를 거치지 않는 배포(Firebase App Distribution 등)용 빌드를 위한 스위치다.
assetPack {
    packName.set("summary_pack")
    dynamicDelivery {
        deliveryType.set(if (project.hasProperty("installTimeModels")) "install-time" else "on-demand")
    }
}
