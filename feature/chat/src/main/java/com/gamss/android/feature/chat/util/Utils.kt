package com.gamss.android.feature.chat.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.gamss.android.feature.chat.R

fun Context.dialOrNotify(phoneNumber: String?) {
    if (phoneNumber == null) return
    if (!dial(phoneNumber)) {
        Toast.makeText(
            this,
            getString(R.string.safety_call_unavailable, phoneNumber),
            Toast.LENGTH_LONG,
        ).show()
    }
}

@Suppress("SwallowedException")
private fun Context.dial(phoneNumber: String): Boolean =
    try {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}")))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
