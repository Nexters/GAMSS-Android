package com.gamss.android.feature.setting.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.modifier.noRippleClickableIfNotNull
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.setting.R

@Suppress("LongParameterList")
@Composable
fun SettingListItem(
    title: String,
    contentColor: Color = GamssTheme.colors.gray950,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    badge: SettingListItemBadge? = null,
    additionalInfo: String? = null,
    contentPadding: PaddingValues = SettingListItemDefaults.ContentPadding,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .noRippleClickableIfNotNull(onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = GamssTheme.typography.body3Medium,
            color = contentColor,
        )

        if (badge != null) {
            BadgeChip(
                modifier = Modifier.padding(start = GamssTheme.spacing.spacing050),
                badge = badge,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (additionalInfo != null) {
            Text(
                text = additionalInfo,
                style = GamssTheme.typography.body3Medium,
                color = GamssTheme.colors.gray500,
            )
        }
    }
}

/**
 * 제목 바로 옆에 붙는 상태 배지입니다. 알림 설정의 ON/OFF 표시에 사용합니다.
 *
 * 라벨과 색을 한곳에 묶어 두어 두 상태가 서로 어긋나지 않게 합니다.
 */
enum class SettingListItemBadge(@StringRes internal val labelRes: Int) {
    On(R.string.setting_badge_on),
    Off(R.string.setting_badge_off),
}

@Composable
private fun BadgeChip(
    badge: SettingListItemBadge,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (badge) {
        SettingListItemBadge.On -> GamssTheme.colors.apricot
        SettingListItemBadge.Off -> GamssTheme.colors.gray200
    }
    val contentColor = when (badge) {
        SettingListItemBadge.On -> GamssTheme.colors.white
        SettingListItemBadge.Off -> GamssTheme.colors.gray900
    }

    Text(
        modifier = modifier
            .background(color = containerColor, shape = CircleShape)
            .padding(
                horizontal = GamssTheme.spacing.spacing075,
                vertical = GamssTheme.spacing.spacing025,
            ),
        text = stringResource(badge.labelRes),
        style = GamssTheme.typography.caption3,
        color = contentColor,
    )
}

object SettingListItemDefaults {
    val ContentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
}
