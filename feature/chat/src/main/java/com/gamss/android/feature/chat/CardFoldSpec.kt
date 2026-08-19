package com.gamss.android.feature.chat

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** 한 번 누를 때 다음 접힘 모양까지 걸리는 시간. */
internal const val CARD_FOLD_STEP_DURATION_MS = 260

/**
 * 통 입구까지의 거리 중 이만큼 내리면 손을 떼도 버려진 것으로 본다.
 * 낮추면 스치기만 해도 버려지고, 높이면 끝까지 끌어야 해서 손이 통을 가린다.
 */
internal const val CARD_FOLD_DISCARD_THRESHOLD = 0.6f

/** 통에 들어가는 모습을 최소로 유지하는 시간. 바로 닫으면 들어간 걸 못 본다. */
internal const val CARD_FOLD_DISCARD_HOLD_MS = 220L

/**
 * 단계별 종이 크기. 원본은 [com.gamss.android.core.designsystem.card.GamssImageCard] 와 같은
 * 366×528 이고, 이후 두 단계는 img_paper_folded_once/twice 원본 비율(1110×813, 774×813)에
 * 맞춘 값이다. 아래로 한 번 접어 높이가 줄고, 옆으로 한 번 접어 폭이 줄어든다.
 */
internal val CardFoldStage.paperSize: DpSize
    get() = when (this) {
        CardFoldStage.Unfolded -> DpSize(366.dp, 528.dp)
        CardFoldStage.FoldedOnce -> DpSize(366.dp, 268.dp)
        CardFoldStage.FoldedTwice -> DpSize(255.dp, 268.dp)
    }

// 아래 값은 실기기에서 눈으로 맞춰야 하는 연출값이다. 바꾸면 접기와 드래그를 다시 확인한다.

/** 통이 화면 아래에 걸치는 높이. 입구가 화면 안에 보여야 어디로 내릴지 알 수 있다. */
internal val CardFoldBinHeight = 132.dp

/** 통 입구 띠의 두께. */
internal val CardFoldBinMouthHeight = 34.dp

/** 종이가 통 입구에 얼마나 파묻힌 상태를 "들어갔다"로 볼지. */
internal val CardFoldBinSinkDepth = 40.dp

/** 안내 문구와 종이 사이 간격. */
internal val CardFoldGuideGap = 28.dp

/** 드래그 방향을 알려주는 화살표 크기. */
internal val CardFoldArrowSize = 56.dp

/** 화살표는 힌트라 종이보다 약하게 둔다. */
internal const val CARD_FOLD_ARROW_ALPHA = 0.85f
