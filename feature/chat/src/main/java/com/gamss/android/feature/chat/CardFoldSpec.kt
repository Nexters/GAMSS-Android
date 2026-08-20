package com.gamss.android.feature.chat

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** 손을 뗀 종이가 통 뒤로 다 들어가기까지 걸리는 시간. */
internal const val CARD_FOLD_DISCARD_SINK_MS = 200

/** 통과 안내, 화살표가 각각 배어 나오는 시간. */
internal const val CARD_FOLD_HINT_FADE_MS = 400

// 다 접히면 쪽지 → 통 → 안내 문구 → 화살표 순으로 하나씩 나온다. 한꺼번에 나오면 접힌 모양을
// 볼 새가 없고, 무엇부터 봐야 하는지도 안 읽힌다. 아래는 마지막으로 접은 시점부터의 시간이다.

/** 접힌 쪽지를 본 뒤 통이 나오기 시작한다. */
internal const val CARD_FOLD_BIN_DELAY_MS = 800

/**
 * 여기서부터 종이를 끌 수 있다. 배어 나오기 시작하는 [CARD_FOLD_BIN_DELAY_MS] 가 아니라 다
 * 나온 뒤여야 한다. 통이 흐릴 때 열어 두면 어디에 버린 건지 못 본 채 화면이 넘어간다.
 */
internal const val CARD_FOLD_BIN_SHOWN_MS = CARD_FOLD_BIN_DELAY_MS + CARD_FOLD_HINT_FADE_MS

/** 통에서 한 박자 뒤 안내 문구. */
internal const val CARD_FOLD_GUIDE_DELAY_MS = CARD_FOLD_BIN_DELAY_MS + 450

/** 안내 문구에서 반 박자 뒤 화살표. 마지막에 나와야 "이 방향으로" 라는 뜻으로 읽힌다. */
internal const val CARD_FOLD_ARROW_DELAY_MS = CARD_FOLD_GUIDE_DELAY_MS + 250

/**
 * 통 입구까지의 거리 중 절반을 내리면 손을 떼도 버려진 것으로 본다.
 * 낮추면 스치기만 해도 버려지고, 높이면 끝까지 끌어야 해서 손이 통을 가린다.
 */
internal const val CARD_FOLD_DISCARD_THRESHOLD = 0.5f

/**
 * 종이가 다 들어간 뒤 통만 남겨 두는 시간. 바로 닫으면 들어간 걸 못 보고, 길면 다 끝난 화면을
 * 쳐다보게 된다. 손을 뗀 뒤 화면이 넘어가기까지는 이 값과 [CARD_FOLD_DISCARD_SINK_MS] 를 더한
 * 만큼 걸린다.
 */
internal const val CARD_FOLD_DISCARD_HOLD_MS = 60L

/**
 * 단계별 종이가 차지하는 칸의 크기.
 *
 * 단계 사이에는 애니메이션을 두지 않는다. 종이 접기는 이어지는 변형이 아니라 한 번에 모양이
 * 바뀌는 동작이라, 중간 모양을 만들어 이어 붙이면 오히려 흐물거린다. 접힌 두 단계는 그림 파일 안에 그림자용 투명 여백이 있어
 * 여백까지 포함한 칸을 준다. 여백을 뺀 실제 종이는 Figma 의 366×528 / 350×250 / 234×251 이 된다.
 *
 * 세 단계는 모두 같은 중심선에 놓인다. Figma trash-01~04 에서 카드가 173..701, 한 번 접힘이
 * 302..572, 두 번 접힘이 312..563 이라 중심이 셋 다 437 로 같다. 그래서 접히는 기준선은 위가
 * 아니라 가운데이고, 종이는 단계가 바뀌어도 화면 중심에 머문다.
 */
internal val CardFoldStage.paperSize: DpSize
    get() = when (this) {
        CardFoldStage.Unfolded -> DpSize(366.dp, 528.dp)
        CardFoldStage.FoldedOnce -> DpSize(366.dp, 268.dp)
        CardFoldStage.FoldedTwice -> DpSize(255.dp, 268.dp)
    }

/**
 * 통 그림(img_paper_discard_bin, Figma 402×232)의 가로:세로. 통은 화면 폭을 꽉 채우고 높이는 이
 * 비율로 따라간다. 높이를 dp 로 박으면 폭이 402 가 아닌 기기에서 손그림 테두리가 눌린다.
 */
internal const val CARD_FOLD_BIN_ASPECT_RATIO = 402f / 232f

/**
 * 통 그림 높이 중 위에서 입구 틈 아래끝까지의 비율. 종이 아래끝이 여기까지 내려오면 틈 안으로
 * 들어간 모습이 된다. 그림에서 재면 232 중 97(틈이 79.3~96.7)이다.
 */
internal const val CARD_FOLD_BIN_SINK_FRACTION = 97f / 232f

/**
 * 안내 문구와 종이 칸 사이 간격.
 *
 * 시안은 종이 그림에서 29dp 를 띄우지만, 두 번 접힌 종이 칸의 위쪽에는 그림자용 투명 여백이
 * 8.6dp(813px 중 26px) 있다. 칸을 기준으로 붙이므로 그만큼 좁혀 잡아야 그림에서 29dp 가 된다.
 */
internal val CardFoldGuideGap = 20.dp

/** 카드 안에 놓는 접기 안내 손글씨의 크기. */
internal val CardFoldTapGuideSize = DpSize(176.dp, 31.dp)

// 본문 아래로 점선, 안내 문구, 손글씨가 차례로 붙는다. Figma 카드 기준으로 본문이 360 에서
// 끝나고 점선 380, 문구 414, 손글씨 446 이라 간격이 아래 값이 된다.
internal val CardFoldSummaryToDividerGap = 20.dp
internal val CardFoldDividerToQuestionGap = 34.dp
internal val CardFoldQuestionToTapGuideGap = 12.dp

/** 드래그 방향을 알려주는 화살표 크기. 그림 파일의 여백까지 포함한 값이다. */
internal val CardFoldArrowSize = DpSize(108.dp, 218.dp)

/**
 * 화살표 그림 아래끝과 통 위끝 사이 간격. 화살표는 통을 가리키므로 통을 기준으로 붙인다.
 * Figma 의 두 export 프레임에서 화살표가 416..634, 통이 642 부터다.
 */
internal val CardFoldArrowBinGap = 8.dp

internal val CardFoldSinkSpec: AnimationSpec<Float> =
    tween(CARD_FOLD_DISCARD_SINK_MS, easing = FastOutSlowInEasing)

/** 튕기면 버리다 만 것처럼 보여 감쇠를 걸어 둔다. */
internal val CardFoldReturnSpec: AnimationSpec<Float> =
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

/** [com.gamss.android.core.designsystem.card.GamssEmotionCard] 의 본문 상한과 같은 값이다. */
internal const val CARD_FOLD_SUMMARY_MAX_LINES = 3

/**
 * 카드가 액션 슬롯을 Figma 값(우상단 28dp)에 맞춰 두므로 아이콘은 슬롯 좌상단에 딱 붙어야 한다.
 * 그런데 터치 영역을 아이콘보다 크게 잡으면 그 차이만큼 아이콘이 안쪽으로 밀리므로,
 * [CardFoldSkipCenteringInset] 만큼 되돌려 아이콘을 시안 위치로 보낸다.
 */
internal val CardFoldSkipIconSize = 20.dp
internal val CardFoldSkipTouchSize = 48.dp
internal val CardFoldSkipCenteringInset = (CardFoldSkipTouchSize - CardFoldSkipIconSize) / 2
