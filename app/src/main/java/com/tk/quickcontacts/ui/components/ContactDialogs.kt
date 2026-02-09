package com.tk.quickcontacts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.R
import com.tk.quickcontacts.models.CustomActions
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.utils.ContactActionAvailability
import com.tk.quickcontacts.utils.PhoneNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
        Pair("Signal", availableMessagingApps.contains(MessagingApp.SIGNAL)),
        Pair("All Options", true),
        Pair("None", true)
    ).filter { it.second } // Only include available options
}

private fun buildSecondaryChipsList(availableMessagingApps: Set<MessagingApp>): List<Pair<String, Boolean>> {
    return listOf(
        Pair("Call", true),
        Pair("Messages", true),
        Pair("WhatsApp", availableMessagingApps.contains(MessagingApp.WHATSAPP)),
        Pair("Telegram", availableMessagingApps.contains(MessagingApp.TELEGRAM)),
        Pair("Signal", availableMessagingApps.contains(MessagingApp.SIGNAL)),
        Pair("All Options", true),
        Pair("None", true)
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

private val DialogScreenPadding = 12.dp
private val DialogBottomPadding = 58.dp

private fun buildActionRows(actions: List<String>): List<List<String>> {
    val normal = actions.filter {
        it != QuickContactAction.ALL_OPTIONS && it != QuickContactAction.NONE
    }
    val fullWidthRow = actions.filter {
        it == QuickContactAction.ALL_OPTIONS || it == QuickContactAction.NONE
    }
    return normal.chunked(3) + if (fullWidthRow.isNotEmpty()) listOf(fullWidthRow) else emptyList()
}

@Composable
fun ActionGridDialog(
    title: String,
    actions: List<String>,
    onActionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val config = LocalConfiguration.current
    val maxSheetHeight = (config.screenHeightDp * 0.85f).dp
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss)
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .wrapContentHeight()
                    .padding(start = DialogScreenPadding, end = DialogScreenPadding, top = DialogScreenPadding, bottom = DialogBottomPadding)
                    .align(Alignment.BottomCenter),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_close)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (rowActions in buildActionRows(actions)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (action in rowActions) {
                                    ContactActionButton(
                                        action = action,
                                        onClick = {
                                            onActionSelected(action)
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        useFullWidth = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
                
                // Filter out duplicate phone numbers (when normalized)
                val uniquePhoneNumbers = contact.phoneNumbers.distinctBy { phoneNumber ->
                    com.tk.quickcontacts.utils.PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
                }
                
                uniquePhoneNumbers.forEach { phoneNumber ->
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
    isCurrentlySwapped: Boolean,
    customActions: CustomActions? = null,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SIGNAL, MessagingApp.SMS),
    onConfirm: (primaryAction: String, secondaryAction: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Available actions based on installed apps
    val availableActions = mutableListOf("Call")
    if (availableMessagingApps.contains(MessagingApp.WHATSAPP)) availableActions.add("WhatsApp")
    if (availableMessagingApps.contains(MessagingApp.TELEGRAM)) availableActions.add("Telegram")
    if (availableMessagingApps.contains(MessagingApp.SIGNAL)) availableActions.add("Signal")
    if (availableMessagingApps.contains(MessagingApp.SMS)) availableActions.add("Messages")
    
    val messagingAppName = when (defaultMessagingApp) {
        MessagingApp.WHATSAPP -> "WhatsApp"
        MessagingApp.SMS -> "Messages"
        MessagingApp.TELEGRAM -> "Telegram"
        MessagingApp.SIGNAL -> "Signal"
    }
    
    val currentPrimary = customActions?.primaryAction ?: "Call"
    val currentSecondary = customActions?.secondaryAction ?: messagingAppName
    
    // State for selected actions
    var selectedPrimary by remember { mutableStateOf(currentPrimary) }
    var selectedSecondary by remember { mutableStateOf(currentSecondary) }
    
    // Check if configuration has changed
    val hasChanged = selectedPrimary != currentPrimary || selectedSecondary != currentSecondary
    
    // Check if selections are valid (different actions, but "None" can be selected for both)
    val isValidSelection = selectedPrimary == "None" || selectedSecondary == "None" || selectedPrimary != selectedSecondary
    
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
    onSignal: (Contact) -> Unit,
    onDismiss: () -> Unit,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SIGNAL, MessagingApp.SMS),
    onAddToQuickList: ((Contact) -> Unit)? = null,
    isInQuickList: Boolean = false,
    onAddToContacts: ((String) -> Unit)? = null,
    getLastShownPhoneNumber: (String) -> String? = { null },
    setLastShownPhoneNumber: (String, String) -> Unit = { _, _ -> }
) {
    val isUnknownContact = contact.id.startsWith("search_number_") || contact.id.startsWith("call_history_")
    var imageLoadFailed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val maxCardHeight = (config.screenHeightDp * 0.72f).dp

    val phoneNumbers = contact.phoneNumbers
    val hasMultipleNumbers = phoneNumbers.size > 1
    var selectedPhoneIndex by remember(contact.id, phoneNumbers) {
        val lastShown = getLastShownPhoneNumber(contact.id)
        val idx = if (lastShown == null) 0 else phoneNumbers.indexOfFirst { PhoneNumberUtils.isSameNumber(it, lastShown) }
        mutableStateOf(if (idx < 0) 0 else idx.coerceIn(0, (phoneNumbers.size - 1).coerceAtLeast(0)))
    }
    val selectedPhoneNumber = phoneNumbers.getOrNull(selectedPhoneIndex) ?: contact.phoneNumbers.firstOrNull()

    LaunchedEffect(selectedPhoneIndex, phoneNumbers, hasMultipleNumbers) {
        if (hasMultipleNumbers && selectedPhoneIndex in phoneNumbers.indices) {
            setLastShownPhoneNumber(contact.id, phoneNumbers[selectedPhoneIndex])
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(max = maxCardHeight),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            selectedPhoneNumber?.let { phoneNumber ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (hasMultipleNumbers && selectedPhoneIndex > 0) {
                                        IconButton(
                                            onClick = { selectedPhoneIndex = selectedPhoneIndex - 1 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronLeft,
                                                contentDescription = "Previous number",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else if (hasMultipleNumbers) {
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }
                                    Text(
                                        text = PhoneNumberUtils.formatPhoneNumberForDisplay(phoneNumber),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (hasMultipleNumbers && selectedPhoneIndex < phoneNumbers.size - 1) {
                                        IconButton(
                                            onClick = { selectedPhoneIndex = selectedPhoneIndex + 1 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Next number",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else if (hasMultipleNumbers) {
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                            val contactWithSelectedNumber = selectedPhoneNumber?.let {
                                contact.copy(phoneNumber = it)
                            } ?: contact
                            if (isUnknownContact && onAddToContacts != null && selectedPhoneNumber != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .clickable {
                                                onAddToContacts(selectedPhoneNumber)
                                                onDismiss()
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PersonAdd,
                                                contentDescription = "Add to Contacts",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Top
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp)
                                        .clickable {
                                            onCall(contactWithSelectedNumber)
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = stringResource(R.string.cd_phone),
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp)
                                        .clickable {
                                            onSms(contactWithSelectedNumber)
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Sms,
                                            contentDescription = stringResource(R.string.cd_sms),
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                if (availableMessagingApps.contains(MessagingApp.WHATSAPP)) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .clickable {
                                                onWhatsApp(contactWithSelectedNumber)
                                                onDismiss()
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.whatsapp_icon),
                                                contentDescription = stringResource(R.string.cd_whatsapp),
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                                if (availableMessagingApps.contains(MessagingApp.TELEGRAM)) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .clickable {
                                                onTelegram(contactWithSelectedNumber)
                                                onDismiss()
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.telegram_icon),
                                                contentDescription = stringResource(R.string.cd_telegram),
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                                if (availableMessagingApps.contains(MessagingApp.SIGNAL)) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .clickable {
                                                onSignal(contactWithSelectedNumber)
                                                onDismiss()
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.signal_icon),
                                                contentDescription = stringResource(R.string.cd_signal),
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (onAddToQuickList != null && !isInQuickList) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .clickable {
                                                onAddToQuickList(contact)
                                                onDismiss()
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add to Quick List",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactActionsGridDialog(
    contact: Contact,
    availableActions: Set<String>,
    onActionSelected: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onAddToQuickList: ((Contact) -> Unit)? = null,
    isInQuickList: Boolean = false,
    onAddToContacts: ((String) -> Unit)? = null,
    getLastShownPhoneNumber: (String) -> String? = { null },
    setLastShownPhoneNumber: (String, String) -> Unit = { _, _ -> }
) {
    val isUnknownContact = contact.id.startsWith("search_number_") || contact.id.startsWith("call_history_")
    val config = LocalConfiguration.current
    val maxSheetHeight = (config.screenHeightDp * 0.85f).dp
    val phoneNumbers = contact.phoneNumbers
    val hasMultipleNumbers = phoneNumbers.size > 1
    var selectedPhoneIndex by remember(contact.id, phoneNumbers) {
        val lastShown = getLastShownPhoneNumber(contact.id)
        val idx = if (lastShown == null) 0 else phoneNumbers.indexOfFirst { PhoneNumberUtils.isSameNumber(it, lastShown) }
        mutableStateOf(if (idx < 0) 0 else idx.coerceIn(0, (phoneNumbers.size - 1).coerceAtLeast(0)))
    }
    val selectedPhoneNumber = phoneNumbers.getOrNull(selectedPhoneIndex) ?: contact.phoneNumbers.firstOrNull() ?: ""

    val context = LocalContext.current
    var contactFilteredActions by remember(selectedPhoneNumber) {
        mutableStateOf(emptySet<String>())
    }
    LaunchedEffect(selectedPhoneNumber, context) {
        contactFilteredActions = withContext(Dispatchers.IO) {
            ContactActionAvailability.getContactAvailableActions(
                context = context,
                phoneNumber = selectedPhoneNumber
            )
        }
    }

    LaunchedEffect(selectedPhoneIndex, phoneNumbers, hasMultipleNumbers) {
        if (hasMultipleNumbers && selectedPhoneIndex in phoneNumbers.indices) {
            setLastShownPhoneNumber(contact.id, phoneNumbers[selectedPhoneIndex])
        }
    }

    val fadeDurationMs = 200
    var visible by remember { mutableStateOf(true) }
    val requestDismiss = { visible = false }
    LaunchedEffect(visible) {
        if (!visible) {
            delay(fadeDurationMs.toLong())
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { requestDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(fadeDurationMs)),
            exit = fadeOut(animationSpec = tween(fadeDurationMs))
        ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { requestDismiss() }
                    )
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .wrapContentHeight()
                    .padding(start = DialogScreenPadding, end = DialogScreenPadding, top = DialogScreenPadding, bottom = DialogBottomPadding)
                    .align(Alignment.BottomCenter),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    var imageLoadFailed by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (contact.photoUri != null && !imageLoadFailed) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(context)
                                                .data(contact.photoUri)
                                                .crossfade(true)
                                                .size(80, 80)
                                                .build(),
                                            onError = { imageLoadFailed = true }
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
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
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = requestDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_close)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasMultipleNumbers && selectedPhoneIndex > 0) {
                            IconButton(
                                onClick = { selectedPhoneIndex = selectedPhoneIndex - 1 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Previous number",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else if (hasMultipleNumbers) {
                            Spacer(modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = PhoneNumberUtils.formatPhoneNumberForDisplay(selectedPhoneNumber),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        if (hasMultipleNumbers && selectedPhoneIndex < phoneNumbers.size - 1) {
                            IconButton(
                                onClick = { selectedPhoneIndex = selectedPhoneIndex + 1 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next number",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else if (hasMultipleNumbers) {
                            Spacer(modifier = Modifier.size(32.dp))
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isUnknownContact && onAddToContacts != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable {
                                        onAddToContacts(selectedPhoneNumber)
                                        requestDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Add to Contacts",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Add to Contacts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (rowActions in QuickContactAction.executableOptionsFiltered(contactFilteredActions).chunked(3)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (action in rowActions) {
                                        ContactActionButton(
                                            action = action,
                                            onClick = {
                                                onActionSelected(action, selectedPhoneNumber)
                                                requestDismiss()
                                            },
                                            modifier = Modifier.weight(1f),
                                            useFullWidth = true
                                        )
                                    }
                                }
                            }
                        }
                        if (onAddToQuickList != null && !isInQuickList) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable {
                                        onAddToQuickList(contact.copy(phoneNumber = selectedPhoneNumber))
                                        requestDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add to Quick List",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Add to Quick List",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
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