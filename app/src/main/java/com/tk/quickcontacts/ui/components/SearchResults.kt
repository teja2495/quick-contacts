package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import com.tk.quickcontacts.ui.theme.SegmentedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import android.content.Context
import com.tk.quickcontacts.R
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.ContactsViewModel
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.models.CustomActions
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
    onSearchOnGoogle: (String) -> Unit = {},
    onSearchInGoogleMaps: (String) -> Unit = {},
    onAddToContacts: (Context, String) -> Unit = { context, phoneNumber ->
        viewModel.addNewContactToPhone(context, phoneNumber)
    }
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val messagingActionCache = remember(defaultMessagingApp) { java.util.concurrent.ConcurrentHashMap<String, String>() }
    val searchActionPreferences by viewModel.searchActionPreferences.collectAsState()

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onSearchOnGoogle(searchQuery) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.search_on),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(R.string.google))
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = { onSearchInGoogleMaps(searchQuery) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.search_on),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google_maps_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(R.string.maps))
                            }
                        }
                    }
                }
            }

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
            itemsIndexed(
                items = searchResults,
                key = { _, contact -> contact.id }
            ) { index, contact ->
                // reverseLayout puts index 0 at the bottom, so flip it for the group shape.
                val itemShape = SegmentedList.shape(searchResults.lastIndex - index, searchResults.size)
                val phoneNumbers = contact.phoneNumbers.ifEmpty { listOf(contact.phoneNumber) }
                val configuredPhoneNumber = viewModel.getLastShownPhoneNumber(contact.id)
                    ?.takeIf { rememberedNumber ->
                        phoneNumbers.any { PhoneNumberUtils.isSameNumber(it, rememberedNumber) }
                    }
                    ?: contact.phoneNumber
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (index == searchResults.lastIndex) 0.dp else SegmentedList.Gap),
                    shape = itemShape,
                    availableMessagingApps = availableMessagingApps,
                    availableActions = availableActions,
                    onExecuteAction = onExecuteAction,
                    onAddToContacts = onAddToContacts,
                    getLastShownPhoneNumber = viewModel::getLastShownPhoneNumber,
                    setLastShownPhoneNumber = viewModel::setLastShownPhoneNumber,
                    configuredPhoneNumber = configuredPhoneNumber,
                    customActions = searchActionPreferences[
                        "${contact.id}|${PhoneNumberUtils.normalizePhoneNumber(configuredPhoneNumber)}"
                    ],
                    onReplaceAction = { contactId, phoneNumber, slot, action ->
                        viewModel.setSearchAction(contactId, phoneNumber, slot, action)
                    },
                    cachedMessagingAction = { candidate ->
                        messagingActionCache[messagingCacheKey(candidate, defaultMessagingApp)]
                    },
                    resolveMessagingAction = { candidate ->
                        val cacheKey = messagingCacheKey(candidate, defaultMessagingApp)
                        messagingActionCache[cacheKey] ?: withContext(Dispatchers.IO) {
                            val defaultMessagingAction = resolveQuickContactActions(null, defaultMessagingApp).secondButtonTapAction
                            val resolvedAction = if (!defaultMessagingApp.requiresContactAccountCheck()) {
                                defaultMessagingAction
                            } else {
                                val hasAccountOnDefaultApp = ContactActionAvailability.hasContactAction(
                                    context = context,
                                    contactId = candidate.id.toLongOrNull(),
                                    phoneNumbers = candidate.phoneNumbers.ifEmpty { listOf(candidate.phoneNumber) },
                                    action = defaultMessagingApp.toChatAction()
                                )
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
    configuredPhoneNumber: String = contact.phoneNumber,
    customActions: CustomActions? = null,
    onReplaceAction: (String, String, QuickContactActionSlot, String) -> Unit = { _, _, _, _ -> },
    shape: Shape = MaterialTheme.shapes.large,
    cachedMessagingAction: (Contact) -> String? = { null },
    resolveMessagingAction: suspend (Contact) -> String
) {
    val isSelected = selectedContacts.any { it.id == contact.id }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var showContactActionsDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf<String?>(null) }
    var actionSlotToReplace by remember { mutableStateOf<QuickContactActionSlot?>(null) }
    val context = LocalContext.current
    val resolvedActions = remember(
        contact.id,
        configuredPhoneNumber,
        customActions,
        defaultMessagingApp
    ) {
        resolveQuickContactActions(customActions, defaultMessagingApp)
    }
    val callAction = resolvedActions.firstButtonTapAction
    // Account lookups hit the contacts provider, so resolve them off the main thread and
    // show the default app's action (or the cached answer) until they finish.
    val autoMessagingAction by produceState(
        initialValue = cachedMessagingAction(contact) ?: resolvedActions.secondButtonTapAction,
        contact.id,
        contact.phoneNumbers,
        defaultMessagingApp
    ) {
        value = resolveMessagingAction(contact)
    }
    val messagingAction = if (customActions?.secondButtonTapAction != null) {
        resolvedActions.secondButtonTapAction
    } else {
        autoMessagingAction
    }

    fun runButtonAction(action: String) {
        when {
            action == QuickContactAction.NONE -> Unit
            action == QuickContactAction.ALL_OPTIONS -> showContactActionsDialog = true
            customActions != null -> onExecuteAction(context, action, configuredPhoneNumber)
            contact.phoneNumbers.size > 1 -> {
                dialogAction = action
                showPhoneNumberDialog = true
            }
            else -> onExecuteAction(context, action, contact.phoneNumber)
        }
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
            title = actionSlotToReplace?.let { slot ->
                val currentAction = when (slot) {
                    QuickContactActionSlot.FIRST_BUTTON_TAP -> callAction
                    QuickContactActionSlot.SECOND_BUTTON_TAP -> messagingAction
                    else -> return@let null
                }
                "Replace $currentAction with"
            },
            onActionSelected = { action, phoneNumber ->
                val slot = actionSlotToReplace
                if (slot != null) {
                    onReplaceAction(contact.id, phoneNumber, slot, action)
                    actionSlotToReplace = null
                } else {
                    when {
                        action == QuickContactAction.NONE || action == QuickContactAction.ALL_OPTIONS -> Unit
                        action == QuickContactAction.CALL -> onContactClick(contact.copy(phoneNumber = phoneNumber))
                        else -> onExecuteAction(context, action, phoneNumber)
                    }
                }
                showContactActionsDialog = false
            },
            onDismiss = {
                showContactActionsDialog = false
                actionSlotToReplace = null
            },
            onAddToQuickList = { contactToAdd -> onAddContact(contactToAdd) },
            onRemoveFromQuickList = { contactToRemove -> onRemoveContact(contactToRemove) },
            isInQuickList = selectedContacts.any { it.id == contact.id },
            onAddToContacts = { phoneNumber -> onAddToContacts(context, phoneNumber) },
            getLastShownPhoneNumber = getLastShownPhoneNumber,
            setLastShownPhoneNumber = setLastShownPhoneNumber
        )
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .combinedClickable(
                onClick = { showContactActionsDialog = true },
                onLongClick = { showContactActionsDialog = true }
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                ContactAvatar(
                    contact = contact,
                    size = 48.dp,
                    onClick = {
                        if (contact.phoneNumbers.size > 1) {
                            dialogAction = "add"
                            showPhoneNumberDialog = true
                        } else if (isSelected) {
                            onRemoveContact(contact)
                        } else {
                            onAddContact(contact)
                        }
                    }
                )
                if (isSelected && contact.phoneNumbers.size <= 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 3.dp, y = 3.dp)
                            .size(22.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                            .padding(2.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "In quick list",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (contact.phoneNumbers.size > 1) "${contact.phoneNumbers.size} numbers"
                        else PhoneNumberUtils.formatPhoneNumber(contact.phoneNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (callAction != QuickContactAction.NONE) {
                    SearchActionButton(
                        action = callAction,
                        contentDescription = "$callAction ${contact.name}",
                        onClick = { runButtonAction(callAction) },
                        onLongClick = {
                            actionSlotToReplace = QuickContactActionSlot.FIRST_BUTTON_TAP
                            showContactActionsDialog = true
                        }
                    )
                }
                if (messagingAction != QuickContactAction.NONE) {
                    SearchActionButton(
                        action = messagingAction,
                        contentDescription = "$messagingAction ${contact.name}",
                        onClick = { runButtonAction(messagingAction) },
                        onLongClick = {
                            actionSlotToReplace = QuickContactActionSlot.SECOND_BUTTON_TAP
                            showContactActionsDialog = true
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchActionButton(
    action: String,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        QuickContactActionIcon(
            action = action,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun messagingCacheKey(contact: Contact, app: MessagingApp): String {
    val numbersKey = contact.phoneNumbers
        .ifEmpty { listOf(contact.phoneNumber) }
        .joinToString("|") { PhoneNumberUtils.normalizePhoneNumber(it) }
    return "${contact.id}|${app.name}|$numbersKey"
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
