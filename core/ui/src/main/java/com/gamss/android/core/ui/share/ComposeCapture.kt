package com.gamss.android.core.ui.share

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

/** 그리기를 기다리다 포기하는 시간. 창이 가려져 아예 다시 그려지지 않는 경우를 끊는다. */
private const val CAPTURE_TIMEOUT_MILLIS = 1_000L

/**
 * [Modifier.captureTo] 를 붙인 컴포저블을 비트맵으로 가져온다.
 *
 * 공유 전에 버튼을 감추는 것처럼 화면을 바꾼 뒤 캡처해야 할 때가 있는데, 바뀐 그림이 레이어에
 * 들어오는 시점은 프레임 수로 셀 수 없다. 그래서 그리기가 실제로 끝날 때마다 신호를 남기고
 * [captureAfterNextDraw] 가 그 신호를 기다린다.
 */
class ComposeCapture internal constructor(internal val layer: GraphicsLayer) {

    /** 그리기 신호. 최신 하나만 있으면 되므로 [Channel.CONFLATED] 를 쓴다. */
    private val draws = Channel<Unit>(Channel.CONFLATED)

    internal fun onDrawn() {
        draws.trySend(Unit)
    }

    /**
     * 다음 그리기가 끝난 뒤의 그림을 준다. 그 사이 화면을 바꿔 뒀다면 바뀐 그림이 담긴다.
     * [CAPTURE_TIMEOUT_MILLIS] 안에 한 번도 그려지지 않으면 `null`.
     *
     * 호출부는 다 쓴 비트맵을 직접 [Bitmap.recycle] 한다.
     */
    suspend fun captureAfterNextDraw(): Bitmap? {
        // 이 호출 전에 쌓인 신호는 바꾸기 전 그림이므로 버린다.
        while (draws.tryReceive().isSuccess) Unit
        return withTimeoutOrNull(CAPTURE_TIMEOUT_MILLIS) {
            draws.receive()
            layer.toImageBitmap().asAndroidBitmap()
        }
    }
}

@Composable
fun rememberComposeCapture(): ComposeCapture {
    val layer = rememberGraphicsLayer()
    return remember(layer) { ComposeCapture(layer) }
}

/** 이 컴포저블이 그려질 때마다 [capture] 에 같은 그림을 남긴다. */
fun Modifier.captureTo(capture: ComposeCapture): Modifier =
    drawWithContent {
        capture.layer.record { this@drawWithContent.drawContent() }
        drawLayer(capture.layer)
        capture.onDrawn()
    }
