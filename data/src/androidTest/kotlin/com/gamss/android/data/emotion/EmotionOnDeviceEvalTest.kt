package com.gamss.android.data.emotion

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 온디바이스 감정 분류 정량 평가.
 * androidTest assets 의 라벨된 문장 세트를 실기기에서 KoELECTRA INT8(LiteRT)로 분류하고,
 * 각 예측을 "EMO_EVAL" 태그로 로그에 남긴다. (원본 HF 모델 결과와 오프라인 비교용)
 */
@RunWith(AndroidJUnit4::class)
class EmotionOnDeviceEvalTest {

    @Test
    fun evaluateTestSetOnDevice() {
        val instr = InstrumentationRegistry.getInstrumentation()
        // 테스트 세트: 테스트 APK(자기 자신) assets 에서 로드
        val json = instr.context.assets.open("emotion_testset.json")
            .bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val samples = root.getJSONArray("samples")

        // 모델: 병합된 main assets 의 emotion_int8.tflite 로드.
        // 네이티브 Interpreter/토크나이저 핸들을 쥐므로, 예외가 나도 닫히게 use 로 감싼다.
        LiteRtClassifier.load(instr.targetContext, EmotionModelSpec.SPEC).use { classifier ->
            var correct = 0
            val perGold = HashMap<String, IntArray>() // [correct, total]
            Log.i(TAG, "===EMO_EVAL_START=== total=${samples.length()}")
            for (i in 0 until samples.length()) {
                val s = samples.getJSONObject(i)
                val text = s.getString("text")
                val gold = s.getString("label")
                val result = classifier.classify(text)
                val pred = LABEL_EN[result.topLabel] ?: result.topLabel
                val conf = result.confidence
                if (pred == gold) correct++
                val bucket = perGold.getOrPut(gold) { IntArray(2) }
                bucket[1]++
                if (pred == gold) bucket[0]++
                // 파싱 가능한 한 줄 (RESULT|idx|gold|pred|conf|text)
                Log.i(TAG, "RESULT|$i|$gold|$pred|${"%.4f".format(conf)}|$text")
            }
            val n = samples.length()
            Log.i(TAG, "===EMO_EVAL_SUMMARY=== accuracy=$correct/$n (${"%.1f".format(correct * 100.0 / n)}%)")
            for ((gold, b) in perGold) {
                Log.i(TAG, "PERLABEL|$gold|${b[0]}/${b[1]}")
            }
            Log.i(TAG, "===EMO_EVAL_END===")
        }
    }

    companion object {
        private const val TAG = "EMO_EVAL"

        /** 한글 라벨 → 원본 모델 영문 라벨(id2label). 평가 로그를 gold 라벨과 대조하기 위한 테스트 전용 매핑. */
        private val LABEL_EN = mapOf(
            "분노" to "angry",
            "불안" to "anxious",
            "당황" to "embarrassed",
            "기쁨" to "happy",
            "상처" to "heartache",
            "슬픔" to "sad",
        )
    }
}
