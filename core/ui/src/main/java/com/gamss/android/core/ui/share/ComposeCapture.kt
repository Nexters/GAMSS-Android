package com.gamss.android.core.ui.share

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer

@Composable
fun rememberCaptureGraphicsLayer(): GraphicsLayer = rememberGraphicsLayer()

fun Modifier.captureToGraphicsLayer(graphicsLayer: GraphicsLayer): Modifier =
    drawWithContent {
        graphicsLayer.record { this@drawWithContent.drawContent() }
        drawLayer(graphicsLayer)
    }

suspend fun GraphicsLayer.toAndroidBitmap(): Bitmap = toImageBitmap().asAndroidBitmap()
