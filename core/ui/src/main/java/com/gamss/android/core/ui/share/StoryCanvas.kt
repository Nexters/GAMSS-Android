package com.gamss.android.core.ui.share

/** 스토리 캔버스 크기(9:16). 인스타그램은 배경 이미지를 이 비율로 늘려 채운다. */
internal const val STORY_WIDTH = 1080
internal const val STORY_HEIGHT = 1920

/** 카드가 스토리 화면에 꽉 차지 않도록 남기는 좌우·상하 여백 비율. */
private const val HORIZONTAL_MARGIN_RATIO = 0.16f
private const val VERTICAL_MARGIN_RATIO = 0.10f

/** 스토리 캔버스 안에서 카드가 차지할 영역. */
internal data class StoryBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * [cardWidth] × [cardHeight] 카드를 스토리 캔버스 가운데에 놓을 영역을 구한다.
 *
 * 원본 비율을 유지하면서 여백 안에 들어가는 가장 큰 크기를 고르므로, 세로로 긴 카드는 높이가,
 * 가로로 넓은 카드는 폭이 먼저 한계에 닿는다. 카드 크기가 0 이면(그려지기 전에 캡처된 경우)
 * 놓을 자리를 정할 수 없어 `null`.
 */
internal fun storyCardBounds(cardWidth: Int, cardHeight: Int): StoryBounds? {
    if (cardWidth <= 0 || cardHeight <= 0) return null

    val availableWidth = STORY_WIDTH * (1f - HORIZONTAL_MARGIN_RATIO * 2)
    val availableHeight = STORY_HEIGHT * (1f - VERTICAL_MARGIN_RATIO * 2)
    val scale = minOf(availableWidth / cardWidth, availableHeight / cardHeight)
    val targetWidth = cardWidth * scale
    val targetHeight = cardHeight * scale
    val left = (STORY_WIDTH - targetWidth) / 2f
    val top = (STORY_HEIGHT - targetHeight) / 2f

    return StoryBounds(
        left = left,
        top = top,
        right = left + targetWidth,
        bottom = top + targetHeight,
    )
}
