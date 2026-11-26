package com.gamss.android.data.game

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gamss.android.domain.game.ClickerScoreRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreClickerScoreHistoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "clicker_score.preferences_pb") }
    }
    private val history by lazy { DataStoreClickerScoreHistory(dataStore) }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `저장한 기록을 순서 그대로 다시 읽는다`() = runTest {
        val records = listOf(
            ClickerScoreRecord(score = 120, playedAtMillis = 1_700_000_000_000L),
            ClickerScoreRecord(score = 80, playedAtMillis = 1_700_000_100_000L),
        )

        val updated = history.update { records }

        assertEquals(records, updated)
        assertEquals(records, history.load())
    }

    @Test
    fun `기록이 없으면 빈 목록이다`() = runTest {
        assertEquals(emptyList<ClickerScoreRecord>(), history.load())
    }

    @Test
    fun `형식이 깨진 항목만 버린다`() = runTest {
        dataStore.edit { it[stringPreferencesKey("clicker_top_scores")] = "10:1,broken,20:x,30:3" }

        assertEquals(
            listOf(
                ClickerScoreRecord(score = 10, playedAtMillis = 1L),
                ClickerScoreRecord(score = 30, playedAtMillis = 3L),
            ),
            history.load(),
        )
    }
}
