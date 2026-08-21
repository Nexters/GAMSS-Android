package com.gamss.android.app.share

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        val context = SecurityFailureContext(applicationContext)

        assertFalse(context.shareTextToKakaoTalk("공유 문구"))
    }

    @Test
    fun 인스타그램_공유_인텐트에_FileProvider_Uri와_읽기_권한을_담는다() {
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
    fun 새_스토리_이미지를_만들기_전에_한시간_지난_캐시를_지운다() {
        val shareDir = File(applicationContext.cacheDir, "shared_images").apply { mkdirs() }
        val expired = File(shareDir, "expired.png").apply {
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

    private class RecordingContext(base: Context) : ContextWrapper(base) {
        var startedIntent: Intent? = null

        override fun startActivity(intent: Intent) {
            startedIntent = intent
        }
    }

    private class SecurityFailureContext(base: Context) : ContextWrapper(base) {
        override fun startActivity(intent: Intent) {
            throw SecurityException("test")
        }
    }
}
