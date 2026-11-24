package com.gamss.android.feature.game.clicker.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.gamss.android.feature.game.R

/** 시안의 game_main 프레임 크기. 아래 좌표는 모두 이 프레임 기준이다. */
internal val ClickerGaugeNoteSize = DpSize(366.dp, 347.dp)

/**
 * 시안의 대기 그림 하나로 그린다. 채움은 그림 아래에, 흰 눈·입은 위에 깔고 둘 다 게이지 높이까지만 보인다.
 * 그래서 0% 는 대기 시안과 같고, 채움에 덮인 눈·입만 흰색이 된다.
 */
@Composable
internal fun ClickerGaugeNote(
    gauge: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(ClickerGaugeNoteSize)) {
        Image(
            painter = painterResource(R.drawable.game_clicker_note_paper),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .offset(x = 34.93.dp, y = 24.96.dp)
                .size(width = 290.dp, height = 303.67.dp),
        )
        Image(
            painter = painterResource(R.drawable.game_clicker_character_fill),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = CharacterModifier.clipGauge(gauge),
        )
        Image(
            painter = painterResource(R.drawable.game_clicker_character),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = CharacterModifier,
        )
        Image(
            painter = painterResource(R.drawable.game_clicker_character_eyes),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = CharacterModifier.clipGauge(gauge),
        )
        Tape(center = CenterOffset(x = 290.27.dp, y = 36.26.dp), rotation = RIGHT_TAPE_ROTATION)
        Tape(center = CenterOffset(x = 49.9.dp, y = 54.93.dp), rotation = LEFT_TAPE_ROTATION)
    }
}

private data class CenterOffset(val x: Dp, val y: Dp)

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

private val TapeSize = DpSize(41.86.dp, 41.83.dp)
private const val RIGHT_TAPE_ROTATION = -15f
private const val LEFT_TAPE_ROTATION = 90f

// 시안 노드(200x189.84, 83,79.08)보다 내보낸 그림이 선 두께만큼 커서 가운데를 맞춘다.
private val CharacterModifier = Modifier
    .offset(x = 81.88.dp, y = 77.53.dp)
    .size(width = 202.25.dp, height = 192.93.dp)

private fun Modifier.clipGauge(gauge: () -> Float): Modifier = drawWithContent {
    clipRect(top = size.height * (1f - gauge().coerceIn(0f, 1f))) {
        this@drawWithContent.drawContent()
    }
}
