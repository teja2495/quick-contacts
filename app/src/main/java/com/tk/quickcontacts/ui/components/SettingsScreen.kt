package com.tk.quickcontacts.ui.components

import com.tk.quickcontacts.ui.theme.SegmentedList
import com.tk.quickcontacts.ui.theme.ExpressiveMotion
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter

import android.content.Intent
import android.content.ComponentName
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sms
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
import com.tk.quickcontacts.BuildConfig
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.R
import com.tk.quickcontacts.services.WhatsAppCallNotificationListener


private val MessagingCardPadding = 18.dp
private val MessagingOptionSpacing = 12.dp
private val MessagingChipPaddingV = 12.dp
private val MessagingChipPaddingH = 12.dp
private val MessagingIconSize = 24.dp
private val MessagingBorderWidth = 1.dp

private fun hasWhatsAppNotificationAccess(context: android.content.Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ).orEmpty()
    return enabledListeners.split(':')
        .mapNotNull(ComponentName::unflattenFromString)
        .any { component ->
            component.packageName == context.packageName &&
                component.className == WhatsAppCallNotificationListener::class.java.name
        }
}

@Composable
fun SettingsScreen(
    viewModel: ContactsViewModel,
    onBackClick: () -> Unit,
    onOpenPermissions: () -> Unit,
    hasCallLogPermission: Boolean = true,
    onRequestCallLogPermission: (() -> Unit)? = null,
    isCallLogPermissionPermanentlyDenied: Boolean = false,
    hasCallPermission: Boolean = true,
    onRequestCallPermission: (() -> Unit)? = null,
    isCallPermissionPermanentlyDenied: Boolean = false,
    onOpenAppSettings: (() -> Unit)? = null,
    onRequestWhatsAppNotificationAccess: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isRecentCallsVisible by viewModel.isRecentCallsVisible.collectAsState()
    val isWhatsAppRecentCallsEnabled by viewModel.isWhatsAppRecentCallsEnabled.collectAsState()
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
    var showWhatsAppNotificationAccessDialog by remember { mutableStateOf(false) }

    if (showWhatsAppNotificationAccessDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppNotificationAccessDialog = false },
            title = { Text("Allow notification access?") },
            text = { Text("Quick Contacts needs notification access to add WhatsApp calls to Recent Calls. It reads only WhatsApp call notifications.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setWhatsAppRecentCallsEnabled(true)
                    onRequestWhatsAppNotificationAccess?.invoke()
                    showWhatsAppNotificationAccessDialog = false
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppNotificationAccessDialog = false }) { Text("Cancel") }
            }
        )
    }

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
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!BuildConfig.DISABLE_RECENT_CALLS) item {
                SettingsSection(title = stringResource(R.string.settings_messaging_card_title)) {
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
            }

            item {
                val rows = buildList<@Composable (Shape) -> Unit> {
                    add { shape ->
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
                            shape = shape,
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
                    }
                    if (isRecentCallsVisible && BuildConfig.ENABLE_WHATSAPP_RECENT_CALLS) add { shape ->
                        SettingToggleRow(
                            painter = painterResource(R.drawable.whatsapp_icon),
                            title = "WhatsApp Calls",
                            description = "Add WhatsApp calls to Recent Calls.",
                            checked = isWhatsAppRecentCallsEnabled,
                            enabled = true,
                            shape = shape,
                            onCheckedChange = { isEnabled ->
                                if (isEnabled) {
                                    if (hasWhatsAppNotificationAccess(context)) {
                                        viewModel.setWhatsAppRecentCallsEnabled(true)
                                    } else {
                                        showWhatsAppNotificationAccessDialog = true
                                    }
                                } else {
                                    viewModel.setWhatsAppRecentCallsEnabled(false)
                                }
                            }
                        )
                    }
                    add { shape ->
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
                            shape = shape,
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
                    add { shape ->
                        SettingClickRow(
                            icon = Icons.Rounded.Refresh,
                            title = stringResource(R.string.settings_refresh_contacts),
                            description = stringResource(R.string.settings_refresh_contacts_description),
                            shape = shape,
                            onClick = {
                                viewModel.refreshContacts(context)
                                Toast.makeText(context, context.getString(R.string.settings_refresh_contacts_toast), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                SettingsSection(title = "General") {
                    rows.forEachIndexed { index, row -> row(SegmentedList.shape(index, rows.size)) }
                }
            }

            item {
                val links = listOf(
                    Triple(stringResource(R.string.settings_permissions_title), stringResource(R.string.settings_permissions_desc), Icons.Rounded.AdminPanelSettings to onOpenPermissions),
                    Triple(stringResource(R.string.settings_feedback_send_title), stringResource(R.string.settings_feedback_send_desc), Icons.Rounded.Email to { sendFeedbackEmail() }),
                    Triple(stringResource(R.string.settings_feedback_github_title), stringResource(R.string.settings_feedback_github_desc), Icons.Rounded.Code to {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/teja2495/quick-contacts")))
                        } catch (_: Exception) {}
                    })
                )
                SettingsSection(title = "Support") {
                    links.forEachIndexed { index, (title, description, iconAndAction) ->
                        SettingClickRow(
                            icon = iconAndAction.first,
                            title = title,
                            description = description,
                            shape = SegmentedList.shape(index, links.size),
                            showChevron = true,
                            onClick = iconAndAction.second
                        )
                    }
                }
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
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SegmentedList.Gap),
            content = content
        )
    }
}

@Composable
private fun SettingIconBadge(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSecondaryContainer) {
            content()
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
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://teja2495.github.io/teja-karlapudi-links/")))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(SegmentedList.Gap)
    ) {
        options.forEachIndexed { index, (app, labelRes) ->
            MessagingOptionChip(
                app = app,
                labelRes = labelRes,
                selected = selectedApp == app,
                isFirst = index == 0,
                isLast = index == options.lastIndex,
                onClick = { onMessagingAppSelected(app) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * One segment of a connected button group: the selected segment morphs to a fully
 * rounded shape and fills with the primary container colour.
 */
@Composable
private fun MessagingOptionChip(
    app: MessagingApp,
    labelRes: Int,
    selected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val outer = SegmentedList.OuterCorner
    val inner = SegmentedList.InnerCorner
    val startCorner by animateDpAsState(
        targetValue = if (selected || isFirst) outer else inner,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "messaging_start_corner"
    )
    val endCorner by animateDpAsState(
        targetValue = if (selected || isLast) outer else inner,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "messaging_end_corner"
    )
    val container by animateColorAsState(
        targetValue = if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainer,
        animationSpec = ExpressiveMotion.defaultEffects(),
        label = "messaging_container"
    )
    val content by animateColorAsState(
        targetValue = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
        animationSpec = ExpressiveMotion.defaultEffects(),
        label = "messaging_content"
    )
    val shape = RoundedCornerShape(
        topStart = startCorner,
        bottomStart = startCorner,
        topEnd = endCorner,
        bottomEnd = endCorner
    )

    Column(
        modifier = modifier
            .height(88.dp)
            .clip(shape)
            .background(container)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        MessagingOptionIcon(app = app, tint = content)
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MessagingOptionIcon(app: MessagingApp, tint: Color) {
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
private fun SettingRowContainer(
    shape: Shape,
    onClick: () -> Unit,
    role: Role? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = role, onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun SettingText(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingClickRow(
    icon: ImageVector,
    title: String,
    description: String,
    shape: Shape,
    showChevron: Boolean = false,
    onClick: () -> Unit
) {
    SettingRowContainer(shape = shape, onClick = onClick) {
        SettingIconBadge {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
        SettingText(title = title, description = description, modifier = Modifier.weight(1f))
        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.cd_navigate_forward),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    shape: Shape,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    painter: Painter? = null
) {
    SettingRowContainer(
        shape = shape,
        onClick = { if (enabled) onCheckedChange(!checked) },
        role = Role.Switch
    ) {
        SettingIconBadge {
            when {
                icon != null -> Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
                painter != null -> Icon(painter = painter, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        SettingText(title = title, description = description, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = settingsSwitchColors(),
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else null
        )
    }
}

@Composable
private fun settingsSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedIconColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = Color.Transparent,
    disabledCheckedThumbColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
    disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    disabledUncheckedThumbColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
)
