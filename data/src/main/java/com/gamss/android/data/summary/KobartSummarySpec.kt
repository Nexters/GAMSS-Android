package com.gamss.android.data.summary

/** kobart 요약 모델(ONNX) 자산 경로와 디코딩 상수. */
internal object KobartSummarySpec {
    const val ENCODER_ASSET = "models/kobart_encoder_int8.onnx"
    const val DECODER_ASSET = "models/kobart_decoder_int8.onnx"
    const val TOKENIZER_ASSET = "models/kobart_tokenizer.json"

    // config.json: bos = eos = decoder_start = 1, vocab 30000
    const val DECODER_START_TOKEN = 1L
    const val EOS_TOKEN = 1L
    const val VOCAB_SIZE = 30_000
    const val MAX_INPUT_TOKENS = 512
    const val MAX_OUTPUT_TOKENS = 64
}
