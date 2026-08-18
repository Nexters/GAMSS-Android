package com.gamss.android.domain.push

interface PushTokenProvider {

    suspend fun getToken(): String?
}
