package com.asrou.arcallblocker.ui.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asrou.arcallblocker.R
import com.asrou.arcallblocker.core.model.UnknownCallAction
import com.asrou.arcallblocker.ui.components.BlockingToggle
import com.asrou.arcallblocker.ui.components.StatusCard
import com.asrou.arcallblocker.ui.theme.StatusGreen
import com.asrou.arcallblocker.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBlockedCalls: () -> Unit,
    onNavigateToBlacklist: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val roleLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshScreeningRole()
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshScreeningRole()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Role warning banner if call screening role is not held
            if (!uiState.isScreeningRoleHeld) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.error_screening_role_lost),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Android requires AR Call Blocker to be set as the Call Screening App so that calls can be blocked, silenced, or rejected without missed call alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = {
                                try {
                                    val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
                                    if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                                        val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING)
                                        roleLauncher.launch(intent)
                                    }
                                } catch (e: Throwable) {
                                    com.asrou.arcallblocker.core.util.AppLogger.e("Failed to launch role request", e)
                                }
                            }
                        ) {
                            Text(stringResource(R.string.onboarding_enable_screening))
                        }
                    }
                }
            }

            // Main toggle
            BlockingToggle(
                isEnabled = uiState.isBlockingEnabled,
                onToggle = { viewModel.toggleBlocking(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Status cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    title = stringResource(R.string.contacts_allowed),
                    value = if (uiState.isBlockingEnabled) "Active" else "—",
                    icon = Icons.Default.CheckCircle,
                    iconTint = if (uiState.isBlockingEnabled) StatusGreen else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    title = stringResource(R.string.blocked_today),
                    value = uiState.blockedTodayCount.toString(),
                    icon = Icons.Default.Block,
                    iconTint = if (uiState.blockedTodayCount > 0) StatusRed else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToBlockedCalls
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unknown Call Action quick selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.unknown_call_action),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(
                            UnknownCallAction.BLOCK to stringResource(R.string.action_badge_blocked),
                            UnknownCallAction.SILENCE to stringResource(R.string.action_badge_silenced),
                            UnknownCallAction.REJECT to stringResource(R.string.action_badge_rejected)
                        )
                        options.forEachIndexed { index, (action, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                onClick = { viewModel.setUnknownCallAction(action) },
                                selected = uiState.unknownCallAction == action,
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick actions
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            QuickActionButton(
                text = stringResource(R.string.blocked_calls),
                icon = Icons.Outlined.History,
                onClick = onNavigateToBlockedCalls
            )
            Spacer(modifier = Modifier.height(8.dp))
            QuickActionButton(
                text = stringResource(R.string.allow_list),
                icon = Icons.Outlined.CheckCircle,
                onClick = onNavigateToWhitelist
            )
            Spacer(modifier = Modifier.height(8.dp))
            QuickActionButton(
                text = stringResource(R.string.block_list),
                icon = Icons.Outlined.Block,
                onClick = onNavigateToBlacklist
            )
            Spacer(modifier = Modifier.height(8.dp))
            QuickActionButton(
                text = stringResource(R.string.schedule),
                icon = Icons.Outlined.Schedule,
                onClick = onNavigateToSchedule
            )
            Spacer(modifier = Modifier.height(8.dp))
            QuickActionButton(
                text = stringResource(R.string.settings),
                icon = Icons.Outlined.Settings,
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(text, modifier = Modifier.weight(1f))
    }
}
