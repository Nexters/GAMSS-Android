package com.gamss.android.feature.setting.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appVersion = remember(context) { context.appVersion() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GamssTheme.colors.gray025)
            .verticalScroll(rememberScrollState()),
    ) {
        GamssTopNavigation(
            modifier = Modifier.padding(bottom = GamssTheme.spacing.spacing150),
            title = stringResource(R.string.setting_title),
            showLeftIcon = true,
            onLeftIconClick = onBackClick,
        )

        SettingListItem(
            title = stringResource(R.string.setting_list_user_account),
            onClick = onAccountInfoClick,
        )
        SettingDivider()

        Column(verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing300)) {
            if (SHOW_SERVICE_TERMS) {
                SettingListItem(
                    title = stringResource(R.string.setting_list_service_term),
                    onClick = onServiceTermsClick,
                )
            }
            SettingListItem(
                title = stringResource(R.string.setting_list_privacy_policy),
                onClick = onPrivacyPolicyClick,
            )
            SettingListItem(
                title = stringResource(R.string.setting_list_app_version),
                additionalInfo = appVersion,
            )
        }
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = GamssTheme.spacing.spacing200),
        thickness = GamssTheme.spacing.spacing025,
        color = GamssTheme.colors.gray075,
    )
}

private fun Context.appVersion(): String =
    packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

/**
 * 서비스 이용약관 항목 노출 여부.
 *
 * 현재는 약관과 개인정보 처리방침이 한 문서에 함께 실려 있어 목록에 두 번 나올 이유가 없다.
 * 문서가 분리되면 이 값을 `true` 로 되돌리면 된다. 화면 진입 경로와 웹뷰 연결은 그대로 살아 있다.
 */
private const val SHOW_SERVICE_TERMS = false
