package com.gamss.android.feature.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.component.GamssText
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationContent
import com.gamss.android.feature.onboarding.component.NotificationPermissionDialog
import kotlinx.coroutines.launch

private val IllustrationMaxWidth = 340.dp
private val IllustrationMaxHeight = 260.dp
private val IllustrationVerticalOffset = (-40).dp
private val ScreenHorizontalPadding = 20.dp
private val MessageTopSpacing = 32.dp
private val IndicatorTopSpacing = 48.dp
private val IndicatorSize = 6.dp
private val IndicatorSpacing = 4.dp
private val ButtonTopSpacing = 28.dp
private val ButtonBottomSpacing = 24.dp

private enum class OnboardingPage(
    @param:DrawableRes val illustrationRes: Int,
    @param:StringRes val illustrationDescriptionRes: Int,
    @param:StringRes val messageRes: Int,
) {
    Write(
        illustrationRes = R.drawable.img_onboarding_step1,
        illustrationDescriptionRes = R.string.onboarding_step1_description,
        messageRes = R.string.onboarding_step1_message,
    ),
    Talk(
        illustrationRes = R.drawable.image_onboarding_step2,
        illustrationDescriptionRes = R.string.onboarding_step2_description,
        messageRes = R.string.onboarding_step2_message,
    ),
    Empty(
        illustrationRes = R.drawable.image_onboarding_step3,
        illustrationDescriptionRes = R.string.onboarding_step3_description,
        messageRes = R.string.onboarding_step3_message,
    ),
}

/**
 * GAMSS의 첫 사용 흐름입니다.
 *
 * 알림 런타임 권한 요청은 Activity가 소유하도록 [onNotificationPermissionRequest]로 위임하고,
 * 온보딩 종료 시점은 권한 선택과 무관하게 [onComplete]로 알립니다.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onNotificationPermissionRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingContent(
        onComplete = onComplete,
        onNotificationPermissionRequest = onNotificationPermissionRequest,
        modifier = modifier,
    )
}

@Composable
private fun OnboardingContent(
    onComplete: () -> Unit,
    onNotificationPermissionRequest: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val pages = OnboardingPage.entries
    val pagerState = rememberPagerState(initialPage = initialPage) { pages.size }
    val coroutineScope = rememberCoroutineScope()
    var showPermissionDialog by remember { mutableStateOf(false) }

    if (showPermissionDialog) {
        NotificationPermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onDisagree = {
                showPermissionDialog = false
                onComplete()
            },
            onAgree = {
                showPermissionDialog = false
                onNotificationPermissionRequest()
                onComplete()
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GamssTheme.colors.background)
            .safeDrawingPadding(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
        ) { index ->
            OnboardingPageContent(page = pages[index])
        }

        OnboardingTopBar(
            showBack = pagerState.currentPage > 0,
            showSkip = pagerState.currentPage < pages.lastIndex,
            onBackClick = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            },
            onSkipClick = onComplete,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            PageIndicator(
                currentPage = pagerState.currentPage,
                pageCount = pages.size,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = IndicatorTopSpacing),
            )

            OnboardingActionButton(
                isLastPage = pagerState.currentPage == pages.lastIndex,
                onNextClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                onStartClick = { showPermissionDialog = true },
            )
        }
    }
}

@Composable
private fun OnboardingActionButton(
    isLastPage: Boolean,
    onNextClick: () -> Unit,
    onStartClick: () -> Unit,
) {
    GamssButton(
        label = stringResource(if (isLastPage) R.string.onboarding_start else R.string.onboarding_next),
        onClick = if (isLastPage) onStartClick else onNextClick,
        modifier = Modifier
            .padding(
                start = ScreenHorizontalPadding,
                top = ButtonTopSpacing,
                end = ScreenHorizontalPadding,
                bottom = ButtonBottomSpacing,
            )
            .fillMaxWidth(),
        variant = if (isLastPage) GamssButtonVariant.Destructive else GamssButtonVariant.Primary,
    )
}

@Composable
private fun OnboardingTopBar(
    showBack: Boolean,
    showSkip: Boolean,
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        GamssTopNavigation(
            content = GamssTopNavigationContent.None,
            backgroundColor = GamssTheme.colors.background,
            showLeftIcon = showBack,
            leftIconContentDescription = stringResource(R.string.onboarding_back_description),
            onLeftIconClick = onBackClick,
        )
        if (showSkip) {
            GamssText(
                text = stringResource(R.string.onboarding_skip),
                style = GamssTheme.typography.body3Medium,
                color = GamssTheme.colors.gray900,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .clickable(onClick = onSkipClick)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Layout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ScreenHorizontalPadding),
        content = {
            Image(
                painter = painterResource(page.illustrationRes),
                contentDescription = stringResource(page.illustrationDescriptionRes),
                contentScale = ContentScale.Fit,
            )
            GamssText(
                text = stringResource(page.messageRes),
                style = GamssTheme.typography.body2Regular,
                color = GamssTheme.colors.gray900,
                textAlign = TextAlign.Center,
            )
        },
    ) { measurables, constraints ->
        val illustration = measurables[0].measure(
            constraints.copy(
                minWidth = 0,
                maxWidth = minOf(constraints.maxWidth, IllustrationMaxWidth.roundToPx()),
                minHeight = 0,
                maxHeight = minOf(constraints.maxHeight, IllustrationMaxHeight.roundToPx()),
            ),
        )
        val message = measurables[1].measure(
            constraints.copy(minWidth = 0, minHeight = 0),
        )
        val illustrationX = (constraints.maxWidth - illustration.width) / 2
        val centeredIllustrationY = (constraints.maxHeight - illustration.height) / 2
        val illustrationY = (centeredIllustrationY + IllustrationVerticalOffset.roundToPx()).coerceAtLeast(0)
        val messageX = (constraints.maxWidth - message.width) / 2
        val messageY = (illustrationY + illustration.height + MessageTopSpacing.roundToPx())
            .coerceAtMost(constraints.maxHeight - message.height)

        layout(constraints.maxWidth, constraints.maxHeight) {
            illustration.placeRelative(illustrationX, illustrationY)
            message.placeRelative(messageX, messageY)
        }
    }
}

@Composable
private fun PageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.onboarding_page_description, currentPage + 1, pageCount)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(IndicatorSpacing),
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(IndicatorSize)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            GamssTheme.colors.gray700
                        } else {
                            GamssTheme.colors.gray200
                        },
                    ),
            )
        }
    }
}

@Preview(name = "Onboarding - Step 1", showBackground = true, heightDp = 800)
@Suppress("UnusedPrivateMember")
@Composable
private fun OnboardingStep1Preview() {
    GamssTheme {
        OnboardingContent(
            onComplete = {},
            onNotificationPermissionRequest = {},
        )
    }
}

@Preview(name = "Onboarding - Step 3", showBackground = true, heightDp = 800)
@Suppress("UnusedPrivateMember")
@Composable
private fun OnboardingStep3Preview() {
    GamssTheme {
        OnboardingContent(
            onComplete = {},
            onNotificationPermissionRequest = {},
            initialPage = OnboardingPage.entries.lastIndex,
        )
    }
}
