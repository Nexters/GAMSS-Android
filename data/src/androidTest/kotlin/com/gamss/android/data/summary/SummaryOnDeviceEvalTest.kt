package com.gamss.android.data.summary

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamss.android.core.common.getOrNull
import com.gamss.android.domain.summary.SummarizeDiaryUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 온디바이스 원문 요약 검증. 실기기에서 kobart INT8(ONNX Runtime Mobile)로 한국어 일기를 요약하고
 * 각 결과를 "SUMMARY_EVAL" 태그로 로그에 남긴다. (Python 레퍼런스 출력과 품질 비교용)
 *
 * 모델은 이제 :models:summary-pack(on-demand 애셋팩)에서 내려받는다. 실제 Play 배포 없이
 * 로컬에서 돌리려면 `bundletool build-apks --local-testing` 으로 만든 로컬 테스트 APK 를 설치해야
 * 애셋팩이 채워진다(그냥 :data:connectedAndroidTest 만으로는 팩이 비어 있어 실패한다).
 */
@RunWith(AndroidJUnit4::class)
class SummaryOnDeviceEvalTest {

    private val diaries = listOf(
        "오늘 회사에서 부장님한테 사람들 앞에서 크게 혼났다. 딱히 내 잘못도 아니었는데 억울하고 하루종일 기분이 안 좋았다. 집에 와서도 계속 그 생각만 났다.",
        "드디어 몇 달간 준비하던 자격증 시험에 합격했다. 밤새 공부한 게 헛되지 않은 것 같아 너무 뿌듯하고 기뻤다. 엄마한테 전화했더니 엄마도 같이 좋아서 울었다.",
        "15년을 함께한 강아지가 오늘 무지개다리를 건넜다. 이제 곁에 없다는 게 아직 실감이 안 난다. 빈 밥그릇을 치우다가 한참을 울었다.",
        "내일 중요한 발표가 있는데 준비를 제대로 못 한 것 같아 너무 불안하다. 자려고 누워도 자꾸 실수하는 장면만 떠올라서 잠이 오지 않는다.",
    )

    @Test
    fun summarizeOnDevice() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        OnnxKobartSummarizer.load(context).use { summarizer ->
            Log.i(TAG, "===SUMMARY_EVAL_START=== total=${diaries.size}")
            diaries.forEachIndexed { i, diary ->
                val started = System.nanoTime()
                val summary = summarizer.summarize(diary)
                val ms = (System.nanoTime() - started) / 1_000_000
                Log.i(TAG, "RESULT|$i|${ms}ms|입력=$diary")
                Log.i(TAG, "RESULT|$i|요약=$summary")
            }
            Log.i(TAG, "===SUMMARY_EVAL_END===")
        }
    }

    /**
     * 게이팅 검증: 짧은 대화 입력은 요약기를 태우지 않고 원문 그대로(반복 루프 없음),
     * 긴 일기는 요약된다. 실제 대화방 USER 발화(짧음) vs 일기(김)로 확인.
     */
    @Test
    fun gatingShortVsLong() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val useCase = SummarizeDiaryUseCase(AndroidDiarySummarizer(context))
        runBlocking {
            val short = useCase(listOf("오늘 억울한 일이 있었어", "그러게 그냥 맛있는거 먹고 쉬려고")).getOrNull()
            val long = useCase(listOf(diaries[0])).getOrNull()
            Log.i(TAG, "GATING|short(원문 유지 기대)|$short")
            Log.i(TAG, "GATING|long(요약 기대)|$long")
        }
    }

    companion object {
        private const val TAG = "SUMMARY_EVAL"
    }
}
