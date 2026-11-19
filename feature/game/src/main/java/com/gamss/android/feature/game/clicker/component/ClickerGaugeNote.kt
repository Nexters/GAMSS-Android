package com.gamss.android.feature.game.clicker.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.game.R

/** 시안의 game_main 프레임 크기. 아래 좌표는 모두 이 프레임 기준이다. */
internal val ClickerGaugeNoteSize = DpSize(366.dp, 347.dp)

/**
 * 대기 중에는 시안의 대기 그림을, 시작한 뒤에는 진행 중 그림을 쓴다. 두 그림은 표정과 크기가 다르다.
 * 채움 실루엣을 외곽선 아래에 깔아야 눈·입의 흰 칠과 손그림 선이 채움 위에 그대로 남는다. */
@Composable
internal fun ClickerGaugeNote(
    gauge: Float,
    isIdle: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(ClickerGaugeNoteSize)) {
        NotePaper(
            center = CenterOffset(x = 180.11.dp, y = 178.79.dp),
            rotation = SHADOW_ROTATION,
            modifier = Modifier
                .background(NoteShadowColor.copy(alpha = NOTE_SHADOW_ALPHA)),
        )
        NotePaper(
            center = CenterOffset(x = 182.21.dp, y = 170.76.dp),
            rotation = NOTE_ROTATION,
            modifier = Modifier
                .background(GamssTheme.colors.white)
                .border(NoteBorderWidth, GamssTheme.colors.gray950),
        )
        if (isIdle) {
            Image(
                painter = painterResource(R.drawable.game_clicker_character_idle),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = IdleCharacterModifier,
            )
        } else {
            Image(
                painter = painterResource(R.drawable.game_clicker_character_fill),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = CharacterModifier.drawWithContent {
                    clipRect(top = size.height * (1f - gauge.coerceIn(0f, 1f))) {
                        this@drawWithContent.drawContent()
                    }
                },
            )
            Image(
                painter = painterResource(R.drawable.game_clicker_character_outline),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = CharacterModifier,
            )
        }
        Tape(center = CenterOffset(x = 290.27.dp, y = 36.26.dp), rotation = RIGHT_TAPE_ROTATION)
        Tape(center = CenterOffset(x = 49.9.dp, y = 54.93.dp), rotation = LEFT_TAPE_ROTATION)
    }
}

private data class CenterOffset(val x: Dp, val y: Dp)

@Composable
private fun NotePaper(
    center: CenterOffset,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .offset(x = center.x - NotePaperSize.width / 2, y = center.y - NotePaperSize.height / 2)
            .size(NotePaperSize)
            .rotate(rotation)
            .then(modifier),
    )
}

@Composable
private fun Tape(center: CenterOffset, rotation: Float) {
    Image(
        painter = painterResource(R.drawable.game_clicker_tape),
        contentDescription = null,
        modifier = Modifier
            .offset(x = center.x - TapeSize.width / 2, y = center.y - TapeSize.height / 2)
            .size(TapeSize)
            .rotate(rotation),
    )
}

private val NotePaperSize = DpSize(260.dp, 270.dp)
private val NoteBorderWidth = 2.dp
private const val NOTE_ROTATION = -4.97f
private const val SHADOW_ROTATION = -3.55f

// 시안의 그림자 색이 팔레트에 없는 값이라 그대로 옮긴다.
private val NoteShadowColor = Color(0xFFC4C4C4)
private const val NOTE_SHADOW_ALPHA = 0.6f

private val TapeSize = DpSize(41.86.dp, 41.83.dp)
private const val RIGHT_TAPE_ROTATION = -15f
private const val LEFT_TAPE_ROTATION = 90f

// 시안 노드(200x189.84, 83,79.08)보다 내보낸 그림이 선 두께만큼 커서 가운데를 맞춘다.
private val IdleCharacterModifier = Modifier
    .offset(x = 81.88.dp, y = 77.53.dp)
    .size(width = 202.25.dp, height = 192.93.dp)

private val CharacterModifier = Modifier
    .offset(x = 69.85.dp, y = 80.43.dp)
    .size(width = 229.54.dp, height = 223.81.dp)
