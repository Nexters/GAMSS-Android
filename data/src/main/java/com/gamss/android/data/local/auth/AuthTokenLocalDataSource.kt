package com.gamss.android.data.local.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gamss.android.data.local.auth.model.StoredAuthTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal interface AuthTokenLocalDataSource {

    suspend fun saveTokens(tokens: StoredAuthTokens)

    suspend fun getTokens(): StoredAuthTokens

    suspend fun clearTokens()
}

@Singleton
internal class EncryptedAuthTokenLocalDataSource @Inject constructor(
    @param:AuthTokenDataStore private val dataStore: DataStore<Preferences>,
    private val tokenCipher: TokenCipher,
    private val tokenProvider: TokenProvider
) : AuthTokenLocalDataSource {

    private val mutex = Mutex()

    override suspend fun saveTokens(tokens: StoredAuthTokens) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val encryptedAccessToken = tokens.accessToken?.let {
                tokenCipher.encrypt(it, ACCESS_TOKEN_ASSOCIATED_DATA)
            }
            val encryptedRefreshToken = tokens.refreshToken?.let {
                tokenCipher.encrypt(it, REFRESH_TOKEN_ASSOCIATED_DATA)
            }

            dataStore.edit { preferences ->
                preferences.setOrRemove(ACCESS_TOKEN_KEY, encryptedAccessToken)
                preferences.setOrRemove(REFRESH_TOKEN_KEY, encryptedRefreshToken)
            }

            // 캐시 갱신
            tokenProvider.updateAccessToken(tokens.accessToken)
        }
    }

    override suspend fun getTokens(): StoredAuthTokens = withContext(Dispatchers.IO) {
        mutex.withLock {
            val preferences = dataStore.data.first()

            val tokens = StoredAuthTokens(
                accessToken = preferences[ACCESS_TOKEN_KEY]?.let {
                    tokenCipher.decrypt(it, ACCESS_TOKEN_ASSOCIATED_DATA)
                },
                refreshToken = preferences[REFRESH_TOKEN_KEY]?.let {
                    tokenCipher.decrypt(it, REFRESH_TOKEN_ASSOCIATED_DATA)
                },
            )

            tokenProvider.updateAccessToken(tokens.accessToken)
            tokens
        }
    }

    override suspend fun clearTokens() = withContext(Dispatchers.IO) {
        mutex.withLock {
            dataStore.edit { preferences ->
                preferences.remove(ACCESS_TOKEN_KEY)
                preferences.remove(REFRESH_TOKEN_KEY)
            }
            tokenProvider.clear()
        }
    }

    private fun MutablePreferences.setOrRemove(
        key: Preferences.Key<String>,
        value: String?,
    ) {
        if (value == null) {
            remove(key)
        } else {
            this[key] = value
        }
    }

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("encrypted_access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("encrypted_refresh_token")

        private const val ACCESS_TOKEN_ASSOCIATED_DATA = "gamss.auth.access_token"
        private const val REFRESH_TOKEN_ASSOCIATED_DATA = "gamss.auth.refresh_token"
    }
}
