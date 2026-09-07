package com.gamss.android.core.ui.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.gamss.android.core.designsystem.theme.GamssCardExportBackground
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "InstagramStoryShare"

private const val INSTAGRAM_PACKAGE = "com.instagram.android"
private const val ADD_TO_STORY_ACTION = "com.instagram.share.ADD_TO_STORY"
private const val PNG_MIME_TYPE = "image/png"

private const val PNG_QUALITY = 100

private const val SHARE_CACHE_DIR = "shared_images"
private const val SHARE_FILE_PREFIX = "story_share_"
private const val SHARE_FILE_SUFFIX = ".png"

/** 지난 공유 이미지를 지우는 기준. 인스타그램이 URI 를 읽는 중일 수 있어 방금 것은 건드리지 않는다. */
private const val SHARE_FILE_TTL_MILLIS = 60L * 60L * 1000L

/** 공유 시도의 결과. 실패 이유마다 사용자에게 알릴 말이 다르므로 나눠서 돌려준다. */
sealed interface StoryShareResult {

    /** 인스타그램 스토리 편집기로 넘어갔다. */
    data object Shared : StoryShareResult

    /** 인스타그램이 없거나 스토리 공유를 받지 못한다. */
    data object InstagramUnavailable : StoryShareResult

    /** 공유할 이미지를 만들지 못했다. 캡처 크기가 0 이거나 캐시에 쓰지 못한 경우. */
    data object ImageUnavailable : StoryShareResult
}

/**
 * [bitmap] 을 인스타그램 스토리로 공유한다.
 *
 * 인스타그램은 넘겨받은 배경 이미지를 9:16 캔버스에 꽉 채우므로, 카드를 그대로 보내면 비율이
 * 맞지 않아 잘리고 여백도 사라진다. 그래서 미리 9:16 캔버스 가운데에 카드를 놓고 남는 곳을
 * [backgroundColor] 로 채운 이미지를 만들어 보낸다.
 *
 * [bitmap] 은 여기서 해제하지 않는다. 호출부가 계속 들고 있을 수 있으므로 소유권을 넘기지 않는다.
 */
fun Context.shareBitmapToInstagramStory(
    bitmap: Bitmap,
    @ColorInt backgroundColor: Int = GamssCardExportBackground.toArgb(),
): StoryShareResult {
    val uri = createStoryImageUri(bitmap, backgroundColor) ?: return StoryShareResult.ImageUnavailable

    val intent = Intent(ADD_TO_STORY_ACTION).apply {
        setDataAndType(uri, PNG_MIME_TYPE)
        setPackage(INSTAGRAM_PACKAGE)
        // 인텐트로 열리는 액티비티에 data URI 읽기를 허용한다. 별도 grantUriPermission 은 회수할
        // 시점이 없어 계속 남으므로 쓰지 않는다.
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        startActivity(intent)
        StoryShareResult.Shared
    } catch (e: ActivityNotFoundException) {
        Log.i(TAG, "인스타그램 스토리 공유를 받을 액티비티가 없다", e)
        StoryShareResult.InstagramUnavailable
    } catch (e: SecurityException) {
        Log.i(TAG, "인스타그램 스토리 공유 액티비티를 실행할 권한이 없다", e)
        StoryShareResult.InstagramUnavailable
    }
}

/** 공유용 9:16 이미지를 만들어 캐시에 쓰고 URI 를 준다. 만들지 못하면 `null`. */
private fun Context.createStoryImageUri(bitmap: Bitmap, @ColorInt backgroundColor: Int): Uri? {
    val story = bitmap.toStoryCanvas(backgroundColor) ?: return null
    return try {
        story.saveToShareCache(this)
    } finally {
        // 파일로 쓴 뒤에는 8MB 남짓한 캔버스를 들고 있을 이유가 없다.
        story.recycle()
    }
}

/**
 * 9:16 캔버스 가운데에 원본 비율을 유지한 카드를 놓고 나머지를 [backgroundColor] 로 채운다.
 * 카드 크기가 0 이거나 소프트웨어 비트맵으로 바꾸지 못하면 그릴 수 없어 `null`.
 */
private fun Bitmap.toStoryCanvas(@ColorInt backgroundColor: Int): Bitmap? {
    val bounds = storyCardBounds(width, height) ?: return null

    return toSoftwareBitmap()?.let { source ->
        val story = createBitmap(STORY_WIDTH, STORY_HEIGHT)
        val canvas = Canvas(story)
        canvas.drawColor(backgroundColor)
        canvas.drawBitmap(
            source,
            null,
            RectF(bounds.left, bounds.top, bounds.right, bounds.bottom),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        // 변환 때문에 새로 만든 복사본만 해제한다. 원본은 호출부 소유다.
        if (source !== this) source.recycle()
        story
    }
}

/**
 * 소프트웨어 [Canvas] 에 그릴 수 있는 비트맵으로 바꾼다. 변환에 실패하면(메모리 부족 등) `null`.
 *
 * Compose 의 GraphicsLayer 캡처는 [Bitmap.Config.HARDWARE] 비트맵을 주는데, 이건 GPU 메모리에만
 * 있어서 소프트웨어 캔버스에 그리면 예외가 난다. PNG 압축만 할 때는 문제가 없어 이 변환이 필요 없었다.
 * [Bitmap.copy] 는 실패하면 원본을 되돌려주지 않고 `null` 을 주므로, 여기서도 그대로 `null` 을
 * 돌려준다. 원본(HARDWARE)을 그대로 반환하면 호출부가 소프트웨어 캔버스에 그리려다 죽는다.
 */
private fun Bitmap.toSoftwareBitmap(): Bitmap? =
    if (config == Bitmap.Config.HARDWARE) {
        copy(Bitmap.Config.ARGB_8888, false)
    } else {
        this
    }

/**
 * 공유 이미지를 캐시에 쓰고 FileProvider URI 를 준다.
 *
 * 인스타그램은 [Context.startActivity] 가 돌아온 뒤 편집기가 열릴 때 URI 를 읽는다. 그래서 이름을
 * 고정하면 다음 공유가 아직 읽히는 중인 파일을 덮어써 깨진 그림이 넘어갈 수 있다. 매번 새 파일을
 * 만들고, 다 읽혔을 만한 지난 파일만 지운다.
 */
private fun Bitmap.saveToShareCache(context: Context): Uri? {
    val dir = File(context.cacheDir, SHARE_CACHE_DIR)
    if (!dir.exists() && !dir.mkdirs()) {
        Log.i(TAG, "공유 캐시 디렉터리를 만들지 못했다: $dir")
        return null
    }
    dir.deleteFilesOlderThan(SHARE_FILE_TTL_MILLIS)

    var file: File? = null
    return try {
        file = File.createTempFile(SHARE_FILE_PREFIX, SHARE_FILE_SUFFIX, dir)
        val compressed = FileOutputStream(file).use { out ->
            compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
        }
        check(compressed) { "공유 이미지를 PNG로 압축하지 못했다" }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: IOException) {
        Log.i(TAG, "공유 이미지를 캐시에 쓰지 못했다", e)
        file?.delete()
        null
    } catch (e: IllegalArgumentException) {
        Log.i(TAG, "공유 이미지의 FileProvider URI를 만들지 못했다", e)
        file?.delete()
        null
    } catch (e: IllegalStateException) {
        Log.i(TAG, e.message.orEmpty())
        file?.delete()
        null
    } catch (e: SecurityException) {
        Log.i(TAG, "공유 이미지 캐시에 접근할 권한이 없다", e)
        file?.delete()
        null
    }
}

private fun File.deleteFilesOlderThan(ageMillis: Long) {
    val threshold = System.currentTimeMillis() - ageMillis
    listFiles()?.forEach { file ->
        if (file.lastModified() < threshold && !file.delete()) {
            Log.i(TAG, "지난 공유 이미지를 지우지 못했다: $file")
        }
    }
}
