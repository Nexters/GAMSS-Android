package com.gamss.android.data.emotion

/** KoELECTRA 6-감정 모델 설정. */
object EmotionModelSpec {
    // 출력 index 순서(id2label): 0 angry, 1 anxious, 2 embarrassed, 3 happy, 4 heartache, 5 sad
    val SPEC = ClassifierSpec(
        modelAsset = "models/emotion_int8.tflite",
        tokenizerAsset = "models/emotion_tokenizer.json",
        labels = listOf("분노", "불안", "당황", "기쁨", "상처", "슬픔"),
    )
}
