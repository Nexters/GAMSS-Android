package com.gamss.android.feature.game.clicker

internal const val GAME_DURATION_MS = 30_000L
internal const val GAUGE_PER_TAP = 0.02f
internal const val GAUGE_DRAIN_PER_SECOND = 0.1f
private const val MS_PER_SECOND = 1_000f
private const val MS_PER_TENTH = 100L
private const val MS_PER_HUNDREDTH = 10L

// 0.02f 를 50번 더하면 float 오차로 1.0 에 조금 못 미친다.
private const val GAUGE_FULL_TOLERANCE = 0.0001f

internal enum class ClickerGamePhase { Ready, Playing, Finished }

/** 진행 중에는 0.1초 단위로 흐르는 시간을, 끝나면 0.01초 단위 기록이나 시간 초과를 보여 준다. */
internal sealed interface ClickerRecord {
    data class Running(val tenths: Long) : ClickerRecord
    data class Cleared(val hundredths: Long) : ClickerRecord
    data object TimeOver : ClickerRecord
}

/**
 * 첫 탭부터 시간이 흐르고, 게이지는 진행 중 내내 빠진다. 가득 채우면 걸린 시간이 기록이 된다.
 * 빠지는 속도보다 느리게 누르면 끝없이 이어지므로 제한 시간이 지나면 시간 초과로 끝낸다.
 */
internal data class ClickerGameState(
    val phase: ClickerGamePhase = ClickerGamePhase.Ready,
    val gauge: Float = 0f,
    val elapsedMs: Long = 0L,
) {
    val record: ClickerRecord
        get() = when {
            phase != ClickerGamePhase.Finished -> ClickerRecord.Running(elapsedMs / MS_PER_TENTH)
            gauge >= 1f -> ClickerRecord.Cleared(elapsedMs / MS_PER_HUNDREDTH)
            else -> ClickerRecord.TimeOver
        }

    fun tap(): ClickerGameState {
        if (phase == ClickerGamePhase.Finished) return this
        val isFull = gauge + GAUGE_PER_TAP >= 1f - GAUGE_FULL_TOLERANCE
        return copy(
            phase = if (isFull) ClickerGamePhase.Finished else ClickerGamePhase.Playing,
            gauge = if (isFull) 1f else gauge + GAUGE_PER_TAP,
        )
    }

    fun advance(deltaMs: Long): ClickerGameState {
        if (phase != ClickerGamePhase.Playing) return this
        val nextElapsed = (elapsedMs + deltaMs).coerceAtMost(GAME_DURATION_MS)
        return copy(
            phase = if (nextElapsed >= GAME_DURATION_MS) ClickerGamePhase.Finished else ClickerGamePhase.Playing,
            gauge = (gauge - GAUGE_DRAIN_PER_SECOND * deltaMs / MS_PER_SECOND).coerceAtLeast(0f),
            elapsedMs = nextElapsed,
        )
    }
}
