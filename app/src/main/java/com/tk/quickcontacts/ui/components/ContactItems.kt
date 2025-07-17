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
    onMarkCallWarningSeen: (() -> Unit)? = null
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var showActionToggleDialog by remember { mutableStateOf(false) }
    var showContactActionsDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf<String?>(null) }
    var showCallWarningDialog by remember { mutableStateOf(false) }
    var pendingCallNumber by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val isInternational = PhoneNumberUtils.isInternationalNumber(context, ContactUtils.getPrimaryPhoneNumber(contact), isInternationalDetectionEnabled)
    
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
            hideIcons = dialogAction == "add"
        )
    }
    
    // Action toggle dialog
    if (showActionToggleDialog) {
        ActionToggleDialog(
            contact = contact,
            isInternational = isInternational,
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
                        val primaryAction = customActions?.primaryAction ?: if (isInternationalDetectionEnabled && isInternational) {
                            messagingAppName  // International: Primary = messaging app
                        } else {
                            "Call"  // Default: Primary = Call
                        }
                        if (contact.phoneNumbers.size > 1) {
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
                                text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
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
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                // Show primary action text (only in normal mode, not in edit mode)
                if (!editMode) {
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Get the primary action for this contact
                    val primaryAction = customActions?.primaryAction ?: if (isInternationalDetectionEnabled && isInternational) {
                        when (defaultMessagingApp) {
                            MessagingApp.WHATSAPP -> "WhatsApp"
                            MessagingApp.SMS -> "SMS"
                            MessagingApp.TELEGRAM -> "Telegram"
                        }
                    } else {
                        "Call"
                    }
                    
                    Text(
                        text = "$primaryAction",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
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
                        val secondaryAction = customActions?.secondaryAction ?: if (isInternationalDetectionEnabled && isInternational) {
                            "Call"  // International: Secondary = Call
                        } else {
                            messagingAppName  // Default: Secondary = messaging app
                        }
                        
                        if (contact.phoneNumbers.size > 1) {
                            dialogAction = secondaryAction.lowercase()
                            showPhoneNumberDialog = true
                        } else {
                            // Use onExecuteAction to match UI label logic
                            onExecuteAction(context, secondaryAction, contact.phoneNumber)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    // Get default messaging app name
                    val messagingAppName = when (defaultMessagingApp) {
                        MessagingApp.WHATSAPP -> "WhatsApp"
                        MessagingApp.SMS -> "SMS"
                        MessagingApp.TELEGRAM -> "Telegram"
                    }
                    
                    // Show secondary action icon based on custom actions or new default logic
                    val secondaryAction = customActions?.secondaryAction ?: if (isInternationalDetectionEnabled && isInternational) {
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
    onExecuteAction: (Context, String, String) -> Unit
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var showContactActionsDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val isInternational = PhoneNumberUtils.isInternationalNumber(context, ContactUtils.getPrimaryPhoneNumber(contact), isInternationalDetectionEnabled)
    
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
                        dialogAction = if (isInternational) "whatsapp" else "call"
                        showPhoneNumberDialog = true
                    } else {
                        if (isInternational) {
                            onWhatsAppClick(contact)
                        } else {
                            onContactClick(contact)
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
                    text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Contact Name - removed clickable modifier since entire row is now clickable
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        )
        
        // For international: Phone button (to call), for domestic: messaging app button
        IconButton(
            onClick = { 
                if (contact.phoneNumbers.size > 1) {
                    dialogAction = if (isInternational) "call" else "whatsapp"
                    showPhoneNumberDialog = true
                } else {
                    if (isInternational) {
                        onContactClick(contact)
                    } else {
                        onWhatsAppClick(contact)
                    }
                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            if (isInternational) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call ${contact.name}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
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
    modifier: Modifier = Modifier
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isInternational = PhoneNumberUtils.isInternationalNumber(context, ContactUtils.getPrimaryPhoneNumber(contact), isInternationalDetectionEnabled)
    
    Column(
        modifier = modifier
            .clickable { 
                if (isInternational) {
                    onWhatsAppClick(contact)
                } else {
                    onContactClick(contact)
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
                    text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
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