package com.gamss.android.data.emotion

import android.content.Context
import com.gamss.android.data.model.OnDemandModelAssets
import com.gamss.android.data.model.readBytes
import com.gamss.android.data.tokenizer.AddedVocabulary
import com.gamss.android.data.tokenizer.REPLACEMENT_CODE_POINT
import com.gamss.android.data.tokenizer.forEachCodePoint
import com.gamss.android.data.tokenizer.isBertPunctuation
import com.gamss.android.data.tokenizer.isChineseChar
import com.gamss.android.data.tokenizer.isControlCodePoint
import com.gamss.android.data.tokenizer.isUnicodeWhitespace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer

/**
 * HuggingFace tokenizer.json 을 그대로 로드해 학습 토크나이저와 동일하게 인코딩한다.
 * 고정 길이(seqLen)로 자르거나 0 패딩해 LiteRT 입력에 맞춘다.
 *
 * 추가 토큰 추출 → BertNormalizer → BertPreTokenizer → WordPiece → [CLS]/[SEP] 부착까지
 * HuggingFace 구현을 옮긴 것이다. 네이티브 토크나이저를 쓰지 않는 이유는 유일하게 배포된
 * ai.djl.android:tokenizer-native 0.33.0 의 .so 가 4KB 페이지 정렬이라 16KB 기기에서 뜨지 않기 때문이다.
 * 출력이 원본과 어긋나면 분류 결과가 조용히 틀어지므로 tokenizer_golden.json 대조 테스트가 계약이다.
 */
internal class WordPieceTokenizer private constructor(
    private val model: Model,
    private val normalizer: BertNormalizer,
    private val addedVocabulary: AddedVocabulary,
    private val specialTokens: SpecialTokens,
    private val seqLen: Int,
) {

    class Model(
        val vocab: Map<String, Int>,
        val unkId: Int,
        val continuingPrefix: String,
        val maxInputCharsPerWord: Int,
    )

    class SpecialTokens(val clsId: Int, val sepId: Int)

    // LongArray 필드라 data class 의 참조기반 equals/hashCode 는 부적절 → 일반 class.
    class Encoded(
        val ids: LongArray,
        val attentionMask: LongArray,
        val typeIds: LongArray,
    )

    fun encode(text: String): Encoded {
        // HuggingFace 는 maxLength 를 특수 토큰까지 포함한 길이로 세므로 본문 예산은 2개를 뺀 값이다.
        val budget = seqLen - SPECIAL_TOKEN_COUNT
        val pieces = ArrayList<Int>(minOf(budget, INITIAL_PIECE_CAPACITY))

        // 추가 토큰을 먼저 떼어내고, 그 사이에 남은 구간만 정규화·사전분할·WordPiece 를 거친다.
        outer@ for (segment in addedVocabulary.split(text)) {
            val ids = when (segment) {
                is AddedVocabulary.Segment.Added -> listOf(segment.id)
                is AddedVocabulary.Segment.Plain ->
                    preTokenize(normalizer.normalize(segment.text)).flatMap(::tokenizeWord)
            }
            for (id in ids) {
                if (pieces.size == budget) break@outer
                pieces.add(id)
            }
        }

        val ids = LongArray(seqLen)
        ids[0] = specialTokens.clsId.toLong()
        pieces.forEachIndexed { index, id -> ids[index + 1] = id.toLong() }
        ids[pieces.size + 1] = specialTokens.sepId.toLong()

        val length = pieces.size + SPECIAL_TOKEN_COUNT
        val attentionMask = LongArray(seqLen) { if (it < length) 1L else 0L }
        // 단일 시퀀스라 TemplateProcessing 의 type_id 는 전부 0 이다.
        return Encoded(ids = ids, attentionMask = attentionMask, typeIds = LongArray(seqLen))
    }

    /**
     * 공백으로 끊고 구두점을 한 글자씩 떼어낸다(BertPreTokenizer).
     * 스페이스만 구분자로 보는 것은 앞선 clean_text 가 모든 공백류를 스페이스로 바꿔주기 때문이다.
     */
    private fun preTokenize(text: String): List<String> {
        val words = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotEmpty()) {
                words.add(current.toString())
                current.setLength(0)
            }
        }

        forEachCodePoint(text) { codePoint ->
            when {
                codePoint == ' '.code -> flush()
                isBertPunctuation(codePoint) -> {
                    flush()
                    words.add(String(Character.toChars(codePoint)))
                }

                else -> current.appendCodePoint(codePoint)
            }
        }
        flush()
        return words
    }

    /** 한 조각이라도 사전에 없으면 조각 단위가 아니라 단어 전체가 UNK 다(HuggingFace 의 is_bad). */
    private fun tokenizeWord(word: String): List<Int> {
        if (word.codePointCount(0, word.length) > model.maxInputCharsPerWord) return listOf(model.unkId)
        return matchGreedily(word) ?: listOf(model.unkId)
    }

    /** 가장 긴 조각부터 맞춰보는 greedy longest-match-first. 맞출 수 없으면 null. */
    private fun matchGreedily(word: String): List<Int>? {
        val ids = mutableListOf<Int>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            var matched: Int? = null
            while (start < end) {
                val piece = word.substring(start, end)
                matched = model.vocab[if (start > 0) model.continuingPrefix + piece else piece]
                if (matched != null) break
                // 코드포인트 경계로 한 글자씩 줄인다.
                end = word.offsetByCodePoints(end, -1)
            }
            ids.add(matched ?: return null)
            start = end
        }
        return ids
    }

    /**
     * HuggingFace BertNormalizer 이식.
     * strip_accents 가 비어 있으면 lowercase 값을 따르는 규칙까지 원본과 같다.
     */
    class BertNormalizer(
        private val cleanText: Boolean,
        private val handleChineseChars: Boolean,
        private val stripAccents: Boolean,
        private val lowercase: Boolean,
    ) {
        fun normalize(text: String): String {
            var result = text
            if (cleanText) result = cleanText(result)
            if (handleChineseChars) result = padChineseChars(result)
            if (stripAccents) result = stripAccents(result)
            if (lowercase) result = result.lowercase()
            return result
        }

        /** NUL·U+FFFD·제어문자는 버리고, 남은 공백류는 스페이스 하나로 통일한다. */
        private fun cleanText(text: String): String = buildString(text.length) {
            forEachCodePoint(text) { codePoint ->
                when {
                    codePoint == 0 || codePoint == REPLACEMENT_CODE_POINT -> Unit
                    isControlCodePoint(codePoint) -> Unit
                    isUnicodeWhitespace(codePoint) -> append(' ')
                    else -> appendCodePoint(codePoint)
                }
            }
        }

        /** CJK 는 앞뒤에 공백을 넣어 한 글자씩 떨어지게 한다. */
        private fun padChineseChars(text: String): String = buildString(text.length) {
            forEachCodePoint(text) { codePoint ->
                if (isChineseChar(codePoint)) {
                    append(' ').appendCodePoint(codePoint).append(' ')
                } else {
                    appendCodePoint(codePoint)
                }
            }
        }

        /** 결합 문자는 보조 평면에도 있어서 UTF-16 단위가 아니라 코드포인트 단위로 걸러야 한다. */
        private fun stripAccents(text: String): String =
            buildString(text.length) {
                forEachCodePoint(Normalizer.normalize(text, Normalizer.Form.NFD)) { codePoint ->
                    if (Character.getType(codePoint) != Character.NON_SPACING_MARK.toInt()) {
                        appendCodePoint(codePoint)
                    }
                }
            }
    }

    companion object {
        private const val SPECIAL_TOKEN_COUNT = 2
        private const val INITIAL_PIECE_CAPACITY = 64
        private const val CLS_TOKEN = "[CLS]"
        private const val SEP_TOKEN = "[SEP]"

        private const val MODEL_TYPE = "WordPiece"
        private const val NORMALIZER_TYPE = "BertNormalizer"
        private const val PRE_TOKENIZER_TYPE = "BertPreTokenizer"

        private val TOKENIZER_JSON = Json { ignoreUnknownKeys = true }

        suspend fun load(
            context: Context,
            packName: String,
            tokenizerAsset: String,
            seqLen: Int,
        ): WordPieceTokenizer {
            val bytes = OnDemandModelAssets(context).resolve(packName, tokenizerAsset).readBytes()
            return fromJson(bytes, seqLen)
        }

        /** Context 없이 tokenizer.json 을 직접 먹이는 경로. 골든 대조 단위 테스트가 쓴다. */
        fun fromJson(bytes: ByteArray, seqLen: Int): WordPieceTokenizer {
            require(seqLen > SPECIAL_TOKEN_COUNT) { "seqLen 은 특수 토큰 수보다 커야 합니다: $seqLen" }

            val file = TOKENIZER_JSON.decodeFromString(TokenizerFile.serializer(), bytes.decodeToString())
            // 모델을 재export 하면서 파이프라인이 바뀌면 결과가 조용히 틀어지므로 로드 시점에 끊는다.
            require(file.model.type == MODEL_TYPE) { "지원하지 않는 model.type: ${file.model.type}" }
            require(file.normalizer?.type == NORMALIZER_TYPE) {
                "지원하지 않는 normalizer.type: ${file.normalizer?.type}"
            }
            require(file.preTokenizer?.type == PRE_TOKENIZER_TYPE) {
                "지원하지 않는 pre_tokenizer.type: ${file.preTokenizer?.type}"
            }

            val vocab = file.model.vocab
            fun idOf(token: String): Int =
                requireNotNull(vocab[token]) { "tokenizer.json vocab 에 $token 이 없습니다." }

            val lowercase = file.normalizer.lowercase == true
            return WordPieceTokenizer(
                model = Model(
                    vocab = vocab,
                    unkId = idOf(file.model.unkToken),
                    continuingPrefix = file.model.continuingSubwordPrefix,
                    maxInputCharsPerWord = file.model.maxInputCharsPerWord,
                ),
                normalizer = BertNormalizer(
                    cleanText = file.normalizer.cleanText != false,
                    handleChineseChars = file.normalizer.handleChineseChars != false,
                    // strip_accents 가 null 이면 lowercase 를 따른다.
                    stripAccents = file.normalizer.stripAccents ?: lowercase,
                    lowercase = lowercase,
                ),
                addedVocabulary = AddedVocabulary.of(file.addedTokens.associate { it.content to it.id }),
                specialTokens = SpecialTokens(clsId = idOf(CLS_TOKEN), sepId = idOf(SEP_TOKEN)),
                seqLen = seqLen,
            )
        }
    }

    @Serializable
    private data class TokenizerFile(
        @SerialName("added_tokens") val addedTokens: List<AddedToken> = emptyList(),
        val normalizer: NormalizerConfig? = null,
        @SerialName("pre_tokenizer") val preTokenizer: TypeOnly? = null,
        val model: ModelConfig,
    )

    @Serializable
    private data class AddedToken(val id: Int, val content: String)

    @Serializable
    private data class TypeOnly(val type: String? = null)

    @Serializable
    private data class NormalizerConfig(
        val type: String? = null,
        @SerialName("clean_text") val cleanText: Boolean? = null,
        @SerialName("handle_chinese_chars") val handleChineseChars: Boolean? = null,
        @SerialName("strip_accents") val stripAccents: Boolean? = null,
        val lowercase: Boolean? = null,
    )

    @Serializable
    private data class ModelConfig(
        val type: String? = null,
        val vocab: Map<String, Int>,
        @SerialName("unk_token") val unkToken: String,
        @SerialName("continuing_subword_prefix") val continuingSubwordPrefix: String,
        @SerialName("max_input_chars_per_word") val maxInputCharsPerWord: Int,
    )
}
