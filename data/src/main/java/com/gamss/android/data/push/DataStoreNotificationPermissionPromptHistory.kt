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

@Singleton
internal class DataStoreNotificationPermissionPromptHistory @Inject constructor(
    @param:NotificationPermissionPromptDataStore private val dataStore: DataStore<Preferences>,
) : NotificationPermissionPromptHistory {

    override suspend fun hasPrompted(): Boolean = withContext(Dispatchers.IO) {
        dataStore.data.first()[PROMPTED_KEY] == true
    }

    override suspend fun markPrompted() {
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences -> preferences[PROMPTED_KEY] = true }
        }
    }

    private companion object {
        val PROMPTED_KEY = booleanPreferencesKey("notification_permission_prompted")
    }
}
