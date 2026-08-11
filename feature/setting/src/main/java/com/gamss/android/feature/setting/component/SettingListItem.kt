package com.gamss.android.feature.setting.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.modifier.noRippleClickableIfNotNull
import com.gamss.android.core.designsystem.theme.GamssTheme

@Composable
fun SettingListItem(
    title: String,
    onClick: (() -> Unit)? = null,
    additionalInfo: String? = null,
) {
    Row(
        modifier = Modifier
            .noRippleClickableIfNotNull(onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = GamssTheme.typography.body3Medium,
            color = GamssTheme.colors.gray950,
        )
        Spacer(modifier = Modifier.weight(1f))
        if(additionalInfo != null) {
            Text(
                text = additionalInfo,
                style = GamssTheme.typography.body3Medium,
                color = GamssTheme.colors.gray400,
            )
        }
    }
}