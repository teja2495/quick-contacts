package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import android.content.Context
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.ContactsViewModel
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.PhoneNumberSelectionDialog
import com.tk.quickcontacts.utils.ContactActionAvailability
import com.tk.quickcontacts.utils.PhoneNumberUtils

@Composable
fun SearchResultsContent(
    viewModel: ContactsViewModel,
    searchQuery: String,
    searchResults: List<Contact>,
    selectedContacts: List<Contact>,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SIGNAL, MessagingApp.SMS),
    availableActions: Set<String> = emptySet(),
    onExecuteAction: (Context, String, String) -> Unit,
    onAddToContacts: (Context, String) -> Unit = { context, phoneNumber ->
        viewModel.addNewContactToPhone(context, phoneNumber)
    }
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val messagingActionCache = remember(defaultMessagingApp) { mutableMapOf<String, String>() }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = 10.dp),
        reverseLayout = true
    ) {
        if (searchQuery.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Reverse the order to account for reverseLayout = true
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Search Contacts",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Type a name or phone number to search through all your contacts",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (searchResults.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Reverse the order to account for reverseLayout = true
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "No Results Found",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Try a different search term",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Instruction text first (will appear between search results and search bar due to reverseLayout)
            item {
                Text(
                    text = "Tap contact to add it to your quick list",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 10.dp, end = 24.dp, bottom = 0.dp)
                )
            }
            
            // Search results (they're already sorted by priority in ContactService, 
            // with reverseLayout=true, most relevant will be closest to search bar)
            items(
                items = searchResults,
                key = { contact -> contact.id }
            ) { contact ->
                SearchResultItem(
                    contact = contact,
                    onContactClick = { contact ->
                        viewModel.makePhoneCall(context, contact.phoneNumber)
                    },
                    onToggleContact = { contact ->
                        val isSelected = selectedContacts.any { it.id == contact.id }
                        if (isSelected) {
                            viewModel.removeContact(contact)
                        } else {
                            viewModel.addContact(contact)
                        }
                    },
                    onAddContact = { contact ->
                        viewModel.addContact(contact)
                    },
                    onRemoveContact = { contact ->
                        viewModel.removeContact(contact)
                    },
                    onWhatsAppClick = { contact ->
                        viewModel.openMessagingApp(context, contact.phoneNumber)
                    },
                    onContactImageClick = { contact ->
                        viewModel.openContactInContactsApp(context, contact)
                    },
                    selectedContacts = selectedContacts,
                    defaultMessagingApp = defaultMessagingApp,
                    modifier = Modifier.fillMaxWidth(),
                    availableMessagingApps = availableMessagingApps,
                    availableActions = availableActions,
                    onExecuteAction = onExecuteAction,
                    onAddToContacts = onAddToContacts,
                    getLastShownPhoneNumber = viewModel::getLastShownPhoneNumber,
                    setLastShownPhoneNumber = viewModel::setLastShownPhoneNumber,
                    resolveMessagingAction = { candidate ->
                        val numbersKey = candidate.phoneNumbers
                            .ifEmpty { listOf(candidate.phoneNumber) }
                            .joinToString("|") { PhoneNumberUtils.normalizePhoneNumber(it) }
                        val cacheKey = "${candidate.id}|${defaultMessagingApp.name}|$numbersKey"
                        messagingActionCache[cacheKey] ?: run {
                            val defaultMessagingAction = resolveQuickContactActions(null, defaultMessagingApp).secondButtonTapAction
                            val resolvedAction = if (!defaultMessagingApp.requiresContactAccountCheck()) {
                                defaultMessagingAction
                            } else {
                                val allNumbers = candidate.phoneNumbers
                                    .ifEmpty { listOf(candidate.phoneNumber) }
                                    .distinctBy { PhoneNumberUtils.normalizePhoneNumber(it) }
                                val appAction = defaultMessagingApp.toChatAction()
                                val hasAccountOnDefaultApp = allNumbers.any { number ->
                                    ContactActionAvailability.getContactAvailableActions(context, number).contains(appAction)
                                }
                                if (hasAccountOnDefaultApp) defaultMessagingAction else QuickContactAction.MESSAGE
                            }
                            messagingActionCache[cacheKey] = resolvedAction
                            resolvedAction
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onToggleContact: (Contact) -> Unit,
    onAddContact: (Contact) -> Unit,
    onRemoveContact: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit,
    onContactImageClick: (Contact) -> Unit,
    selectedContacts: List<Contact>,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SIGNAL, MessagingApp.SMS),
    availableActions: Set<String> = emptySet(),
    onExecuteAction: (Context, String, String) -> Unit,
    onAddToContacts: (Context, String) -> Unit = { _, _ -> },
    getLastShownPhoneNumber: (String) -> String? = { null },
    setLastShownPhoneNumber: (String, String) -> Unit = { _, _ -> },
    resolveMessagingAction: (Contact) -> String
) {
    val isSelected = selectedContacts.any { it.id == contact.id }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var showContactActionsDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf<String?>(null) }
    var imageLoadFailed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val messagingAction = remember(
        contact.id,
        contact.phoneNumber,
        contact.phoneNumbers,
        defaultMessagingApp
    ) {
        resolveMessagingAction(contact)
    }
    
    // Phone number selection dialog
    if (showPhoneNumberDialog) {
        PhoneNumberSelectionDialog(
            contact = contact,
            onPhoneNumberSelected = { selectedNumber: String ->
                val allNumbers = contact.phoneNumbers.ifEmpty { listOf(selectedNumber) }
                    .distinctBy { com.tk.quickcontacts.utils.PhoneNumberUtils.normalizePhoneNumber(it) }
                val numbersWithSelected = if (allNumbers.any { com.tk.quickcontacts.utils.PhoneNumberUtils.isSameNumber(it, selectedNumber) }) allNumbers else allNumbers + selectedNumber
                val contactWithSelectedNumber = contact.copy(phoneNumber = selectedNumber, phoneNumbers = numbersWithSelected)
                when {
                    dialogAction == QuickContactAction.CALL || dialogAction == "call" -> onContactClick(contactWithSelectedNumber)
                    dialogAction == "add" -> onAddContact(contactWithSelectedNumber)
                    dialogAction != null -> onExecuteAction(context, dialogAction!!, selectedNumber)
                }
                showPhoneNumberDialog = false
                dialogAction = null
            },
            onDismiss = {
                showPhoneNumberDialog = false
                dialogAction = null
            },
            selectedContacts = selectedContacts,
            onAddContact = if (dialogAction == "add") { contactToAdd ->
                val allNumbers = contact.phoneNumbers.ifEmpty { listOf(contactToAdd.phoneNumber) }
                    .distinctBy { com.tk.quickcontacts.utils.PhoneNumberUtils.normalizePhoneNumber(it) }
                val numbersWithDefault = if (allNumbers.any { com.tk.quickcontacts.utils.PhoneNumberUtils.isSameNumber(it, contactToAdd.phoneNumber) }) allNumbers else allNumbers + contactToAdd.phoneNumber
                onAddContact(contact.copy(phoneNumber = contactToAdd.phoneNumber, phoneNumbers = numbersWithDefault))
            } else null,
            onRemoveContact = if (dialogAction == "add") { contactToRemove ->
                onRemoveContact(contactToRemove)
            } else null,
            hideIcons = false,
            showInstructionText = false
        )
    }
    
    if (showContactActionsDialog) {
        ContactActionsGridDialog(
            contact = contact,
            availableActions = availableActions,
            onActionSelected = { action, phoneNumber ->
                when {
                    action == QuickContactAction.NONE || action == QuickContactAction.ALL_OPTIONS -> Unit
                    action == QuickContactAction.CALL -> onContactClick(contact.copy(phoneNumber = phoneNumber))
                    else -> onExecuteAction(context, action, phoneNumber)
                }
                showContactActionsDialog = false
            },
            onDismiss = { showContactActionsDialog = false },
            onAddToQuickList = { contactToAdd -> onAddContact(contactToAdd) },
            onRemoveFromQuickList = { contactToRemove -> onRemoveContact(contactToRemove) },
            isInQuickList = selectedContacts.any { it.id == contact.id },
            onAddToContacts = { phoneNumber -> onAddToContacts(context, phoneNumber) },
            getLastShownPhoneNumber = getLastShownPhoneNumber,
            setLastShownPhoneNumber = setLastShownPhoneNumber
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .combinedClickable(
                onClick = {
                    showContactActionsDialog = true
                },
                onLongClick = {
                    showContactActionsDialog = true
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .combinedClickable(
                        onClick = {
                            if (contact.phoneNumbers.size > 1) {
                                dialogAction = "add"
                                showPhoneNumberDialog = true
                            } else {
                                if (isSelected) {
                                    onRemoveContact(contact)
                                } else {
                                    onAddContact(contact)
                                }
                            }
                        }
                    )
            ) {
                if (contact.photoUri != null && !imageLoadFailed) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(context)
                                .data(contact.photoUri)
                                .crossfade(true)
                                .size(48, 48)
                                .memoryCacheKey("contact_${contact.id}")
                                .build(),
                            onError = { imageLoadFailed = true }
                        ),
                        contentDescription = "Contact photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                if (isSelected && contact.phoneNumbers.size <= 1) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "In quick list",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(20.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Contact Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = if (contact.phoneNumbers.size > 1) "${contact.phoneNumbers.size} numbers"
                        else PhoneNumberUtils.formatPhoneNumber(contact.phoneNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .combinedClickable(
                            onClick = {
                                if (contact.phoneNumbers.size > 1) {
                                    dialogAction = "call"
                                    showPhoneNumberDialog = true
                                } else {
                                    onContactClick(contact)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    QuickContactActionIcon(
                        action = QuickContactAction.CALL,
                        contentDescription = "Call ${contact.name}",
                        modifier = Modifier.size(28.dp)
                    )
                }
                if (messagingAction != QuickContactAction.NONE) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .combinedClickable(
                                onClick = {
                                    if (contact.phoneNumbers.size > 1) {
                                        dialogAction = messagingAction
                                        showPhoneNumberDialog = true
                                    } else {
                                        onExecuteAction(context, messagingAction, contact.phoneNumber)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        QuickContactActionIcon(
                            action = messagingAction,
                            contentDescription = "$messagingAction ${contact.name}",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun MessagingApp.requiresContactAccountCheck(): Boolean {
    return this == MessagingApp.WHATSAPP || this == MessagingApp.TELEGRAM || this == MessagingApp.SIGNAL
}

private fun MessagingApp.toChatAction(): String {
    return when (this) {
        MessagingApp.WHATSAPP -> QuickContactAction.WHATSAPP_CHAT
        MessagingApp.TELEGRAM -> QuickContactAction.TELEGRAM_CHAT
        MessagingApp.SIGNAL -> QuickContactAction.SIGNAL_CHAT
        MessagingApp.SMS -> QuickContactAction.MESSAGE
    }
}
