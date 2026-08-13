package com.gamss.android.feature.chat.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.card.GamssOutlinedCard
import com.gamss.android.core.designsystem.checkbox.GamssCheckbox
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * 대화 한 줄. 선택 모드에서만 체크박스가 보입니다.
 *
 * 체크박스에는 클릭을 걸지 않고 카드 전체가 탭을 받습니다. 두 곳이 각각 클릭을 받으면 체크박스
 * 바로 옆을 눌렀을 때 동작이 갈리고, 접근성 트리에도 같은 동작이 두 번 노출됩니다.
 *
 * 높이는 고정이 아니라 최소값입니다. 제목이 한 줄에 안 들어가면 두 줄까지 늘어나고 카드도 함께
 * 커집니다. 콘텐츠 폭이 334dp 라 한 줄로는 20자 남짓만 들어가는데, 선택 모드에서는 체크박스가
 * 32dp 를 더 먹어 그보다 짧아집니다.
 */
@Suppress("LongParameterList")
@Composable
internal fun ConversationCard(
    title: String,
    timeLabel: String?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GamssOutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ConversationCardMinHeight)
            .semantics { if (isSelectionMode) selected = isSelected },
        onClick = onClick,
        onLongClick = onLongClick,
        borderColor = if (isSelectionMode && !isSelected) {
            GamssTheme.colors.gray200
        } else {
            GamssTheme.colors.gray950
        },
        contentPadding = ConversationCardPadding,
    ) {
        // SpaceBetween 은 쓸 수 없다. 제목이 짧으면 남는 폭이 자식 사이로 흩어져, 시각이 없는
        // 행에서 제목이 오른쪽 끝으로 밀려난다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                GamssCheckbox(checked = isSelected)
                Spacer(modifier = Modifier.width(GamssTheme.spacing.spacing200))
            }

            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = GamssTheme.typography.subtitle4,
                color = GamssTheme.colors.gray900,
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )

            if (timeLabel != null) {
                Spacer(modifier = Modifier.width(GamssTheme.spacing.spacing100))
                Text(
                    text = timeLabel,
                    style = GamssTheme.typography.body6Medium,
                    color = GamssTheme.colors.gray500,
                )
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun ConversationCardLightPreview() {
    GamssTheme(darkTheme = false) {
        ConversationCardPreviewContent()
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun ConversationCardDarkPreview() {
    GamssTheme(darkTheme = true) {
        ConversationCardPreviewContent()
    }
}

@Composable
private fun ConversationCardPreviewContent() {
    Column(
        modifier = Modifier.padding(GamssTheme.spacing.spacing300),
        verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
    ) {
        ConversationCard(
            title = "너무졸려서 지하철에서 걍 눕고싶엇어",
            timeLabel = "오전 4:20",
            isSelectionMode = false,
            isSelected = false,
            onClick = {},
            onLongClick = {},
        )
        ConversationCard(
            title = "부장이랑 싸웠는데 밥도 맛없는 거 먹은 날날날날",
            timeLabel = "오전 2:43",
            isSelectionMode = true,
            isSelected = true,
            onClick = {},
            onLongClick = {},
        )
        ConversationCard(
            title = "스물다섯자짜리제목이두줄로어떻게보이는지확인용문구",
            timeLabel = "오전 4:20",
            isSelectionMode = true,
            isSelected = true,
            onClick = {},
            onLongClick = {},
        )
        ConversationCard(
            title = "제목 없는 대화",
            timeLabel = null,
            isSelectionMode = true,
            isSelected = false,
            onClick = {},
            onLongClick = {},
        )
    }
}

private const val TITLE_MAX_LINES = 2

// 디자인 실측: 카드 366 폭에 내부 334, 좌우 16. 높이 59 는 한 줄일 때의 값이라 최소값으로 둔다.
private val ConversationCardMinHeight = 59.dp
private val ConversationCardPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
