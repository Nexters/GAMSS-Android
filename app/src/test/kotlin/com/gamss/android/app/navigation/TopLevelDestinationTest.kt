package com.gamss.android.app.navigation

import com.gamss.android.feature.archive.navigation.ArchiveKey
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.game.navigation.GameKey
import com.gamss.android.feature.home.navigation.HomeKey
import com.gamss.android.feature.store.navigation.StoreKey
import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun 탭은_상점_보관함_홈_대화_오락_다섯_종이다() {
        assertEquals(listOf(StoreKey, ArchiveKey, HomeKey, ChatKey, GameKey), topLevelDestinations().map { it.key })
    }
}
