package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import android.content.Context
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.R
import com.tk.quickcontacts.models.CustomActions
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.utils.PhoneNumberUtils
import com.tk.quickcontacts.utils.ContactUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean,
    onDeleteContact: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {},
    onContactImageClick: (Contact) -> Unit = {},
    reorderState: org.burnoutcrew.reorderable.ReorderableLazyListState? = null,
    onLongClick: (Contact) -> Unit = {},
    onSetCustomActions: (Contact, String, String) -> Unit = { _, _, _ -> },
    customActions: CustomActions? = null,
    isInternationalDetectionEnabled: Boolean = true,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS),
    selectedContacts: List<Contact> = emptyList(),
    onExecuteAction: (Context, String, String) -> Unit,
    onUpdateContactNumber: (Contact, String) -> Unit = { _, _ -> },
    hasSeenCallWarning: Boolean = true,
    onMarkCallWarningSeen: (() -> Unit)? = null,
    homeCountryCode: String? = null,
    onEditContactName: (Contact, String) -> Unit,
    callActivity: Contact? = null // New parameter for call activity data
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var showActionToggleDialog by remember { mutableStateOf(false) }
    var showContactActionsDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf<String?>(null) }
    var showCallWarningDialog by remember { mutableStateOf(false) }
    var pendingCallNumber by remember { mutableStateOf<String?>(null) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val isInternational = PhoneNumberUtils.isInternationalNumber(context, ContactUtils.getPrimaryPhoneNumber(contact), isInternationalDetectionEnabled, homeCountryCode)
    
    // Phone number selection dialog
    if (showPhoneNumberDialog) {
        PhoneNumberSelectionDialog(
            contact = contact,
            onPhoneNumberSelected = { selectedNumber: String ->
                val contactWithSelectedNumber = contact.copy(
                    phoneNumber = selectedNumber,
                    phoneNumbers = listOf(selectedNumber)
                )
                // Update the quick list contact to have only the selected number
                onUpdateContactNumber(contact, selectedNumber)
                when (dialogAction) {
                    "call" -> onExecuteAction(context, "Call", selectedNumber)
                    "whatsapp" -> onExecuteAction(context, "WhatsApp", selectedNumber)
                    "sms" -> onExecuteAction(context, "Messages", selectedNumber)
                    "telegram" -> onExecuteAction(context, "Telegram", selectedNumber)
                }
                showPhoneNumberDialog = false
                dialogAction = null
            },
            onDismiss = {
                showPhoneNumberDialog = false
                dialogAction = null
            },
            selectedContacts = selectedContacts,
            onAddContact = if (dialogAction == "add") { contactWithSelectedNumber ->
                onContactClick(contactWithSelectedNumber)
            } else null,
            onRemoveContact = if (dialogAction == "add") { contactWithSelectedNumber ->
                onContactClick(contactWithSelectedNumber)
            } else null,
            hideIcons = dialogAction == "add",
            showInstructionText = true
        )
    }
    
    // Action toggle dialog
    if (showActionToggleDialog) {
        ActionToggleDialog(
            contact = contact,
            isInternational = isInternational == true, // Pass non-null Boolean
            isCurrentlySwapped = false, // No longer using swap logic
            customActions = customActions,
            defaultMessagingApp = defaultMessagingApp,
            isInternationalDetectionEnabled = isInternationalDetectionEnabled,
            availableMessagingApps = availableMessagingApps,
            onConfirm = { primaryAction, secondaryAction ->
                onSetCustomActions(contact, primaryAction, secondaryAction)
                showActionToggleDialog = false
            },
            onDismiss = {
                showActionToggleDialog = false
            }
        )
    }
    
    // Contact actions dialog for long press
    if (showContactActionsDialog) {
        ContactActionsDialog(
            contact = contact,
            onCall = { contactToCall ->
                if (contactToCall.phoneNumbers.size > 1) {
                    dialogAction = "call"
                    showPhoneNumberDialog = true
                } else {
                    onExecuteAction(context, "Call", contactToCall.phoneNumber)
                }
                showContactActionsDialog = false
            },
            onSms = { contactToSms ->
                if (contactToSms.phoneNumbers.size > 1) {
                    dialogAction = "sms"
                    showPhoneNumberDialog = true
                } else {
                    onExecuteAction(context, "Messages", contactToSms.phoneNumber)
                }
                showContactActionsDialog = false
            },
            onWhatsApp = { contactToWhatsApp ->
                if (contactToWhatsApp.phoneNumbers.size > 1) {
                    dialogAction = "whatsapp"
                    showPhoneNumberDialog = true
                } else {
                    onExecuteAction(context, "WhatsApp", contactToWhatsApp.phoneNumber)
                }
                showContactActionsDialog = false
            },
            onTelegram = { contactToTelegram ->
                if (contactToTelegram.phoneNumbers.size > 1) {
                    dialogAction = "telegram"
                    showPhoneNumberDialog = true
                } else {
                    onExecuteAction(context, "Telegram", contactToTelegram.phoneNumber)
                }
                showContactActionsDialog = false
            },
            onDismiss = {
                showContactActionsDialog = false
            },
            availableMessagingApps = availableMessagingApps
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .combinedClickable(
                enabled = true, // Always enabled to handle both edit and normal mode
                onClick = {
                    if (editMode) {
                        // In edit mode, show action toggle dialog on press
                        showActionToggleDialog = true
                    } else {
                        // Normal mode: perform contact action
                        val messagingAppName = when (defaultMessagingApp) {
                            MessagingApp.WHATSAPP -> "WhatsApp"
                            MessagingApp.SMS -> "SMS"
                            MessagingApp.TELEGRAM -> "Telegram"
                        }
                        val primaryAction = customActions?.primaryAction ?: if (isInternationalDetectionEnabled && isInternational == true) {
                            messagingAppName  // International: Primary = messaging app
                        } else {
                            "Call"  // Default: Primary = Call
                        }
                        if (primaryAction == "All Options") {
                            showContactActionsDialog = true
                        } else if (contact.phoneNumbers.size > 1) {
                            dialogAction = primaryAction.lowercase()
                            showPhoneNumberDialog = true
                        } else {
                            if (primaryAction == "Call" && !hasSeenCallWarning) {
                                pendingCallNumber = contact.phoneNumber
                                showCallWarningDialog = true
                            } else {
                                onExecuteAction(context, primaryAction, contact.phoneNumber)
                            }
                        }
                    }
                },
                onLongClick = {
                    if (!editMode) {
                        showContactActionsDialog = true
                    }
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(300))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editMode) {
                // Reorder handle icon - draggable handle for reordering
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .then(
                            if (reorderState != null) {
                                Modifier.detectReorder(reorderState)
                            } else {
                                Modifier
                            }
                        )
                )
            }

            // Contact Photo (hidden in edit mode) - clickable to open in contacts app
            if (!editMode) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable { onContactImageClick(contact) }
                ) {
                    if (contact.photoUri != null && !imageLoadFailed) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(contact.photoUri)
                                    .crossfade(true)
                                    .size(48, 48) // Optimize size for better performance
                                    .memoryCacheKey("contact_${contact.id}")
                                    .build(),
                                onError = { imageLoadFailed = true }
                            ),
                            contentDescription = "Contact photo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Default avatar
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.name.firstOrNull()?.let { if (!it.isLetter()) "#" else it.toString().uppercase() } ?: "?",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Contact Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (editMode) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showEditNameDialog = true }
                        )
                    } else {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Show primary action text or call status (only in normal mode, not in edit mode)
                if (!editMode) {
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Get the primary action for this contact
                    val primaryAction = customActions?.primaryAction ?: if (isInternationalDetectionEnabled && isInternational == true) {
                        when (defaultMessagingApp) {
                            MessagingApp.WHATSAPP -> "WhatsApp"
                            MessagingApp.SMS -> "SMS"
                            MessagingApp.TELEGRAM -> "Telegram"
                        }
                    } else {
                        "Call"
                    }
                    
                    // Show call status if available, otherwise show primary action
                    // Only show call activity if primary action is "Call"
                    if (callActivity?.callType != null && callActivity.callTimestamp != null && primaryAction == "Call") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Show call activity information only
                            Icon(
                                imageVector = when (callActivity.callType) {
                                    "missed" -> Icons.AutoMirrored.Filled.CallMissed
                                    "incoming" -> Icons.AutoMirrored.Filled.CallReceived
                                    "outgoing" -> Icons.AutoMirrored.Filled.CallMade
                                    else -> Icons.Default.Call
                                },
                                contentDescription = callActivity.callType.replaceFirstChar { it.uppercase() },
                                tint = when (callActivity.callType) {
                                    "missed" -> Color(0xFFE57373) // Subtle red
                                    "incoming" -> Color(0xFF81C784) // Subtle green
                                    "outgoing" -> Color(0xFF64B5F6) // Subtle blue
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = com.tk.quickcontacts.utils.ContactUtils.formatCallTimestamp(callActivity.callTimestamp!!),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        // Show only primary action when no call activity is available or for international contacts
                        Text(
                            text = "$primaryAction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
                
                // Show phone number in edit mode
                if (editMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = PhoneNumberUtils.formatPhoneNumber(contact.phoneNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show additional phone numbers if there are multiple
                    if (contact.phoneNumbers.size > 1) {
                        contact.phoneNumbers.forEachIndexed { index, number ->
                            if (index > 0 && number != contact.phoneNumber) {
                                Text(
                                    text = PhoneNumberUtils.formatPhoneNumber(number),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (editMode) {
                IconButton(onClick = { onDeleteContact(contact) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Contact",
                        tint = Color(0xFFE57373)
                    )
                }
            } else {
                // Secondary action button - opposite of primary action
                IconButton(
                    onClick = { 
                        // Get default messaging app name
                        val messagingAppName = when (defaultMessagingApp) {
                            MessagingApp.WHATSAPP -> "WhatsApp"
                            MessagingApp.SMS -> "SMS"
                            MessagingApp.TELEGRAM -> "Telegram"
                        }
                        // Use custom secondary action or determine based on new default logic
                        val secondaryAction = customActions?.secondaryAction ?: if (isInternationalDetectionEnabled && isInternational == true) {
                            "Call"  // International: Secondary = Call
                        } else {
                            messagingAppName  // Default: Secondary = messaging app
                        }
                        if (secondaryAction == "All Options") {
                            showContactActionsDialog = true
                        } else if (contact.phoneNumbers.size > 1) {
                            dialogAction = secondaryAction.lowercase()
                            showPhoneNumberDialog = true
                        } else {
                            // Use onExecuteAction to match UI label logic
                            onExecuteAction(context, secondaryAction, contact.phoneNumber)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    // Show secondary action icon based on custom actions or new default logic
                    val messagingAppName = when (defaultMessagingApp) {
                        MessagingApp.WHATSAPP -> "WhatsApp"
                        MessagingApp.SMS -> "SMS"
                        MessagingApp.TELEGRAM -> "Telegram"
                    }
                    val secondaryAction = customActions?.secondaryAction ?: if (isInternationalDetectionEnabled && isInternational == true) {
                        "Call"  // International: Secondary = Call
                    } else {
                        messagingAppName  // Default: Secondary = messaging app
                    }
                    when (secondaryAction) {
                        "Call" -> {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call ${contact.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        "WhatsApp" -> {
                            Icon(
                                painter = painterResource(id = R.drawable.whatsapp_icon),
                                contentDescription = "WhatsApp ${contact.name}",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        "SMS" -> {
                            Icon(
                                painter = painterResource(id = R.drawable.sms_icon),
                                contentDescription = "SMS ${contact.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        "Telegram" -> {
                            Icon(
                                painter = painterResource(id = R.drawable.telegram_icon),
                                contentDescription = "Telegram ${contact.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        "All Options" -> {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz, // Use a menu or grid icon
                                contentDescription = "All Options for ${contact.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        else -> {
                            // Fallback for unknown actions
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call ${contact.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    // Show call warning dialog if needed
    if (showCallWarningDialog && pendingCallNumber != null) {
        CallWarningDialog(
            onConfirm = {
                onMarkCallWarningSeen?.invoke()
                onExecuteAction(context, "Call", pendingCallNumber!!)
                showCallWarningDialog = false
                pendingCallNumber = null
            },
            onDismiss = {
                showCallWarningDialog = false
                pendingCallNumber = null
            }
        )
    }
    if (showEditNameDialog) {
        EditContactNameDialog(
            contact = contact,
            onSave = { newName ->
                onEditContactName(contact, newName)
                showEditNameDialog = false
            },
            onDismiss = { showEditNameDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentCallVerticalItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {},
    onContactImageClick: (Contact) -> Unit = {},
    isInternationalDetectionEnabled: Boolean = true,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier,
    selectedContacts: List<Contact> = emptyList(),
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS),
    onExecuteAction: (Context, String, String) -> Unit,
    homeCountryCode: String? = null
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var showContactActionsDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val isInternational = PhoneNumberUtils.isInternationalNumber(context, ContactUtils.getPrimaryPhoneNumber(contact), isInternationalDetectionEnabled, homeCountryCode)
    
    // Phone number selection dialog
    if (showPhoneNumberDialog) {
        PhoneNumberSelectionDialog(
            contact = contact,
            onPhoneNumberSelected = { selectedNumber: String ->
                val contactWithSelectedNumber = contact.copy(
                    phoneNumber = selectedNumber,
                    phoneNumbers = listOf(selectedNumber)
                )
                when (dialogAction) {
                    "call" -> onContactClick(contactWithSelectedNumber)
                    "whatsapp" -> onWhatsAppClick(contactWithSelectedNumber)
                    "sms" -> onExecuteAction(context, "Messages", selectedNumber)
                    "telegram" -> onExecuteAction(context, "Telegram", selectedNumber)
                }
                showPhoneNumberDialog = false
                dialogAction = null
            },
            onDismiss = {
                showPhoneNumberDialog = false
                dialogAction = null
            },
            selectedContacts = selectedContacts,
            onAddContact = { contactWithSelectedNumber ->
                onContactClick(contactWithSelectedNumber)
            },
            onRemoveContact = { contactWithSelectedNumber ->
                onContactClick(contactWithSelectedNumber)
            }
        )
    }
    
    // Contact actions dialog for long press
    if (showContactActionsDialog) {
        ContactActionsDialog(
            contact = contact,
            onCall = { contactToCall ->
                if (contactToCall.phoneNumbers.size > 1) {
                    dialogAction = "call"
                    showPhoneNumberDialog = true
                } else {
                    onContactClick(contactToCall)
                }
                showContactActionsDialog = false
            },
            onSms = { contactToSms ->
                if (contactToSms.phoneNumbers.size > 1) {
                    dialogAction = "sms"
                    showPhoneNumberDialog = true
                } else {
                    onExecuteAction(context, "Messages", contactToSms.phoneNumber)
                }
                showContactActionsDialog = false
            },
            onWhatsApp = { contactToWhatsApp ->
                if (contactToWhatsApp.phoneNumbers.size > 1) {
                    dialogAction = "whatsapp"
                    showPhoneNumberDialog = true
                } else {
                    onWhatsAppClick(contactToWhatsApp)
                }
                showContactActionsDialog = false
            },
            onTelegram = { contactToTelegram ->
                if (contactToTelegram.phoneNumbers.size > 1) {
                    dialogAction = "telegram"
                    showPhoneNumberDialog = true
                } else {
                    onExecuteAction(context, "Telegram", contactToTelegram.phoneNumber)
                }
                showContactActionsDialog = false
            },
            onDismiss = {
                showContactActionsDialog = false
            },
            availableMessagingApps = availableMessagingApps
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { 
                    if (contact.phoneNumbers.size > 1) {
                        dialogAction = if (isInternational == true) "whatsapp" else "call"
                        showPhoneNumberDialog = true
                    } else {
                        if (isInternational == true) {
                            onWhatsAppClick(contact)
                        } else if (isInternational == false) {
                            onContactClick(contact)
                        } else {
                            // TODO: Prompt user for country code
                        }
                    }
                },
                onLongClick = {
                    showContactActionsDialog = true
                }
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contact Photo - clickable to open in contacts app
        if (contact.photoUri != null && !imageLoadFailed) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(contact.photoUri)
                        .crossfade(true)
                        .build(),
                    onError = { imageLoadFailed = true }
                ),
                contentDescription = "Contact photo",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onContactImageClick(contact) },
                contentScale = ContentScale.Crop
            )
        } else {
            // Default avatar - clickable to open in contacts app
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onContactImageClick(contact) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.let { if (!it.isLetter()) "#" else it.toString().uppercase() } ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Contact Name and Call Type
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            // Display call type icon and timestamp if available
            if (contact.callType != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when (contact.callType) {
                            "missed" -> Icons.AutoMirrored.Filled.CallMissed
                            "incoming" -> Icons.AutoMirrored.Filled.CallReceived
                            "outgoing" -> Icons.AutoMirrored.Filled.CallMade
                            else -> Icons.Default.Call
                        },
                        contentDescription = contact.callType.replaceFirstChar { it.uppercase() },
                        tint = when (contact.callType) {
                            "missed" -> Color(0xFFE57373) // Subtle red
                            "incoming" -> Color(0xFF81C784) // Subtle green
                            "outgoing" -> Color(0xFF64B5F6) // Subtle blue
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = contact.callTimestamp?.let { timestamp ->
                            com.tk.quickcontacts.utils.ContactUtils.formatCallTimestamp(timestamp)
                        } ?: contact.callType.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (contact.callType) {
                            "missed" -> MaterialTheme.colorScheme.error
                            "incoming" -> MaterialTheme.colorScheme.secondary
                            "outgoing" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
        
        // For international: Phone button (to call), for domestic: messaging app button
        IconButton(
            onClick = { 
                if (contact.phoneNumbers.size > 1) {
                    dialogAction = if (isInternational == true) "call" else "whatsapp"
                    showPhoneNumberDialog = true
                } else {
                    if (isInternational == true) {
                        onContactClick(contact)
                    } else if (isInternational == false) {
                        onWhatsAppClick(contact)
                    } else {
                        // TODO: Prompt user for country code
                    }
                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            if (isInternational == true) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call ${contact.name}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (isInternational == false) {
                when (defaultMessagingApp) {
                    MessagingApp.WHATSAPP -> {
                        Icon(
                            painter = painterResource(id = R.drawable.whatsapp_icon),
                            contentDescription = "WhatsApp ${contact.name}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    MessagingApp.SMS -> {
                        Icon(
                            painter = painterResource(id = R.drawable.sms_icon),
                            contentDescription = "SMS ${contact.name}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    MessagingApp.TELEGRAM -> {
                        Icon(
                            painter = painterResource(id = R.drawable.telegram_icon),
                            contentDescription = "Telegram ${contact.name}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                // TODO: Prompt user for country code
            }
        }
    }
}

@Composable
fun RecentCallItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {},
    isInternationalDetectionEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    homeCountryCode: String? = null
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isInternational = PhoneNumberUtils.isInternationalNumber(context, ContactUtils.getPrimaryPhoneNumber(contact), isInternationalDetectionEnabled, homeCountryCode)
    
    Column(
        modifier = modifier
            .clickable { 
                if (isInternational == true) {
                    onWhatsAppClick(contact)
                } else if (isInternational == false) {
                    onContactClick(contact)
                } else {
                    // TODO: Prompt user for country code
                }
            }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Contact Photo
        if (contact.photoUri != null && !imageLoadFailed) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(contact.photoUri)
                        .crossfade(true)
                        .build(),
                    onError = { imageLoadFailed = true }
                ),
                contentDescription = "Contact photo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Default avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.let { if (!it.isLetter()) "#" else it.toString().uppercase() } ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(72.dp)
        )
    }
} 