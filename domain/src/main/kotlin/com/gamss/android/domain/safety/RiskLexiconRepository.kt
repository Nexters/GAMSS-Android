package com.gamss.android.domain.safety

interface RiskLexiconRepository {

    /**
     * 조회는 실패하지 않는다. 원격과 캐시가 모두 불가하면 앱 내장 사전을 돌려준다.
     * 안전 기능이 네트워크 상태에 좌우되면 안 되기 때문이다.
     */
    suspend fun getLexicon(): RiskLexicon

    /**
     * 원격 사전을 확인해 캐시를 갱신한다. 실패해도 예외를 던지지 않는다.
     */
    suspend fun refresh()
}
