package com.asrou.arcallblocker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asrou.arcallblocker.R
import com.asrou.arcallblocker.ui.theme.BlockingActiveBg
import com.asrou.arcallblocker.ui.theme.BlockingActiveBgDark
import com.asrou.arcallblocker.ui.theme.BlockingActiveGreen
import com.asrou.arcallblocker.ui.theme.BlockingActiveGreenDark
import com.asrou.arcallblocker.ui.theme.BlockingInactiveBg
import com.asrou.arcallblocker.ui.theme.BlockingInactiveBgDark
import com.asrou.arcallblocker.ui.theme.BlockingInactiveGray
import com.asrou.arcallblocker.ui.theme.BlockingInactiveGrayDark
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * The main blocking toggle — the most important UI component.
 *
 * Large, prominent, with clear state communication via color, text, and icon.
 * Uses both visual and text cues (not color alone) for accessibility.
 */
@Composable
fun BlockingToggle(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isEnabled && isDark -> BlockingActiveBgDark
            isEnabled -> BlockingActiveBg
            isDark -> BlockingInactiveBgDark
            else -> BlockingInactiveBg
        },
        animationSpec = tween(300),
        label = "toggleBg"
    )

    val accentColor by animateColorAsState(
        targetValue = when {
            isEnabled && isDark -> BlockingActiveGreenDark
            isEnabled -> BlockingActiveGreen
            isDark -> BlockingInactiveGrayDark
            else -> BlockingInactiveGray
        },
        animationSpec = tween(300),
        label = "toggleAccent"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isEnabled) 1.1f else 1.0f,
        animationSpec = tween(300),
        label = "iconScale"
    )

    val stateText = if (isEnabled) {
        stringResource(R.string.blocking_on)
    } else {
        stringResource(R.string.blocking_off)
    }

    val descriptionText = if (isEnabled) {
        stringResource(R.string.blocking_on_description)
    } else {
        stringResource(R.string.blocking_off_description)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onToggle(!isEnabled) }
            .padding(24.dp)
            .semantics {
                role = Role.Switch
                contentDescription = "Call Blocking Toggle"
                stateDescription = stateText
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Shield icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .scale(iconScale),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = stateText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}
