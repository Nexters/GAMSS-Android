package com.gamss.android.core.common.util

import java.security.MessageDigest

/**
 * SHA-256 해시의 16진 문자열. Play Billing의 setObfuscatedAccountId처럼, 식별자를 원본 그대로
 * 넘기지 않고 안정적이면서도 되돌릴 수 없는 값으로 바꿔야 할 때 쓴다 — 같은 입력은 항상 같은
 * 값을 내므로 "안정적" 요구사항을 만족한다.
 */
fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}
