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

// Helper data class for chip layout
data class ChipLayout(
    val rows: Int,
    val columns: Int,
    val lastRowAlignment: String = "center" // "center" or "left"
)

// Helper functions for dynamic chip layout
private fun buildPrimaryChipsList(availableMessagingApps: Set<MessagingApp>): List<Pair<String, Boolean>> {
    return listOf(
        Pair("Call", true),
        Pair("Messages", true),
        Pair("WhatsApp", availableMessagingApps.contains(MessagingApp.WHATSAPP)),
        Pair("Telegram", availableMessagingApps.contains(MessagingApp.TELEGRAM)),
        Pair("All Options", true)
    ).filter { it.second } // Only include available options
}

private fun buildSecondaryChipsList(availableMessagingApps: Set<MessagingApp>): List<Pair<String, Boolean>> {
    return listOf(
        Pair("Call", true),
        Pair("Messages", true),
        Pair("WhatsApp", availableMessagingApps.contains(MessagingApp.WHATSAPP)),
        Pair("Telegram", availableMessagingApps.contains(MessagingApp.TELEGRAM)),
        Pair("All Options", true)
    ).filter { it.second } // Only include available options
}

private fun calculateChipLayout(chipCount: Int): ChipLayout {
    return when (chipCount) {
        2 -> ChipLayout(rows = 1, columns = 2)
        3 -> ChipLayout(rows = 2, columns = 2, lastRowAlignment = "left")
        4 -> ChipLayout(rows = 2, columns = 2)
        5 -> ChipLayout(rows = 3, columns = 2, lastRowAlignment = "left")
        else -> ChipLayout(rows = 3, columns = 2) // Default fallback
    }
}

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
                text = "Change Actions for ${contact.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                
                // Primary action selection
                Text(
                    text = "Tap Card Action",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                // Dynamic grid for Primary Action based on available apps
                val primaryChips = buildPrimaryChipsList(availableMessagingApps)
                val primaryLayout = calculateChipLayout(primaryChips.size)
                
                for (row in 0 until primaryLayout.rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val chipsInThisRow = if (row == primaryLayout.rows - 1 && primaryLayout.lastRowAlignment == "left") {
                            // Last row with left alignment - only show chips that should be in this row
                            val chipsInLastRow = primaryChips.size - (primaryLayout.rows - 1) * primaryLayout.columns
                            chipsInLastRow
                        } else {
                            primaryLayout.columns
                        }
                        
                        for (col in 0 until primaryLayout.columns) {
                            val idx = row * primaryLayout.columns + col
                            if (idx < primaryChips.size && col < chipsInThisRow) {
                                val (label, show) = primaryChips[idx]
                                if (show) {
                                    FilterChip(
                                        onClick = { selectedPrimary = label },
                                        label = { Text(label) },
                                        selected = selectedPrimary == label,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                // Secondary action selection
                Text(
                    text = "Tap Icon Action",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                )
                
                // Dynamic grid for Secondary Action based on available apps
                val secondaryChips = buildSecondaryChipsList(availableMessagingApps)
                val secondaryLayout = calculateChipLayout(secondaryChips.size)
                
                for (row in 0 until secondaryLayout.rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val chipsInThisRow = if (row == secondaryLayout.rows - 1 && secondaryLayout.lastRowAlignment == "left") {
                            // Last row with left alignment - only show chips that should be in this row
                            val chipsInLastRow = secondaryChips.size - (secondaryLayout.rows - 1) * secondaryLayout.columns
                            chipsInLastRow
                        } else {
                            secondaryLayout.columns
                        }
                        
                        for (col in 0 until secondaryLayout.columns) {
                            val idx = row * secondaryLayout.columns + col
                            if (idx < secondaryChips.size && col < chipsInThisRow) {
                                val (label, show) = secondaryChips[idx]
                                if (show) {
                                    FilterChip(
                                        onClick = { selectedSecondary = label },
                                        label = { Text(label) },
                                        selected = selectedSecondary == label,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
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