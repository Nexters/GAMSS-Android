package com.gamss.android.domain.conversation

/** 서버가 받는 컨텍스트 압축본의 최대 길이. 넘기면 전송 자체가 INVALID_INPUT 으로 거절된다. */
const val MAX_CONTEXT_SUMMARY_LENGTH = 2000

/** 원문으로 남기는 최근 발화 수. 직전 맥락이 답글 품질에 가장 크게 기여한다. */
const val RECENT_RAW_UTTERANCES = 3

/** 요약기(kobart) 인코더 입력 한계. 이 크기로 청크를 끊어 한 번씩만 요약한다. */
const val SUMMARY_CHUNK_TOKEN_BUDGET = 512
