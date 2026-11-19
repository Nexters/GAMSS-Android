package com.gamss.android.data.remote

internal fun String.redactTokenValues(): String =
    TOKEN_JSON_PATTERN.replace(this) { matchResult ->
        "${matchResult.groupValues[1]}<redacted>${matchResult.groupValues[2]}"
    }

private val TOKEN_JSON_PATTERN =
    Regex("(\"(?:idToken|accessToken|refreshToken|purchaseToken|accountToken)\"\\s*:\\s*\")[^\"]*(\")")
