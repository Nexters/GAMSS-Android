package com.gamss.android.feature.chat

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** 한 번 누를 때 다음 접힘 모양까지 걸리는 시간. */
internal const val CARD_FOLD_STEP_DURATION_MS = 260

/**
 * 통 입구까지의 거리 중 절반을 내리면 손을 떼도 버려진 것으로 본다.
 * 낮추면 스치기만 해도 버려지고, 높이면 끝까지 끌어야 해서 손이 통을 가린다.
 */
internal const val CARD_FOLD_DISCARD_THRESHOLD = 0.5f

/** 통에 들어가는 모습을 최소로 유지하는 시간. 바로 닫으면 들어간 걸 못 본다. */
internal const val CARD_FOLD_DISCARD_HOLD_MS = 220L

/**
 * 단계별 종이가 차지하는 칸의 크기. 접힌 두 단계는 그림 파일 안에 그림자용 투명 여백이 있어
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
 * 시안 프레임의 폭. 이보다 좁은 기기에서는 종이와 힌트를 폭 비율만큼 함께 줄인다. 그대로 두면
 * 366dp 카드가 화면을 꽉 채워 시안의 좌우 여백이 사라진다.
 */
internal val CardFoldDesignWidth = 402.dp

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

/** 안내 문구와 종이 사이 간격. */
internal val CardFoldGuideGap = 29.dp

/** 카드 안에 겹쳐 놓는 접기 안내 손글씨의 크기와, 카드 아래끝에서 띄우는 거리. */
internal val CardFoldTapGuideSize = DpSize(176.dp, 31.dp)
internal val CardFoldTapGuideBottomInset = 70.dp

/** 드래그 방향을 알려주는 화살표 크기. 그림 파일의 여백까지 포함한 값이다. */
internal val CardFoldArrowSize = DpSize(108.dp, 218.dp)

/** 화살촉과 통 사이 간격. 화살표는 통을 가리키므로 통을 기준으로 붙인다. */
internal val CardFoldArrowBinGap = 22.dp
