package com.gamss.android.app.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamss.android.core.ui.share.StoryShareResult
import com.gamss.android.core.ui.share.shareBitmapToInstagramStory
import com.gamss.android.core.ui.share.shareTextToKakaoTalk
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardShareInstrumentedTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun 카카오톡_공유_인텐트에_패키지와_문구를_담는다() {
        val context = RecordingContext(applicationContext)

        val shared = context.shareTextToKakaoTalk("카드 문구\nhttps://gamss.kr/cards/42")

        val intent = requireNotNull(context.startedIntent)
        assertTrue(shared)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("com.kakao.talk", intent.`package`)
        assertEquals("카드 문구\nhttps://gamss.kr/cards/42", intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun 카카오톡_공유가_보안_예외로_막히면_false를_반환한다() {
        val context = ThrowingContext(applicationContext, SecurityException("test"))

        assertFalse(context.shareTextToKakaoTalk("공유 문구"))
    }

    @Test
    fun 카카오톡이_없으면_false를_반환한다() {
        val context = ThrowingContext(applicationContext, ActivityNotFoundException("test"))

        assertFalse(context.shareTextToKakaoTalk("공유 문구"))
    }

    @Test
    fun 인스타그램_공유_인텐트에_FileProvider_Uri와_읽기_권한을_담는다() = runBlocking {
        val context = RecordingContext(applicationContext)
        val bitmap = createBitmap(width = 20, height = 30)

        val result = try {
            context.shareBitmapToInstagramStory(bitmap)
        } finally {
            bitmap.recycle()
        }

        val intent = requireNotNull(context.startedIntent)
        assertEquals(StoryShareResult.Shared, result)
        assertEquals("com.instagram.share.ADD_TO_STORY", intent.action)
        assertEquals("com.instagram.android", intent.`package`)
        assertEquals("image/png", intent.type)
        assertEquals("content", intent.data?.scheme)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertNotNull(applicationContext.contentResolver.openInputStream(requireNotNull(intent.data)).use { it?.read() })
    }

    @Test
    fun 새_스토리_이미지를_만들기_전에_한시간_지난_캐시를_지운다() = runBlocking {
        val shareDir = File(applicationContext.cacheDir, "shared_images").apply { mkdirs() }
        val expired = File(shareDir, "story_share_expired.png").apply {
            writeBytes(byteArrayOf(1))
            setLastModified(System.currentTimeMillis() - 2 * 60 * 60 * 1_000L)
        }
        val context = RecordingContext(applicationContext)
        val bitmap: Bitmap = createBitmap(width = 20, height = 30)

        try {
            assertEquals(StoryShareResult.Shared, context.shareBitmapToInstagramStory(bitmap))
        } finally {
            bitmap.recycle()
        }

        assertFalse(expired.exists())
    }

    @Test
    fun 인스타그램이_없으면_InstagramUnavailable을_반환한다() = runBlocking {
        val context = ThrowingContext(applicationContext, ActivityNotFoundException("test"))
        val bitmap = createBitmap(width = 20, height = 30)

        val result = try {
            context.shareBitmapToInstagramStory(bitmap)
        } finally {
            bitmap.recycle()
        }

        assertEquals(StoryShareResult.InstagramUnavailable, result)
    }

    @Test
    fun 공유_캐시_디렉터리_자리에_파일이_있으면_ImageUnavailable을_반환한다() = runBlocking {
        // shared_images 자리를 디렉터리가 아닌 일반 파일로 선점해, 캐시 쓰기가 실제로
        // 실패하는 경로(IOException)를 재현한다.
        val shareDirPath = File(applicationContext.cacheDir, "shared_images")
        shareDirPath.deleteRecursively()
        check(shareDirPath.createNewFile()) { "테스트 전제 조건: $shareDirPath 를 파일로 선점하지 못했다" }
        val context = RecordingContext(applicationContext)
        val bitmap = createBitmap(width = 20, height = 30)

        val result = try {
            context.shareBitmapToInstagramStory(bitmap)
        } finally {
            bitmap.recycle()
            shareDirPath.delete()
        }

        assertEquals(StoryShareResult.ImageUnavailable, result)
        assertNull(context.startedIntent)
    }

    private class RecordingContext(base: Context) : ContextWrapper(base) {
        var startedIntent: Intent? = null

        override fun startActivity(intent: Intent) {
            startedIntent = intent
        }
    }

    /** [startActivity] 를 호출하면 [exception] 을 던진다. 공유 대상 앱이 없거나 권한이 없는 상황을 흉내낸다. */
    private class ThrowingContext(base: Context, private val exception: RuntimeException) : ContextWrapper(base) {
        override fun startActivity(intent: Intent) {
            throw exception
        }
    }
}
