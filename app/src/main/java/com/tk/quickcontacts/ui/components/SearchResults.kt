package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Context
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.ContactsViewModel
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.R
import com.tk.quickcontacts.utils.ContactUtils
import com.tk.quickcontacts.utils.PhoneNumberUtils
import com.tk.quickcontacts.PhoneNumberSelectionDialog

@Composable
fun SearchResultsContent(
    viewModel: ContactsViewModel,
    searchQuery: String,
    searchResults: List<Contact>,
    selectedContacts: List<Contact>,
    isInternationalDetectionEnabled: Boolean = true,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS),
    onExecuteAction: (Context, String, String) -> Unit
) {
    val context = LocalContext.current
    val homeCountryCode by viewModel.homeCountryCode.collectAsState()
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = 10.dp),
        reverseLayout = true // This makes the LazyColumn scroll from bottom to top
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
                        text = "Type a name to search through all your contacts",
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
                    text = "Tap + to add to your quick list",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 10.dp, end = 24.dp, bottom = 0.dp)
                )
            }
            
            // Search results (they're already reversed, and with reverseLayout=true, 
            // most relevant will be closest to search bar)
            items(searchResults) { contact ->
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
                    isInternationalDetectionEnabled = isInternationalDetectionEnabled,
                    defaultMessagingApp = defaultMessagingApp,
                    modifier = Modifier.fillMaxWidth(),
                    availableMessagingApps = availableMessagingApps,
                    onExecuteAction = onExecuteAction,
                    homeCountryCode = homeCountryCode
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
    isInternationalDetectionEnabled: Boolean = true,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS),
    onExecuteAction: (Context, String, String) -> Unit,
    homeCountryCode: String? = null
) {
    val isSelected = selectedContacts.any { it.id == contact.id }
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
                    "add" -> onAddContact(contactWithSelectedNumber)
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
                val newContact = contact.copy(
                    phoneNumber = contactWithSelectedNumber.phoneNumber,
                    phoneNumbers = listOf(contactWithSelectedNumber.phoneNumber)
                )
                onAddContact(newContact)
            } else null,
            onRemoveContact = if (dialogAction == "add") { contactToRemove ->
                onRemoveContact(contactToRemove)
            } else null,
            hideIcons = false,
            showInstructionText = false
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
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
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
            // Toggle quick contacts button
            if (contact.phoneNumbers.size > 1) {
                // For multiple numbers, always open dialog, do not show tick/+
                IconButton(
                    onClick = {
                        dialogAction = "add"
                        showPhoneNumberDialog = true
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add ${contact.name} to quick contacts",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        if (isSelected) {
                            onRemoveContact(contact)
                        } else {
                            onAddContact(contact)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Done else Icons.Default.Add,
                        contentDescription = if (isSelected)
                            "Remove ${contact.name} from quick contacts"
                        else
                            "Add ${contact.name} to quick contacts",
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Contact Info - for international: open WhatsApp, for domestic: call
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
                
                if (contact.phoneNumbers.size > 1) {
                    Text(
                        text = "${contact.phoneNumbers.size} numbers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
                                contentDescription = "Send WhatsApp message to ${contact.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        MessagingApp.SMS -> {
                            Icon(
                                painter = painterResource(id = R.drawable.sms_icon),
                                contentDescription = "Send SMS to ${contact.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        MessagingApp.TELEGRAM -> {
                            Icon(
                                painter = painterResource(id = R.drawable.telegram_icon),
                                contentDescription = "Send Telegram message to ${contact.name}",
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
} 