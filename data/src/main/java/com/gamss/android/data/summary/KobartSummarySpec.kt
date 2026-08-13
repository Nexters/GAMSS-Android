package com.gamss.android.data.summary

/** kobart 요약 모델(ONNX) 자산 경로와 디코딩 상수. */
internal object KobartSummarySpec {
    // :models:summary-pack 의 assetPack { packName } 과 일치해야 한다.
    const val PACK_NAME = "summary_pack"

    const val ENCODER_ASSET = "models/kobart_encoder_int8.onnx"
    const val DECODER_ASSET = "models/kobart_decoder_int8.onnx"
    const val TOKENIZER_ASSET = "models/kobart_tokenizer.json"

    // config.json: bos = eos = decoder_start = 1, vocab 30000
    const val DECODER_START_TOKEN = 1L
    const val EOS_TOKEN = 1L
    const val VOCAB_SIZE = 30_000
    const val MAX_INPUT_TOKENS = 512
    const val MAX_OUTPUT_TOKENS = 64

    // 그리디 디코딩이 짧은/저정보 입력에서 반복 루프에 빠지지 않도록 반복 3-gram을 금지한다.
    const val NO_REPEAT_NGRAM = 3
}
