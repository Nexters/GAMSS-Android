package com.gamss.android.domain.safety

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectRiskInTextUseCaseTest {

    private class FakeRiskLexiconRepository(private val lexicon: RiskLexicon) : RiskLexiconRepository {
        override suspend fun getLexicon(): RiskLexicon = lexicon
        override suspend fun refresh() = Unit
    }

    private val lexicon = RiskLexicon(
        version = 1,
        terms = listOf(RiskTerm("죽고싶", RiskLevel.CRITICAL)),
        safePhrases = emptyList(),
        agencies = listOf(
            SupportAgency("b", "청소년 상담전화", "청소년 전문 상담", "1388", null, 3),
            SupportAgency("a", "자살예방 상담전화", "24시간 무료 전문 상담", "109", null, 1),
        ),
    )

    private val useCase = DetectRiskInTextUseCase(
        repository = FakeRiskLexiconRepository(lexicon),
        matcher = RiskTermMatcher(),
    )

    @Test
    fun 사전에_있는_위험_표현을_감지하고_기관을_priority_순으로_안내한다() = runBlocking {
        val detection = useCase("요즘 정말 죽고싶다는 생각뿐이다")

        assertEquals(RiskLevel.CRITICAL, detection.level)
        assertTrue(detection.shouldBlock)
        assertEquals(listOf("109", "1388"), detection.agencies.map { it.phoneNumber })
    }

    @Test
    fun 위험_표현이_없으면_기관을_안내하지_않는다() = runBlocking {
        val detection = useCase("오늘은 친구와 맛있는 걸 먹었다")

        assertEquals(RiskLevel.NONE, detection.level)
        assertEquals(emptyList<SupportAgency>(), detection.agencies)
    }

    @Test
    fun 사전이_비어_있으면_감지하지_않는다() = runBlocking {
        val emptyLexicon = RiskLexicon(0, emptyList(), emptyList(), emptyList())
        val useCaseWithEmptyLexicon = DetectRiskInTextUseCase(
            repository = FakeRiskLexiconRepository(emptyLexicon),
            matcher = RiskTermMatcher(),
        )

        assertEquals(RiskLevel.NONE, useCaseWithEmptyLexicon("죽고싶다").level)
    }
}
