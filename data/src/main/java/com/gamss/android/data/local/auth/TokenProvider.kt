package com.gamss.android.data.local.auth

import javax.inject.Inject
import javax.inject.Singleton

interface TokenProvider {
    fun getAccessToken(): String?
    fun updateAccessToken(accessToken: String?)
    fun clear()
}

@Singleton
 class TokenProviderImpl @Inject constructor() : TokenProvider {

    @Volatile
    private var accessToken: String? = null

    override fun getAccessToken(): String? {
        return accessToken
    }

    override fun updateAccessToken(accessToken: String?) {
        this.accessToken = accessToken
    }

    override fun clear() {
        accessToken = null
    }
}