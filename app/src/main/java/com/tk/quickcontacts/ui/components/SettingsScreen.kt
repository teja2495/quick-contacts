package com.tk.quickcontacts.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quickcontacts.ContactsViewModel
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.R

private val SettingsCardBackground = Color(0xFF181724)
private val SettingsAccent = Color(0xFFD0BCFF)
private val SettingsTitleColor = Color(0xFFF2F1F6)
private val SettingsBodyColor = Color(0xFFB6B4C4)
private val SettingsDivider = Color(0xFF343246)

@Composable
fun SettingsScreen(
    viewModel: ContactsViewModel,
    onBackClick: () -> Unit,
    hasCallLogPermission: Boolean = true,
    onRequestCallLogPermission: (() -> Unit)? = null,
    isCallLogPermissionPermanentlyDenied: Boolean = false,
    hasCallPermission: Boolean = true,
    onRequestCallPermission: (() -> Unit)? = null,
    isCallPermissionPermanentlyDenied: Boolean = false,
    onOpenAppSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isRecentCallsVisible by viewModel.isRecentCallsVisible.collectAsState()
    val isDirectDialEnabled by viewModel.isDirectDialEnabled.collectAsState()
    val defaultMessagingApp by viewModel.defaultMessagingApp.collectAsState()
    val availableMessagingApps by viewModel.availableMessagingApps.collectAsState()

    val versionName = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var showCallPermissionSettingsDialog by remember { mutableStateOf(false) }

    // Refresh available messaging apps when settings screen is opened
    LaunchedEffect(Unit) {
        viewModel.refreshAvailableMessagingApps()
    }

    val isWhatsAppAvailable = availableMessagingApps.contains(MessagingApp.WHATSAPP)
    val isTelegramAvailable = availableMessagingApps.contains(MessagingApp.TELEGRAM)

    fun sendFeedbackEmail() {
        val appVersion = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }

        val androidVersion = android.os.Build.VERSION.RELEASE
        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val emailBody = """


            ---
            App Version: $appVersion
            Android Version: $androidVersion
            Device: $deviceModel
        """.trimIndent()

        val subject = context.getString(R.string.feedback_subject)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(
                "mailto:tejakarlapudi.apps@gmail.com?subject=${Uri.encode(subject)}&body=${Uri.encode(emailBody)}"
            )
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // No compatible email app on device.
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsGroupCard {
                    Text(
                        text = "Default Messaging App",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = SettingsTitleColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_messaging_app_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = SettingsBodyColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MessagingAppOptionRow(
                        label = stringResource(R.string.messaging_app_sms),
                        subtitle = null,
                        iconRes = R.drawable.sms_icon,
                        iconSize = 18.dp,
                        selected = defaultMessagingApp == MessagingApp.SMS,
                        enabled = true,
                        onClick = { viewModel.setMessagingApp(MessagingApp.SMS) }
                    )
                    MessagingAppOptionRow(
                        label = stringResource(R.string.messaging_app_whatsapp),
                        subtitle = if (isWhatsAppAvailable) null else stringResource(R.string.settings_not_installed),
                        iconRes = R.drawable.whatsapp_icon,
                        iconSize = 22.dp,
                        selected = defaultMessagingApp == MessagingApp.WHATSAPP,
                        enabled = isWhatsAppAvailable,
                        onClick = {
                            if (isWhatsAppAvailable) {
                                viewModel.setMessagingApp(MessagingApp.WHATSAPP)
                            }
                        }
                    )
                    MessagingAppOptionRow(
                        label = stringResource(R.string.messaging_app_telegram),
                        subtitle = if (isTelegramAvailable) null else stringResource(R.string.settings_not_installed),
                        iconRes = R.drawable.telegram_icon,
                        iconSize = 18.dp,
                        selected = defaultMessagingApp == MessagingApp.TELEGRAM,
                        enabled = isTelegramAvailable,
                        onClick = {
                            if (isTelegramAvailable) {
                                viewModel.setMessagingApp(MessagingApp.TELEGRAM)
                            }
                        }
                    )
                }
            }

            item {
                SettingsGroupCard {
                    SettingToggleRow(
                        title = "Show Recent Calls",
                        description = if (hasCallLogPermission) {
                            stringResource(R.string.recent_calls_description)
                        } else {
                            "Requires call history permission to display recent calls."
                        },
                        checked = isRecentCallsVisible && hasCallLogPermission,
                        enabled = true,
                        onCheckedChange = { newValue ->
                            if (newValue && !hasCallLogPermission) {
                                if (isCallLogPermissionPermanentlyDenied) {
                                    showPermissionSettingsDialog = true
                                } else {
                                    onRequestCallLogPermission?.invoke()
                                }
                            } else {
                                viewModel.toggleRecentCallsVisibility()
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = SettingsDivider
                    )
                    SettingToggleRow(
                        title = "Direct Dial",
                        description = if (hasCallPermission) {
                            "Calls are made immediately without opening the dialer."
                        } else {
                            "Requires phone permission to make calls directly."
                        },
                        checked = isDirectDialEnabled && hasCallPermission,
                        enabled = true,
                        onCheckedChange = { newValue ->
                            if (newValue && !hasCallPermission) {
                                if (isCallPermissionPermanentlyDenied) {
                                    showCallPermissionSettingsDialog = true
                                } else {
                                    onRequestCallPermission?.invoke()
                                }
                            } else {
                                viewModel.toggleDirectDial()
                            }
                        }
                    )
                }
            }

            item {
                SettingsGroupCard {
                    Text(
                        text = "Support",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SettingsTitleColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { sendFeedbackEmail() }
                            .background(SettingsAccent.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Send Feedback & Bug Reports",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = SettingsTitleColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelLarge,
                            color = SettingsAccent
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = SettingsBodyColor
            )
        }
    }

    // Permission Settings Dialog (for Call History)
    if (showPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_permission_required_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_permission_required_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionSettingsDialog = false
                        onOpenAppSettings?.invoke()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.dialog_open_settings),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionSettingsDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
    
    // Call Permission Settings Dialog (for Direct Dial)
    if (showCallPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showCallPermissionSettingsDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_permission_required_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    text = "To enable direct dial, you need to grant Phone permission in your device settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCallPermissionSettingsDialog = false
                        onOpenAppSettings?.invoke()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.dialog_open_settings),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCallPermissionSettingsDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsCardBackground)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            content = content
        )
    }
}

@Composable
private fun MessagingAppOptionRow(
    label: String,
    subtitle: String?,
    iconRes: Int,
    iconSize: Dp,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (enabled) SettingsTitleColor else SettingsBodyColor.copy(alpha = 0.65f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) SettingsAccent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = SettingsAccent,
                unselectedColor = SettingsBodyColor,
                disabledSelectedColor = SettingsBodyColor.copy(alpha = 0.45f),
                disabledUnselectedColor = SettingsBodyColor.copy(alpha = 0.45f)
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (enabled) SettingsAccent else SettingsBodyColor.copy(alpha = 0.55f),
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsBodyColor
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) SettingsTitleColor else SettingsBodyColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SettingsBodyColor
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = settingsSwitchColors()
        )
    }
}

@Composable
private fun settingsSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color(0xFF35205A),
    checkedTrackColor = SettingsAccent,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Color(0xFFC1BFCC),
    uncheckedTrackColor = Color(0xFF5C596B),
    uncheckedBorderColor = Color.Transparent,
    disabledCheckedThumbColor = Color(0xFF514A63),
    disabledCheckedTrackColor = Color(0xFF696277),
    disabledUncheckedThumbColor = Color(0xFF857F92),
    disabledUncheckedTrackColor = Color(0xFF5A5666)
)
