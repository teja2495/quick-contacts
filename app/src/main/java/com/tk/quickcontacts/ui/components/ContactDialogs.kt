package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.R
import com.tk.quickcontacts.models.CustomActions
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.utils.PhoneNumberUtils

@Composable
fun PhoneNumberSelectionDialog(
    contact: Contact,
    onPhoneNumberSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    selectedContacts: List<Contact> = emptyList(),
    onAddContact: ((Contact) -> Unit)? = null,
    onRemoveContact: ((Contact) -> Unit)? = null,
    hideIcons: Boolean = false, // NEW FLAG
    showInstructionText: Boolean = true // NEW FLAG
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Phone Number",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val context = LocalContext.current
            Column {
                if (showInstructionText) {
                    Text(
                        text = "The number you choose will be set as your default for this contact. To change it later, remove and re-add the contact with a different number.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                contact.phoneNumbers.forEach { phoneNumber ->
                    // Use remember to make this reactive to selectedContacts changes
                    val isNumberInQuickList by remember(selectedContacts, contact.id, phoneNumber) {
                        mutableStateOf(
                            selectedContacts.any { selectedContact ->
                                selectedContact.id == contact.id && selectedContact.phoneNumber == phoneNumber
                            }
                        )
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                // When card is clicked, call onPhoneNumberSelected and dismiss the dialog
                                onPhoneNumberSelected(phoneNumber)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = PhoneNumberUtils.formatPhoneNumber(phoneNumber),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            if (!hideIcons && (onAddContact != null && onRemoveContact != null)) {
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isNumberInQuickList) {
                                    IconButton(onClick = {
                                        onRemoveContact?.invoke(contact.copy(phoneNumber = phoneNumber, phoneNumbers = listOf(phoneNumber)))
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "In quick list",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    IconButton(onClick = {
                                        onAddContact?.invoke(contact.copy(phoneNumber = phoneNumber, phoneNumbers = listOf(phoneNumber)))
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add to quick list",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (onAddContact != null && onRemoveContact != null) "Done" else "Cancel")
            }
        }
    )
}

@Composable
fun ActionToggleDialog(
    contact: Contact,
    isInternational: Boolean,
    isCurrentlySwapped: Boolean,
    customActions: CustomActions? = null,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    isInternationalDetectionEnabled: Boolean = true,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS),
    onConfirm: (primaryAction: String, secondaryAction: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Available actions based on installed apps
    val availableActions = mutableListOf("Call")
    if (availableMessagingApps.contains(MessagingApp.WHATSAPP)) availableActions.add("WhatsApp")
    if (availableMessagingApps.contains(MessagingApp.TELEGRAM)) availableActions.add("Telegram")
    if (availableMessagingApps.contains(MessagingApp.SMS)) availableActions.add("Messages")
    
    // Get default messaging app name
    val messagingAppName = when (defaultMessagingApp) {
        MessagingApp.WHATSAPP -> "WhatsApp"
        MessagingApp.SMS -> "Messages"
        MessagingApp.TELEGRAM -> "Telegram"
    }
    
    // Current actions - use custom actions if available, otherwise use new default logic
    val currentPrimary = customActions?.primaryAction ?: if (isInternationalDetectionEnabled && isInternational) {
        messagingAppName  // International: Primary = messaging app
    } else {
        "Call"  // Default: Primary = Call
    }
    
    val currentSecondary = customActions?.secondaryAction ?: if (isInternationalDetectionEnabled && isInternational) {
        "Call"  // International: Secondary = Call
    } else {
        messagingAppName  // Default: Secondary = messaging app
    }
    
    // State for selected actions
    var selectedPrimary by remember { mutableStateOf(currentPrimary) }
    var selectedSecondary by remember { mutableStateOf(currentSecondary) }
    
    // Check if configuration has changed
    val hasChanged = selectedPrimary != currentPrimary || selectedSecondary != currentSecondary
    
    // Check if selections are valid (different actions)
    val isValidSelection = selectedPrimary != selectedSecondary
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Set actions for ${contact.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Primary action selection
                Text(
                    text = "Primary (Tap Card)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                // Two rows of FilterChips for Primary Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        onClick = { selectedPrimary = "Call" },
                        label = { Text("Call") },
                        selected = selectedPrimary == "Call",
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                    )
                    FilterChip(
                        onClick = { selectedPrimary = "Messages" },
                        label = { Text("Messages") },
                        selected = selectedPrimary == "Messages",
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                    )
                }
                
                // Only show second row if there are apps to show
                val hasWhatsApp = availableMessagingApps.contains(MessagingApp.WHATSAPP)
                val hasTelegram = availableMessagingApps.contains(MessagingApp.TELEGRAM)
                if (hasWhatsApp || hasTelegram) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasWhatsApp && hasTelegram) {
                            FilterChip(
                                onClick = { selectedPrimary = "WhatsApp" },
                                label = { Text("WhatsApp") },
                                selected = selectedPrimary == "WhatsApp",
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .height(32.dp)
                                    .weight(1f)
                            )
                            FilterChip(
                                onClick = { selectedPrimary = "Telegram" },
                                label = { Text("Telegram") },
                                selected = selectedPrimary == "Telegram",
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .height(32.dp)
                                    .weight(1f)
                            )
                        } else if (hasWhatsApp) {
                            FilterChip(
                                onClick = { selectedPrimary = "WhatsApp" },
                                label = { Text("WhatsApp") },
                                selected = selectedPrimary == "WhatsApp",
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .height(32.dp)
                                    .weight(1f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        } else if (hasTelegram) {
                            FilterChip(
                                onClick = { selectedPrimary = "Telegram" },
                                label = { Text("Telegram") },
                                selected = selectedPrimary == "Telegram",
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .height(32.dp)
                                    .weight(1f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                
                // Secondary action selection
                Text(
                    text = "Secondary (Tap Icon)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                // Two rows of FilterChips for Secondary Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        onClick = { selectedSecondary = "Call" },
                        label = { Text("Call") },
                        selected = selectedSecondary == "Call",
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                    )
                    FilterChip(
                        onClick = { selectedSecondary = "Messages" },
                        label = { Text("Messages") },
                        selected = selectedSecondary == "Messages",
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                    )
                }
                
                // Only show second row if there are apps to show
                val hasWhatsAppSec = availableMessagingApps.contains(MessagingApp.WHATSAPP)
                val hasTelegramSec = availableMessagingApps.contains(MessagingApp.TELEGRAM)
                if (hasWhatsAppSec || hasTelegramSec) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasWhatsAppSec && hasTelegramSec) {
                            FilterChip(
                                onClick = { selectedSecondary = "WhatsApp" },
                                label = { Text("WhatsApp") },
                                selected = selectedSecondary == "WhatsApp",
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .height(32.dp)
                                    .weight(1f)
                            )
                            FilterChip(
                                onClick = { selectedSecondary = "Telegram" },
                                label = { Text("Telegram") },
                                selected = selectedSecondary == "Telegram",
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .height(32.dp)
                                    .weight(1f)
                            )
                        } else if (hasWhatsAppSec) {
                            FilterChip(
                                onClick = { selectedSecondary = "WhatsApp" },
                                label = { Text("WhatsApp") },
                                selected = selectedSecondary == "WhatsApp",
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .height(32.dp)
                                    .weight(1f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        } else if (hasTelegramSec) {
                            FilterChip(
                                onClick = { selectedSecondary = "Telegram" },
                                label = { Text("Telegram") },
                                selected = selectedSecondary == "Telegram",
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .height(32.dp)
                                    .weight(1f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                
                // Error message for invalid selection
                if (!isValidSelection) {
                    Text(
                        text = "Primary and secondary actions must be different",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValidSelection) {
                        onConfirm(selectedPrimary, selectedSecondary)
                    }
                },
                enabled = isValidSelection && hasChanged
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ContactActionsDialog(
    contact: Contact,
    onCall: (Contact) -> Unit,
    onSms: (Contact) -> Unit,
    onWhatsApp: (Contact) -> Unit,
    onTelegram: (Contact) -> Unit,
    onDismiss: () -> Unit,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call option - always available
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCall(contact)
                            onDismiss()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = stringResource(R.string.cd_phone),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.action_call),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // SMS option - always available
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSms(contact)
                            onDismiss()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.sms_icon),
                            contentDescription = stringResource(R.string.cd_sms),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.action_sms),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // WhatsApp option - only if available
                if (availableMessagingApps.contains(MessagingApp.WHATSAPP)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onWhatsApp(contact)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.whatsapp_icon),
                                contentDescription = stringResource(R.string.cd_whatsapp),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.action_whatsapp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Telegram option - only if available
                if (availableMessagingApps.contains(MessagingApp.TELEGRAM)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTelegram(contact)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.telegram_icon),
                                contentDescription = stringResource(R.string.cd_telegram),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.action_telegram),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun CallWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Warning", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Text("You are about to make a phone call. Carrier charges may apply. This warning will only appear once. Do you want to continue?", style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Continue") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
} 

@Composable
fun EditContactNameDialog(
    contact: Contact,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(contact.name) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = if (it.isBlank()) "Name cannot be empty" else null
                    },
                    label = { Text("Name") },
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onSave(name)
                    else error = "Name cannot be empty"
                },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
} 