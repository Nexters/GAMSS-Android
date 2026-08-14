package com.gamss.android.data.tokenizer

import com.gamss.android.data.emotion.WordPieceTokenizer
import com.gamss.android.data.summary.KobartTokenizer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * 순수 Kotlin 토크나이저가 교체 이전 DJL 0.33.0 출력과 한 글자도 다르지 않은지 검증한다.
 *
 * tokenizer.json 을 재export 하면 골든도 다시 만들어야 한다. DJL 을 testImplementation 으로 잠시
 * 붙여 `HuggingFaceTokenizer` 로 같은 케이스를 돌린 뒤 tokenizer_golden.json 을 덮어쓴다.
 * 옵션은 addSpecialTokens=true, truncation=true/false, decode 는 skipSpecialTokens=true + trim.
 */
class TokenizerGoldenTest {

    @Test
    fun `WordPiece 인코딩이 골든과 일치한다`() {
        val golden = GOLDEN.getValue("wordpiece").jsonObject
        val tokenizer = WordPieceTokenizer.fromJson(
            bytes = File(EMOTION_TOKENIZER).readBytes(),
            seqLen = golden.getValue("seqLen").jsonPrimitive.int,
        )

        golden.getValue("cases").jsonArray.forEach { element ->
            val case = element.jsonObject
            val text = case.getValue("text").jsonPrimitive.content
            val encoded = tokenizer.encode(text)

            assertEquals(label("ids", text), case.longs("ids"), encoded.ids.toList())
            assertEquals(label("attentionMask", text), case.longs("attentionMask"), encoded.attentionMask.toList())
            assertEquals(label("typeIds", text), case.longs("typeIds"), encoded.typeIds.toList())
        }
    }

    @Test
    fun `kobart 인코딩과 라운드트립 디코딩이 골든과 일치한다`() {
        val golden = GOLDEN.getValue("kobart").jsonObject
        val tokenizer = KobartTokenizer.fromJson(
            bytes = File(KOBART_TOKENIZER).readBytes(),
            maxInput = golden.getValue("maxInput").jsonPrimitive.int,
        )

        golden.getValue("cases").jsonArray.forEach { element ->
            val case = element.jsonObject
            val text = case.getValue("text").jsonPrimitive.content
            val encoded = tokenizer.encode(text)

            assertEquals(label("ids", text), case.longs("ids"), encoded.ids.toList())
            assertEquals(label("attentionMask", text), case.longs("attentionMask"), encoded.attentionMask.toList())
            assertEquals(
                label("decoded", text),
                case.getValue("decoded").jsonPrimitive.content,
                tokenizer.decode(encoded.ids),
            )
        }
    }

    @Test
    fun `kobart 절단 없는 인코딩이 골든과 일치한다`() {
        val golden = GOLDEN.getValue("kobart").jsonObject
        val tokenizer = KobartTokenizer.fromJson(File(KOBART_TOKENIZER).readBytes(), maxInput = null)

        golden.getValue("noTruncationCases").jsonArray.forEach { element ->
            val case = element.jsonObject
            val text = case.getValue("text").jsonPrimitive.content
            assertEquals(label("ids", text), case.longs("ids"), tokenizer.encode(text).ids.toList())
        }
    }

    @Test
    fun `kobart 디코딩이 특수 토큰 섞인 시퀀스에서도 골든과 일치한다`() {
        val golden = GOLDEN.getValue("kobart").jsonObject
        val tokenizer = KobartTokenizer.fromJson(
            bytes = File(KOBART_TOKENIZER).readBytes(),
            maxInput = golden.getValue("maxInput").jsonPrimitive.int,
        )

        golden.getValue("decodeCases").jsonArray.forEach { element ->
            val case = element.jsonObject
            val ids = case.longs("ids")
            assertEquals(
                label("decoded", ids.toString()),
                case.getValue("decoded").jsonPrimitive.content,
                tokenizer.decode(ids.toLongArray()),
            )
        }
    }

    private fun JsonObject.longs(key: String): List<Long> =
        getValue(key).jsonArray.map { it.jsonPrimitive.long }

    private fun label(field: String, input: String): String =
        "$field 불일치 (입력: ${input.take(LABEL_MAX_LENGTH)})"

    companion object {
        private const val LABEL_MAX_LENGTH = 40
        private const val EMOTION_TOKENIZER = "../models/emotion-pack/src/main/assets/models/emotion_tokenizer.json"
        private const val KOBART_TOKENIZER = "../models/summary-pack/src/main/assets/models/kobart_tokenizer.json"

        private val GOLDEN: JsonObject by lazy {
            val stream = requireNotNull(TokenizerGoldenTest::class.java.getResourceAsStream("/tokenizer_golden.json")) {
                "tokenizer_golden.json 이 테스트 리소스에 없습니다."
            }
            Json.parseToJsonElement(stream.use { it.readBytes().decodeToString() }).jsonObject
        }
    }
}
