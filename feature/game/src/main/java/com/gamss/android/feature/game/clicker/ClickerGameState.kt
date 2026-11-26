package com.gamss.android.feature.game.clicker

internal const val COUNTDOWN_MS = 3_000L
internal const val GAME_DURATION_MS = 30_000L
private const val TOTAL_DURATION_MS = COUNTDOWN_MS + GAME_DURATION_MS

// 시안 진행 중 화면이 232점일 때 게이지가 절반이라 464번에 가득 찬다.
internal const val FULL_GAUGE_SCORE = 464
private const val MS_PER_TENTH = 100L
private const val MS_PER_SECOND = 1_000L

internal enum class ClickerGamePhase { Countdown, Playing, Finished }

/**
 * 3초 카운트다운 뒤 30초 동안 누른 횟수가 점수다. 단계는 카운트다운부터 흐른 시간 하나로 정한다.
 * 게이지는 점수를 보여 줄 뿐 빠지지 않고, 가득 차도 계속 센다.
 */
internal data class ClickerGameState(
    val score: Int = 0,
    val elapsedMs: Long = 0L,
) {
    val phase: ClickerGamePhase
        get() = when {
            elapsedMs < COUNTDOWN_MS -> ClickerGamePhase.Countdown
            elapsedMs < TOTAL_DURATION_MS -> ClickerGamePhase.Playing
            else -> ClickerGamePhase.Finished
        }

    val gauge: Float get() = (score.toFloat() / FULL_GAUGE_SCORE).coerceAtMost(1f)

    val countdownSecond: Int?
        get() = if (phase == ClickerGamePhase.Countdown) {
            ((COUNTDOWN_MS - elapsedMs + MS_PER_SECOND - 1) / MS_PER_SECOND).toInt()
        } else {
            null
        }

    /** 0.0초는 시간이 실제로 다 됐을 때만 보이도록 0.1초 단위로 올림한다. */
    val remainingTenths: Long
        get() = (TOTAL_DURATION_MS - elapsedMs.coerceAtLeast(COUNTDOWN_MS) + MS_PER_TENTH - 1) / MS_PER_TENTH

    fun tap(): ClickerGameState {
        if (phase != ClickerGamePhase.Playing) return this
        return copy(score = score + 1)
    }

    fun advance(deltaMs: Long): ClickerGameState {
        if (phase == ClickerGamePhase.Finished) return this
        return copy(elapsedMs = (elapsedMs + deltaMs).coerceAtMost(TOTAL_DURATION_MS))
    }

    companion object {
        fun playing(score: Int = 0, playedMs: Long = 0L) =
            ClickerGameState(score = score, elapsedMs = COUNTDOWN_MS + playedMs)

        fun finished(score: Int) = ClickerGameState(score = score, elapsedMs = TOTAL_DURATION_MS)
    }
}
