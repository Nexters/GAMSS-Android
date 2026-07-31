package com.gamss.android.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gamss.android.app.ui.theme.GamssTheme
import com.gamss.android.data.remote.conversation.model.response.ConversationMessage
import com.gamss.android.data.remote.conversation.model.response.userUtterances
import com.gamss.android.domain.card.CardInput
import com.gamss.android.domain.card.GenerateCardInputUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 디버그/검증 전용 화면. 프로덕션 아님.
 * 시드 대화(짧음/긴 멀티턴)에서 USER 발화를 추출해 온디바이스 카드 파이프라인
 * (감정 분류 + 캐릭터 매핑 + kobart 요약)을 돌려 결과를 화면에 표시한다.
 * 실행: adb shell am start -n com.gamss.android/com.gamss.android.app.debug.CardDebugActivity
 */
@AndroidEntryPoint
class CardDebugActivity : ComponentActivity() {

    @Inject
    lateinit var generateCardInput: GenerateCardInputUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GamssTheme {
                CardDebugScreen(generateCardInput)
            }
        }
    }
}

private data class DebugResult(
    val title: String,
    val utterances: List<String>,
    val card: CardInput?,
)

@Composable
private fun CardDebugScreen(useCase: GenerateCardInputUseCase) {
    val results by produceState<List<DebugResult>?>(initialValue = null) {
        value = SAMPLES.map { (title, messages) ->
            val utts = messages.userUtterances()
            DebugResult(title, utts, useCase(utts))
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("카드 파이프라인 디버그 (온디바이스)", style = MaterialTheme.typography.titleLarge)
            val current = results
            if (current == null) {
                CircularProgressIndicator()
                Text("모델 로드 + 감정/요약 추론 중...")
            } else {
                current.forEach { ResultCard(it) }
            }
        }
    }
}

@Composable
private fun ResultCard(result: DebugResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(result.title, style = MaterialTheme.typography.titleMedium)
            Text("USER 발화 (${result.utterances.size}):", style = MaterialTheme.typography.labelLarge)
            result.utterances.forEach { Text("· $it") }
            val card = result.card
            if (card == null) {
                Text("→ 감정 없음 (USER 발화 없음)")
            } else {
                Text("감정: ${card.emotion.koLabel}  →  캐릭터: ${card.character.displayName}")
                Text("요약: ${card.summary ?: "(없음)"}")
            }
        }
    }
}

/** 시드 대화: (제목, 서버 메시지 목록). USER 발화만 파이프라인에 들어간다. */
private val SAMPLES: List<Pair<String, List<ConversationMessage>>> = listOf(
    "샘플 C · 긴 멀티턴 (실제 압축 요약)" to listOf(
        msg(101, "USER", null, "아 오늘 진짜 최악이었어 아침부터 지하철을 놓쳐서 회사에 지각했거든"),
        msg(102, "USER", null, "근데 하필 그날 팀 회의가 있어서 부장님이 다들 앞에서 나만 콕 집어서 뭐라고 했어"),
        msg(103, "CHARACTER", "ANGER", "사람들 앞에서? 진짜 너무하네."),
        msg(104, "USER", null, "내가 준비한 자료가 부족했다는 건데 사실 그건 어제 갑자기 시킨 거였잖아"),
        msg(105, "USER", null, "너무 억울하고 분한데 아무 말도 못 하고 그냥 듣고만 있었어"),
        msg(106, "USER", null, "점심도 혼자 먹고 오후 내내 일이 손에 안 잡히더라고"),
        msg(107, "CHARACTER", "WARM", "많이 속상했겠다..."),
        msg(108, "USER", null, "집에 오는 길에 괜히 눈물이 났어 내가 뭘 그렇게 잘못했나 싶고"),
        msg(109, "USER", null, "그냥 오늘은 아무것도 안 하고 맛있는거 시켜 먹고 일찍 잘래"),
        msg(110, "USER", null, "내일은 좀 나아지겠지"),
    ),
    "샘플 A · 짧은 대화 (요약 게이팅 → 원문 유지)" to listOf(
        msg(1, "USER", null, "오늘 억울한 일이 있었어"),
        msg(2, "CHARACTER", "GRUMPY", "세상에 억울한 일 한두 개냐, 그냥 넘겨."),
        msg(3, "CHARACTER", "ANGER", "억울하면 짚고 넘어가야지. 누가 그랬는지 이름 대."),
        msg(11, "USER", null, "그러게 그냥 맛있는거 먹고 쉬려고"),
    ),
    "샘플 B · 긴 멀티턴 (실제 요약)" to listOf(
        msg(1, "USER", null, "오늘 회사에서 부장님이 사람들 다 있는 앞에서 나한테 소리를 질렀어"),
        msg(2, "CHARACTER", "ANGER", "뭐? 사람들 앞에서? 그건 선 넘었네."),
        msg(3, "USER", null, "내가 뭘 그렇게 잘못했다고 그러는지 진짜 억울하고 분해"),
        msg(4, "CHARACTER", "ANXIETY", "그러다 더 찍히면 어떡해..?"),
        msg(5, "USER", null, "집에 와서도 계속 그 생각만 나서 아무것도 손에 안 잡혀"),
        msg(6, "USER", null, "그냥 오늘은 맛있는거 먹고 일찍 자려고"),
    ),
)

private fun msg(id: Long, sender: String, emotion: String?, content: String) = ConversationMessage(
    id = id,
    conversationId = 1,
    senderType = sender,
    emotionType = emotion,
    content = content,
    repliesToMessageId = null,
    rootMessageId = null,
    createdAt = "2026-07-27T00:00:00Z",
)
