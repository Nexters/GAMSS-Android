package com.gamss.android.data.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.gamss.android.domain.push.NotificationPermissionPromptHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 온보딩에서 권한을 결정한 직후 메인 화면이 같은 값을 읽는다. 디스크 쓰기가 끝나기를 기다리면
 * 그 사이에 요청이 한 번 더 나가므로 메모리에 먼저 반영한다.
 */
@Singleton
internal class DataStoreNotificationPermissionPromptHistory @Inject constructor(
    @param:NotificationPermissionPromptDataStore private val dataStore: DataStore<Preferences>,
) : NotificationPermissionPromptHistory {

    @Volatile
    private var prompted: Boolean = false

    override suspend fun hasPrompted(): Boolean {
        if (prompted) return true
        val stored = withContext(Dispatchers.IO) {
            dataStore.data.first()[PROMPTED_KEY] == true
        }
        if (stored) prompted = true
        return stored
    }

    override suspend fun markPrompted() {
        prompted = true
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences -> preferences[PROMPTED_KEY] = true }
        }
    }

    private companion object {
        val PROMPTED_KEY = booleanPreferencesKey("notification_permission_prompted")
    }
}
