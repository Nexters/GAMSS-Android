package com.gamss.android.core.designsystem.topnavigation

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.modifier.noRippleClickableIfNotNull
import com.gamss.android.core.designsystem.theme.GamssTheme

enum class GamssTopNavigationTitleAlignment {
    Start,
    Center,
}

/**
 * 내비게이션 아이콘 슬롯에 그릴 글리프입니다. 드로어블 리소스를 외부로 노출하지 않기 위해 열거형으로 둡니다.
 */
enum class GamssTopNavigationIcon(@DrawableRes internal val drawableRes: Int) {
    LeftChevron(R.drawable.ic_left_chevron),

    CreateCard(R.drawable.ic_create_card),
    Menu(R.drawable.ic_menu),

    CheckToken(R.drawable.ic_token_check),

    Search(R.drawable.ic_search)
}

/**
 * 오른쪽 끝에 놓이는 아이콘 하나를 나타냅니다. [GamssTopNavigation]의 `rightActions`에 순서대로
 * 담으면 오른쪽 끝부터 [TopNavigationRightActionSpacing] 간격으로 나열되고, 각각 자기 [onClick]으로
 * 클릭 이벤트를 받습니다.
 */
@Immutable
data class GamssTopNavigationIconAction(
    val icon: GamssTopNavigationIcon,
    val onClick: () -> Unit,
    val contentDescription: String? = null,
)

sealed interface GamssTopNavigationContent {
    /**
     * 로고도 제목도 두지 않는 형태입니다. 선택 모드처럼 좌우 아이콘만 남기는 화면에 사용합니다.
     * 빈 [Title]로 대신하면 접근성 트리에 빈 텍스트 노드가 남으므로 별도 형태로 둡니다.
     */
    data object None : GamssTopNavigationContent

    data object Logo : GamssTopNavigationContent

    data class Title(
        val text: String,
        val alignment: GamssTopNavigationTitleAlignment = GamssTopNavigationTitleAlignment.Start,
    ) : GamssTopNavigationContent
}

/**
 * GAMSS 화면 상단에서 사용하는 내비게이션입니다.
 *
 * 컴포넌트 위에는 디자인 가이드의 Safety area 간격 6dp가 포함됩니다. 실제 상태바 inset은
 * 이 컴포넌트가 소비하지 않으므로, 화면의 [androidx.compose.material3.Scaffold] 또는 상위 레이아웃에서
 * 처리해 주세요. 화면과 자연스럽게 이어지는 내비게이션은 [backgroundColor]에 화면 배경색이나
 * [Color.Transparent]를 전달해 사용할 수 있습니다.
 *
 * [content]로 로고, 왼쪽 정렬 제목, 중앙 정렬 날짜/제목 형태를 선택할 수 있습니다.
 * 오른쪽 끝에는 [rightActions]에 담은 아이콘들이 오른쪽부터 순서대로 놓입니다. 각 아이콘은
 * 서로 다른 글리프와 클릭 이벤트를 가질 수 있습니다.
 */
@Suppress("LongParameterList")
@Composable
fun GamssTopNavigation(
    content: GamssTopNavigationContent,
    modifier: Modifier = Modifier,
    backgroundColor: Color = if (GamssTheme.isDarkTheme) {
        GamssTheme.colors.black
    } else {
        GamssTheme.colors.white
    },
    showLeftIcon: Boolean = false,
    leftIconContentDescription: String? = null,
    onLeftIconClick: () -> Unit = {},
    rightActions: List<GamssTopNavigationIconAction> = emptyList(),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = GamssTheme.spacing.spacing075)
            .background(backgroundColor)
            .height(TopNavigationHeight),
    ) {
        TopNavigationIconSlot(
            modifier = Modifier
                .padding(start = TopNavigationContentHorizontalPadding)
                .align(Alignment.CenterStart),
            visible = showLeftIcon,
            icon = GamssTopNavigationIcon.LeftChevron,
            contentDescription = leftIconContentDescription,
            onClick = onLeftIconClick,
        )

        TopNavigationContent(
            modifier = when (content) {
                GamssTopNavigationContent.None -> Modifier.align(Alignment.CenterStart)

                GamssTopNavigationContent.Logo ->
                    Modifier
                        .padding(start = TopNavigationContentHorizontalPadding)
                        .align(Alignment.CenterStart)

                is GamssTopNavigationContent.Title ->
                    when (content.alignment) {
                        GamssTopNavigationTitleAlignment.Start ->
                            Modifier
                                .padding(
                                    start = if (showLeftIcon) {
                                        TopNavigationTitleStartWithIcon
                                    } else {
                                        TopNavigationContentHorizontalPadding
                                    },
                                    end = TopNavigationTitleEndPadding,
                                )
                                .align(Alignment.CenterStart)

                        GamssTopNavigationTitleAlignment.Center ->
                            Modifier.align(Alignment.Center)
                    }
            },
            content = content,
        )

        if (rightActions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(end = TopNavigationContentHorizontalPadding)
                    .align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(TopNavigationRightActionSpacing),
            ) {
                rightActions.forEach { action ->
                    TopNavigationIconSlot(
                        visible = true,
                        icon = action.icon,
                        contentDescription = action.contentDescription,
                        onClick = action.onClick,
                    )
                }
            }
        }
    }
}

/**
 * 문자열 제목을 사용하는 화면을 위한 간단한 API입니다.
 */
@Suppress("LongParameterList")
@Composable
fun GamssTopNavigation(
    title: String,
    modifier: Modifier = Modifier,
    titleAlignment: GamssTopNavigationTitleAlignment = GamssTopNavigationTitleAlignment.Start,
    // 다크 테마 시안 나오기 전까지 배경색 고정
    backgroundColor: Color = GamssTheme.colors.white,
    showLeftIcon: Boolean = false,
    leftIconContentDescription: String? = null,
    onLeftIconClick: () -> Unit = {},
    rightActions: List<GamssTopNavigationIconAction> = emptyList(),
) {
    GamssTopNavigation(
        content = GamssTopNavigationContent.Title(
            text = title,
            alignment = titleAlignment,
        ),
        modifier = modifier,
        backgroundColor = backgroundColor,
        showLeftIcon = showLeftIcon,
        leftIconContentDescription = leftIconContentDescription,
        onLeftIconClick = onLeftIconClick,
        rightActions = rightActions,
    )
}

@Composable
private fun TopNavigationContent(
    content: GamssTopNavigationContent,
    modifier: Modifier = Modifier,
) {
    when (content) {
        GamssTopNavigationContent.None -> Unit

        GamssTopNavigationContent.Logo -> Image(
            modifier = modifier.size(width = LogoWidth, height = LogoHeight),
            painter = painterResource(
                if (GamssTheme.isDarkTheme) {
                    R.drawable.ic_logo_dark
                } else {
                    R.drawable.ic_logo_light
                },
            ),
            contentDescription = null,
        )

        is GamssTopNavigationContent.Title -> Text(
            modifier = modifier,
            text = content.text,
            style = GamssTheme.typography.subtitle2,
            color = GamssTheme.colors.gray900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TopNavigationIconSlot(
    visible: Boolean,
    icon: GamssTopNavigationIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    if (visible) {
        Box(
            modifier = modifier
                .size(IconTouchTargetSize)
                .noRippleClickableIfNotNull(onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(IconSize),
                painter = painterResource(icon.drawableRes),
                contentDescription = contentDescription,
                tint = GamssTheme.colors.gray900,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssTopNavigationLightPreview() {
    GamssTheme(darkTheme = false) {
        TopNavigationPreviewContent()
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssTopNavigationDarkPreview() {
    GamssTheme(darkTheme = true) {
        TopNavigationPreviewContent()
    }
}

@Composable
private fun TopNavigationPreviewContent() {
    Column {
        GamssTopNavigation(
            content = GamssTopNavigationContent.Logo,
            rightActions = listOf(
                GamssTopNavigationIconAction(icon = GamssTopNavigationIcon.CreateCard, onClick = {}),
            ),
        )
        GamssTopNavigation(
            content = GamssTopNavigationContent.Logo,
            rightActions = listOf(
                GamssTopNavigationIconAction(icon = GamssTopNavigationIcon.Menu, onClick = {}),
            ),
        )
        GamssTopNavigation(
            title = "YY.MM.DD",
            titleAlignment = GamssTopNavigationTitleAlignment.Center,
            showLeftIcon = true,
            rightActions = listOf(
                GamssTopNavigationIconAction(icon = GamssTopNavigationIcon.CreateCard, onClick = {}),
                GamssTopNavigationIconAction(icon = GamssTopNavigationIcon.Menu, onClick = {}),
            ),
        )
        GamssTopNavigation(
            title = "Title",
            showLeftIcon = true,
            rightActions = listOf(
                GamssTopNavigationIconAction(icon = GamssTopNavigationIcon.CreateCard, onClick = {}),
            ),
        )
    }
}

/**
 * 오른쪽 아이콘 아래에 팝업/툴팁을 띄우는 화면이 있어 공개한다. [GamssTopNavigation] 자체 높이는
 * 이 값이지만, 컴포넌트 위에 얹히는 safety area 간격([GamssTheme.spacing.spacing075])까지 더해야
 * 화면에 실제로 차지하는 전체 높이가 나온다.
 */
val GamssTopNavigationHeight = 64.dp

/** 오른쪽 아이콘이 화면 끝에서 떨어진 여백. 그 아이콘에 맞춰 팝업 위치를 잡을 때 같이 쓴다. */
val GamssTopNavigationHorizontalPadding = 18.dp

private val TopNavigationHeight = GamssTopNavigationHeight
private val TopNavigationContentHorizontalPadding = GamssTopNavigationHorizontalPadding
private val TopNavigationTitleStartWithIcon = 60.dp
private val TopNavigationTitleEndPadding = 60.dp
private val TopNavigationRightActionSpacing = 20.dp
private val IconTouchTargetSize = 24.dp
private val IconSize = 24.dp
private val LogoWidth = 79.dp
private val LogoHeight = 26.dp
