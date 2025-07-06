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

@Composable
fun SettingsScreen(
    viewModel: ContactsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInternationalDetectionEnabled by viewModel.isInternationalDetectionEnabled.collectAsState()
    val isRecentCallsVisible by viewModel.isRecentCallsVisible.collectAsState()
    val useWhatsAppAsDefault by viewModel.useWhatsAppAsDefault.collectAsState()
    val defaultMessagingApp by viewModel.defaultMessagingApp.collectAsState()
    val availableMessagingApps by viewModel.availableMessagingApps.collectAsState()
    
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
                        Icon(
                            painter = painterResource(id = R.drawable.sms_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SMS",
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
                        Icon(
                            painter = painterResource(id = R.drawable.whatsapp_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
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
                        Icon(
                            painter = painterResource(id = R.drawable.telegram_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
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
                            text = "Display recent calls at the top of the home screen for quick access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isRecentCallsVisible,
                        onCheckedChange = { viewModel.toggleRecentCallsVisibility() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            item {
                // International Number Detection Setting (no Card)
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
                        Text(
                            text = when {
                                defaultMessagingApp == MessagingApp.SMS ->
                                    "Disabled when SMS is the default messaging app"
                                else -> 
                                    "For international numbers, tapping the card opens WhatsApp or Telegram." 
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (defaultMessagingApp == MessagingApp.SMS) 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isInternationalDetectionEnabled,
                        onCheckedChange = { viewModel.toggleInternationalDetection() },
                        enabled = defaultMessagingApp != MessagingApp.SMS,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            item {
                // Feedback button just below international number detection
                val context = LocalContext.current
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:quickcontacts.feedback@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Quick Contacts Feedback")
                        }
                        context.startActivity(intent)
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
} 