package com.gamss.android.feature.game.clicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import kotlin.math.roundToInt

// 화면이 가려지면 프레임이 멈춘다. 돌아왔을 때 그 공백을 한 프레임으로 몰아 받으면 시간이
// 한꺼번에 흘러가 버리므로, 한 프레임에 반영하는 시간을 제한해 가려진 동안은 멈춘 것으로 친다.
private const val MAX_FRAME_DELTA_MS = 100L
private const val PERCENT = 100f

/** [gauge] 는 매 프레임 바뀌므로 그리기 단계에서만 읽는다. */
@Stable
internal class ClickerGameController(initial: ClickerGameState = ClickerGameState()) {
    var state by mutableStateOf(initial)
        private set

    val isPlaying by derivedStateOf { state.phase == ClickerGamePhase.Playing }
    val score by derivedStateOf { state.score }
    val remainingTenths by derivedStateOf { state.remainingTenths }

    // 접근성 값이 매 프레임 바뀌지 않도록 1% 단위로 끊는다.
    val gaugeProgress by derivedStateOf { (state.gauge * PERCENT).roundToInt() / PERCENT }

    val gauge: Float get() = state.gauge

    fun tap(): Boolean {
        val next = state.tap()
        if (next === state) return false
        state = next
        return true
    }

    fun replay() {
        state = ClickerGameState()
    }

    suspend fun runClock(): Nothing {
        var lastFrameMs = withFrameMillis { it }
        while (true) {
            val frameMs = withFrameMillis { it }
            state = state.advance((frameMs - lastFrameMs).coerceAtMost(MAX_FRAME_DELTA_MS))
            lastFrameMs = frameMs
        }
    }

    companion object {
        val Saver = listSaver<ClickerGameController, Any>(
            save = { with(it.state) { listOf(phase.name, score, gauge, elapsedMs) } },
            restore = {
                ClickerGameController(
                    ClickerGameState(
                        phase = ClickerGamePhase.valueOf(it[0] as String),
                        score = it[1] as Int,
                        gauge = it[2] as Float,
                        elapsedMs = it[3] as Long,
                    ),
                )
            },
        )
    }
}

@Composable
internal fun rememberClickerGameController(): ClickerGameController =
    rememberSaveable(saver = ClickerGameController.Saver) { ClickerGameController() }
