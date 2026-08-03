package com.gamss.android.data.safety

import android.content.Context
import com.gamss.android.data.safety.model.RiskLexiconDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱에 내장된 기본 사전. 네트워크와 캐시가 모두 없을 때의 최종 폴백이라 실패하면 그대로 던진다.
 */
@Singleton
internal class BundledRiskLexiconDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) {
    suspend fun load(): RiskLexiconDto = withContext(Dispatchers.IO) {
        context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
            json.decodeFromString<RiskLexiconDto>(reader.readText())
        }
    }

    private companion object {
        const val ASSET_NAME = "risk_lexicon.json"
    }
}
