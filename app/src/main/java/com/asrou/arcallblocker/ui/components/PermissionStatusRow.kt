package com.asrou.arcallblocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asrou.arcallblocker.ui.theme.StatusGreen
import com.asrou.arcallblocker.ui.theme.StatusOrange
import com.asrou.arcallblocker.ui.theme.StatusRed

/**
 * A row showing the status of a permission or system role.
 * Uses icon + text (not just color) for accessibility.
 */
@Composable
fun PermissionStatusRow(
    label: String,
    isGranted: Boolean,
    statusText: String,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null,
    actionText: String? = null,
    isWarning: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = when {
                    isGranted -> Icons.Default.CheckCircle
                    isWarning -> Icons.Default.Warning
                    else -> Icons.Default.Error
                },
                contentDescription = if (isGranted) "Active" else "Inactive",
                modifier = Modifier.size(20.dp),
                tint = when {
                    isGranted -> StatusGreen
                    isWarning -> StatusOrange
                    else -> StatusRed
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (onAction != null && actionText != null && !isGranted) {
            TextButton(onClick = onAction) {
                Text(actionText)
            }
        } else {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isGranted) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
