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

/** 시간은 매 프레임 흐르므로 화면에는 바뀐 단위로만 알리는 파생 값을 내보낸다. */
@Stable
internal class ClickerGameController(initial: ClickerGameState = ClickerGameState()) {
    var state by mutableStateOf(initial)
        private set

    val isFinished by derivedStateOf { state.phase == ClickerGamePhase.Finished }
    val countdownSecond by derivedStateOf { state.countdownSecond }
    val score by derivedStateOf { state.score }
    val remainingTenths by derivedStateOf { state.remainingTenths }

    // 접근성 값이 매 프레임 바뀌지 않도록 1% 단위로 끊는다.
    val gaugeProgress by derivedStateOf { (state.gauge * PERCENT).roundToInt() / PERCENT }

    val gauge by derivedStateOf { state.gauge }

    fun tap(): Boolean {
        val next = state.tap()
        if (next === state) return false
        state = next
        return true
    }

    fun replay() {
        state = ClickerGameState()
    }

    suspend fun runClock(onFinished: (score: Int) -> Unit) {
        var lastFrameMs = withFrameMillis { it }
        while (!isFinished) {
            val frameMs = withFrameMillis { it }
            advance((frameMs - lastFrameMs).coerceAtMost(MAX_FRAME_DELTA_MS), onFinished)
            lastFrameMs = frameMs
        }
    }

    fun advance(deltaMs: Long, onFinished: (score: Int) -> Unit) {
        val wasFinished = isFinished
        state = state.advance(deltaMs)
        if (!wasFinished && isFinished) onFinished(state.score)
    }

    companion object {
        val Saver = listSaver<ClickerGameController, Long>(
            save = { with(it.state) { listOf(score.toLong(), elapsedMs) } },
            restore = { ClickerGameController(ClickerGameState(score = it[0].toInt(), elapsedMs = it[1])) },
        )
    }
}

@Composable
internal fun rememberClickerGameController(): ClickerGameController =
    rememberSaveable(saver = ClickerGameController.Saver) { ClickerGameController() }
