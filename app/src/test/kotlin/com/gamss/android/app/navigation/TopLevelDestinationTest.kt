package com.gamss.android.app.navigation

import com.gamss.android.feature.archive.navigation.ArchiveKey
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.home.navigation.HomeKey
import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun 탭은_보관함_홈_대화_세_종이다() {
        assertEquals(listOf(ArchiveKey, HomeKey, ChatKey), topLevelDestinations().map { it.key })
    }
}
