package com.gamss.android.feature.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.component.GamssText
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.onboarding.R

private val DialogMaxWidth = 360.dp
private val DialogHorizontalMargin = 20.dp
private val PermissionPanelTopSpacing = 24.dp
private val PermissionIconSize = 24.dp
private val PermissionInfoIconSize = 20.dp

@Composable
internal fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onDisagree: () -> Unit,
    onAgree: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DialogHorizontalMargin),
            contentAlignment = Alignment.Center,
        ) {
            PermissionDialogContent(
                onDisagree = onDisagree,
                onAgree = onAgree,
            )
        }
    }
}

@Composable
private fun PermissionDialogContent(
    onDisagree: () -> Unit,
    onAgree: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = DialogMaxWidth)
            .fillMaxWidth()
            .background(
                color = GamssTheme.colors.gray025,
                shape = RoundedCornerShape(GamssTheme.radius.radius500),
            )
            .padding(GamssTheme.spacing.spacing500),
    ) {
        GamssText(
            text = stringResource(R.string.onboarding_permission_title),
            style = GamssTheme.typography.title3,
            color = GamssTheme.colors.gray950,
        )
        Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing200))
        GamssText(
            text = stringResource(R.string.onboarding_permission_subtitle),
            style = GamssTheme.typography.body3Medium,
            color = GamssTheme.colors.gray500,
        )
        Spacer(modifier = Modifier.height(PermissionPanelTopSpacing))
        PermissionPanel()
        Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing500))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
        ) {
            GamssButton(
                label = stringResource(R.string.onboarding_permission_disagree),
                onClick = onDisagree,
                modifier = Modifier.weight(1f),
                variant = GamssButtonVariant.Secondary,
            )
            GamssButton(
                label = stringResource(R.string.onboarding_permission_agree),
                onClick = onAgree,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PermissionPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = GamssTheme.colors.gray050,
                shape = RoundedCornerShape(GamssTheme.radius.radius300),
            )
            .padding(GamssTheme.spacing.spacing300),
        verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing300),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = GamssTheme.colors.gray300,
                    shape = RoundedCornerShape(GamssTheme.radius.radius200),
                )
                .padding(GamssTheme.spacing.spacing300),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing200),
        ) {
            Icon(
                painter = painterResource(GamssIcons.NotificationAlert),
                contentDescription = stringResource(R.string.onboarding_notification_permission_description),
                tint = GamssTheme.colors.red,
                modifier = Modifier.size(PermissionIconSize),
            )
            GamssText(
                text = stringResource(R.string.onboarding_notification_permission_name),
                style = GamssTheme.typography.body4Medium,
                color = GamssTheme.colors.gray900,
            )
            GamssText(
                text = stringResource(R.string.onboarding_notification_permission_reason),
                style = GamssTheme.typography.body4Medium,
                color = GamssTheme.colors.gray500,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
        ) {
            Icon(
                painter = painterResource(GamssIcons.InfoOutline),
                contentDescription = null,
                tint = GamssTheme.colors.gray300,
                modifier = Modifier.size(PermissionInfoIconSize),
            )
            GamssText(
                text = stringResource(R.string.onboarding_permission_optional),
                style = GamssTheme.typography.caption2,
                color = GamssTheme.colors.gray400,
            )
        }
    }
}

@Preview(name = "Notification permission", showBackground = true, widthDp = 402, heightDp = 874)
@Suppress("UnusedPrivateMember")
@Composable
private fun NotificationPermissionDialogPreview() {
    GamssTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GamssTheme.colors.gray700)
                .padding(DialogHorizontalMargin),
            contentAlignment = Alignment.Center,
        ) {
            PermissionDialogContent(onDisagree = {}, onAgree = {})
        }
    }
}
