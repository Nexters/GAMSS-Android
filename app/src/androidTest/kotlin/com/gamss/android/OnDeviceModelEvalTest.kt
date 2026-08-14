package com.gamss.android

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamss.android.data.emotion.AndroidEmotionClassifier
import com.gamss.android.data.summary.AndroidDiarySummarizer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * 온디바이스 감정 분류·원문 요약을 실기기에서 검증한다.
 *
 * :data 의 같은 성격 테스트는 라이브러리 모듈이라 테스트 APK 패키지가 애셋팩 패키지와 달라
 * Play Core 가 팩을 거부해서 영영 돌지 않았다. :app 은 패키지가 일치해 로컬 테스트 경로가 통한다.
 *
 * :app 은 abiFilters 가 arm64-v8a 하나라 **arm64 기기/AVD 에서만** 돈다. x86_64 AVD 에서는
 * UnsatisfiedLinkError 가 난다.
 *
 * Gradle 의 connectedAndroidTest 는 매번 재설치하며 외부 저장소를 지우므로 쓰지 않는다.
 *
 * ```
 * ./gradlew :app:bundleDebug
 * bundletool build-apks --bundle=app/build/outputs/bundle/debug/app-debug.aab \
 *   --output=/tmp/app.apks --local-testing --connected-device \
 *   --ks=~/.android/debug.keystore --ks-pass=pass:android \
 *   --ks-key-alias=androiddebugkey --key-pass=pass:android
 * unzip -o /tmp/app.apks -d /tmp/apks
 *
 * ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 *
 * # 외부 파일 디렉터리는 앱이 만들게 둔다. shell 이 먼저 만들면 앱이 읽지 못하는 기기가 있다.
 * adb shell am instrument -w com.gamss.android.dev.test/androidx.test.runner.AndroidJUnitRunner
 * DIR=/sdcard/Android/data/com.gamss.android.dev/files/local_testing
 * adb shell mkdir -p $DIR
 * adb push /tmp/apks/asset-slices/emotion_pack-master.apk $DIR/
 * adb push /tmp/apks/asset-slices/summary_pack-master.apk $DIR/
 *
 * adb shell am instrument -w -e class com.gamss.android.OnDeviceModelEvalTest \
 *   com.gamss.android.dev.test/androidx.test.runner.AndroidJUnitRunner
 * adb logcat -d -s ONDEVICE_EVAL
 * ```
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
        val summarizer = AndroidDiarySummarizer(context)
        Log.i(TAG, "===SUMMARY_START=== total=${diaries.size}")
        diaries.forEachIndexed { index, diary ->
            val started = System.nanoTime()
            val summary = summarizer.summarize(diary)
            val ms = (System.nanoTime() - started) / 1_000_000
            Log.i(TAG, "SUMMARY|$index|${ms}ms|입력=$diary")
            Log.i(TAG, "SUMMARY|$index|출력=$summary")
            // 디코딩이 무너지면 빈 문자열이나 특수 토큰만 남는다. 품질까지는 로그로 눈으로 본다.
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
        // 모델은 한국어 라벨을 내고 테스트셋은 영문 라벨을 쓴다. 대응표는 테스트셋의 label_ko 를 뒤집어 쓴다.
        val labelKo = testSet.getJSONObject("label_ko")
        val koToEn = labelKo.keys().asSequence().associateBy { labelKo.getString(it) }

        val classifier = AndroidEmotionClassifier(instrumentation.targetContext)
        val perLabel = HashMap<String, IntArray>() // 라벨 → [정답, 전체]
        var correct = 0
        Log.i(TAG, "===EMOTION_START=== total=${samples.length()}")
        for (index in 0 until samples.length()) {
            val sample = samples.getJSONObject(index)
            val text = sample.getString("text")
            val gold = sample.getString("label")
            val result = classifier.classify(text)
            // 폴백을 두면 매핑이 깨졌을 때 정확도 저하로 위장된다. 즉시 끊는다.
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
        // 6클래스 중 어느 감정이 무너지는지가 전체 정확도보다 진단에 유용하다.
        perLabel.toSortedMap().forEach { (label, bucket) ->
            Log.i(TAG, "PERLABEL|$label|${bucket[0]}/${bucket[1]}")
        }

        assertTrue("정확도 회귀: $correct/$total (하한 $ACCURACY_FLOOR)", correct >= ACCURACY_FLOOR)
    }

    private companion object {
        const val TAG = "ONDEVICE_EVAL"
        const val TEST_SET = "emotion_testset.json"
        const val PERCENT = 100.0

        /**
         * 교체 시점 실측이 44/60(73.3%)이다. 토크나이저 교체는 골든 대조로 입력 동일성이 보장되므로
         * 이 값이 흔들린다면 모델이나 추론 런타임이 바뀐 것이다. 노이즈 여유를 두고 40 으로 잡는다.
         */
        const val ACCURACY_FLOOR = 40
    }
}
