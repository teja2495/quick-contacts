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
            .fillMaxSize()
            .reorderable(reorderState)
            .detectReorderAfterLongPress(reorderState),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Calls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(recentCalls) { contact ->
                        RecentCallItem(
                            contact = contact,
                            onContactClick = onContactClick
                        )
                    }
                }
            }
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
            .padding(4.dp),
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
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Default avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(84.dp)
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
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(enabled = !editMode) { onContactClick(contact) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                IconButton(onClick = { onWhatsAppClick(contact) }) {
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