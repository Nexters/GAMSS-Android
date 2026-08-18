package com.gamss.android.feature.calendar.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.calendar.component.CardDetailDialog
import java.time.LocalDate

/**
 * 카드 상세 팝업의 인스타그램 스토리 공유를 검증하는 디버그 전용 화면. 프로덕션 아님.
 *
 * 로그인·서버·실제 카드 없이 [CardDetailDialog] 를 그대로 띄운다. 공유 경로(카드 캡처 →
 * FileProvider → 인스타그램 인텐트)가 Dialog 창 안에서도 동작하는지 보려면 실제 화면과
 * 같은 컴포저블을 써야 하므로, 팝업을 흉내 내지 않고 [CardDetailDialog] 를 직접 호출한다.
 *
 * 실행: adb shell am start -n com.gamss.android.dev/com.gamss.android.feature.calendar.debug.CardShareDebugActivity
 */
class CardShareDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GamssTheme {
                CardDetailDialog(
                    card = SampleCard,
                    onDismiss = ::finish,
                    onDiscardClick = {},
                    onViewConversationClick = {},
                )
            }
        }
    }
}

private val SampleCard = Card(
    id = 1L,
    conversationId = 1L,
    character = EmotionCharacter.ANGER,
    emotionLabel = "분노",
    summary = "오늘 화~나네",
    message = "회의에서 준비한 자료로 한 소리 들었지만 그래도 내일은 나아질 거야.",
    date = LocalDate.of(2026, 8, 3),
)
