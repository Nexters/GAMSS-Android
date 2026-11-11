package com.gamss.android.feature.chat

import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.PendingConversationReveal
import com.gamss.android.domain.conversation.SentMessage
import com.gamss.android.domain.push.MarkNotificationPermissionPromptedUseCase
import com.gamss.android.domain.push.ShouldPromptNotificationPermissionUseCase
import com.gamss.android.domain.push.SyncDeviceTokenUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.OrbitTestContext
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRoomNotificationPermissionTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 새_대화에_들어오면_새_대화임을_알린다() = runTest {
        chatRoomViewModel(pendingReveal = newConversationReveal()).test(this) {
            containerHost.start(ROOM_ID)
            assertEquals(ChatRoomSideEffect.NewConversationOpened, awaitNextSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 기존_대화를_열면_새_대화로_알리지_않는다() = runTest {
        chatRoomViewModel().test(this) {
            containerHost.start(ROOM_ID)
            skipItems(1) // isLoading = true
            awaitState()
            runCurrent()
            expectNoItems()

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 권한도_이력도_없으면_묻는다() = runTest {
        promptViewModel(FakeNotificationPermission(granted = false)).test(this) {
            containerHost.onNewConversationOpened(shouldShowRationale = false)
            assertEquals(true, awaitState())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 거절한_뒤_OS가_더_묻지_않으면_묻지_않는다() = runTest {
        promptViewModel(FakeNotificationPermission(granted = false, prompted = true)).test(this) {
            containerHost.onNewConversationOpened(shouldShowRationale = false)
            runCurrent()
            expectNoItems()

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 권한이_있으면_묻지_않는다() = runTest {
        promptViewModel(FakeNotificationPermission(granted = true)).test(this) {
            containerHost.onNewConversationOpened(shouldShowRationale = false)
            runCurrent()
            expectNoItems()

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 한_번_거절해_OS가_다시_물을_수_있으면_묻는다() = runTest {
        promptViewModel(FakeNotificationPermission(granted = false, prompted = true)).test(this) {
            containerHost.onNewConversationOpened(shouldShowRationale = true)
            assertEquals(true, awaitState())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 확인하면_권한을_요청하고_결과를_받으면_토큰을_동기화한다() = runTest {
        val permission = FakeNotificationPermission(granted = false)
        val deviceTokens = RecordingDeviceTokenRepository()
        promptViewModel(permission, deviceTokens).test(this) {
            containerHost.onNewConversationOpened(shouldShowRationale = false)
            assertEquals(true, awaitState())

            containerHost.onConfirm()
            assertEquals(false, awaitState())
            assertEquals(RequestNotificationPermission, awaitSideEffect())

            containerHost.onResult(granted = false, shouldShowRationale = true)
            runCurrent()
            assertEquals(1, deviceTokens.unregisterCount)
            assertEquals(0, deviceTokens.registerCount)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 처음_거절하면_거절_이력을_남긴다() = runTest {
        val permission = FakeNotificationPermission(granted = false)
        promptViewModel(permission).test(this) {
            containerHost.onResult(granted = false, shouldShowRationale = true)
            runCurrent()
            assertTrue(permission.prompted)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 시스템_팝업을_선택_없이_닫으면_이력을_남기지_않는다() = runTest {
        val permission = FakeNotificationPermission(granted = false)
        promptViewModel(permission).test(this) {
            containerHost.onResult(granted = false, shouldShowRationale = false)
            runCurrent()
            assertFalse(permission.prompted)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 바깥을_눌러_닫으면_이력을_남기지_않는다() = runTest {
        val permission = FakeNotificationPermission(granted = false)
        promptViewModel(permission).test(this) {
            containerHost.onNewConversationOpened(shouldShowRationale = false)
            assertEquals(true, awaitState())

            containerHost.onDismiss()
            assertEquals(false, awaitState())
            runCurrent()
            assertFalse(permission.prompted)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 닫힌_뒤에_들어온_확인은_무시한다() = runTest {
        val permission = FakeNotificationPermission(granted = false)
        promptViewModel(permission).test(this) {
            containerHost.onNewConversationOpened(shouldShowRationale = false)
            assertEquals(true, awaitState())
            containerHost.onDismiss()
            assertEquals(false, awaitState())

            containerHost.onConfirm()
            runCurrent()
            expectNoItems()

            cancelAndIgnoreRemainingItems()
        }
    }

    private fun promptViewModel(
        permission: FakeNotificationPermission,
        deviceTokens: RecordingDeviceTokenRepository = RecordingDeviceTokenRepository(),
    ) = NotificationPermissionPromptViewModel(
        shouldPromptNotificationPermission = ShouldPromptNotificationPermissionUseCase(
            notificationPermissionChecker = permission,
            promptHistory = permission,
        ),
        markNotificationPermissionPrompted = MarkNotificationPermissionPromptedUseCase(permission),
        syncDeviceToken = SyncDeviceTokenUseCase(
            pushTokenProvider = FixedPushTokenProvider,
            notificationPermissionChecker = permission,
            deviceTokenRepository = deviceTokens,
        ),
    )

    private suspend fun newConversationReveal() = PendingConversationReveal().apply {
        save(
            SentMessage(
                message = message(id = USER_ID, conversationId = ROOM_ID, sender = MessageSender.User, content = INPUT),
                commentStatus = CommentGenerationStatus.DONE,
                comments = emptyList(),
            ),
        )
    }

    private suspend fun OrbitTestContext<ChatRoomState, ChatRoomSideEffect, ChatRoomViewModel>.awaitNextSideEffect():
        ChatRoomSideEffect {
        while (true) {
            val item = awaitItem()
            if (item is Item.SideEffectItem) return item.value
        }
    }
}
