package com.gamss.android.data.summary

import android.content.Context
import com.gamss.android.data.model.OnDemandModelAssets
import com.gamss.android.data.model.readBytes
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
 * kobart tokenizer.json 을 로드해 요약 인코더 입력을 만들고, 디코더 출력 토큰을 문자열로 되돌린다.
 * 감정용 WordPieceTokenizer 와 달리 고정 길이 패딩 없이 실제 길이를 그대로 쓰고(seq2seq 인코더는
 * 가변 길이 입력을 받음), decode 를 제공한다.
 *
 * 추가 토큰 추출 → NFKC → Metaspace → BPE → <s>/</s> 부착까지 HuggingFace 구현을 옮긴 것이다.
 * 네이티브 토크나이저를 쓰지 않는 이유와 골든 대조가 계약인 이유는 WordPieceTokenizer 와 같다.
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

    /** decode 에서 건너뛸 id 는 added_tokens 중 special 로 표시된 것들이다. */
    class SpecialTokens(val bosId: Int, val eosId: Int, val skipOnDecodeIds: Set<Int>)

    class Encoded(val ids: LongArray, val attentionMask: LongArray)

    fun encode(text: String): Encoded {
        val budget = maxInput?.minus(SPECIAL_TOKEN_COUNT) ?: Int.MAX_VALUE
        val body = ArrayList<Int>()

        // 추가 토큰을 먼저 떼어내고, 그 사이에 남은 구간만 정규화·사전분할·BPE 를 거친다.
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

    /** skipSpecialTokens 고정: `</s>`·`<pad>` 등 추가 토큰은 건너뛴다. */
    fun decode(ids: LongArray): String {
        val decoded = StringBuilder()
        var emitted = 0
        for (rawId in ids) {
            val token = decodableToken(rawId.toInt()) ?: continue
            // Metaspace 디코더: 첫 토큰의 구분자는 버리고, 이후 토큰의 구분자는 스페이스로 되돌린다.
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

    /** normalizer 는 NFKC 하나뿐이다. 함께 걸린 BertNormalizer 는 모든 플래그가 꺼져 있어 아무 일도 하지 않는다. */
    private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)

    /** 스페이스를 구분자로 바꾸고 선두에 하나 붙인 뒤, 구분자 앞에서 끊는다(Metaspace, prepend_scheme=always). */
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

    /** 코드포인트 단위로 쪼갠 뒤 병합 규칙을 순위가 낮은 것부터 적용한다(BPE). */
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
     * 겹치지 않는 모든 자리를 왼쪽부터 한 번에 병합한다.
     * HuggingFace 는 힙에서 (rank, 위치) 순으로 하나씩 꺼내 쓰는데, 병합으로 새로 생기는 쌍의 rank 가
     * 방금 적용한 rank 보다 항상 크다는 학습 BPE 의 성질 덕분에 결과가 같다.
     * 이 성질이 깨진 merges 테이블이 들어오면 HuggingFace 와 결과가 달라진다.
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
            context: Context,
            packName: String,
            tokenizerAsset: String,
            maxInput: Int,
        ): KobartTokenizer = fromJson(readTokenizer(context, packName, tokenizerAsset), maxInput)

        /** 토큰 수를 재려면 절단이 없어야 한다. 절단하면 한계 이상은 전부 같은 값으로 보인다. */
        suspend fun loadWithoutTruncation(
            context: Context,
            packName: String,
            tokenizerAsset: String,
        ): KobartTokenizer = fromJson(readTokenizer(context, packName, tokenizerAsset), maxInput = null)

        private suspend fun readTokenizer(
            context: Context,
            packName: String,
            tokenizerAsset: String,
        ): ByteArray = OnDemandModelAssets(context).resolve(packName, tokenizerAsset).readBytes()

        /** Context 없이 tokenizer.json 을 직접 먹이는 경로. 골든 대조 단위 테스트가 쓴다. */
        fun fromJson(bytes: ByteArray, maxInput: Int?): KobartTokenizer {
            require(maxInput == null || maxInput > SPECIAL_TOKEN_COUNT) {
                "maxInput 은 특수 토큰 수보다 커야 합니다: $maxInput"
            }

            val root = TOKENIZER_JSON.parseToJsonElement(bytes.decodeToString()).jsonObject
            val model = root.getValue("model").jsonObject
            // 모델을 재export 하면서 파이프라인이 바뀌면 결과가 조용히 틀어지므로 로드 시점에 끊는다.
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

        /**
         * 병합 규칙을 토큰 문자열이 아니라 id 쌍으로 미리 풀어둔다.
         * vocab 으로 옮길 수 없는 규칙은 적용할 방법이 없으므로 버린다(현재 파일에는 해당 없음).
         */
        private fun parseMerges(merges: JsonArray, vocab: Map<String, Int>): Map<Long, MergeRule> {
            val rules = HashMap<Long, MergeRule>(merges.size)
            merges.forEachIndexed { rank, element ->
                // 신형은 ["a", "b"], 구형은 "a b" 한 줄이다.
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
            // prepend_scheme·split 은 코드가 always/true 로 고정 구현이라 다르면 결과가 달라진다.
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
