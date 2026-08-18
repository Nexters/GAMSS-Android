package com.gamss.android.core.ui.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val INSTAGRAM_PACKAGE = "com.instagram.android"
private const val ADD_TO_STORY_ACTION = "com.instagram.share.ADD_TO_STORY"
private const val SHARE_CACHE_DIR = "shared_images"
private const val SHARE_FILE_NAME = "story_share.png"

/** 스토리 캔버스 크기(9:16). 인스타그램은 배경 이미지를 이 비율로 늘려 채운다. */
private const val STORY_WIDTH = 1080
private const val STORY_HEIGHT = 1920

/** 카드가 스토리 화면에 꽉 차지 않도록 남기는 좌우·상하 여백 비율. */
private const val HORIZONTAL_MARGIN_RATIO = 0.16f
private const val VERTICAL_MARGIN_RATIO = 0.10f

/** 여백을 채우는 기본 배경색. 밝은 카드가 돋보이도록 앱의 진한 톤(GrayLight950)을 쓴다. */
private const val DEFAULT_STORY_BACKGROUND_COLOR = 0xFF1E1F22.toInt()

/**
 * [bitmap] 을 인스타그램 스토리로 공유한다. 인스타그램이 없으면 `false`.
 *
 * 인스타그램은 넘겨받은 배경 이미지를 9:16 캔버스에 꽉 채우므로, 카드를 그대로 보내면
 * 비율이 맞지 않아 잘리고 여백도 사라진다. 그래서 미리 9:16 캔버스 가운데에 카드를 놓고
 * 남는 곳을 [backgroundColor] 로 채운 이미지를 만들어 보낸다.
 */
@Suppress("SwallowedException")
fun Context.shareBitmapToInstagramStory(
    bitmap: Bitmap,
    @ColorInt backgroundColor: Int = DEFAULT_STORY_BACKGROUND_COLOR,
): Boolean {
    val uri = bitmap.toStoryCanvas(backgroundColor).saveToShareCache(this) ?: return false
    val intent = Intent(ADD_TO_STORY_ACTION).apply {
        setDataAndType(uri, "image/png")
        setPackage(INSTAGRAM_PACKAGE)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    grantUriPermission(INSTAGRAM_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** 9:16 캔버스 가운데에 원본 비율을 유지한 카드를 놓고 나머지를 [backgroundColor] 로 채운다. */
private fun Bitmap.toStoryCanvas(@ColorInt backgroundColor: Int): Bitmap {
    val story = createBitmap(STORY_WIDTH, STORY_HEIGHT)
    val canvas = Canvas(story)
    canvas.drawColor(backgroundColor)

    val availableWidth = STORY_WIDTH * (1f - HORIZONTAL_MARGIN_RATIO * 2)
    val availableHeight = STORY_HEIGHT * (1f - VERTICAL_MARGIN_RATIO * 2)
    val scale = minOf(availableWidth / width, availableHeight / height)
    val targetWidth = width * scale
    val targetHeight = height * scale
    val left = (STORY_WIDTH - targetWidth) / 2f
    val top = (STORY_HEIGHT - targetHeight) / 2f

    canvas.drawBitmap(
        toSoftwareBitmap(),
        null,
        RectF(left, top, left + targetWidth, top + targetHeight),
        Paint(Paint.FILTER_BITMAP_FLAG),
    )
    return story
}

/**
 * 소프트웨어 [Canvas] 에 그릴 수 있는 비트맵으로 바꾼다.
 *
 * Compose 의 GraphicsLayer 캡처는 [Bitmap.Config.HARDWARE] 비트맵을 주는데, 이건 GPU 메모리에만
 * 있어서 소프트웨어 캔버스에 그리면 예외가 난다. PNG 압축만 할 때는 문제가 없어 이 변환이 필요 없었다.
 */
private fun Bitmap.toSoftwareBitmap(): Bitmap =
    if (config == Bitmap.Config.HARDWARE) {
        copy(Bitmap.Config.ARGB_8888, false) ?: this
    } else {
        this
    }

@Suppress("SwallowedException")
private fun Bitmap.saveToShareCache(context: Context): Uri? {
    val dir = File(context.cacheDir, SHARE_CACHE_DIR).apply { mkdirs() }
    val file = File(dir, SHARE_FILE_NAME)
    return try {
        FileOutputStream(file).use { out -> compress(Bitmap.CompressFormat.PNG, 100, out) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: IOException) {
        null
    }
}
