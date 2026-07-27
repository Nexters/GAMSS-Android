package com.gamss.android.data.emotion

/** KoELECTRA 6-감정 모델 설정. */
object EmotionModelSpec {
    // 출력 index 순서(id2label): 0 angry, 1 anxious, 2 embarrassed, 3 happy, 4 heartache, 5 sad
    val SPEC = ClassifierSpec(
        modelAsset = "models/emotion_int8.tflite",
        tokenizerAsset = "models/emotion_tokenizer.json",
        labels = listOf("분노", "불안", "당황", "기쁨", "상처", "슬픔"),
    )

    /** 한글 라벨 → 원본 모델 영문 라벨(id2label). 평가/로그 대조의 단일 출처. */
    val LABEL_EN = linkedMapOf(
        "분노" to "angry",
        "불안" to "anxious",
        "당황" to "embarrassed",
        "기쁨" to "happy",
        "상처" to "heartache",
        "슬픔" to "sad",
    )
}
