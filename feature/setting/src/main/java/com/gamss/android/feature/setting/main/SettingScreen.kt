package com.gamss.android.feature.setting.main

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.feature.setting.R
import com.gamss.android.feature.setting.component.SettingListItem

@Composable
fun SettingScreen(
    onBackClick: () -> Unit,
    onAccountInfoClick: () -> Unit,
    onServiceTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    context: Context,
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        GamssTopNavigation(
            modifier = Modifier.padding(bottom = 4.dp),
            title = stringResource(R.string.setting_title),
            showLeftIcon = true,
            onLeftIconClick = onBackClick,
        )

        SettingListItem(
            title = stringResource(R.string.setting_list_user_account),
            onClick = onAccountInfoClick,
        )
        HorizontalDivider(thickness = 2.dp, color = GamssTheme.colors.gray075)
        SettingListItem(
            title = stringResource(R.string.setting_list_set_alarm),
            onClick = {},
        )
        HorizontalDivider(thickness = 2.dp, color = GamssTheme.colors.gray075)
        SettingListItem(
            title = stringResource(R.string.setting_list_service_term),
            onClick = onServiceTermsClick,
        )
        SettingListItem(
            title = stringResource(R.string.setting_list_privacy_policy),
            onClick = onPrivacyPolicyClick,
        )
        SettingListItem(
            title = stringResource(R.string.setting_list_app_version),
            additionalInfo = packageInfo.versionName
        )
    }
}
