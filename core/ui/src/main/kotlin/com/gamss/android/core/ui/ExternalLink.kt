package com.gamss.android.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/**
 * 다이얼러에 번호만 채우고 실제 발신은 사용자가 누른다. CALL_PHONE 권한을 요구하지 않기 위해서다.
 *
 * 다이얼러가 없는 기기에서도 크래시 없이 false 를 돌려준다. 위기 안내에서 버튼이 아무 반응 없이
 * 무시되면 안 되므로 호출부가 대체 안내를 띄울 수 있어야 한다.
 */
@Suppress("SwallowedException")
fun Context.dial(phoneNumber: String): Boolean =
    try {
        startActivity(Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(phoneNumber)}".toUri()))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
