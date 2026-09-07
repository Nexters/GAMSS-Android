package com.gamss.android.core.ui.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "KakaoTalkShare"

private const val KAKAO_TALK_PACKAGE = "com.kakao.talk"
private const val PLAIN_TEXT_MIME_TYPE = "text/plain"

/**
 * [text] 를 카카오톡으로 보낸다. 카카오톡이 없으면 `false`.
 *
 * 카카오 SDK 대신 [Intent.ACTION_SEND] 를 카카오톡에 직접 지정한다. 보내는 것이 한 줄 문구와
 * 링크뿐이라 SDK·앱키·메시지 템플릿을 들일 이유가 없다. 링크 미리보기는 카카오톡이 링크의 OG
 * 태그를 읽어 알아서 만든다.
 *
 * 받는 사람을 고르는 화면은 카카오톡이 띄운다. 이 함수는 거기까지만 책임진다.
 */
fun Context.shareTextToKakaoTalk(text: String): Boolean {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = PLAIN_TEXT_MIME_TYPE
        setPackage(KAKAO_TALK_PACKAGE)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Log.i(TAG, "카카오톡 공유를 받을 액티비티가 없다", e)
        false
    } catch (e: SecurityException) {
        Log.i(TAG, "카카오톡 공유 액티비티를 실행할 권한이 없다", e)
        false
    }
}
