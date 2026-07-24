package com.gamss.android.data.local.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.gamss.android.data.local.auth.model.StoredAuthTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class EncryptedAuthTokenLocalDataSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tokenCipher: FakeTokenCipher
    private lateinit var dataSource: EncryptedAuthTokenLocalDataSource

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStoreFile = temporaryFolder.newFolder().resolve("auth_tokens.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        tokenCipher = FakeTokenCipher()
        dataSource = EncryptedAuthTokenLocalDataSource(dataStore, tokenCipher)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `save and restore every nullable token combination`() = runBlocking {
        val cases = listOf(
            StoredAuthTokens(accessToken = "access", refreshToken = "refresh"),
            StoredAuthTokens(accessToken = "access", refreshToken = null),
            StoredAuthTokens(accessToken = null, refreshToken = "refresh"),
            StoredAuthTokens(accessToken = null, refreshToken = null),
        )

        cases.forEach { expected ->
            dataSource.saveTokens(expected)

            assertEquals(expected, dataSource.getTokens())
        }
    }

    @Test
    fun `stored preferences do not contain plaintext tokens`() = runBlocking {
        dataSource.saveTokens(
            StoredAuthTokens(accessToken = "plain-access", refreshToken = "plain-refresh"),
        )

        val preferences = dataStore.data.first()

        assertNotEquals(
            "plain-access",
            preferences[EncryptedAuthTokenLocalDataSource.ACCESS_TOKEN_KEY],
        )
        assertNotEquals(
            "plain-refresh",
            preferences[EncryptedAuthTokenLocalDataSource.REFRESH_TOKEN_KEY],
        )
    }

    @Test
    fun `saving null removes the previous token`() = runBlocking {
        dataSource.saveTokens(
            StoredAuthTokens(accessToken = "access", refreshToken = "refresh"),
        )

        dataSource.saveTokens(
            StoredAuthTokens(accessToken = null, refreshToken = "new-refresh"),
        )

        val preferences = dataStore.data.first()
        assertNull(preferences[EncryptedAuthTokenLocalDataSource.ACCESS_TOKEN_KEY])
        assertEquals(
            StoredAuthTokens(accessToken = null, refreshToken = "new-refresh"),
            dataSource.getTokens(),
        )
    }

    @Test
    fun `empty storage returns an auth response with null tokens`() = runBlocking {
        assertEquals(StoredAuthTokens(accessToken = null, refreshToken = null), dataSource.getTokens())
    }

    @Test
    fun `clear removes both tokens`() = runBlocking {
        dataSource.saveTokens(
            StoredAuthTokens(accessToken = "access", refreshToken = "refresh"),
        )

        dataSource.clearTokens()

        assertEquals(StoredAuthTokens(accessToken = null, refreshToken = null), dataSource.getTokens())
    }

    @Test
    fun `tampered ciphertext is not converted to a null token`() {
        runBlocking {
            dataStore.edit { preferences ->
                preferences[EncryptedAuthTokenLocalDataSource.ACCESS_TOKEN_KEY] = "tampered"
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { dataSource.getTokens() }
        }
    }

    @Test
    fun `failed update keeps the previous token pair`() = runBlocking {
        val previous = StoredAuthTokens(accessToken = "old-access", refreshToken = "old-refresh")
        dataSource.saveTokens(previous)
        val failingDataSource = EncryptedAuthTokenLocalDataSource(
            dataStore = FailingUpdateDataStore(dataStore),
            tokenCipher = tokenCipher,
        )

        assertThrows(IOException::class.java) {
            runBlocking {
                failingDataSource.saveTokens(
                    StoredAuthTokens(accessToken = "new-access", refreshToken = null),
                )
            }
        }
        assertEquals(previous, dataSource.getTokens())
    }

    private class FakeTokenCipher : TokenCipher {
        override fun encrypt(value: String, associatedData: String): String =
            "$associatedData:$value".reversed()

        override fun decrypt(value: String, associatedData: String): String {
            val decoded = value.reversed()
            val prefix = "$associatedData:"
            require(decoded.startsWith(prefix)) { "Ciphertext authentication failed." }
            return decoded.removePrefix(prefix)
        }
    }

    private class FailingUpdateDataStore(
        private val delegate: DataStore<Preferences>,
    ) : DataStore<Preferences> {
        override val data: Flow<Preferences> = delegate.data

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = throw IOException("DataStore update failed.")
    }
}
