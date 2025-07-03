package com.tk.quickcontacts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate

@Composable
fun ContactList(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean,
    onDeleteContact: (Contact) -> Unit,
    onMove: (Int, Int) -> Unit = { _, _ -> },
    onWhatsAppClick: (Contact) -> Unit = {}
) {
    val reorderState = rememberReorderableLazyListState(onMove = { from, to ->
        onMove(from.index, to.index)
    })

    LazyColumn(
        state = reorderState.listState,
        modifier = modifier
            .reorderable(reorderState)
            .detectReorderAfterLongPress(reorderState),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            items = contacts,
            key = { contact -> contact.id }
        ) { contact ->
            ReorderableItem(reorderState, key = contact.id) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) 4.dp else 0.dp, label = "dragElevation")
                ContactItem(
                    contact = contact,
                    onContactClick = onContactClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation.value),
                    editMode = editMode,
                    onDeleteContact = onDeleteContact,
                    onWhatsAppClick = onWhatsAppClick
                )
            }
        }
    }
}

@Composable
fun RecentCallsSection(
    recentCalls: List<Contact>,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentCalls.isNotEmpty()) {
        var isExpanded by remember { mutableStateOf(false) }
        val rotationAngle by animateFloatAsState(
            targetValue = if (isExpanded) 180f else 0f,
            animationSpec = tween(300),
            label = "arrow_rotation"
        )
        
        // Show 2 items by default, all when expanded
        val itemsToShow = if (isExpanded) recentCalls else recentCalls.take(2)
        
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
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
                            onClick = { isExpanded = !isExpanded }
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
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsToShow.forEach { contact ->
                        RecentCallVerticalItem(
                            contact = contact,
                            onContactClick = onContactClick,
                            modifier = Modifier.fillMaxWidth()
                        )
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
    modifier: Modifier = Modifier
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    
    // Phone number selection dialog
    if (showPhoneNumberDialog) {
        PhoneNumberSelectionDialog(
            contact = contact,
            onPhoneNumberSelected = { selectedNumber: String ->
                val contactWithSelectedNumber = contact.copy(phoneNumber = selectedNumber)
                onContactClick(contactWithSelectedNumber)
                showPhoneNumberDialog = false
            },
            onDismiss = {
                showPhoneNumberDialog = false
            }
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                if (contact.phoneNumbers.size > 1) {
                    showPhoneNumberDialog = true
                } else {
                    onContactClick(contact)
                }
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
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
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Default avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
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
        
        // Contact Name
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        // WhatsApp button
        IconButton(
            onClick = { onContactClick(contact) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "WhatsApp ${contact.name}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun RecentCallItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .clickable { onContactClick(contact) }
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

@Composable
fun ContactItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean,
    onDeleteContact: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {}
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf<String?>(null) }
    
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
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable(enabled = !editMode) { 
                if (contact.phoneNumbers.size > 1) {
                    dialogAction = "call"
                    showPhoneNumberDialog = true
                } else {
                    onContactClick(contact)
                }
            },
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
            if (editMode) {
                // Reorder handle icon - purely visual hint for drag & drop
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Contact Photo (hidden in edit mode)
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
            }

            if (editMode) {
                IconButton(onClick = { onDeleteContact(contact) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Contact",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // WhatsApp icon when not in edit mode
                IconButton(onClick = { 
                    if (contact.phoneNumbers.size > 1) {
                        dialogAction = "whatsapp"
                        showPhoneNumberDialog = true
                    } else {
                        onWhatsAppClick(contact)
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Open WhatsApp Chat",
                        tint = Color(0xFF25D366) // WhatsApp green color
                    )
                }
            }
        }
    }
}

