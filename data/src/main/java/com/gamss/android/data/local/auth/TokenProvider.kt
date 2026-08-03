package com.gamss.android.data.local.auth

import javax.inject.Inject
import javax.inject.Singleton

internal interface TokenProvider {
    fun getAccessToken(): String?
    fun updateAccessToken(accessToken: String?)
    fun clear()
}

@Singleton
internal class TokenProviderImpl @Inject constructor() : TokenProvider {

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
