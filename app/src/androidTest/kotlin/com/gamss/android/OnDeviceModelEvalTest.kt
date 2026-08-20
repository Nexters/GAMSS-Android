package com.gamss.android

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamss.android.data.emotion.AndroidEmotionClassifier
import com.gamss.android.data.model.LocalAssetsModelSource
import com.gamss.android.data.summary.AndroidDiarySummarizer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * 온디바이스 감정 분류·원문 요약 검증. :data 가 아니라 :app 에 두는 이유는 예전 애셋팩(PAD) 경로일 때
 * 애셋팩 패키지가 앱과 일치해야 했기 때문인데, 지금은 [LocalAssetsModelSource]로 debug variant 의
 * APK 에 번들된 assets(:models:emotion-pack/:models:summary-pack 원본을 data 모듈 debug sourceSet 이
 * 참조)를 직접 읽으므로 PAD/bundletool 준비가 전혀 필요 없다 — `:app:connectedDebugAndroidTest`
 * 하나로 바로 돈다. :app 은 abiFilters 가 arm64-v8a 하나라 arm64 기기/AVD 에서만 돈다.
 */
@RunWith(AndroidJUnit4::class)
class OnDeviceModelEvalTest {

    private val diaries = listOf(
        "오늘 회사에서 부장님한테 사람들 앞에서 크게 혼났다. 딱히 내 잘못도 아니었는데 억울하고 " +
            "하루종일 기분이 안 좋았다. 집에 와서도 계속 그 생각만 났다.",
        "드디어 몇 달간 준비하던 자격증 시험에 합격했다. 밤새 공부한 게 헛되지 않은 것 같아 너무 " +
            "뿌듯하고 기뻤다. 엄마한테 전화했더니 엄마도 같이 좋아서 울었다.",
        "15년을 함께한 강아지가 오늘 무지개다리를 건넜다. 이제 곁에 없다는 게 아직 실감이 안 난다. " +
            "빈 밥그릇을 치우다가 한참을 울었다.",
        "내일 중요한 발표가 있는데 준비를 제대로 못 한 것 같아 너무 불안하다. 자려고 누워도 자꾸 " +
            "실수하는 장면만 떠올라서 잠이 오지 않는다.",
    )

    @Test
    fun summarizeOnDevice(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val summarizer = AndroidDiarySummarizer(LocalAssetsModelSource(context))
        Log.i(TAG, "===SUMMARY_START=== total=${diaries.size}")
        diaries.forEachIndexed { index, diary ->
            val started = System.nanoTime()
            val summary = summarizer.summarize(diary)
            val ms = (System.nanoTime() - started) / 1_000_000
            Log.i(TAG, "SUMMARY|$index|${ms}ms|입력=$diary")
            Log.i(TAG, "SUMMARY|$index|출력=$summary")
            // 품질까지는 로그로 눈으로 본다.
            assertTrue("요약이 비었습니다(index=$index)", summary.isNotBlank())
        }
        Log.i(TAG, "===SUMMARY_END===")
    }

    @Test
    fun classifyOnDevice(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val testSet = JSONObject(
            instrumentation.context.assets.open(TEST_SET).bufferedReader().use { it.readText() },
        )
        val samples = testSet.getJSONArray("samples")
        // 모델은 한국어 라벨, 테스트셋은 영문 라벨을 쓴다.
        val labelKo = testSet.getJSONObject("label_ko")
        val koToEn = labelKo.keys().asSequence().associateBy { labelKo.getString(it) }

        val classifier = AndroidEmotionClassifier(LocalAssetsModelSource(instrumentation.targetContext))
        val perLabel = HashMap<String, IntArray>() // 라벨 → [정답, 전체]
        var correct = 0
        Log.i(TAG, "===EMOTION_START=== total=${samples.length()}")
        for (index in 0 until samples.length()) {
            val sample = samples.getJSONObject(index)
            val text = sample.getString("text")
            val gold = sample.getString("label")
            val result = classifier.classify(text)
            // 폴백을 두면 매핑 파손이 정확도 저하로 위장된다.
            val predicted = koToEn[result.topLabel]
                ?: error("테스트셋 label_ko 에 없는 모델 라벨: ${result.topLabel}")

            val bucket = perLabel.getOrPut(gold) { IntArray(2) }
            bucket[1]++
            if (predicted == gold) {
                correct++
                bucket[0]++
            }
            val confidence = String.format(Locale.ROOT, "%.4f", result.confidence)
            Log.i(TAG, "EMOTION|$index|$gold|$predicted|$confidence|$text")
        }

        val total = samples.length()
        val percent = String.format(Locale.ROOT, "%.1f", correct * PERCENT / total)
        Log.i(TAG, "===EMOTION_END=== accuracy=$correct/$total ($percent%)")
        perLabel.toSortedMap().forEach { (label, bucket) ->
            Log.i(TAG, "PERLABEL|$label|${bucket[0]}/${bucket[1]}")
        }

        assertTrue("정확도 회귀: $correct/$total (하한 $ACCURACY_FLOOR)", correct >= ACCURACY_FLOOR)
    }

    private companion object {
        const val TAG = "ONDEVICE_EVAL"
        const val TEST_SET = "emotion_testset.json"
        const val PERCENT = 100.0

        /** 교체 시점 실측 44/60. 흔들리면 모델이나 추론 런타임이 바뀐 것이다. */
        const val ACCURACY_FLOOR = 40
    }
}
