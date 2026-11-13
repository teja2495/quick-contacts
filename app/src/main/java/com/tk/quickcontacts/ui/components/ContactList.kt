package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.lazy.itemsIndexed
import com.tk.quickcontacts.ui.theme.SegmentedList

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.zIndex
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
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
    // Local copy so the dragged order updates in the same frame; the change is
    // persisted through onMove and replaced when the source list changes.
    var orderedContacts by remember(contacts) { mutableStateOf(contacts) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        orderedContacts = orderedContacts.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onMove(from.index, to.index)
    }

    val contactItems = remember(orderedContacts, customActionPreferences) {
        orderedContacts.map { contact ->
            contact to customActionPreferences[contact.id]
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(SegmentedList.Gap)
    ) {
        itemsIndexed(
            items = contactItems,
            key = { _, (contact, _) -> contact.id }
        ) { index, (contact, customActions) ->
            val itemShape = SegmentedList.shape(index, contactItems.size)
            val callActivity = callActivityMap[contact.id]
            // Always wrap in ReorderableItem (enabled only in edit mode) so toggling edit
            // mode keeps the same composition and ContactItem can animate between states.
            ReorderableItem(reorderState, key = contact.id, enabled = editMode) { isDragging ->
                ContactItem(
                    contact = contact,
                    onContactClick = onContactClick,
                    editMode = editMode,
                    onDeleteContact = onDeleteContact,
                    onWhatsAppClick = onWhatsAppClick,
                    onContactImageClick = onContactImageClick,
                    dragHandleModifier = Modifier.draggableHandle(enabled = editMode),
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f),
                    isDragging = isDragging,
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
                    callActivity = callActivity,
                    shape = itemShape
                )
            }
        }
    }
} 
