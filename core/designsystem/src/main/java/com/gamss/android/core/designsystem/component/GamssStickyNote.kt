package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.theme.GamssTheme

private val NoteGroupSize = DpSize(116.12.dp, 120.75.dp)
private const val SHADOW_ROTATION = -9.67f
private const val NOTE_ROTATION = -16.26f
private const val TAPE_ROTATION = -11.26f
private const val TEXT_ROTATION = -20.3f
private const val TEXT_ALPHA = 0.7f

/**
 * 보드에 붙인 포스트잇.
 *
 * 종이 두 장이 살짝 어긋나게 겹치고 위를 마스킹 테이프로 붙인 모양이라 조각마다 회전각이 다르다.
 * 좌표와 각도는 Figma 노드(3138:6119)를 그대로 옮긴 값이라 하나만 바꾸면 겹침이 어긋난다.
 */
@Composable
fun GamssStickyNote(
    text: String,
    modifier: Modifier = Modifier,
    noteColor: Color = GamssTheme.sticker.stickyYellow,
    shadowColor: Color = GamssTheme.sticker.stickyShadow,
    tapeColor: Color = GamssTheme.sticker.tapeSkyblue,
    textColor: Color = GamssTheme.sticker.handwriting,
) {
    Box(modifier = modifier.size(NoteGroupSize)) {
        Slot(x = 0.7.dp, y = 16.25.dp, size = DpSize(102.5.dp, 102.5.dp)) {
            Box(
                modifier = Modifier
                    .size(88.84.dp)
                    .rotate(SHADOW_ROTATION)
                    .background(shadowColor),
            )
        }
        Slot(x = 0.dp, y = 4.63.dp, size = DpSize(116.12.dp, 116.12.dp)) {
            Box(
                modifier = Modifier
                    .size(93.64.dp)
                    .rotate(NOTE_ROTATION)
                    .background(noteColor),
            )
        }
        Slot(x = 35.25.dp, y = 0.dp, size = DpSize(25.83.dp, 30.48.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 20.98.dp, height = 26.9.dp)
                    .rotate(TAPE_ROTATION)
                    .background(tapeColor),
            )
        }
        Slot(x = 37.99.dp, y = 14.05.dp, size = DpSize(23.14.dp, 16.44.dp)) {
            Image(
                painter = painterResource(R.drawable.img_sticky_tape),
                contentDescription = null,
                modifier = Modifier.rotate(TAPE_ROTATION),
            )
        }
        Slot(x = 30.03.dp, y = 38.81.dp, size = DpSize(57.96.dp, 53.82.dp)) {
            GamssText(
                text = text,
                modifier = Modifier.rotate(TEXT_ROTATION),
                style = GamssTheme.typography.handwritingBody4,
                color = textColor.copy(alpha = TEXT_ALPHA),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Figma 의 "회전 전 크기를 담는 상자" 구조를 그대로 옮긴 자리.
 *
 * 자식은 이 상자 한가운데에서 자기 중심을 기준으로 회전한다.
 */
@Composable
private fun BoxScope.Slot(
    x: Dp,
    y: Dp,
    size: DpSize,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = x, y = y)
            .size(size),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}
