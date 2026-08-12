package com.gamss.android.data.safety

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal data class CachedRiskLexicon(
    val json: String,
    val fetchedAtMillis: Long,
)

@Singleton
internal class RiskLexiconLocalDataSource @Inject constructor(
    @param:RiskLexiconDataStore private val dataStore: DataStore<Preferences>,
) {
    suspend fun read(): CachedRiskLexicon? = withContext(Dispatchers.IO) {
        val preferences = dataStore.data.first()
        val json = preferences[LEXICON_JSON_KEY] ?: return@withContext null
        CachedRiskLexicon(
            json = json,
            fetchedAtMillis = preferences[FETCHED_AT_KEY] ?: 0L,
        )
    }

    suspend fun write(json: String, fetchedAtMillis: Long): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[LEXICON_JSON_KEY] = json
            preferences[FETCHED_AT_KEY] = fetchedAtMillis
        }
    }

    suspend fun markFetched(fetchedAtMillis: Long): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[FETCHED_AT_KEY] = fetchedAtMillis
        }
    }

    private companion object {
        val LEXICON_JSON_KEY = stringPreferencesKey("risk_lexicon_json")
        val FETCHED_AT_KEY = longPreferencesKey("risk_lexicon_fetched_at")
    }
}
