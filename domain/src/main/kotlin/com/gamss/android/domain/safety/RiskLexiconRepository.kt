package com.gamss.android.domain.safety

interface RiskLexiconRepository {

    /**
     * 예외를 던지지 않는다. 원격과 캐시가 모두 불가하면 앱 내장 사전을, 그마저 실패하면 빈 사전을 돌려준다.
     * 감지가 네트워크 상태에 좌우되면 안 되므로 구현체는 이 계약을 반드시 지켜야 한다.
     */
    suspend fun getLexicon(): RiskLexicon

    /** 원격 사전을 확인해 캐시를 갱신한다. 실패해도 예외를 던지지 않는다. */
    suspend fun refresh()
}
