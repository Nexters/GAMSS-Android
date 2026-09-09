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
 * [captureWith] 가 그 신호를 기다린다.
 */
class ComposeCapture internal constructor(internal val layer: GraphicsLayer) {

    /** 그리기 신호. 최신 하나만 있으면 되므로 [Channel.CONFLATED] 를 쓴다. */
    private val draws = Channel<Unit>(Channel.CONFLATED)

    internal fun onDrawn() {
        draws.trySend(Unit)
    }

    /**
     * [setCapturing] 으로 캡처용 화면 전환을 켠 뒤, 그 전환이 실제로 그려진 다음의 그림을 준다.
     * 상태를 켜고 끄는 순서를 이 함수가 스스로 지켜서, 호출부가 순서를 잘못 둬 이전 프레임이
     * 그대로 캡처될 일이 없다. 시간 초과나 예외로 끊겨도 [setCapturing] 은 항상 꺼진 채로 끝난다.
     *
     * [CAPTURE_TIMEOUT_MILLIS] 안에 한 번도 그려지지 않으면 `null`.
     * 호출부는 다 쓴 비트맵을 직접 [Bitmap.recycle] 한다.
     */
    suspend fun captureWith(setCapturing: (Boolean) -> Unit): Bitmap? {
        setCapturing(true)
        return try {
            // 이 호출 전에 쌓인 신호는 바꾸기 전 그림이므로 버린다.
            while (draws.tryReceive().isSuccess) Unit
            val drew = withTimeoutOrNull(CAPTURE_TIMEOUT_MILLIS) { draws.receive() } != null
            // toImageBitmap() 은 취소되면 만든 비트맵을 돌려줄 곳이 없어 새 버리므로, 그리기를
            // 기다리는 구간에만 시간 제한을 걸고 캡처 자체는 취소되지 않게 한다.
            if (drew) layer.toImageBitmap().asAndroidBitmap() else null
        } finally {
            setCapturing(false)
        }
    }
}

@Composable
fun rememberComposeCapture(): ComposeCapture {
    val layer = rememberGraphicsLayer()
    return remember(layer) { ComposeCapture(layer) }
}

/**
 * [isCapturing] 인 동안만 이 컴포저블을 [capture] 의 오프스크린 레이어에 기록해 캡처할 수 있게
 * 한다. 레이어 기록 자체가 그리기 비용이라, 캡처를 시도하지 않을 때는 화면에 바로 그려 여분의
 * 레이어를 물고 있지 않는다.
 */
fun Modifier.captureTo(capture: ComposeCapture, isCapturing: Boolean): Modifier =
    drawWithContent {
        if (isCapturing) {
            capture.layer.record { this@drawWithContent.drawContent() }
            drawLayer(capture.layer)
            capture.onDrawn()
        } else {
            drawContent()
        }
    }
