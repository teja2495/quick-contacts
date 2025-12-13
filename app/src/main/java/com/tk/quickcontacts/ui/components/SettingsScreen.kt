package com.tk.quickcontacts.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quickcontacts.ContactsViewModel
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.R
import androidx.compose.ui.res.stringResource
import com.tk.quickcontacts.utils.PhoneNumberUtils
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

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
    val isInternationalDetectionEnabled by viewModel.isInternationalDetectionEnabled.collectAsState()
    val isRecentCallsVisible by viewModel.isRecentCallsVisible.collectAsState()
    val isDirectDialEnabled by viewModel.isDirectDialEnabled.collectAsState()
    val useWhatsAppAsDefault by viewModel.useWhatsAppAsDefault.collectAsState()
    val defaultMessagingApp by viewModel.defaultMessagingApp.collectAsState()
    val availableMessagingApps by viewModel.availableMessagingApps.collectAsState()
    
    // Dialog state for permission settings
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var showCallPermissionSettingsDialog by remember { mutableStateOf(false) }
    
    // Refresh available messaging apps when settings screen is opened
    LaunchedEffect(Unit) {
        viewModel.refreshAvailableMessagingApps()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Settings content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                // Messaging App Setting (no Card)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Default Messaging App",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Explanation text for default messaging app setting
                    Text(
                        text = "You can change this for individual contacts in quick list by tapping on the edit button",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // SMS Option (always available)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                viewModel.setMessagingApp(MessagingApp.SMS)
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultMessagingApp == MessagingApp.SMS,
                            onClick = { 
                                viewModel.setMessagingApp(MessagingApp.SMS)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.width(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.sms_icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Messages",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // WhatsApp Option
                    val isWhatsAppAvailable = availableMessagingApps.contains(MessagingApp.WHATSAPP)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isWhatsAppAvailable) { 
                                if (isWhatsAppAvailable) {
                                    viewModel.setMessagingApp(MessagingApp.WHATSAPP)
                                }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultMessagingApp == MessagingApp.WHATSAPP,
                            onClick = { 
                                if (isWhatsAppAvailable) {
                                    viewModel.setMessagingApp(MessagingApp.WHATSAPP)
                                }
                            },
                            enabled = isWhatsAppAvailable,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                disabledSelectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                disabledUnselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.width(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.whatsapp_icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "WhatsApp",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isWhatsAppAvailable) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            if (!isWhatsAppAvailable) {
                                Text(
                                    text = "Not installed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    // Telegram Option
                    val isTelegramAvailable = availableMessagingApps.contains(MessagingApp.TELEGRAM)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isTelegramAvailable) { 
                                if (isTelegramAvailable) {
                                    viewModel.setMessagingApp(MessagingApp.TELEGRAM)
                                }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultMessagingApp == MessagingApp.TELEGRAM,
                            onClick = { 
                                if (isTelegramAvailable) {
                                    viewModel.setMessagingApp(MessagingApp.TELEGRAM)
                                }
                            },
                            enabled = isTelegramAvailable,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                disabledSelectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                disabledUnselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.width(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.telegram_icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Telegram",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isTelegramAvailable) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            if (!isTelegramAvailable) {
                                Text(
                                    text = "Not installed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                // Recent Calls Visibility Setting (no Card)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Show Recent Calls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasCallLogPermission) {
                                stringResource(R.string.recent_calls_description)
                            } else {
                                "Requires call history permission to display recent calls."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isRecentCallsVisible && hasCallLogPermission,
                        onCheckedChange = { newValue ->
                            if (newValue && !hasCallLogPermission) {
                                // User wants to enable but doesn't have permission
                                if (isCallLogPermissionPermanentlyDenied) {
                                    // Permission is permanently denied, show dialog
                                    showPermissionSettingsDialog = true
                                } else {
                                    // Request permission normally
                                    onRequestCallLogPermission?.invoke()
                                }
                            } else {
                                // User wants to disable or already has permission
                                viewModel.toggleRecentCallsVisibility()
                            }
                        },
                        enabled = true, // Always enabled to allow interaction
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            item {
                // Direct Dial Setting (no Card)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Direct Dial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasCallPermission) {
                                "Calls are made immediately without opening the dialer."
                            } else {
                                "Requires phone permission to make calls directly."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isDirectDialEnabled && hasCallPermission,
                        onCheckedChange = { newValue ->
                            if (newValue && !hasCallPermission) {
                                // User wants to enable but doesn't have permission
                                if (isCallPermissionPermanentlyDenied) {
                                    // Permission is permanently denied, show dialog
                                    showCallPermissionSettingsDialog = true
                                } else {
                                    // Request permission normally
                                    onRequestCallPermission?.invoke()
                                }
                            } else {
                                // User wants to disable or already has permission
                                viewModel.toggleDirectDial()
                            }
                        },
                        enabled = true, // Always enabled to allow interaction
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            item {
                // International Number Detection Setting (no Card)
                val context = LocalContext.current
                // Observe home country code from ViewModel
                val homeCountryCode by viewModel.homeCountryCode.collectAsState()
                val isoCountry = remember(context, isInternationalDetectionEnabled, defaultMessagingApp) {
                    // Always detect country code, regardless of international detection status
                    // This allows the dialog to be pre-populated when user tries to enable international detection
                    val country = PhoneNumberUtils.getUserCountryCode(context)
                    country
                }
                val defaultDialingCode = isoCountry?.let { PhoneNumberUtils.isoToDialingCode(it) } ?: ""
                val displayDialingCode = homeCountryCode ?: defaultDialingCode

                // Dialog state for editing country code
                var showEditCountryCodeDialog by remember { mutableStateOf(false) }
                var tempCountryCode by remember(displayDialingCode) { 
                    mutableStateOf(displayDialingCode) 
                }
                var pendingEnableInternationalDetection by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "International Number Detection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        when {
                            defaultMessagingApp == MessagingApp.SMS -> {
                                Text(
                                    text = "Disabled when SMS is the default messaging app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            isInternationalDetectionEnabled && defaultMessagingApp != MessagingApp.SMS && displayDialingCode.isNotBlank() -> {
                                Text(
                                    text = "Phone number must have country code.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Home Country Code: ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = displayDialingCode,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary, // Feedback color
                                        modifier = Modifier
                                            .clickable {
                                                tempCountryCode = displayDialingCode
                                                showEditCountryCodeDialog = true
                                            }
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = "For international numbers, tapping the card opens WhatsApp or Telegram.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isInternationalDetectionEnabled,
                        onCheckedChange = { newValue ->
                            if (newValue) {
                                // Enabling: check if country code is available
                                if (homeCountryCode.isNullOrBlank() && defaultDialingCode.isBlank()) {
                                    // Prompt for country code
                                    pendingEnableInternationalDetection = true
                                    tempCountryCode = displayDialingCode
                                    showEditCountryCodeDialog = true
                                } else {
                                    viewModel.toggleInternationalDetection()
                                }
                            } else {
                                // Disabling: just toggle off
                                viewModel.toggleInternationalDetection()
                            }
                        },
                        enabled = defaultMessagingApp != MessagingApp.SMS,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
                // Edit Country Code Dialog
                if (showEditCountryCodeDialog) {
                    val isCountryCodeValid = remember(tempCountryCode) {
                        com.tk.quickcontacts.utils.PhoneNumberUtils.isValidCountryDialingCode(tempCountryCode.trim())
                    }
                    AlertDialog(
                        onDismissRequest = {
                            showEditCountryCodeDialog = false
                            if (pendingEnableInternationalDetection) {
                                // User cancelled, revert toggle and clear country code
                                pendingEnableInternationalDetection = false
                                viewModel.clearHomeCountryCode()
                                if (!isInternationalDetectionEnabled) {
                                    // Already off, do nothing
                                } else {
                                    viewModel.toggleInternationalDetection()
                                }
                            }
                        },
                        title = {
                            Text("Set Home Country Code", style = MaterialTheme.typography.titleMedium)
                        },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = tempCountryCode,
                                    onValueChange = { input ->
                                        // Only allow digits and plus sign
                                        val filtered = input.filter { it.isDigit() || it == '+' }
                                        tempCountryCode = filtered
                                    },
                                    label = { Text("Country Code (e.g. +1)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    isError = tempCountryCode.isNotBlank() && !isCountryCodeValid
                                )
                                if (tempCountryCode.isNotBlank() && !isCountryCodeValid) {
                                    Text(
                                        text = "Invalid country code. Please enter a valid code like +1, +91, etc.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (tempCountryCode.isBlank() || !isCountryCodeValid) {
                                    // Don't allow blank or invalid, treat as cancel
                                    showEditCountryCodeDialog = false
                                    if (pendingEnableInternationalDetection) {
                                        pendingEnableInternationalDetection = false
                                        viewModel.clearHomeCountryCode()
                                        if (!isInternationalDetectionEnabled) {
                                            // Already off, do nothing
                                        } else {
                                            viewModel.toggleInternationalDetection()
                                        }
                                    }
                                } else {
                                    // Ensure country code starts with '+'
                                    val codeToSave = if (tempCountryCode.trim().startsWith("+")) tempCountryCode.trim() else "+" + tempCountryCode.trim()
                                    viewModel.setHomeCountryCode(codeToSave)
                                    showEditCountryCodeDialog = false
                                    if (pendingEnableInternationalDetection) {
                                        pendingEnableInternationalDetection = false
                                        if (!isInternationalDetectionEnabled) {
                                            viewModel.toggleInternationalDetection()
                                        }
                                    }
                                }
                            }, enabled = tempCountryCode.isNotBlank() && isCountryCodeValid) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showEditCountryCodeDialog = false
                                if (pendingEnableInternationalDetection) {
                                    pendingEnableInternationalDetection = false
                                    viewModel.clearHomeCountryCode()
                                    if (!isInternationalDetectionEnabled) {
                                        // Already off, do nothing
                                    } else {
                                        viewModel.toggleInternationalDetection()
                                    }
                                }
                            }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
            item {
                // Feedback button just below international number detection
                val context = LocalContext.current
                TextButton(
                    onClick = {
                        val versionName = try {
                            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            packageInfo.versionName ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }
                        
                        val androidVersion = android.os.Build.VERSION.RELEASE
                        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                        
                        val emailBody = """
                            
                            
                            ---
                            App Version: $versionName
                            Android Version: $androidVersion
                            Device: $deviceModel
                        """.trimIndent()

                        val subject = "Quick Contacts Feedback"

                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:tejakarlapudi.apps@gmail.com?subject=${Uri.encode(subject)}&body=${Uri.encode(emailBody)}")
                        }

                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle case where no email app is installed
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Send Feedback & Bug Reports",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        // Version number at the bottom
        val context = LocalContext.current
        val versionName = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Version $versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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