package com.gamss.android.data.summary

import com.gamss.android.data.model.ModelAssetSource
import com.gamss.android.data.tokenizer.AddedVocabulary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.Normalizer

/**
 * HuggingFace BPE 파이프라인 이식. seq2seq 인코더라 고정 길이 패딩 없이 실제 길이를 쓴다.
 * 네이티브를 안 쓰는 이유와 골든 대조가 계약인 이유는 [WordPieceTokenizer] 와 같다.
 */
internal class KobartTokenizer private constructor(
    private val model: BpeModel,
    private val addedVocabulary: AddedVocabulary,
    private val specialTokens: SpecialTokens,
    private val replacement: Char,
    /** null 이면 절단하지 않는다. */
    private val maxInput: Int?,
) {

    class BpeModel(
        val vocab: Map<String, Int>,
        val tokenById: Array<String?>,
        val mergeRules: Map<Long, MergeRule>,
        val unkId: Int,
    )

    class MergeRule(val rank: Int, val mergedId: Int)

    class SpecialTokens(val bosId: Int, val eosId: Int, val skipOnDecodeIds: Set<Int>)

    class Encoded(val ids: LongArray, val attentionMask: LongArray)

    fun encode(text: String): Encoded {
        val budget = maxInput?.minus(SPECIAL_TOKEN_COUNT) ?: Int.MAX_VALUE
        val body = ArrayList<Int>()

        outer@ for (segment in addedVocabulary.split(text)) {
            val ids = when (segment) {
                is AddedVocabulary.Segment.Added -> listOf(segment.id)
                is AddedVocabulary.Segment.Plain ->
                    preTokenize(normalize(segment.text)).flatMap(::encodePiece)
            }
            for (id in ids) {
                if (body.size == budget) break@outer
                body.add(id)
            }
        }

        val ids = LongArray(body.size + SPECIAL_TOKEN_COUNT)
        ids[0] = specialTokens.bosId.toLong()
        body.forEachIndexed { index, id -> ids[index + 1] = id.toLong() }
        ids[ids.lastIndex] = specialTokens.eosId.toLong()
        return Encoded(ids = ids, attentionMask = LongArray(ids.size) { 1L })
    }

    /** skipSpecialTokens 고정. */
    fun decode(ids: LongArray): String {
        val decoded = StringBuilder()
        var emitted = 0
        for (rawId in ids) {
            val token = decodableToken(rawId.toInt()) ?: continue
            // 첫 토큰의 구분자만 버린다(Metaspace 규칙).
            for (char in token) {
                when {
                    char != replacement -> decoded.append(char)
                    emitted > 0 -> decoded.append(' ')
                }
            }
            emitted++
        }
        return decoded.toString().trim()
    }

    private fun decodableToken(id: Int): String? =
        if (id in specialTokens.skipOnDecodeIds) null else model.tokenById.getOrNull(id)

    /** 함께 걸린 BertNormalizer 는 모든 플래그가 꺼져 있어 NFKC 만 유효하다. */
    private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)

    /** Metaspace(prepend_scheme=always). */
    private fun preTokenize(text: String): List<String> {
        val replaced = text.replace(' ', replacement)
        val prefixed = if (replaced.startsWith(replacement)) replaced else replacement + replaced

        val pieces = mutableListOf<String>()
        var start = 0
        for (index in 1 until prefixed.length) {
            if (prefixed[index] == replacement) {
                pieces.add(prefixed.substring(start, index))
                start = index
            }
        }
        pieces.add(prefixed.substring(start))
        return pieces
    }

    /** 코드포인트 단위로 쪼갠 뒤 순위가 낮은 병합부터 적용한다. */
    private fun encodePiece(piece: String): List<Int> {
        val symbols = mutableListOf<Int>()
        var index = 0
        while (index < piece.length) {
            val codePoint = piece.codePointAt(index)
            index += Character.charCount(codePoint)
            symbols.add(model.vocab[String(Character.toChars(codePoint))] ?: model.unkId)
        }

        while (symbols.size > 1) {
            var bestRank = Int.MAX_VALUE
            var bestKey = NO_KEY
            for (position in 0 until symbols.size - 1) {
                val key = pairKey(symbols[position], symbols[position + 1])
                val rule = model.mergeRules[key] ?: continue
                if (rule.rank < bestRank) {
                    bestRank = rule.rank
                    bestKey = key
                }
            }
            if (bestKey == NO_KEY) break
            mergeAllOccurrences(symbols, bestKey)
        }
        return symbols
    }

    /**
     * 한 번에 병합해도 HuggingFace 의 힙 방식과 결과가 같은 근거: 병합으로 새로 생기는 쌍의 rank 는
     * 방금 적용한 rank 보다 항상 크다(학습 BPE 의 성질). 이 성질이 깨진 merges 에서는 달라진다.
     */
    private fun mergeAllOccurrences(symbols: MutableList<Int>, mergeKey: Long) {
        val mergedId = model.mergeRules.getValue(mergeKey).mergedId
        val merged = ArrayList<Int>(symbols.size)
        var position = 0
        while (position < symbols.size) {
            val isMergeSite = position < symbols.size - 1 &&
                pairKey(symbols[position], symbols[position + 1]) == mergeKey
            if (isMergeSite) {
                merged.add(mergedId)
                position += 2
            } else {
                merged.add(symbols[position])
                position++
            }
        }
        symbols.clear()
        symbols.addAll(merged)
    }

    companion object {
        private const val SPECIAL_TOKEN_COUNT = 2
        private const val BOS_TOKEN = "<s>"
        private const val EOS_TOKEN = "</s>"
        private const val DEFAULT_REPLACEMENT = '▁'
        private const val NO_KEY = -1L
        private const val UNSIGNED_INT_MASK = 0xFFFFFFFFL

        private const val MODEL_TYPE = "BPE"
        private const val PRE_TOKENIZER_TYPE = "Metaspace"
        private const val PREPEND_SCHEME_ALWAYS = "always"

        private val TOKENIZER_JSON = Json { ignoreUnknownKeys = true }

        suspend fun load(
            modelAssetSource: ModelAssetSource,
            packName: String,
            tokenizerAsset: String,
            maxInput: Int,
        ): KobartTokenizer = fromJson(readTokenizer(modelAssetSource, packName, tokenizerAsset), maxInput)

        /** 토큰 수를 재려면 절단이 없어야 한다. 절단하면 한계 이상은 전부 같은 값으로 보인다. */
        suspend fun loadWithoutTruncation(
            modelAssetSource: ModelAssetSource,
            packName: String,
            tokenizerAsset: String,
        ): KobartTokenizer = fromJson(readTokenizer(modelAssetSource, packName, tokenizerAsset), maxInput = null)

        private suspend fun readTokenizer(
            modelAssetSource: ModelAssetSource,
            packName: String,
            tokenizerAsset: String,
        ): ByteArray = modelAssetSource.readBytes(packName, tokenizerAsset)

        /** Context 없이 tokenizer.json 을 직접 먹이는 경로. 골든 대조 단위 테스트가 쓴다. */
        fun fromJson(bytes: ByteArray, maxInput: Int?): KobartTokenizer {
            require(maxInput == null || maxInput > SPECIAL_TOKEN_COUNT) {
                "maxInput 은 특수 토큰 수보다 커야 합니다: $maxInput"
            }

            val root = TOKENIZER_JSON.parseToJsonElement(bytes.decodeToString()).jsonObject
            val model = root.getValue("model").jsonObject
            // 재export 로 파이프라인이 바뀌면 조용히 틀어지므로 로드 시점에 끊는다.
            require(model["type"]?.jsonPrimitive?.content == MODEL_TYPE) {
                "지원하지 않는 model.type: ${model["type"]}"
            }

            val vocab = model.getValue("vocab").jsonObject
                .mapValues { (_, value) -> value.jsonPrimitive.int }

            fun idOf(token: String): Int =
                requireNotNull(vocab[token]) { "tokenizer.json vocab 에 $token 이 없습니다." }

            val tokenById = arrayOfNulls<String>((vocab.values.maxOrNull() ?: -1) + 1)
            vocab.forEach { (token, id) -> tokenById[id] = token }

            val addedTokens = parseAddedTokens(root)
            return KobartTokenizer(
                model = BpeModel(
                    vocab = vocab,
                    tokenById = tokenById,
                    mergeRules = parseMerges(model.getValue("merges").jsonArray, vocab),
                    unkId = idOf(model.getValue("unk_token").jsonPrimitive.content),
                ),
                addedVocabulary = AddedVocabulary.of(addedTokens.associate { it.content to it.id }),
                specialTokens = SpecialTokens(
                    bosId = idOf(BOS_TOKEN),
                    eosId = idOf(EOS_TOKEN),
                    skipOnDecodeIds = addedTokens.filter { it.special }.map { it.id }.toSet(),
                ),
                replacement = parseReplacement(root),
                maxInput = maxInput,
            )
        }

        /** vocab 으로 옮길 수 없는 규칙은 버린다(현재 파일에는 해당 없음). */
        private fun parseMerges(merges: JsonArray, vocab: Map<String, Int>): Map<Long, MergeRule> {
            val rules = HashMap<Long, MergeRule>(merges.size)
            merges.forEachIndexed { rank, element ->
                // 신형 ["a","b"], 구형 "a b".
                val (left, right) = when (element) {
                    is JsonArray -> element[0].jsonPrimitive.content to element[1].jsonPrimitive.content
                    is JsonPrimitive -> element.content.split(' ', limit = 2).let { it.first() to it.last() }
                    else -> error("merges 항목 형식을 알 수 없습니다: $element")
                }
                val leftId = vocab[left] ?: return@forEachIndexed
                val rightId = vocab[right] ?: return@forEachIndexed
                val mergedId = vocab[left + right] ?: return@forEachIndexed
                rules[pairKey(leftId, rightId)] = MergeRule(rank, mergedId)
            }
            return rules
        }

        private fun parseAddedTokens(root: JsonObject): List<AddedToken> =
            root["added_tokens"]?.jsonArray.orEmpty().map { element ->
                val token = element.jsonObject
                AddedToken(
                    content = token.getValue("content").jsonPrimitive.content,
                    id = token.getValue("id").jsonPrimitive.int,
                    special = token["special"]?.jsonPrimitive?.boolean == true,
                )
            }

        private fun parseReplacement(root: JsonObject): Char {
            val preTokenizer = root.getValue("pre_tokenizer").jsonObject
            require(preTokenizer["type"]?.jsonPrimitive?.content == PRE_TOKENIZER_TYPE) {
                "지원하지 않는 pre_tokenizer.type: ${preTokenizer["type"]}"
            }
            // always/true 로 고정 구현이라 다르면 결과가 달라진다.
            require(preTokenizer["prepend_scheme"]?.jsonPrimitive?.content == PREPEND_SCHEME_ALWAYS) {
                "지원하지 않는 prepend_scheme: ${preTokenizer["prepend_scheme"]}. " +
                    "구버전 tokenizers 로 export 하면 prepend_scheme 대신 add_prefix_space 가 들어간다."
            }
            require(preTokenizer["split"]?.jsonPrimitive?.boolean != false) { "split=false 는 지원하지 않습니다." }

            val configured = preTokenizer["replacement"]?.jsonPrimitive?.content ?: return DEFAULT_REPLACEMENT
            require(configured.length == 1) { "Metaspace replacement 는 한 글자여야 합니다: $configured" }
            return configured.first()
        }

        private fun pairKey(left: Int, right: Int): Long =
            (left.toLong() shl Int.SIZE_BITS) or (right.toLong() and UNSIGNED_INT_MASK)
    }

    private class AddedToken(val content: String, val id: Int, val special: Boolean)
}
