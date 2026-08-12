package com.gamss.android.feature.setting.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.modifier.noRippleClickableIfNotNull
import com.gamss.android.core.designsystem.theme.GamssTheme

@Composable
fun SettingListItem(
    title: String,
    contentColor: Color = GamssTheme.colors.gray950,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
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

object SettingListItemDefaults {
    val ContentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
}
