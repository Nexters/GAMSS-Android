package com.gamss.android.domain.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskTermMatcherTest {

    private val matcher = RiskTermMatcher()

    private val lexicon = RiskLexicon(
        version = 1,
        terms = listOf(
            RiskTerm("자살", RiskLevel.CRITICAL),
            RiskTerm("죽고싶", RiskLevel.CRITICAL),
            RiskTerm("살기싫", RiskLevel.WARNING),
            RiskTerm("관찰만하는단어", RiskLevel.NONE),
        ),
        safePhrases = listOf("배고파죽", "죽고싶지않", "자살예방"),
        agencies = listOf(
            SupportAgency("b", "청소년 상담전화", "청소년 전문 상담", "1388", null, 3),
            SupportAgency("a", "자살예방 상담전화", "24시간 무료 전문 상담", "109", null, 1),
        ),
    )

    @Test
    fun 위험_표현이_없으면_NONE을_반환한다() {
        assertEquals(RiskLevel.NONE, matcher.match("오늘은 날씨가 좋아서 오래 산책했다", lexicon).level)
    }

    @Test
    fun CRITICAL_표현이_있으면_CRITICAL을_반환한다() {
        assertEquals(RiskLevel.CRITICAL, matcher.match("자살에 대해 계속 생각한다", lexicon).level)
    }

    @Test
    fun WARNING_표현만_있으면_WARNING을_반환한다() {
        assertEquals(RiskLevel.WARNING, matcher.match("요즘은 그냥 살기싫다", lexicon).level)
    }

    @Test
    fun CRITICAL과_WARNING이_함께_있으면_CRITICAL을_반환한다() {
        assertEquals(RiskLevel.CRITICAL, matcher.match("살기싫고 죽고싶다", lexicon).level)
    }

    @Test
    fun 관용_표현은_감지하지_않는다() {
        assertEquals(RiskLevel.NONE, matcher.match("배고파 죽겠다", lexicon).level)
    }

    @Test
    fun 부정_표현은_감지하지_않는다() {
        assertEquals(RiskLevel.NONE, matcher.match("죽고 싶지 않아", lexicon).level)
    }

    @Test
    fun 안전한_합성어는_감지하지_않는다() {
        assertEquals(RiskLevel.NONE, matcher.match("학교에서 자살예방 교육을 들었다", lexicon).level)
    }

    @Test
    fun 띄어쓰기나_구두점으로_나눠_써도_감지한다() {
        assertEquals(RiskLevel.CRITICAL, matcher.match("자 살", lexicon).level)
        assertEquals(RiskLevel.CRITICAL, matcher.match("죽.고.싶.다", lexicon).level)
    }

    @Test
    fun 짧은_표현이_어절_경계를_넘어_겹치면_감지하지_않는다() {
        assertEquals(RiskLevel.NONE, matcher.match("과자 살까 말까 고민했다", lexicon).level)
        assertEquals(RiskLevel.NONE, matcher.match("혼자 살아보니 편하다", lexicon).level)
    }

    @Test
    fun 긴_표현은_어절_경계와_무관하게_감지한다() {
        assertEquals(RiskLevel.CRITICAL, matcher.match("요즘죽고싶다", lexicon).level)
    }

    @Test
    fun 등급이_NONE인_항목은_감지하지_않는다() {
        assertEquals(RiskLevel.NONE, matcher.match("관찰만하는단어", lexicon).level)
    }

    @Test
    fun 빈_입력과_공백만_있는_입력은_NONE을_반환한다() {
        assertEquals(RiskLevel.NONE, matcher.match("", lexicon).level)
        assertEquals(RiskLevel.NONE, matcher.match("   \n  ", lexicon).level)
    }

    @Test
    fun 감지되면_기관을_priority_순으로_반환한다() {
        val detection = matcher.match("자살", lexicon)

        assertEquals(listOf("자살예방 상담전화", "청소년 상담전화"), detection.agencies.map { it.name })
    }

    @Test
    fun CRITICAL만_전송을_차단한다() {
        assertTrue(matcher.match("죽고싶다", lexicon).shouldBlock)
        assertFalse(matcher.match("살기싫다", lexicon).shouldBlock)
        assertFalse(matcher.match("평범한 하루였다", lexicon).shouldBlock)
    }
}
