plugins {
    alias(libs.plugins.androidAssetPack)
}

// 원문 요약(kobart INT8) 인코더/디코더 모델 + tokenizer. on-demand 로 배포되어 base 앱 크기에 잡히지 않는다.
// 실제 파일: src/main/assets/models/kobart_encoder_int8.onnx, kobart_decoder_int8.onnx, kobart_tokenizer.json
assetPack {
    packName.set("summary_pack")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
