package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import android.content.Context
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.models.CustomActions
import com.tk.quickcontacts.models.MessagingApp

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
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS),
    selectedContacts: List<Contact> = emptyList(),
    onExecuteAction: (Context, String, String) -> Unit,
    onUpdateContactNumber: (Contact, String) -> Unit = { _, _ -> },
    hasSeenCallWarning: Boolean = true,
    onMarkCallWarningSeen: (() -> Unit)? = null
) {
    // Memoize the reorder state to prevent unnecessary recreations
    val reorderState = rememberReorderableLazyListState(onMove = { from, to ->
        onMove(from.index, to.index)
    })
    
    // Memoize expensive computations
    val contactItems = remember(contacts, customActionPreferences, editMode, isInternationalDetectionEnabled, defaultMessagingApp, availableMessagingApps, selectedContacts) {
        contacts.map { contact ->
            contact to customActionPreferences[contact.id]
        }
    }

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
            items = contactItems,
            key = { (contact, _) -> contact.id }
        ) { (contact, customActions) ->
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
                        customActions = customActions,
                        isInternationalDetectionEnabled = isInternationalDetectionEnabled,
                        defaultMessagingApp = defaultMessagingApp,
                        availableMessagingApps = availableMessagingApps,
                        selectedContacts = selectedContacts,
                        onExecuteAction = onExecuteAction,
                        onUpdateContactNumber = onUpdateContactNumber,
                        hasSeenCallWarning = hasSeenCallWarning,
                        onMarkCallWarningSeen = onMarkCallWarningSeen
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
                    customActions = customActions,
                    isInternationalDetectionEnabled = isInternationalDetectionEnabled,
                    defaultMessagingApp = defaultMessagingApp,
                    availableMessagingApps = availableMessagingApps,
                    selectedContacts = selectedContacts,
                    onExecuteAction = onExecuteAction,
                    onUpdateContactNumber = onUpdateContactNumber,
                    hasSeenCallWarning = hasSeenCallWarning,
                    onMarkCallWarningSeen = onMarkCallWarningSeen
                )
            }
        }
    }
} 