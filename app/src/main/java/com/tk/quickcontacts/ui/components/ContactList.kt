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
    onOpenActionEditor: (Contact) -> Unit = {},
    customActionPreferences: Map<String, CustomActions> = emptyMap(),
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SIGNAL, MessagingApp.SMS),
    availableActions: Set<String> = emptySet(),
    selectedContacts: List<Contact> = emptyList(),
    onExecuteAction: (Context, String, String) -> Unit,
    onUpdateContactNumber: (Contact, String) -> Unit = { _, _ -> },
    hasSeenCallWarning: Boolean = true,
    onMarkCallWarningSeen: (() -> Unit)? = null,
    onEditContactName: (Contact, String) -> Unit = { _, _ -> },
    callActivityMap: Map<String, Contact> = emptyMap()
) {
    val reorderState = rememberReorderableLazyListState(onMove = { from, to ->
        onMove(from.index, to.index)
    })
    
    val contactItems = remember(contacts, customActionPreferences, editMode, defaultMessagingApp, availableMessagingApps, availableActions, selectedContacts) {
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
            val callActivity = callActivityMap[contact.id]
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
                        onOpenActionEditor = onOpenActionEditor,
                        customActions = customActions,
                        defaultMessagingApp = defaultMessagingApp,
                        availableMessagingApps = availableMessagingApps,
                        availableActions = availableActions,
                        selectedContacts = selectedContacts,
                        onExecuteAction = onExecuteAction,
                        onUpdateContactNumber = onUpdateContactNumber,
                        hasSeenCallWarning = hasSeenCallWarning,
                        onMarkCallWarningSeen = onMarkCallWarningSeen,
                        onEditContactName = onEditContactName,
                        callActivity = callActivity
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
                    onOpenActionEditor = onOpenActionEditor,
                    customActions = customActions,
                    defaultMessagingApp = defaultMessagingApp,
                    availableMessagingApps = availableMessagingApps,
                    availableActions = availableActions,
                    selectedContacts = selectedContacts,
                    onExecuteAction = onExecuteAction,
                    onUpdateContactNumber = onUpdateContactNumber,
                    hasSeenCallWarning = hasSeenCallWarning,
                    onMarkCallWarningSeen = onMarkCallWarningSeen,
                    onEditContactName = onEditContactName,
                    callActivity = callActivity
                )
            }
        }
    }
} 
