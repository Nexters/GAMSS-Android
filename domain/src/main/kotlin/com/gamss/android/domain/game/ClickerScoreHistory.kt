package com.gamss.android.domain.game

/** 연타 게임 기록은 기기에만 남는다. */
interface ClickerScoreHistory {

    suspend fun load(): List<ClickerScoreRecord>

    /** 읽고 쓰는 사이에 다른 기록이 끼어들지 않도록 한 번에 바꾸고, 바뀐 목록을 반환한다. */
    suspend fun update(transform: (List<ClickerScoreRecord>) -> List<ClickerScoreRecord>): List<ClickerScoreRecord>
}
