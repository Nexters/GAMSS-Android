package com.gamss.android.domain.emotion

/** 카톡형 대화의 한 발화. */
data class ConversationTurn(
    val speaker: String,
    val message: String,
)

/**
 * "화자: 메시지" 형식의 카톡형 대화 텍스트를 발화 목록으로 파싱한다.
 * 화자 접두가 없는 줄은 직전 발화의 이어지는 메시지로 합친다.
 */
object ConversationParser {

    // 줄 앞부분의 "이름:" (이름은 콜론/개행 없는 1~20자)
    private val SPEAKER_LINE = Regex("""^\s*([^:\n]{1,20}?)\s*:\s*(.+)$""")

    // 화자 접두가 전혀 없는 일기 단문일 때의 기본 화자
    const val DIARY_SPEAKER = "나"

    fun parse(raw: String): List<ConversationTurn> {
        val turns = mutableListOf<ConversationTurn>()
        raw.lineSequence().forEach { line ->
            val match = SPEAKER_LINE.find(line)
            when {
                match != null -> turns += ConversationTurn(
                    speaker = match.groupValues[1].trim(),
                    message = match.groupValues[2].trim(),
                )

                line.isNotBlank() && turns.isNotEmpty() -> {
                    val last = turns.removeAt(turns.lastIndex)
                    turns += last.copy(message = "${last.message} ${line.trim()}".trim())
                }
            }
        }
        // '화자: 메시지' 형식이 없으면(예: 일기 단문) 전체를 한 화자의 발화로 본다.
        if (turns.isEmpty() && raw.isNotBlank()) {
            turns += ConversationTurn(DIARY_SPEAKER, raw.trim().replace("\n", " "))
        }
        return turns
    }
}

/** 대표(메인) 화자 = 발화한 글자 수가 가장 많은 화자. */
fun List<ConversationTurn>.mainSpeaker(): String? =
    groupBy { it.speaker }
        .maxByOrNull { (_, turns) -> turns.sumOf { it.message.length } }
        ?.key
