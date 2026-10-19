package com.asrou.arcallblocker.ui.diagnostics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asrou.arcallblocker.R
import com.asrou.arcallblocker.data.cache.ContactCache
import com.asrou.arcallblocker.ui.components.PermissionStatusRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val result by viewModel.diagnosticResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostics)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // System Status
            Text(
                text = "System Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PermissionStatusRow(
                        label = stringResource(R.string.call_screening_role),
                        isGranted = result.hasCallScreeningRole,
                        statusText = if (result.hasCallScreeningRole)
                            stringResource(R.string.permission_active) else stringResource(R.string.permission_not_active)
                    )
                    PermissionStatusRow(
                        label = stringResource(R.string.contacts_permission),
                        isGranted = result.hasContactsPermission,
                        statusText = if (result.hasContactsPermission)
                            stringResource(R.string.permission_granted) else stringResource(R.string.permission_required)
                    )
                    PermissionStatusRow(
                        label = stringResource(R.string.notification_permission),
                        isGranted = result.hasNotificationPermission,
                        statusText = if (result.hasNotificationPermission)
                            stringResource(R.string.permission_enabled) else stringResource(R.string.permission_disabled),
                        isWarning = !result.hasNotificationPermission
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Contact Cache
            Text(
                text = stringResource(R.string.contact_cache_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DiagnosticRow(
                        label = "Status",
                        value = when (result.contactCacheState) {
                            ContactCache.State.READY -> "Ready"
                            ContactCache.State.SYNCING -> "Syncing…"
                            ContactCache.State.ERROR -> "Error"
                            ContactCache.State.UNINITIALIZED -> "Not initialized"
                        }
                    )
                    DiagnosticRow(
                        label = "Indexed Numbers",
                        value = result.contactCacheSize.toString()
                    )
                    if (result.lastSyncTimestamp > 0) {
                        val dateFormat = SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault())
                        DiagnosticRow(
                            label = stringResource(R.string.last_sync),
                            value = dateFormat.format(Date(result.lastSyncTimestamp))
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Device Info
            Text(
                text = stringResource(R.string.device_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DiagnosticRow(
                        label = stringResource(R.string.device_manufacturer),
                        value = result.deviceManufacturer
                    )
                    DiagnosticRow(
                        label = stringResource(R.string.android_version),
                        value = "${result.androidVersion} (API ${result.sdkVersion})"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.runDiagnostic() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.run_diagnostic))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
