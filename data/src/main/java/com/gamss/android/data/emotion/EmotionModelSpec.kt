package com.gamss.android.data.emotion

import com.gamss.android.domain.emotion.EmotionLabel

/** KoELECTRA 6-감정 모델 설정. */
internal object EmotionModelSpec {
    // :models:emotion-pack 의 assetPack { packName } 과 일치해야 한다.
    const val PACK_NAME = "emotion_pack"

    val SPEC = ClassifierSpec(
        packName = PACK_NAME,
        modelAsset = "models/emotion_int8.tflite",
        tokenizerAsset = "models/emotion_tokenizer.json",
        // 출력 index 순서(id2label)가 곧 계약이라 EmotionLabel 선언 순서에서 파생한다.
        labels = EmotionLabel.entries.map { it.koLabel },
    )
}
