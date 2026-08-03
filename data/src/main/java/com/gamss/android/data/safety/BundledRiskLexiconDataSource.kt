package com.gamss.android.data.safety

import android.content.Context
import com.gamss.android.data.safety.model.RiskLexiconDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

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
