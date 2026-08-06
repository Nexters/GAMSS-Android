package com.gamss.android.domain.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationTitlePolicyTest {

    @Test
    fun 앞머리의_감탄사와_감정_표현을_걷어내고_사실만_남긴다() {
        val title = conversationTitleFrom("아 진짜 짜증나 팀장이 회의에서 아이디어 가로챘어")

        assertEquals("팀장이 회의에서 아이디어 가로챘어", title)
    }

    @Test
    fun 감정뿐인_문장은_건너뛰고_사실이_있는_문장을_고른다() {
        assertEquals("발표 망쳤거든", conversationTitleFrom("너무 속상해. 발표 망쳤거든"))
        assertEquals(
            "팀장이 회의에서 아이디어 가로챘어",
            conversationTitleFrom("오늘 너무 힘들었어. 팀장이 회의에서 아이디어 가로챘어"),
        )
    }

    @Test
    fun 사실이_짧게_남는_문장에서_멈추지_않는다() {
        assertEquals("팀장이 아이디어 가로챘어", conversationTitleFrom("아 몰라. 팀장이 아이디어 가로챘어"))
        assertEquals("지갑 잃어버렸어", conversationTitleFrom("아 놔. 지갑 잃어버렸어"))
        assertEquals("면접 떨어졌어", conversationTitleFrom("아우 씨. 면접 떨어졌어"))
        assertEquals("팀장이 아이디어 가로챘어", conversationTitleFrom("진짜 짜증나 참. 오늘 팀장이 아이디어 가로챘어"))
    }

    @Test
    fun 짧은_사실뿐이면_그것을_쓴다() {
        assertEquals("팀장 탓", conversationTitleFrom("아 진짜. 팀장 탓"))
    }

    @Test
    fun 대화_시점과_겹치는_말만_앞머리에서_뗀다() {
        assertEquals("팀장이 화냈어", conversationTitleFrom("오늘 팀장이 화냈어"))
        assertEquals("어제 계약서 도장 잘못 찍었어", conversationTitleFrom("어제 계약서 도장 잘못 찍었어"))
        assertEquals("내일 면접이야", conversationTitleFrom("내일 면접이야"))
        assertEquals("또 지각했어", conversationTitleFrom("또 지각했어"))
    }

    @Test
    fun 문장_중간과_뒤의_감정_표현은_건드리지_않는다() {
        val title = conversationTitleFrom("팀장이 짜증나게 했어")

        assertEquals("팀장이 짜증나게 했어", title)
    }

    @Test
    fun 감정어를_품은_사실_명사는_지우지_않는다() {
        assertEquals("괴롭힘 신고했어 결국", conversationTitleFrom("괴롭힘 신고했어 결국"))
        assertEquals("화가 친구 전시회 갔어", conversationTitleFrom("화가 친구 전시회 갔어"))
        assertEquals("행복주택 청약 떨어졌어", conversationTitleFrom("행복주택 청약 떨어졌어"))
        assertEquals("개 산책시키다 넘어졌어", conversationTitleFrom("개 산책시키다 넘어졌어"))
        assertEquals("불안 장애 진단받았어", conversationTitleFrom("불안 장애 진단받았어"))
        assertEquals("눈물 났어 결국", conversationTitleFrom("눈물 났어 결국"))
    }

    @Test
    fun 불규칙_활용_감정어도_걷어낸다() {
        assertEquals("성적표 나왔어", conversationTitleFrom("아 슬퍼. 성적표 나왔어"))
        assertEquals("합격 소식 들었어", conversationTitleFrom("진짜 기뻐. 합격 소식 들었어"))
        assertEquals("동생이 내 옷 입고 나갔어", conversationTitleFrom("화났어 동생이 내 옷 입고 나갔어"))
    }

    @Test
    fun 붙여_쓴_감정_표현도_걷어낸다() {
        assertEquals(
            "팀장새끼 나 일시켜놓고 지혼자 쏠랑…",
            conversationTitleFrom("이진짜개짜증나 팀장새끼 나 일시켜놓고 지혼자 쏠랑 튀었어"),
        )
        assertEquals("지갑 잃어버렸어", conversationTitleFrom("아진짜 개짜증나 지갑 잃어버렸어"))
    }

    @Test
    fun 군더더기로_시작하는_사실_명사는_지우지_않는다() {
        assertEquals("이직 확정됐어", conversationTitleFrom("이직 확정됐어"))
        assertEquals("개발자 모임 다녀왔어", conversationTitleFrom("개발자 모임 다녀왔어"))
        assertEquals("이별했어 결국", conversationTitleFrom("이별했어 결국"))
        assertEquals("그날 일이 자꾸 생각나", conversationTitleFrom("그날 일이 자꾸 생각나"))
    }

    @Test
    fun 어절_끝의_기호는_판정을_막지_않는다() {
        val title = conversationTitleFrom("아ㅠㅠ 진짜, 짜증나ㅋㅋ 지갑 잃어버렸어")

        assertEquals("지갑 잃어버렸어", title)
    }

    @Test
    fun 사실이_없는_시드는_원문을_그대로_쓴다() {
        assertEquals("오늘 너무 힘들었어", conversationTitleFrom("오늘 너무 힘들었어"))
        assertEquals("아 진짜 짜증나", conversationTitleFrom("아 진짜 짜증나"))
        assertEquals("오늘 많이 힘들었어", conversationTitleFrom("오늘\n\n  많이   힘들었어"))
    }

    @Test
    fun 첫_문장에_사실이_있으면_거기서_멈춘다() {
        val title = conversationTitleFrom("억울한 일이 있었어. 팀장이 화를 냈다.")

        assertEquals("억울한 일이 있었어", title)
    }

    @Test
    fun 감정뿐인_시드는_첫_문장을_쓰되_한_마디면_이어_붙인다() {
        assertEquals("짜증나 힘들어", conversationTitleFrom("짜증나. 힘들어."))
    }

    @Test
    fun 소수점은_문장_경계가_아니다() {
        val title = conversationTitleFrom("3.5시간 잤어 진짜 피곤해")

        assertEquals("3.5시간 잤어 진짜 피곤해", title)
    }

    @Test
    fun 최대_길이를_넘으면_말줄임으로_상한_안에_담는다() {
        val title = conversationTitleFrom("가".repeat(MAX_CONVERSATION_TITLE_LENGTH + 1))

        assertEquals("가".repeat(MAX_CONVERSATION_TITLE_LENGTH - 1) + "…", title)
    }

    @Test
    fun 최대_길이와_같으면_그대로_쓴다() {
        val seed = "가".repeat(MAX_CONVERSATION_TITLE_LENGTH)

        assertEquals(seed, conversationTitleFrom(seed))
    }

    @Test
    fun 문장부호_없이_긴_입력도_상한_안에_담는다() {
        val title = conversationTitleFrom("가".repeat(MAX_MESSAGE_LENGTH))

        assertEquals(MAX_CONVERSATION_TITLE_LENGTH, title?.length)
    }

    @Test
    fun 자르는_자리가_공백이면_공백을_남기지_않는다() {
        val title = conversationTitleFrom("가".repeat(MAX_CONVERSATION_TITLE_LENGTH - 2) + " 나나나나나")

        assertEquals("가".repeat(MAX_CONVERSATION_TITLE_LENGTH - 2) + "…", title)
    }

    @Test
    fun 이모지가_잘리는_자리에_오면_통째로_뺀다() {
        val title = conversationTitleFrom("가".repeat(MAX_CONVERSATION_TITLE_LENGTH - 2) + "😀좋다")

        assertEquals("가".repeat(MAX_CONVERSATION_TITLE_LENGTH - 2) + "…", title)
    }

    @Test
    fun 상한_안에_들어가는_이모지는_그대로_둔다() {
        val seed = "가".repeat(MAX_CONVERSATION_TITLE_LENGTH - 2) + "😀"

        assertEquals(seed, conversationTitleFrom(seed))
    }

    @Test
    fun 문장부호와_공백만_있으면_제목을_만들지_않는다() {
        assertNull(conversationTitleFrom("   \n  "))
        assertNull(conversationTitleFrom("..."))
    }
}
