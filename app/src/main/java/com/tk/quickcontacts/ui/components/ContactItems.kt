package com.tk.quickcontacts.ui.components

import com.tk.quickcontacts.ui.theme.ExpressiveMotion

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
import androidx.compose.material.icons.rounded.Sms
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.tween
import android.content.Context
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.BuildConfig
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
    dragHandleModifier: Modifier = Modifier,
    onLongClick: (Contact) -> Unit = {},
    onOpenActionEditor: (Contact) -> Unit = {},
    customActions: CustomActions? = null,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SIGNAL, MessagingApp.SMS),
    availableActions: Set<String> = emptySet(),
    selectedContacts: List<Contact> = emptyList(),
    onExecuteAction: (Context, String, String) -> Unit,
    onUpdateContactNumber: (Contact, String) -> Unit = { _, _ -> },
    hasSeenCallWarning: Boolean = true,
    onMarkCallWarningSeen: (() -> Unit)? = null,
    onEditContactName: (Contact, String) -> Unit,
    callActivity: Contact? = null, // New parameter for call activity data
    showButtonActionLabels: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.large,
    isDragging: Boolean = false
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var showContactActionsDialog by remember { mutableStateOf(false) }
    var pendingActionForNumberSelection by remember { mutableStateOf<String?>(null) }
    var showCallWarningDialog by remember { mutableStateOf(false) }
    var pendingCallAction by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val resolvedActions = resolveQuickContactActions(
        customActions = customActions,
        defaultMessagingApp = defaultMessagingApp
    )
    val cardTapActionLabel = when (resolvedActions.cardTapAction) {
        QuickContactAction.CALL -> "Call"
        QuickContactAction.MESSAGE -> "Message"
        QuickContactAction.GOOGLE_MEET -> "Meet"
        QuickContactAction.WHATSAPP_CHAT -> "WhatsApp"
        QuickContactAction.TELEGRAM_CHAT -> "Telegram"
        QuickContactAction.SIGNAL_CHAT -> "Signal"
        QuickContactAction.WHATSAPP_VOICE_CALL -> "WhatsApp Audio"
        QuickContactAction.TELEGRAM_VOICE_CALL -> "Telegram Audio"
        QuickContactAction.SIGNAL_VOICE_CALL -> "Signal Audio"
        QuickContactAction.WHATSAPP_VIDEO_CALL -> "WhatsApp Video"
        QuickContactAction.TELEGRAM_VIDEO_CALL -> "Telegram Video"
        QuickContactAction.SIGNAL_VIDEO_CALL -> "Signal Video"
        QuickContactAction.ALL_OPTIONS -> "All Options"
        QuickContactAction.NONE -> null
        else -> resolvedActions.cardTapAction
    }

    fun executeResolvedAction(action: String, phoneNumber: String) {
        when {
            action == QuickContactAction.NONE -> Unit
            action == QuickContactAction.ALL_OPTIONS -> showContactActionsDialog = true
            actionNeedsPhoneNumber(action) && contact.phoneNumbers.size > 1 -> {
                pendingActionForNumberSelection = action
                showPhoneNumberDialog = true
            }
            action == QuickContactAction.CALL && !hasSeenCallWarning -> {
                pendingCallAction = action to phoneNumber
                showCallWarningDialog = true
            }
            else -> onExecuteAction(context, action, phoneNumber)
        }
    }
    
    // Phone number selection dialog
    if (showPhoneNumberDialog) {
        PhoneNumberSelectionDialog(
            contact = contact,
            onPhoneNumberSelected = { selectedNumber: String ->
                // Update the quick list contact to have only the selected number
                onUpdateContactNumber(contact, selectedNumber)
                pendingActionForNumberSelection?.let { selectedAction ->
                    executeResolvedAction(selectedAction, selectedNumber)
                }
                showPhoneNumberDialog = false
                pendingActionForNumberSelection = null
            },
            onDismiss = {
                showPhoneNumberDialog = false
                pendingActionForNumberSelection = null
            },
            selectedContacts = selectedContacts,
            onAddContact = null,
            onRemoveContact = null,
            hideIcons = true,
            showInstructionText = true
        )
    }
    
    if (showContactActionsDialog) {
        ContactActionsGridDialog(
            contact = contact,
            availableActions = availableActions,
            onActionSelected = { action, phoneNumber ->
                executeResolvedAction(action, phoneNumber)
            },
            onDismiss = { showContactActionsDialog = false },
            onAddToQuickList = null,
            isInQuickList = true
        )
    }
    
    val dragElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "drag_elevation"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.surfaceContainerHighest
            editMode -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = ExpressiveMotion.defaultEffects(),
        label = "contact_container"
    )
    val nameColor by animateColorAsState(
        targetValue = if (editMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = ExpressiveMotion.defaultEffects(),
        label = "contact_name_color"
    )
    val horizontalEnter = fadeIn(ExpressiveMotion.defaultEffects()) +
        expandHorizontally(ExpressiveMotion.defaultSpatialNoBounce(), expandFrom = Alignment.Start)
    val horizontalExit = fadeOut(ExpressiveMotion.fastEffects()) +
        shrinkHorizontally(ExpressiveMotion.defaultSpatialNoBounce(), shrinkTowards = Alignment.Start)
    val verticalEnter = fadeIn(ExpressiveMotion.defaultEffects()) +
        expandVertically(ExpressiveMotion.defaultSpatialNoBounce(), expandFrom = Alignment.Top)
    val verticalExit = fadeOut(ExpressiveMotion.fastEffects()) +
        shrinkVertically(ExpressiveMotion.defaultSpatialNoBounce(), shrinkTowards = Alignment.Top)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(dragElevation, shape, clip = false)
            .clip(shape)
            .then(
                if (editMode) {
                    Modifier.clickable { onOpenActionEditor(contact) }
                } else {
                    Modifier.combinedClickable(
                        enabled = true,
                        onClick = {
                            executeResolvedAction(resolvedActions.cardTapAction, contact.phoneNumber)
                        },
                        onLongClick = {
                            executeResolvedAction(resolvedActions.cardLongPressAction, contact.phoneNumber)
                        }
                    )
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle slides in from the start edge in edit mode.
            AnimatedVisibility(visible = editMode, enter = horizontalEnter, exit = horizontalExit) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .then(dragHandleModifier)
                        .size(28.dp)
                )
            }

            // Contact photo collapses away in edit mode.
            AnimatedVisibility(visible = !editMode, enter = horizontalEnter, exit = horizontalExit) {
                ContactAvatar(
                    contact = contact,
                    size = 48.dp,
                    modifier = Modifier.padding(end = 14.dp),
                    onClick = { onContactImageClick(contact) }
                )
            }

            // Contact Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(enabled = editMode) { showEditNameDialog = true }
                )
                
                AnimatedVisibility(visible = !editMode, enter = verticalEnter, exit = verticalExit) {
                Column {
                if (BuildConfig.DISABLE_RECENT_CALLS) {
                    cardTapActionLabel?.let { actionLabel ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                } else if (callActivity?.callType != null && callActivity.callTimestamp != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (callActivity.callSource == "whatsapp") {
                            Icon(
                                painter = painterResource(R.drawable.whatsapp_icon),
                                contentDescription = "WhatsApp call",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                imageVector = when (callActivity.callType) {
                                    "missed" -> Icons.AutoMirrored.Filled.CallMissed
                                    "rejected" -> Icons.AutoMirrored.Filled.CallReceived
                                    "incoming" -> Icons.AutoMirrored.Filled.CallReceived
                                    "outgoing" -> Icons.AutoMirrored.Filled.CallMade
                                    else -> Icons.Default.Call
                                },
                                contentDescription = callActivity.callType.replaceFirstChar { it.uppercase() },
                                tint = callTypeColor(callActivity.callType),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = com.tk.quickcontacts.utils.ContactUtils.formatCallTimestamp(callActivity.callTimestamp!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
                }
                }

                // Phone numbers expand in under the name in edit mode.
                AnimatedVisibility(visible = editMode, enter = verticalEnter, exit = verticalExit) {
                Column {
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
            }

            AnimatedContent(
                targetState = editMode,
                transitionSpec = {
                    (fadeIn(ExpressiveMotion.defaultEffects()) +
                        scaleIn(ExpressiveMotion.fastSpatial(), initialScale = 0.6f)) togetherWith
                        (fadeOut(ExpressiveMotion.fastEffects()) +
                            scaleOut(ExpressiveMotion.fastSpatial(), targetScale = 0.6f)) using
                        SizeTransform(clip = false) { _, _ -> ExpressiveMotion.defaultSpatialNoBounce() }
                },
                contentAlignment = Alignment.CenterEnd,
                label = "contact_trailing"
            ) { isEditing ->
            if (isEditing) {
                IconButton(onClick = { onDeleteContact(contact) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Contact",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (resolvedActions.firstButtonTapAction != QuickContactAction.NONE) {
                        QuickContactActionButton(
                            action = resolvedActions.firstButtonTapAction,
                            contactName = contact.name,
                            onTap = { executeResolvedAction(resolvedActions.firstButtonTapAction, contact.phoneNumber) },
                            onLongPress = { executeResolvedAction(resolvedActions.firstButtonLongPressAction, contact.phoneNumber) },
                            showLabel = showButtonActionLabels
                        )
                    }
                    if (resolvedActions.secondButtonTapAction != QuickContactAction.NONE) {
                        QuickContactActionButton(
                            action = resolvedActions.secondButtonTapAction,
                            contactName = contact.name,
                            onTap = { executeResolvedAction(resolvedActions.secondButtonTapAction, contact.phoneNumber) },
                            onLongPress = { executeResolvedAction(resolvedActions.secondButtonLongPressAction, contact.phoneNumber) },
                            showLabel = showButtonActionLabels
                        )
                    }
                }
            }
            }
        }
    }
    // Show call warning dialog if needed
    if (showCallWarningDialog && pendingCallAction != null) {
        CallWarningDialog(
            onConfirm = {
                onMarkCallWarningSeen?.invoke()
                val pending = pendingCallAction
                if (pending != null) {
                    onExecuteAction(context, pending.first, pending.second)
                }
                showCallWarningDialog = false
                pendingCallAction = null
            },
            onDismiss = {
                showCallWarningDialog = false
                pendingCallAction = null
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
private fun QuickContactActionButton(
    action: String,
    contactName: String,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    showLabel: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(if (showLabel) 74.dp else 48.dp)
            .clip(if (showLabel) MaterialTheme.shapes.large else CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showLabel) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                QuickContactActionIcon(
                    action = action,
                    contentDescription = "$action for $contactName",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        } else {
            QuickContactActionIcon(
                action = action,
                contentDescription = "$action for $contactName",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentCallVerticalItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {},
    onContactImageClick: (Contact) -> Unit = {},
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier,
    selectedContacts: List<Contact> = emptyList(),
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SIGNAL, MessagingApp.SMS),
    availableActions: Set<String> = emptySet(),
    onExecuteAction: (Context, String, String) -> Unit,
    onAddToQuickList: ((Contact) -> Unit)? = null,
    onRemoveFromQuickList: ((Contact) -> Unit)? = null,
    getLastShownPhoneNumber: (String) -> String? = { null },
    setLastShownPhoneNumber: (String, String) -> Unit = { _, _ -> },
    nowMs: Long = -1L
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var showContactActionsDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
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
                    "call" -> if (contact.callSource == "whatsapp") onWhatsAppClick(contactWithSelectedNumber) else onContactClick(contactWithSelectedNumber)
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
    
    if (showContactActionsDialog) {
        ContactActionsGridDialog(
            contact = contact,
            availableActions = availableActions,
            onActionSelected = { action, phoneNumber ->
                when {
                    action == QuickContactAction.NONE || action == QuickContactAction.ALL_OPTIONS -> Unit
                    action == QuickContactAction.CALL -> if (contact.callSource == "whatsapp") onWhatsAppClick(contact.copy(phoneNumber = phoneNumber)) else onContactClick(contact.copy(phoneNumber = phoneNumber))
                    else -> onExecuteAction(context, action, phoneNumber)
                }
                showContactActionsDialog = false
            },
            onDismiss = { showContactActionsDialog = false },
            onAddToQuickList = { contactToAdd -> onAddToQuickList?.invoke(contactToAdd) },
            onRemoveFromQuickList = { contactToRemove -> onRemoveFromQuickList?.invoke(contactToRemove) },
            isInQuickList = selectedContacts.any { it.id == contact.id },
            getLastShownPhoneNumber = getLastShownPhoneNumber,
            setLastShownPhoneNumber = setLastShownPhoneNumber
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { 
                    showContactActionsDialog = true
                },
                onLongClick = {
                    showContactActionsDialog = true
                }
            )
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            contact = contact,
            size = 40.dp,
            onClick = { onContactImageClick(contact) }
        )

        Spacer(modifier = Modifier.width(12.dp))
        
        // Contact Name and Call Type
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                            "rejected" -> Icons.AutoMirrored.Filled.CallReceived
                            "incoming" -> Icons.AutoMirrored.Filled.CallReceived
                            "outgoing" -> Icons.AutoMirrored.Filled.CallMade
                            else -> Icons.Default.Call
                        },
                        contentDescription = contact.callType.replaceFirstChar { it.uppercase() },
                        tint = callTypeColor(contact.callType),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = contact.callTimestamp?.let { timestamp ->
                            if (nowMs >= 0L) {
                                com.tk.quickcontacts.utils.ContactUtils.formatCallTimestamp(timestamp, nowMs)
                            } else {
                                com.tk.quickcontacts.utils.ContactUtils.formatCallTimestamp(timestamp)
                            }
                        } ?: contact.callType.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (contact.callType == "missed") MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
        
        IconButton(
            onClick = { 
                if (contact.phoneNumbers.size > 1) {
                    dialogAction = "call"
                    showPhoneNumberDialog = true
                } else {
                    if (contact.callSource == "whatsapp") onWhatsAppClick(contact) else onContactClick(contact)
                }
            },
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            if (contact.callSource == "whatsapp") {
                Icon(
                    painter = painterResource(R.drawable.whatsapp_voice_call_icon),
                    contentDescription = "WhatsApp call ${contact.name}",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call ${contact.name}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun RecentCallItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .clickable { onContactClick(contact) }
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
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
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
