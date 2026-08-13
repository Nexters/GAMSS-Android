package com.gamss.android.feature.chat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.safety.SupportAgency
import com.gamss.android.feature.chat.R

private val DialogShape = RoundedCornerShape(28.dp)
private val SupportPanelShape = RoundedCornerShape(20.dp)
private val ActionShape = RoundedCornerShape(12.dp)

@Composable
internal fun SupportAgencyDialog(
    agencies: List<SupportAgency>,
    onCallClick: (SupportAgency) -> Unit,
    onEmergencyCallClick: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GamssTheme.spacing.spacing300),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = DialogShape,
                color = GamssTheme.colors.gray025,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(GamssTheme.spacing.spacing500),
                    verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing500),
                ) {
                    DialogHeader()
                    SupportPanel(
                        agencies = agencies,
                        onCallClick = onCallClick,
                        onEmergencyCallClick = onEmergencyCallClick,
                    )
                    DialogActions(onDismiss = onDismiss, onConfirm = onConfirm)
                }
            }
        }
    }
}

@Composable
private fun DialogHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing200)) {
        Text(
            text = stringResource(R.string.chat_room_risk_dialog_dialog_title),
            style = GamssTheme.typography.title3,
            color = GamssTheme.colors.gray950,
        )
        Text(
            text = stringResource(R.string.chat_room_risk_dialog_dialog_subTitle),
            style = GamssTheme.typography.body3Regular,
            color = GamssTheme.colors.gray500,
        )
    }
}

@Composable
private fun SupportPanel(
    agencies: List<SupportAgency>,
    onCallClick: (SupportAgency) -> Unit,
    onEmergencyCallClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SupportPanelShape)
            .background(GamssTheme.colors.gray050)
            .padding(GamssTheme.spacing.spacing300),
        verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
    ) {
        agencies.forEach { agency ->
            SupportAgencyRow(agency = agency, onCallClick = onCallClick)
        }
        EmergencyCallButton(onClick = onEmergencyCallClick)
        AdditionalInfo()
    }
}

@Composable
private fun SupportAgencyRow(
    agency: SupportAgency,
    onCallClick: (SupportAgency) -> Unit,
) {
    val phoneNumber = agency.phoneNumber
    val callDescription = stringResource(
        R.string.safety_agency_call_description,
        agency.name,
        phoneNumber.orEmpty(),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(ActionShape)
            .border(width = 1.dp, color = GamssTheme.colors.gray300, shape = ActionShape)
            .clickable(enabled = phoneNumber != null) { onCallClick(agency) }
            .semantics {
                if (phoneNumber != null) {
                    contentDescription = callDescription
                    role = Role.Button
                }
            }
            .padding(horizontal = GamssTheme.spacing.spacing300),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildAnnotatedString {
                append(agency.name)
                phoneNumber?.let {
                    append(" (")
                    withStyle(SpanStyle(color = GamssTheme.colors.gray400)) { append(it) }
                    append(")")
                }
            },
            style = GamssTheme.typography.body4Medium,
            color = GamssTheme.colors.gray900,
            modifier = Modifier.weight(1f),
        )
        if (phoneNumber != null) {
            CallIcon(tint = GamssTheme.colors.gray700)
        }
    }
}

@Composable
private fun EmergencyCallButton(onClick: () -> Unit) {
    val callDescription = stringResource(R.string.safety_emergency_call_description)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(ActionShape)
            .background(GamssTheme.colors.red)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = callDescription
                role = Role.Button
            }
            .padding(horizontal = GamssTheme.spacing.spacing300),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.chat_room_risk_dialog_emergency_button_label),
            style = GamssTheme.typography.body3Medium,
            color = GamssTheme.colors.white,
        )
        CallIcon(tint = GamssTheme.colors.white)
    }
}

@Composable
private fun AdditionalInfo() {
    Row(
        modifier = Modifier.padding(
            start = GamssTheme.spacing.spacing050,
            top = GamssTheme.spacing.spacing100,
            bottom = GamssTheme.spacing.spacing050,
        ),
        horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = GamssTheme.colors.gray300,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.chat_room_risk_dialog_additional_info),
            style = GamssTheme.typography.body5Regular,
            color = GamssTheme.colors.gray500,
        )
    }
}

@Composable
private fun DialogActions(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
    ) {
        DialogActionButton(
            label = stringResource(R.string.chat_room_end_dialog_dismiss_button_label),
            containerColor = GamssTheme.colors.gray100,
            contentColor = GamssTheme.colors.gray600,
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
        )
        DialogActionButton(
            label = stringResource(R.string.chat_room_risk_dialog_confirm_button_label),
            containerColor = GamssTheme.colors.gray950,
            contentColor = GamssTheme.colors.gray025,
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DialogActionButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(ActionShape)
            .background(containerColor)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = GamssTheme.typography.subtitle3, color = contentColor)
    }
}

@Composable
private fun CallIcon(tint: Color) {
    Icon(
        painter = painterResource(R.drawable.ic_call),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(18.dp),
    )
}

@Preview(name = "Support agency dialog", showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun SupportAgencyDialogPreview() {
    GamssTheme(darkTheme = false) {
        SupportAgencyDialog(
            agencies = listOf(
                SupportAgency("109", "자살예방 상담", "24시간 무료 전문 상담", "109", 1),
                SupportAgency("mental", "정신건강 상담", "정신건강 위기 상담", "1577-0199", 2),
                SupportAgency("youth", "청소년 상담", "청소년 전문 상담", "1388", 3),
            ),
            onCallClick = {},
            onEmergencyCallClick = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
