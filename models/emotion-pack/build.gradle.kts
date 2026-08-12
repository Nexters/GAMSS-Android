plugins {
    alias(libs.plugins.androidAssetPack)
}

// 감정 분류(KoELECTRA INT8) 모델 + tokenizer. on-demand 로 배포되어 base 앱 크기에 잡히지 않는다.
// 실제 파일: src/main/assets/models/emotion_int8.tflite, emotion_tokenizer.json
assetPack {
    packName.set("emotion_pack")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
