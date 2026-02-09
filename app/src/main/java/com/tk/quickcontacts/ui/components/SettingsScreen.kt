package com.tk.quickcontacts.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quickcontacts.ContactsViewModel
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.R


private val MessagingCardPadding = 18.dp
private val MessagingOptionSpacing = 12.dp
private val MessagingChipPaddingV = 12.dp
private val MessagingChipPaddingH = 12.dp
private val MessagingIconSize = 24.dp
private val MessagingBorderWidth = 1.dp

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
    val isSignalAvailable = availableMessagingApps.contains(MessagingApp.SIGNAL)

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
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                DefaultMessagingAppCard(
                    selectedApp = defaultMessagingApp,
                    onMessagingAppSelected = { app ->
                        val isInstalled = when (app) {
                            MessagingApp.SMS -> true
                            MessagingApp.WHATSAPP -> isWhatsAppAvailable
                            MessagingApp.TELEGRAM -> isTelegramAvailable
                            MessagingApp.SIGNAL -> isSignalAvailable
                        }
                        if (isInstalled) {
                            viewModel.setMessagingApp(app)
                        } else {
                            val appName = when (app) {
                                MessagingApp.WHATSAPP -> context.getString(R.string.messaging_app_whatsapp)
                                MessagingApp.TELEGRAM -> context.getString(R.string.messaging_app_telegram)
                                MessagingApp.SMS -> context.getString(R.string.messaging_app_sms)
                                MessagingApp.SIGNAL -> context.getString(R.string.messaging_app_signal)
                            }
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_messaging_app_not_installed, appName),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }

            item {
                SettingsGroupCard {
                    SettingToggleRow(
                        icon = Icons.Rounded.History,
                        title = "Recent Calls",
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
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SettingToggleRow(
                        icon = Icons.Rounded.Phone,
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
                SettingsFeedbackDevelopmentCard(
                    onRateApp = {
                        val packageName = context.packageName
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("market://details?id=$packageName")
                                setPackage("com.android.vending")
                            }
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    },
                    onSendFeedback = { sendFeedbackEmail() },
                    onOpenGitHub = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/teja2495/quick-contacts")))
                        } catch (_: Exception) {}
                    }
                )
            }

            item {
                SettingsVersionDisplay(
                    versionName = versionName,
                    modifier = Modifier.padding(top = 40.dp, bottom = 60.dp)
                )
            }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsFeedbackDevelopmentCard(
    onRateApp: () -> Unit,
    onSendFeedback: () -> Unit,
    onOpenGitHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column {
            listOf(
                Triple(stringResource(R.string.settings_feedback_rate_title), stringResource(R.string.settings_feedback_rate_desc), Icons.Rounded.Star to onRateApp),
                Triple(stringResource(R.string.settings_feedback_send_title), stringResource(R.string.settings_feedback_send_desc), Icons.Rounded.Email to onSendFeedback),
                Triple(stringResource(R.string.settings_feedback_github_title), stringResource(R.string.settings_feedback_github_desc), Icons.Rounded.Code to onOpenGitHub)
            ).forEachIndexed { index, (title, description, iconAndAction) ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = iconAndAction.second)
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = iconAndAction.first,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.cd_navigate_forward),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsVersionDisplay(
    versionName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayVersion = versionName.ifEmpty { "1.0" }
    val appName = stringResource(R.string.app_name)
    val developerName = stringResource(R.string.settings_feedback_developer_name)
    val developerDesc = stringResource(R.string.settings_feedback_developer_desc, developerName)
    val annotatedDeveloperDesc = buildAnnotatedString {
        val parts = developerDesc.split(developerName)
        if (parts.size > 1) {
            append(parts[0])
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(developerName)
            }
            append(parts[1])
        } else {
            append(developerDesc)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.settings_feedback_developer_title, appName, displayVersion),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = annotatedDeveloperDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://hihello.com/p/e11b6338-b4a5-49d8-93c8-03ac219de738")))
                } catch (_: Exception) {}
            }
        )
    }
}

@Composable
private fun DefaultMessagingAppCard(
    selectedApp: MessagingApp,
    onMessagingAppSelected: (MessagingApp) -> Unit
) {
    val options = listOf(
        Pair(MessagingApp.SMS, R.string.messaging_app_sms),
        Pair(MessagingApp.WHATSAPP, R.string.messaging_app_whatsapp),
        Pair(MessagingApp.TELEGRAM, R.string.messaging_app_telegram),
        Pair(MessagingApp.SIGNAL, R.string.messaging_app_signal)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
    Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MessagingCardPadding),
            verticalArrangement = Arrangement.spacedBy(MessagingOptionSpacing)
        ) {
            Text(
                text = stringResource(R.string.settings_messaging_card_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(MessagingOptionSpacing)
            ) {
                options.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MessagingOptionSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowOptions.forEach { (app, labelRes) ->
                            MessagingOptionChip(
                                app = app,
                                labelRes = labelRes,
                                selected = selectedApp == app,
                                onClick = { onMessagingAppSelected(app) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(2 - rowOptions.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagingOptionChip(
    app: MessagingApp,
    labelRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val borderColor = if (selected) colorScheme.primary else colorScheme.outlineVariant
    val backgroundColor = if (selected) colorScheme.primary.copy(alpha = 0.14f) else colorScheme.outlineVariant.copy(alpha = 0.35f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(MessagingBorderWidth, borderColor, RoundedCornerShape(16.dp))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = MessagingChipPaddingV, horizontal = MessagingChipPaddingH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MessagingOptionIcon(app = app)
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MessagingOptionIcon(app: MessagingApp) {
    val tint = MaterialTheme.colorScheme.primary
    when (app) {
        MessagingApp.SMS -> Icon(
            imageVector = Icons.Rounded.Sms,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(MessagingIconSize)
        )
        MessagingApp.WHATSAPP -> Icon(
            painter = painterResource(R.drawable.whatsapp_icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(MessagingIconSize)
        )
        MessagingApp.TELEGRAM -> Icon(
            painter = painterResource(R.drawable.telegram_icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(MessagingIconSize)
        )
        MessagingApp.SIGNAL -> Icon(
            painter = painterResource(R.drawable.signal_icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(MessagingIconSize)
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
    val colorScheme = MaterialTheme.colorScheme
    val textColor = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorScheme.primary,
                unselectedColor = colorScheme.onSurfaceVariant,
                disabledSelectedColor = colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                disabledUnselectedColor = colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
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
                tint = if (enabled) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
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
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) colorScheme.onSurfaceVariant else colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
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
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = Color.Transparent,
    disabledCheckedThumbColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
    disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    disabledUncheckedThumbColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
)
