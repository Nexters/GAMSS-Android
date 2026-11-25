package com.gamss.android.feature.game.clicker

internal const val GAME_DURATION_MS = 30_000L

// 시안 진행 중 화면이 232점일 때 게이지가 절반이라 464번에 가득 찬다.
internal const val FULL_GAUGE_SCORE = 464
private const val MS_PER_TENTH = 100L

internal enum class ClickerGamePhase { Ready, Playing, Finished }

/** 첫 탭부터 30초 동안 누른 횟수가 점수다. 게이지는 점수를 보여 줄 뿐 빠지지 않고, 가득 차도 계속 센다. */
internal data class ClickerGameState(
    val phase: ClickerGamePhase = ClickerGamePhase.Ready,
    val score: Int = 0,
    val elapsedMs: Long = 0L,
) {
    val gauge: Float get() = (score.toFloat() / FULL_GAUGE_SCORE).coerceAtMost(1f)

    /** 0.0초는 시간이 실제로 다 됐을 때만 보이도록 0.1초 단위로 올림한다. */
    val remainingTenths: Long get() = (GAME_DURATION_MS - elapsedMs + MS_PER_TENTH - 1) / MS_PER_TENTH

    fun tap(): ClickerGameState {
        if (phase == ClickerGamePhase.Finished) return this
        return copy(phase = ClickerGamePhase.Playing, score = score + 1)
    }

    fun advance(deltaMs: Long): ClickerGameState {
        if (phase != ClickerGamePhase.Playing) return this
        val nextElapsed = (elapsedMs + deltaMs).coerceAtMost(GAME_DURATION_MS)
        return copy(
            phase = if (nextElapsed >= GAME_DURATION_MS) ClickerGamePhase.Finished else ClickerGamePhase.Playing,
            elapsedMs = nextElapsed,
        )
    }
}
