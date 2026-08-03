package com.gamss.android.data.safety

import com.gamss.android.data.safety.model.RiskLexiconDto
import com.gamss.android.data.safety.model.toDomain
import com.gamss.android.domain.safety.RiskLevel
import com.gamss.android.domain.safety.RiskTermMatcher
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.File

class BundledRiskLexiconContentTest {

    private val lexicon = JSON
        .decodeFromString<RiskLexiconDto>(File(ASSET_PATH).readText())
        .toDomain()

    private val matcher = RiskTermMatcher()

    @Test
    fun `차단해야 하는 문장은 CRITICAL로 판정한다`() {
        CRITICAL_SENTENCES.forEach { sentence ->
            assertEquals(sentence, RiskLevel.CRITICAL, matcher.match(sentence, lexicon).level)
        }
    }

    @Test
    fun `안내가 필요한 문장은 감지한다`() {
        WARNING_SENTENCES.forEach { sentence ->
            assertNotEquals(sentence, RiskLevel.NONE, matcher.match(sentence, lexicon).level)
        }
    }

    @Test
    fun `일상적인 문장은 감지하지 않는다`() {
        BENIGN_SENTENCES.forEach { sentence ->
            assertEquals(sentence, RiskLevel.NONE, matcher.match(sentence, lexicon).level)
        }
    }

    @Test
    fun `감지되면 상담 기관을 priority 순으로 안내한다`() {
        val detection = matcher.match(CRITICAL_SENTENCES.first(), lexicon)

        assertEquals("109", detection.agencies.first().phoneNumber)
        assertEquals(detection.agencies.map { it.priority }.sorted(), detection.agencies.map { it.priority })
    }

    private companion object {
        const val ASSET_PATH = "src/main/assets/risk_lexicon.json"

        val JSON = Json { ignoreUnknownKeys = true }

        val CRITICAL_SENTENCES = listOf(
            "요즘 계속 죽고 싶다는 생각만 든다",
            "자살에 대해 계속 검색하게 된다",
            "자 살",
            "죽.고.싶.다",
            "어제 또 자해를 했다",
            "이제 정말 살고 싶지 않아",
            "유서를 미리 써두었다",
            "목을 매는 상상을 한다",
        )

        val WARNING_SENTENCES = listOf(
            "그냥 조용히 사라지고 싶어",
            "요즘은 그냥 살기 싫다",
            "다 포기하고 싶은 하루였다",
        )

        val BENIGN_SENTENCES = listOf(
            "오늘 점심이 정말 맛있었다",
            "배고파 죽겠다",
            "웃겨 죽는 줄 알았어",
            "너무 귀여워 죽겠어",
            "더워 죽겠네",
            "죽을 만큼 노력했다",
            "죽도록 공부했다",
            "친구랑 죽을 맛인 하루를 보냈다",
            "학교에서 자살예방 교육을 들었다",
            "OECD 자살률 기사를 읽었다",
            "경기에서 자살골을 넣었다",
            "수비가 자살 패스를 했다",
            "유서 깊은 사찰에 다녀왔다",
            "유서 있는 마을을 걸었다",
            "걔한테 너무 목매지 마",
            "그 회사에 목매고 있다",
            "손목 시계를 새로 샀다",
            "이 동네는 살기 좋다",
            "편의점에서 과자 살까 말까 고민했다",
            "요즘은 혼자 살아보니 편하다",
            "감자 살 때 크기를 봤다",
        )
    }
}
