package com.gamss.android.feature.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewUrlPolicyTest {

    private val allowedHosts = setOf("gamss.example.com")

    @Test
    fun `허용 호스트의 https 주소는 웹뷰가 연다`() {
        assertTrue(
            WebViewUrlPolicy.isInAppUrl("https://gamss.example.com/terms", allowedHosts),
        )
    }

    @Test
    fun `호스트 대소문자가 달라도 허용한다`() {
        assertTrue(
            WebViewUrlPolicy.isInAppUrl("https://GAMSS.Example.com/terms", allowedHosts),
        )
    }

    @Test
    fun `허용 목록에 없는 호스트는 외부로 넘긴다`() {
        assertFalse(
            WebViewUrlPolicy.isInAppUrl("https://attacker.example.com/terms", allowedHosts),
        )
    }

    @Test
    fun `허용 호스트를 접미사로만 흉내낸 주소는 외부로 넘긴다`() {
        assertFalse(
            WebViewUrlPolicy.isInAppUrl("https://evil-gamss.example.com.attacker.io", allowedHosts),
        )
    }

    @Test
    fun `허용 호스트를 userinfo 에 넣은 주소는 외부로 넘긴다`() {
        assertFalse(
            WebViewUrlPolicy.isInAppUrl("https://gamss.example.com@attacker.io/", allowedHosts),
        )
    }

    @Test
    fun `웹이 아닌 스킴은 호스트와 무관하게 웹뷰가 열지 않는다`() {
        assertFalse(WebViewUrlPolicy.isInAppUrl("mailto:help@gamss.example.com", allowedHosts))
        assertFalse(WebViewUrlPolicy.isInAppUrl("tel:0212345678", allowedHosts))
        assertFalse(WebViewUrlPolicy.isInAppUrl("intent://gamss.example.com#Intent;end", allowedHosts))
    }

    @Test
    fun `파싱할 수 없는 주소는 웹뷰가 열지 않는다`() {
        assertFalse(WebViewUrlPolicy.isInAppUrl("https://gamss.example.com/a b", allowedHosts))
        assertFalse(WebViewUrlPolicy.isInAppUrl("", allowedHosts))
    }

    @Test
    fun `등록된 모든 페이지 주소는 웹뷰가 열 수 있어야 한다`() {
        GamssWebPage.entries.forEach { page ->
            assertTrue(page.name, WebViewUrlPolicy.isInAppUrl(page.url))
        }
    }

    @Test
    fun `웹과 연락처 스킴만 외부 앱으로 넘긴다`() {
        assertTrue(WebViewUrlPolicy.isExternallyOpenable("https://attacker.example.com/x"))
        assertTrue(WebViewUrlPolicy.isExternallyOpenable("http://attacker.example.com/x"))
        assertTrue(WebViewUrlPolicy.isExternallyOpenable("mailto:help@gamss.example.com"))
        assertTrue(WebViewUrlPolicy.isExternallyOpenable("tel:0212345678"))
    }

    @Test
    fun `임의의 앱 딥링크는 외부 앱으로 넘기지 않는다`() {
        assertFalse(WebViewUrlPolicy.isExternallyOpenable("market://details?id=com.gamss.android"))
        assertFalse(WebViewUrlPolicy.isExternallyOpenable("sms:01012345678"))
        assertFalse(WebViewUrlPolicy.isExternallyOpenable("intent://evil#Intent;end"))
        assertFalse(WebViewUrlPolicy.isExternallyOpenable("javascript:alert(1)"))
    }
}
