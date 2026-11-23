package com.gamss.android.feature.chat

import androidx.compose.ui.text.input.TextFieldValue
import com.gamss.android.domain.analytics.AnalyticsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class ChattingListAnalyticsTest {

    private val analyticsTracker = RecordingAnalyticsTracker()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 카드를_눌러_대화를_열면_클릭_이벤트를_남긴다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L)))

        chattingListViewModel(repository, analyticsTracker).test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardClick(1L)
            runCurrent()

            assertEquals(
                listOf(AnalyticsEvent.CHAT_LIST_CARD_CLICK to emptyMap<String, Any>()),
                analyticsTracker.events,
            )
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 선택_모드에서_카드를_누르면_클릭_이벤트를_남기지_않는다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))

        chattingListViewModel(repository, analyticsTracker).test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            containerHost.onCardClick(2L)
            runCurrent()

            assertEquals(emptyList<Pair<String, Map<String, Any>>>(), analyticsTracker.events)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 검색을_실행하면_검색_이벤트를_남기고_규칙에_막힌_검색어는_남기지_않는다() = runTest {
        val viewModel = chattingListViewModel(FakeChattingListRepository(), analyticsTracker)
        backgroundScope.launch { viewModel.chattingRooms.collect {} }

        viewModel.test(this) {
            containerHost.onSearchKeywordChanged(TextFieldValue("a"))
            containerHost.search()
            runCurrent()
            assertEquals(emptyList<Pair<String, Map<String, Any>>>(), analyticsTracker.events)

            containerHost.onSearchKeywordChanged(TextFieldValue("ab"))
            containerHost.search()
            runCurrent()
            assertEquals(
                listOf(AnalyticsEvent.CHAT_SEARCH to emptyMap<String, Any>()),
                analyticsTracker.events,
            )

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 삭제를_확인하면_확인_이벤트를_남기고_삭제_뒤_재검색은_검색으로_세지_않는다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L)))
        val viewModel = chattingListViewModel(repository, analyticsTracker)
        backgroundScope.launch { viewModel.chattingRooms.collect {} }

        viewModel.test(this) {
            containerHost.load()
            containerHost.onSearchModeEnter()
            containerHost.onSearchKeywordChanged(TextFieldValue("ab"))
            containerHost.search()
            runCurrent()

            containerHost.onCardLongClick(1L)
            containerHost.onDeleteRequest()
            containerHost.onDeleteConfirm()
            runCurrent()

            assertEquals(
                listOf(AnalyticsEvent.CHAT_SEARCH, AnalyticsEvent.CHAT_DELETE_CONFIRM),
                analyticsTracker.events.map { it.first },
            )
            assertEquals(listOf("ab", "ab"), repository.requestedKeywords)
            cancelAndIgnoreRemainingItems()
        }
    }
}
