package com.gamss.android.data.safety

import com.gamss.android.data.safety.model.RiskLexiconDto
import com.gamss.android.data.safety.model.RiskTermDto
import com.gamss.android.data.safety.model.SupportAgencyDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RiskLexiconRepositoryImplTest {

    private lateinit var bundled: BundledRiskLexiconDataSource
    private lateinit var remote: FirestoreRiskLexiconDataSource
    private lateinit var local: RiskLexiconLocalDataSource

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        bundled = mockk()
        remote = mockk()
        local = mockk(relaxed = true)
    }

    private fun repository() = RiskLexiconRepositoryImpl(bundled, remote, local, json)

    @Test
    fun `Firestore 조회가 실패해도 내장 사전을 반환한다`() = runTest {
        coEvery { bundled.load() } returns dto(version = 1)
        coEvery { local.read() } returns null
        coEvery { remote.fetch() } throws IOException("network down")

        val repository = repository()
        repository.refresh()

        assertEquals(1, repository.getLexicon().version)
        coVerify(exactly = 0) { local.write(any(), any()) }
    }

    @Test
    fun `내장 사전 로딩이 실패해도 예외를 던지지 않는다`() = runTest {
        coEvery { bundled.load() } throws IOException("asset missing")
        coEvery { local.read() } returns null

        assertEquals(0, repository().getLexicon().version)
    }

    @Test
    fun `원격 version이 내장본보다 낮으면 갱신하지 않는다`() = runTest {
        coEvery { bundled.load() } returns dto(version = 5)
        coEvery { local.read() } returns null
        coEvery { remote.fetch() } returns dto(version = 2, term = "원격단어")

        val repository = repository()
        repository.refresh()

        coVerify(exactly = 0) { local.write(any(), any()) }
        assertEquals(5, repository.getLexicon().version)
    }

    @Test
    fun `원격 version이 더 크면 캐시를 갱신하고 원격 사전을 쓴다`() = runTest {
        coEvery { bundled.load() } returns dto(version = 1)
        coEvery { local.read() } returns null
        coEvery { remote.fetch() } returns dto(version = 9, term = "원격단어")

        val repository = repository()
        repository.refresh()

        coVerify(exactly = 1) { local.write(any(), any()) }
        val lexicon = repository.getLexicon()
        assertEquals(9, lexicon.version)
        assertEquals(listOf("원격단어"), lexicon.terms.map { it.term })
    }

    @Test
    fun `캐시된 사전이 내장본보다 최신이면 캐시를 쓴다`() = runTest {
        coEvery { bundled.load() } returns dto(version = 1)
        coEvery { local.read() } returns CachedRiskLexicon(
            json = json.encodeToString(dto(version = 4, term = "캐시단어")),
            fetchedAtMillis = System.currentTimeMillis(),
        )

        val lexicon = repository().getLexicon()

        assertEquals(4, lexicon.version)
        assertEquals(listOf("캐시단어"), lexicon.terms.map { it.term })
    }

    @Test
    fun `캐시가 손상되면 내장 사전으로 폴백한다`() = runTest {
        coEvery { bundled.load() } returns dto(version = 1)
        coEvery { local.read() } returns CachedRiskLexicon(
            json = "{ not valid json",
            fetchedAtMillis = System.currentTimeMillis(),
        )

        assertEquals(1, repository().getLexicon().version)
    }

    @Test
    fun `캐시가 아직 신선하면 원격을 조회하지 않는다`() = runTest {
        coEvery { bundled.load() } returns dto(version = 1)
        coEvery { local.read() } returns CachedRiskLexicon(
            json = json.encodeToString(dto(version = 4)),
            fetchedAtMillis = System.currentTimeMillis(),
        )

        repository().refresh()

        coVerify(exactly = 0) { remote.fetch() }
    }

    private fun dto(version: Int, term: String = "죽고싶") = RiskLexiconDto(
        version = version,
        terms = listOf(RiskTermDto(term = term, level = "CRITICAL")),
        safePhrases = emptyList(),
        agencies = listOf(
            SupportAgencyDto(
                id = "kr-109",
                name = "자살예방 상담전화",
                description = "24시간 무료 전문 상담",
                phoneNumber = "109",
                priority = 1,
            ),
        ),
    )
}
