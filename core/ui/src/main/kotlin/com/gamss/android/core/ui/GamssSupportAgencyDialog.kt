package com.gamss.android.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gamss.android.domain.safety.SupportAgency

@Composable
fun GamssSupportAgencyDialog(
    agencies: List<SupportAgency>,
    isBlocking: Boolean,
    onCallClick: (SupportAgency) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.safety_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(
                        if (isBlocking) {
                            R.string.safety_dialog_body_blocked
                        } else {
                            R.string.safety_dialog_body_notice
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                agencies.forEach { agency ->
                    SupportAgencyRow(agency = agency, onCallClick = onCallClick)
                }
                Text(
                    text = stringResource(R.string.safety_dialog_emergency),
                    style = MaterialTheme.typography.bodySmall,
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
            Text(text = agency.name, style = MaterialTheme.typography.titleSmall)
            Text(text = agency.description, style = MaterialTheme.typography.bodySmall)
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
