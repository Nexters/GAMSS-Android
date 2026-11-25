package com.gamss.android.feature.game.clicker.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.gamss.android.feature.game.R

private const val PRESSED_SCALE = 0.94f
private const val PRESS_ANIMATION_MS = 50

/**
 * 손을 떼는 시점이 아니라 닿는 순간 [onPress]를 부른다. 연타는 누르는 박자로 세야 손맛이 맞는다.
 */
@Composable
internal fun ClickerKeyCap(
    onPress: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val currentOnPress by rememberUpdatedState(onPress)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = tween(PRESS_ANIMATION_MS),
        label = "keyCapScale",
    )

    Image(
        painter = painterResource(R.drawable.game_clicker_key),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            // 눌려 작아진 모양을 따라 터치 영역까지 줄면 가장자리 연타가 빗나간다.
            .pointerInput(interactionSource) { detectEachDown(interactionSource) { currentOnPress() } }
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                onClick {
                    currentOnPress()
                    true
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    )
}

/** 한 키를 두 손가락으로 번갈아 쳐도 닿을 때마다 센다. 눌린 모양은 손가락이 하나라도 닿아 있는 동안 유지한다. */
private suspend fun PointerInputScope.detectEachDown(
    interactionSource: MutableInteractionSource,
    onDown: () -> Unit,
) {
    awaitEachGesture {
        val firstDown = awaitFirstDown()
        val press = PressInteraction.Press(firstDown.position)
        interactionSource.tryEmit(press)
        firstDown.consume()
        onDown()
        do {
            val event = awaitPointerEvent()
            event.changes.fastForEach {
                if (it.changedToDown()) {
                    it.consume()
                    onDown()
                }
            }
        } while (event.changes.fastAny { it.pressed })
        interactionSource.tryEmit(PressInteraction.Release(press))
    }
}
