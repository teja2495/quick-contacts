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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Add
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
import android.content.Context
import com.tk.quickcontacts.utils.PhoneNumberUtils
import com.tk.quickcontacts.utils.ContactUtils
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.models.CustomActions
import com.tk.quickcontacts.ui.components.*

@Composable
fun PhoneNumberSelectionDialog(
    contact: Contact,
    onPhoneNumberSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    selectedContacts: List<Contact> = emptyList(),
    onAddContact: ((Contact) -> Unit)? = null,
    onRemoveContact: ((Contact) -> Unit)? = null
) {
    com.tk.quickcontacts.ui.components.PhoneNumberSelectionDialog(
        contact = contact,
        onPhoneNumberSelected = onPhoneNumberSelected,
        onDismiss = onDismiss,
        selectedContacts = selectedContacts,
        onAddContact = onAddContact,
        onRemoveContact = onRemoveContact
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
    com.tk.quickcontacts.ui.components.ActionToggleDialog(
        contact = contact,
        isInternational = isInternational,
        isCurrentlySwapped = isCurrentlySwapped,
        customActions = customActions,
        defaultMessagingApp = defaultMessagingApp,
        isInternationalDetectionEnabled = isInternationalDetectionEnabled,
        availableMessagingApps = availableMessagingApps,
        onConfirm = onConfirm,
        onDismiss = onDismiss
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
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS),
    selectedContacts: List<Contact> = emptyList(),
    onExecuteAction: (Context, String, String) -> Unit
) {
    com.tk.quickcontacts.ui.components.ContactList(
        contacts = contacts,
        onContactClick = onContactClick,
        modifier = modifier,
        editMode = editMode,
        onDeleteContact = onDeleteContact,
        onMove = onMove,
        onWhatsAppClick = onWhatsAppClick,
        onContactImageClick = onContactImageClick,
        onLongClick = onLongClick,
        onSetCustomActions = onSetCustomActions,
        customActionPreferences = customActionPreferences,
        isInternationalDetectionEnabled = isInternationalDetectionEnabled,
        defaultMessagingApp = defaultMessagingApp,
        availableMessagingApps = availableMessagingApps,
        selectedContacts = selectedContacts,
        onExecuteAction = onExecuteAction
    )
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
    modifier: Modifier = Modifier,
    selectedContacts: List<Contact> = emptyList(),
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS),
    onExecuteAction: (Context, String, String) -> Unit
) {
    com.tk.quickcontacts.ui.components.RecentCallsSection(
        recentCalls = recentCalls,
        onContactClick = onContactClick,
        onWhatsAppClick = onWhatsAppClick,
        onContactImageClick = onContactImageClick,
        onExpandedChange = onExpandedChange,
        isInternationalDetectionEnabled = isInternationalDetectionEnabled,
        defaultMessagingApp = defaultMessagingApp,
        modifier = modifier,
        selectedContacts = selectedContacts,
        availableMessagingApps = availableMessagingApps,
        onExecuteAction = onExecuteAction
    )
}

@Composable
fun RecentCallVerticalItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {},
    onContactImageClick: (Contact) -> Unit = {},
    isInternationalDetectionEnabled: Boolean = true,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier,
    selectedContacts: List<Contact> = emptyList(),
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS),
    onExecuteAction: (Context, String, String) -> Unit,
    homeCountryCode: String? = null
) {
    com.tk.quickcontacts.ui.components.RecentCallVerticalItem(
        contact = contact,
        onContactClick = onContactClick,
        onWhatsAppClick = onWhatsAppClick,
        onContactImageClick = onContactImageClick,
        isInternationalDetectionEnabled = isInternationalDetectionEnabled,
        defaultMessagingApp = defaultMessagingApp,
        modifier = modifier,
        selectedContacts = selectedContacts,
        availableMessagingApps = availableMessagingApps,
        onExecuteAction = onExecuteAction,
        homeCountryCode = homeCountryCode
    )
}

@Composable
fun RecentCallItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {},
    isInternationalDetectionEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    homeCountryCode: String? = null
) {
    com.tk.quickcontacts.ui.components.RecentCallItem(
        contact = contact,
        onContactClick = onContactClick,
        onWhatsAppClick = onWhatsAppClick,
        isInternationalDetectionEnabled = isInternationalDetectionEnabled,
        modifier = modifier,
        homeCountryCode = homeCountryCode
    )
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
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS),
    selectedContacts: List<Contact> = emptyList(),
    onExecuteAction: (Context, String, String) -> Unit,
    homeCountryCode: String? = null
) {
    com.tk.quickcontacts.ui.components.ContactItem(
        contact = contact,
        onContactClick = onContactClick,
        modifier = modifier,
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
        availableMessagingApps =  availableMessagingApps,
        selectedContacts = selectedContacts,
        onExecuteAction = onExecuteAction,
        homeCountryCode = homeCountryCode
    )
}

