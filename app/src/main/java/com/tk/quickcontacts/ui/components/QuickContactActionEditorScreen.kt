package com.tk.quickcontacts.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.models.CustomActions
import com.tk.quickcontacts.models.MessagingApp

@Composable
fun QuickContactActionEditorScreen(
    contact: Contact,
    customActions: CustomActions?,
    defaultMessagingApp: MessagingApp,
    availableMessagingApps: Set<MessagingApp>,
    selectedContacts: List<Contact>,
    hasSeenCallWarning: Boolean,
    onExecuteAction: (Context, String, String) -> Unit,
    onUpdateContactNumber: (Contact, String) -> Unit,
    onActionUpdated: (CustomActions) -> Unit
) {
    val initialActions = remember(contact.id, customActions, defaultMessagingApp) {
        resolveQuickContactActions(customActions, defaultMessagingApp)
    }
    var draftActions by remember(initialActions) { mutableStateOf(initialActions) }
    var editingSlot by remember { mutableStateOf<QuickContactActionSlot?>(null) }

    LaunchedEffect(initialActions) {
        draftActions = initialActions
    }

    val sections = listOf(
        Pair("Card", listOf(QuickContactActionSlot.CARD_TAP, QuickContactActionSlot.CARD_LONG_PRESS)),
        Pair("Button 1", listOf(QuickContactActionSlot.FIRST_BUTTON_TAP, QuickContactActionSlot.FIRST_BUTTON_LONG_PRESS)),
        Pair("Button 2", listOf(QuickContactActionSlot.SECOND_BUTTON_TAP, QuickContactActionSlot.SECOND_BUTTON_LONG_PRESS))
    )

    if (editingSlot != null) {
        val slot = editingSlot!!
        ActionGridDialog(
            title = "Select action for ${slot.sectionTitle} (${slot.interactionTitle})",
            actions = QuickContactAction.editableOptions,
            onActionSelected = { action ->
                val updatedActions = draftActions.update(slot, action)
                draftActions = updatedActions
                onActionUpdated(updatedActions.toCustomActions())
                editingSlot = null
            },
            onDismiss = { editingSlot = null }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                key(draftActions) {
                    ContactItem(
                        contact = contact,
                        onContactClick = {},
                        editMode = false,
                        onDeleteContact = {},
                        onWhatsAppClick = {},
                        onContactImageClick = {},
                        onLongClick = {},
                        onOpenActionEditor = {},
                        customActions = draftActions.toCustomActions(),
                        defaultMessagingApp = defaultMessagingApp,
                        availableMessagingApps = availableMessagingApps,
                        selectedContacts = selectedContacts,
                        onExecuteAction = onExecuteAction,
                        onUpdateContactNumber = onUpdateContactNumber,
                        hasSeenCallWarning = hasSeenCallWarning,
                        onMarkCallWarningSeen = null,
                        onEditContactName = { _, _ -> },
                        callActivity = null
                    )
                }
            }

            items(sections) { (title, slotGroup) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionSlotTile(
                            title = slotGroup[0].interactionTitle,
                            action = draftActions.actionFor(slotGroup[0]),
                            onTap = { editingSlot = slotGroup[0] },
                            modifier = Modifier.weight(1f)
                        )
                        ActionSlotTile(
                            title = slotGroup[1].interactionTitle,
                            action = draftActions.actionFor(slotGroup[1]),
                            onTap = { editingSlot = slotGroup[1] },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionSlotTile(
    title: String,
    action: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1.2f)
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (action == QuickContactAction.NONE) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add action",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        QuickContactActionIcon(
                            action = action,
                            contentDescription = action
                        )
                        Text(
                            text = action,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

