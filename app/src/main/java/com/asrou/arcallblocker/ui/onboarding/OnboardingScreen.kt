package com.asrou.arcallblocker.ui.onboarding

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asrou.arcallblocker.R
import com.asrou.arcallblocker.ui.theme.StatusGreen

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val context = LocalContext.current

    // Permission launchers
    var contactsGranted by remember { mutableStateOf(viewModel.hasContactsPermission()) }
    var screeningGranted by remember { mutableStateOf(viewModel.hasCallScreeningRole()) }
    var notificationsGranted by remember { mutableStateOf(viewModel.hasNotificationPermission()) }
    var callLogGranted by remember { mutableStateOf(viewModel.hasCallLogPermission()) }

    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        contactsGranted = granted
        if (granted) {
            viewModel.onContactsPermissionGranted()
        }
    }

    val screeningLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        screeningGranted = result.resultCode == Activity.RESULT_OK
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
    }

    val callLogLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        callLogGranted = permissions[Manifest.permission.READ_CALL_LOG] == true &&
            permissions[Manifest.permission.WRITE_CALL_LOG] == true
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / viewModel.totalSteps },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Step content
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (currentStep) {
                    0 -> WelcomeStep()
                    1 -> ContactsStep(
                        isGranted = contactsGranted,
                        onRequest = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }
                    )
                    2 -> ScreeningStep(
                        isGranted = screeningGranted,
                        onRequest = {
                            try {
                                val roleManager = context.getSystemService(RoleManager::class.java)
                                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                    screeningLauncher.launch(intent)
                                }
                            } catch (e: Throwable) {
                                // Ignore role request failure
                            }
                        }
                    )
                    3 -> NotificationStep(
                        isGranted = notificationsGranted,
                        onRequest = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                    4 -> CallLogStep(
                        isGranted = callLogGranted,
                        onRequest = {
                            callLogLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.WRITE_CALL_LOG
                                )
                            )
                        }
                    )
                    5 -> ReadyStep(
                        contactsGranted = contactsGranted,
                        screeningGranted = screeningGranted,
                        callLogGranted = callLogGranted
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 0) {
                    OutlinedButton(onClick = { viewModel.previousStep() }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (currentStep < viewModel.totalSteps - 1) {
                    Button(onClick = { viewModel.nextStep() }) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.completeOnboarding()
                            onComplete()
                        }
                    ) {
                        Text(stringResource(R.string.onboarding_get_started))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Icon(
        imageVector = Icons.Default.Shield,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_desc),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ContactsStep(isGranted: Boolean, onRequest: () -> Unit) {
    Icon(
        imageVector = Icons.Default.Contacts,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = if (isGranted) StatusGreen else MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_contacts_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_contacts_desc),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))
    if (isGranted) {
        GrantedBadge()
    } else {
        FilledTonalButton(onClick = onRequest) {
            Text(stringResource(R.string.onboarding_grant_permission))
        }
    }
}

@Composable
private fun ScreeningStep(isGranted: Boolean, onRequest: () -> Unit) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.PhoneCallback,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = if (isGranted) StatusGreen else MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_screening_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_screening_desc),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))
    if (isGranted) {
        GrantedBadge()
    } else {
        FilledTonalButton(onClick = onRequest) {
            Text(stringResource(R.string.onboarding_enable_screening))
        }
    }
}

@Composable
private fun NotificationStep(isGranted: Boolean, onRequest: () -> Unit) {
    Icon(
        imageVector = Icons.Default.Notifications,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = if (isGranted) StatusGreen else MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_notifications_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_notifications_desc),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))
    if (isGranted) {
        GrantedBadge()
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {}) {
                Text(stringResource(R.string.onboarding_skip))
            }
            FilledTonalButton(onClick = onRequest) {
                Text(stringResource(R.string.onboarding_grant_permission))
            }
        }
    }
}

@Composable
private fun CallLogStep(isGranted: Boolean, onRequest: () -> Unit) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.PhoneCallback,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = if (isGranted) StatusGreen else MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Call Log Access",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Allow AR Call Blocker to access and manage your call log so that blocked, silenced, and rejected calls leave no trace in your dialer.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))
    if (isGranted) {
        GrantedBadge()
    } else {
        FilledTonalButton(onClick = onRequest) {
            Text(stringResource(R.string.onboarding_grant_permission))
        }
    }
}

@Composable
private fun ReadyStep(contactsGranted: Boolean, screeningGranted: Boolean, callLogGranted: Boolean) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = StatusGreen
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_ready_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_ready_desc),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (!contactsGranted || !screeningGranted || !callLogGranted) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "⚠ Some permissions were not granted. The app may not work fully.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GrantedBadge() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Granted",
            tint = StatusGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.padding(4.dp))
        Text("Granted", color = StatusGreen, fontWeight = FontWeight.Medium)
    }
}
