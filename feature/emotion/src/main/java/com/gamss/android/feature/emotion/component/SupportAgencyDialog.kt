package com.gamss.android.feature.emotion.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.safety.RiskLevel
import com.gamss.android.domain.safety.SupportAgency
import com.gamss.android.feature.emotion.R

@Composable
internal fun SupportAgencyDialog(
    agencies: List<SupportAgency>,
    riskLevel: RiskLevel,
    onCallClick: (SupportAgency) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.safety_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing200),
            ) {
                Text(
                    text = bodyTextFor(riskLevel),
                    style = GamssTheme.typography.body4Regular,
                )
                agencies.forEach { agency ->
                    SupportAgencyRow(agency = agency, onCallClick = onCallClick)
                }
                Text(
                    text = stringResource(R.string.safety_dialog_emergency),
                    style = GamssTheme.typography.body5Medium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.safety_dialog_confirm))
            }
        },
    )
}

@Composable
private fun bodyTextFor(riskLevel: RiskLevel): String = stringResource(
    when (riskLevel) {
        RiskLevel.CRITICAL -> R.string.safety_dialog_body_blocked
        RiskLevel.WARNING, RiskLevel.NONE -> R.string.safety_dialog_body_notice
    },
)

@Composable
private fun SupportAgencyRow(
    agency: SupportAgency,
    onCallClick: (SupportAgency) -> Unit,
) {
    val callDescription = stringResource(R.string.safety_agency_call_description, agency.name)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = agency.name, style = GamssTheme.typography.subtitle4)
            Text(text = agency.description, style = GamssTheme.typography.body5Medium)
        }
        agency.phoneNumber?.let { phoneNumber ->
            TextButton(
                onClick = { onCallClick(agency) },
                modifier = Modifier.semantics { contentDescription = callDescription },
            ) {
                Text("$phoneNumber ${stringResource(R.string.safety_agency_call)}")
            }
        }
    }
}
