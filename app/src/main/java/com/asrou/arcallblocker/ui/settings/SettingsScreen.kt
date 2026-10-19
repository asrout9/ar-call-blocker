package com.asrou.arcallblocker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asrou.arcallblocker.BuildConfig
import com.asrou.arcallblocker.R
import com.asrou.arcallblocker.core.model.NotificationMode
import com.asrou.arcallblocker.core.model.PrivateNumberPolicy
import com.asrou.arcallblocker.core.model.UnknownCallAction
import com.asrou.arcallblocker.ui.components.ConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.clear_local_data),
            message = "This will delete all blocked call history, allow list, and block list entries. This cannot be undone.",
            onDismiss = { showClearDialog = false },
            onConfirm = {
                viewModel.clearAllData()
                showClearDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
            // ── Protection ──
            SectionHeader(stringResource(R.string.protection))

            SettingsSwitch(
                title = stringResource(R.string.contacts_only_mode),
                description = stringResource(R.string.contacts_only_mode_desc),
                checked = uiState.isBlockingEnabled,
                onCheckedChange = { viewModel.setBlockingEnabled(it) }
            )

            SettingsRadioGroup(
                title = stringResource(R.string.private_numbers),
                description = stringResource(R.string.private_numbers_desc),
                options = listOf(
                    "Block" to PrivateNumberPolicy.BLOCK,
                    "Allow" to PrivateNumberPolicy.ALLOW,
                    "Follow contacts-only policy" to PrivateNumberPolicy.FOLLOW_CONTACTS_ONLY
                ),
                selected = uiState.privateNumberPolicy,
                onSelected = { viewModel.setPrivateNumberPolicy(it) }
            )

            SettingsRadioGroup(
                title = stringResource(R.string.unknown_call_action),
                description = stringResource(R.string.unknown_call_action_desc),
                options = listOf(
                    stringResource(R.string.action_block) to UnknownCallAction.BLOCK,
                    stringResource(R.string.action_silence) to UnknownCallAction.SILENCE,
                    stringResource(R.string.action_reject) to UnknownCallAction.REJECT
                ),
                selected = uiState.unknownCallAction,
                onSelected = { viewModel.setUnknownCallAction(it) }
            )

            SectionDivider()

            // ── Notifications ──
            SectionHeader(stringResource(R.string.notification_settings))

            SettingsRadioGroup(
                title = "Blocked call notifications",
                description = null,
                options = listOf(
                    stringResource(R.string.notify_every_call) to NotificationMode.EVERY_CALL,
                    stringResource(R.string.notify_daily_summary) to NotificationMode.DAILY_SUMMARY,
                    stringResource(R.string.notify_none) to NotificationMode.NONE
                ),
                selected = uiState.notificationMode,
                onSelected = { viewModel.setNotificationMode(it) }
            )

            SectionDivider()

            // ── Security & App Lock ──
            SectionHeader(stringResource(R.string.security_section))

            val isCapabilityAvailable = uiState.authCapability == com.asrou.arcallblocker.security.BiometricAuthManager.AuthCapability.AVAILABLE

            SettingsSwitch(
                title = stringResource(R.string.app_lock),
                description = if (isCapabilityAvailable) stringResource(R.string.app_lock_desc) else stringResource(R.string.app_lock_unavailable),
                checked = uiState.isAppLockEnabled && isCapabilityAvailable,
                onCheckedChange = { if (isCapabilityAvailable) viewModel.setAppLockEnabled(it) }
            )

            if (uiState.isAppLockEnabled && isCapabilityAvailable) {
                SettingsRadioGroup(
                    title = stringResource(R.string.lock_timeout),
                    description = null,
                    options = listOf(
                        stringResource(R.string.lock_timeout_immediately) to 0,
                        stringResource(R.string.lock_timeout_30s) to 30,
                        stringResource(R.string.lock_timeout_1m) to 60,
                        stringResource(R.string.lock_timeout_5m) to 300
                    ),
                    selected = uiState.lockTimeoutSeconds,
                    onSelected = { viewModel.setLockTimeout(it) }
                )
            }

            SectionDivider()

            // ── Privacy ──
            SectionHeader(stringResource(R.string.privacy))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = stringResource(R.string.privacy_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsClickable(
                title = stringResource(R.string.clear_local_data),
                description = stringResource(R.string.clear_local_data_desc),
                onClick = { showClearDialog = true }
            )

            SectionDivider()

            // ── About ──
            SectionHeader(stringResource(R.string.about))

            SettingsClickable(
                title = stringResource(R.string.app_version),
                description = BuildConfig.VERSION_NAME,
                onClick = { }
            )

            SettingsClickable(
                title = stringResource(R.string.diagnostics),
                description = "View system status and troubleshoot issues",
                onClick = onNavigateToDiagnostics
            )

            // Carrier behavior note
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.carrier_behavior_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Dual-SIM note
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.dual_sim_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> SettingsRadioGroup(
    title: String,
    description: String?,
    options: List<Pair<String, T>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        options.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(value) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == value, onClick = { onSelected(value) })
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsClickable(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
