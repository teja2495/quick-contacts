package com.tk.quickcontacts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
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
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

@Composable
fun PhoneNumberSelectionDialog(
    contact: Contact,
    onPhoneNumberSelected: (String) -> Unit,
    onDismiss: () -> Unit
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
            Column {
                Text(
                    text = "Choose a phone number for ${contact.name}:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                contact.phoneNumbers.forEach { phoneNumber ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onPhoneNumberSelected(phoneNumber) },
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
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = phoneNumber,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
    if (availableMessagingApps.contains(MessagingApp.SMS)) availableActions.add("SMS")
    
    // Get default messaging app name
    val messagingAppName = when (defaultMessagingApp) {
        MessagingApp.WHATSAPP -> "WhatsApp"
        MessagingApp.SMS -> "SMS"
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
                
                // Two rows of FilterChips
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
                        onClick = { 
                            if (availableMessagingApps.contains(MessagingApp.WHATSAPP)) {
                                selectedPrimary = "WhatsApp"
                            }
                        },
                        label = { Text("WhatsApp") },
                        selected = selectedPrimary == "WhatsApp",
                        enabled = availableMessagingApps.contains(MessagingApp.WHATSAPP),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        onClick = { 
                            if (availableMessagingApps.contains(MessagingApp.TELEGRAM)) {
                                selectedPrimary = "Telegram"
                            }
                        },
                        label = { Text("Telegram") },
                        selected = selectedPrimary == "Telegram",
                        enabled = availableMessagingApps.contains(MessagingApp.TELEGRAM),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                    )
                    FilterChip(
                        onClick = { selectedPrimary = "SMS" },
                        label = { Text("SMS") },
                        selected = selectedPrimary == "SMS",
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                    )
                }
                
                // Secondary action selection
                Text(
                    text = "Secondary (Tap Icon)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                // Two rows of FilterChips
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
                        onClick = { 
                            if (availableMessagingApps.contains(MessagingApp.WHATSAPP)) {
                                selectedSecondary = "WhatsApp"
                            }
                        },
                        label = { Text("WhatsApp") },
                        selected = selectedSecondary == "WhatsApp",
                        enabled = availableMessagingApps.contains(MessagingApp.WHATSAPP),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        onClick = { 
                            if (availableMessagingApps.contains(MessagingApp.TELEGRAM)) {
                                selectedSecondary = "Telegram"
                            }
                        },
                        label = { Text("Telegram") },
                        selected = selectedSecondary == "Telegram",
                        enabled = availableMessagingApps.contains(MessagingApp.TELEGRAM),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                    )
                    FilterChip(
                        onClick = { selectedSecondary = "SMS" },
                        label = { Text("SMS") },
                        selected = selectedSecondary == "SMS",
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                    )
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
fun ContactList(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean,
    onDeleteContact: (Contact) -> Unit,
    onMove: (Int, Int) -> Unit = { _, _ -> },
    onWhatsAppClick: (Contact) -> Unit = {},
    onContactImageClick: (Contact) -> Unit = {},
    onLongClick: (Contact) -> Unit = {},
    onSetCustomActions: (Contact, String, String) -> Unit = { _, _, _ -> },
    customActionPreferences: Map<String, CustomActions> = emptyMap(),
    isInternationalDetectionEnabled: Boolean = true,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS)
) {
    val reorderState = rememberReorderableLazyListState(onMove = { from, to ->
        onMove(from.index, to.index)
    })

    LazyColumn(
        state = reorderState.listState,
        modifier = modifier.then(
            if (editMode) {
                Modifier.reorderable(reorderState)
            } else {
                Modifier
            }
        ),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            items = contacts,
            key = { contact -> contact.id }
        ) { contact ->
            if (editMode) {
                ReorderableItem(reorderState, key = contact.id) { isDragging ->
                    ContactItem(
                        contact = contact,
                        onContactClick = onContactClick,
                        modifier = Modifier.fillMaxWidth(),
                        editMode = editMode,
                        onDeleteContact = onDeleteContact,
                        onWhatsAppClick = onWhatsAppClick,
                        onContactImageClick = onContactImageClick,
                        reorderState = reorderState,
                        onLongClick = onLongClick,
                        onSetCustomActions = onSetCustomActions,
                        customActions = customActionPreferences[contact.id],
                        isInternationalDetectionEnabled = isInternationalDetectionEnabled,
                        defaultMessagingApp = defaultMessagingApp,
                        availableMessagingApps = availableMessagingApps
                    )
                }
            } else {
                ContactItem(
                    contact = contact,
                    onContactClick = onContactClick,
                    modifier = Modifier.fillMaxWidth(),
                    editMode = editMode,
                    onDeleteContact = onDeleteContact,
                    onWhatsAppClick = onWhatsAppClick,
                    onContactImageClick = onContactImageClick,
                    reorderState = null,
                    onLongClick = onLongClick,
                    onSetCustomActions = onSetCustomActions,
                    customActions = customActionPreferences[contact.id],
                    isInternationalDetectionEnabled = isInternationalDetectionEnabled,
                    defaultMessagingApp = defaultMessagingApp,
                    availableMessagingApps = availableMessagingApps
                )
            }
        }
    }
}

@Composable
fun RecentCallsSection(
    recentCalls: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {},
    onContactImageClick: (Contact) -> Unit = {},
    onExpandedChange: (Boolean) -> Unit = {},
    isInternationalDetectionEnabled: Boolean = true,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier
) {
    if (recentCalls.isNotEmpty()) {
        var isExpanded by remember { mutableStateOf(false) }
        val rotationAngle by animateFloatAsState(
            targetValue = if (isExpanded) 180f else 0f,
            animationSpec = tween(300),
            label = "arrow_rotation"
        )
        
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .offset(y = (-8).dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 0.dp)
                ) {
                    Text(
                        text = "Recent Calls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Expand/Collapse arrow (only show if more than 2 items)
                    if (recentCalls.size > 2) {
                        IconButton(
                            onClick = { 
                                isExpanded = !isExpanded
                                onExpandedChange(isExpanded)
                            },
                            modifier = Modifier.padding(end = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(rotationAngle)
                            )
                        }
                    }
                }
                
                // Vertical list of recent calls
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.offset(y = (-4).dp)
                ) {
                    recentCalls.take(2).forEach { contact ->
                        RecentCallVerticalItem(
                            contact = contact,
                            onContactClick = onContactClick,
                            onWhatsAppClick = onWhatsAppClick,
                            onContactImageClick = onContactImageClick,
                            isInternationalDetectionEnabled = isInternationalDetectionEnabled,
                            defaultMessagingApp = defaultMessagingApp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.offset(y = (-4).dp)
                    ) {
                        recentCalls.drop(2).forEach { contact ->
                            RecentCallVerticalItem(
                                contact = contact,
                                onContactClick = onContactClick,
                                onWhatsAppClick = onWhatsAppClick,
                                onContactImageClick = onContactImageClick,
                                isInternationalDetectionEnabled = isInternationalDetectionEnabled,
                                defaultMessagingApp = defaultMessagingApp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentCallVerticalItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {},
    onContactImageClick: (Contact) -> Unit = {},
    isInternationalDetectionEnabled: Boolean = true,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val isInternational = isInternationalNumber(context, contact.getPrimaryPhoneNumber(), isInternationalDetectionEnabled)
    
    // Phone number selection dialog
    if (showPhoneNumberDialog) {
        PhoneNumberSelectionDialog(
            contact = contact,
            onPhoneNumberSelected = { selectedNumber: String ->
                val contactWithSelectedNumber = contact.copy(phoneNumber = selectedNumber)
                when (dialogAction) {
                    "call" -> onContactClick(contactWithSelectedNumber)
                    "whatsapp" -> onWhatsAppClick(contactWithSelectedNumber)
                }
                showPhoneNumberDialog = false
                dialogAction = null
            },
            onDismiss = {
                showPhoneNumberDialog = false
                dialogAction = null
            }
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
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
        
        // Contact Name - for international: open WhatsApp, for domestic: call
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .clickable { 
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
                }
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
    val isInternational = isInternationalNumber(context, contact.getPrimaryPhoneNumber(), isInternationalDetectionEnabled)
    
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
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS)
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var showActionToggleDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val isInternational = isInternationalNumber(context, contact.getPrimaryPhoneNumber(), isInternationalDetectionEnabled)
    
    // Phone number selection dialog
    if (showPhoneNumberDialog) {
        PhoneNumberSelectionDialog(
            contact = contact,
            onPhoneNumberSelected = { selectedNumber: String ->
                val contactWithSelectedNumber = contact.copy(phoneNumber = selectedNumber)
                // Route based on which action triggered the dialog
                when (dialogAction) {
                    "call", "whatsapp", "sms", "telegram" -> {
                        // Check if this was triggered by primary or secondary action
                        val primaryAction = customActions?.primaryAction ?: if (isInternationalDetectionEnabled && isInternational) {
                            when (defaultMessagingApp) {
                                MessagingApp.WHATSAPP -> "WhatsApp"
                                MessagingApp.SMS -> "SMS"
                                MessagingApp.TELEGRAM -> "Telegram"
                            }
                        } else {
                            "Call"
                        }
                        
                        // If the dialog action matches the primary action, it was triggered by primary action
                        if (dialogAction == primaryAction.lowercase()) {
                            onContactClick(contactWithSelectedNumber) // Execute primary action
                        } else {
                            onWhatsAppClick(contactWithSelectedNumber) // Execute secondary action
                        }
                    }
                }
                showPhoneNumberDialog = false
                dialogAction = null
            },
            onDismiss = {
                showPhoneNumberDialog = false
                dialogAction = null
            }
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
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .combinedClickable(
                enabled = true, // Always enabled to handle both edit and normal mode
                onClick = { 
                    if (!editMode) {
                        // Normal mode: perform contact action
                        // Get default messaging app name
                        val messagingAppName = when (defaultMessagingApp) {
                            MessagingApp.WHATSAPP -> "WhatsApp"
                            MessagingApp.SMS -> "SMS"
                            MessagingApp.TELEGRAM -> "Telegram"
                        }
                        
                        // Use custom primary action or determine based on new default logic
                        val primaryAction = customActions?.primaryAction ?: if (isInternationalDetectionEnabled && isInternational) {
                            messagingAppName  // International: Primary = messaging app
                        } else {
                            "Call"  // Default: Primary = Call
                        }
                        
                        if (contact.phoneNumbers.size > 1) {
                            dialogAction = primaryAction.lowercase()
                            showPhoneNumberDialog = true
                        } else {
                            // Primary action always calls onContactClick (which executes primary action)
                            onContactClick(contact)
                        }
                    }
                    // In edit mode, clicks are disabled for contact actions
                },
                onLongClick = {
                    if (editMode) {
                        // Only show action toggle dialog in edit mode
                        showActionToggleDialog = true
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
                        .padding(end = 8.dp)
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
                            .clip(CircleShape)
                            .clickable { onContactImageClick(contact) },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Default avatar - clickable to open in contacts app
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onContactImageClick(contact) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Contact Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                // Show primary action text (always visible, not just in edit mode)
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
                
                // Show phone number in edit mode
                if (editMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show additional phone numbers if there are multiple
                    if (contact.phoneNumbers.size > 1) {
                        contact.phoneNumbers.forEachIndexed { index, number ->
                            if (index > 0 && number != contact.phoneNumber) {
                                Text(
                                    text = number,
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
                            // Secondary action always calls onWhatsAppClick (which executes secondary action)
                            onWhatsAppClick(contact)
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
}

