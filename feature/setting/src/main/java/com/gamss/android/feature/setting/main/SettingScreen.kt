package com.gamss.android.feature.setting.main

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.feature.setting.R
import com.gamss.android.feature.setting.component.SettingListItem
import com.gamss.android.feature.setting.component.SettingListItemBadge

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

    // 알림 허용 여부는 앱 밖(OS 설정)에서 바뀌므로, 화면이 다시 보일 때마다 읽어 배지를 갱신한다.
    var notificationsEnabled by remember(context) { mutableStateOf(context.notificationsEnabled()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = context.notificationsEnabled()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GamssTheme.colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        GamssTopNavigation(
            modifier = Modifier.padding(bottom = GamssTheme.spacing.spacing300),
            title = stringResource(R.string.setting_title),
            showLeftIcon = true,
            onLeftIconClick = onBackClick,
        )

        SettingListItem(
            title = stringResource(R.string.setting_list_user_account),
            onClick = onAccountInfoClick,
        )
        SettingDivider()

        SettingListItem(
            title = stringResource(R.string.setting_list_notification),
            onClick = { context.openNotificationSettings() },
            badge = if (notificationsEnabled) {
                SettingListItemBadge.On
            } else {
                SettingListItemBadge.Off
            },
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

private fun Context.notificationsEnabled(): Boolean =
    NotificationManagerCompat.from(this).areNotificationsEnabled()

/**
 * 앱 내에 알림 토글을 두지 않고 OS의 앱별 알림 설정으로 보낸다.
 */
private fun Context.openNotificationSettings() {
    try {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    } catch (_: ActivityNotFoundException) {
        // 앱별 알림 설정 화면이 없는 기기를 위해 앱 정보 화면으로 대체한다. 둘 다 없는 기기도 있어
        // 폴백 실패는 무시한다. 설정 진입 실패로 앱이 죽어서는 안 된다.
        runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
        }
    }
}

// 약관과 처리방침이 한 문서에 함께 실려 있다. 문서가 분리되면 true 로 되돌린다.
private const val SHOW_SERVICE_TERMS = false
