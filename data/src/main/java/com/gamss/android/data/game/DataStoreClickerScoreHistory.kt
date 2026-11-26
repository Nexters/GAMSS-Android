package com.gamss.android.data.game

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gamss.android.domain.game.ClickerScoreHistory
import com.gamss.android.domain.game.ClickerScoreRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 기록은 최대 10개라 "점수:시각" 을 쉼표로 이은 문자열 하나로 저장한다. */
@Singleton
internal class DataStoreClickerScoreHistory @Inject constructor(
    @param:ClickerScoreDataStore private val dataStore: DataStore<Preferences>,
) : ClickerScoreHistory {

    override suspend fun load(): List<ClickerScoreRecord> = withContext(Dispatchers.IO) {
        decode(dataStore.data.first()[RECORDS_KEY])
    }

    override suspend fun update(
        transform: (List<ClickerScoreRecord>) -> List<ClickerScoreRecord>,
    ): List<ClickerScoreRecord> = withContext(Dispatchers.IO) {
        lateinit var updated: List<ClickerScoreRecord>
        dataStore.edit { preferences ->
            updated = transform(decode(preferences[RECORDS_KEY]))
            preferences[RECORDS_KEY] = encode(updated)
        }
        updated
    }

    private fun encode(records: List<ClickerScoreRecord>): String =
        records.joinToString(RECORD_SEPARATOR) { "${it.score}$FIELD_SEPARATOR${it.playedAtMillis}" }

    // 형식이 깨진 항목은 버리고 나머지 기록은 살린다.
    private fun decode(value: String?): List<ClickerScoreRecord> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(RECORD_SEPARATOR).mapNotNull { entry ->
            val fields = entry.split(FIELD_SEPARATOR)
            val score = fields.getOrNull(0)?.toIntOrNull()
            val playedAtMillis = fields.getOrNull(1)?.toLongOrNull()
            if (fields.size != 2 || score == null || playedAtMillis == null) return@mapNotNull null
            ClickerScoreRecord(score = score, playedAtMillis = playedAtMillis)
        }
    }

    private companion object {
        val RECORDS_KEY = stringPreferencesKey("clicker_top_scores")
        const val RECORD_SEPARATOR = ","
        const val FIELD_SEPARATOR = ":"
    }
}
