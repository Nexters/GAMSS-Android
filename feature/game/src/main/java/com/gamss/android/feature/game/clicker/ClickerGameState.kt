package com.gamss.android.feature.game.clicker

internal const val GAME_DURATION_MS = 30_000L
internal const val GAUGE_PER_TAP = 0.02f
internal const val GAUGE_DRAIN_PER_SECOND = 0.1f
private const val MS_PER_SECOND = 1_000f
private const val MS_PER_TENTH = 100L

// 0.02f 를 50번 더하면 float 오차로 1.0 에 조금 못 미친다.
private const val GAUGE_FULL_TOLERANCE = 0.0001f

enum class ClickerGamePhase { Ready, Playing, Finished }

/** 시간은 첫 탭부터 흐르고, 게이지는 진행 중 내내 빠진다. 가득 차거나 시간이 다 되면 끝난다. */
data class ClickerGameState(
    val phase: ClickerGamePhase = ClickerGamePhase.Ready,
    val score: Int = 0,
    val gauge: Float = 0f,
    val elapsedMs: Long = 0L,
) {
    /** 0.0초는 시간이 실제로 다 됐을 때만 보이도록 0.1초 단위로 올림한다. */
    val remainingTenths: Long get() = (GAME_DURATION_MS - elapsedMs + MS_PER_TENTH - 1) / MS_PER_TENTH

    fun tap(): ClickerGameState {
        if (phase == ClickerGamePhase.Finished) return this
        val isFull = gauge + GAUGE_PER_TAP >= 1f - GAUGE_FULL_TOLERANCE
        return copy(
            phase = if (isFull) ClickerGamePhase.Finished else ClickerGamePhase.Playing,
            score = score + 1,
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
