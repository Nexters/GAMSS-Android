package com.gamss.android.feature.emotion

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

@Suppress("SwallowedException")
fun Context.dial(phoneNumber: String): Boolean =
    try {
        startActivity(Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(phoneNumber)}".toUri()))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
