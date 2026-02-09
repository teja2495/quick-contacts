package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.content.Context
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.models.MessagingApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.res.stringResource
import com.tk.quickcontacts.R
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.ui.graphics.Color

@Composable
fun RecentCallsSection(
    recentCalls: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {},
    onContactImageClick: (Contact) -> Unit = {},
    onExpandedChange: (Boolean) -> Unit = {},
    isExpanded: Boolean = false,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier,
    selectedContacts: List<Contact> = emptyList(),
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SIGNAL, MessagingApp.SMS),
    availableActions: Set<String> = emptySet(),
    onExecuteAction: (Context, String, String) -> Unit,
    onAddToQuickList: ((Contact) -> Unit)? = null,
    showRecentCallsHint: Boolean = false,
    onDismissRecentCallsHint: () -> Unit = {},
    getLastShownPhoneNumber: (String) -> String? = { null },
    setLastShownPhoneNumber: (String, String) -> Unit = { _, _ -> }
) {

    
    if (recentCalls.isNotEmpty()) {
        val rotationAngle by animateFloatAsState(
            targetValue = if (isExpanded) 180f else 0f,
            animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            label = "arrow_rotation"
        )
        
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .offset(y = (-8).dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 0.dp)
                ) {
                    Text(
                        text = "Recent Calls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Expand/Collapse arrow (only show if more than 2 items)
                    // Expand/Collapse arrow - always show when there are recent calls, but only enable when more than 2 items
                    IconButton(
                        onClick = { 
                            if (recentCalls.size > 2) {
                                onExpandedChange(!isExpanded)
                            }
                        },
                        modifier = Modifier.padding(end = 5.dp),
                        enabled = recentCalls.size > 2
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (recentCalls.size > 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotationAngle)
                        )
                    }
                }
                
                // Show recent calls hint banner when expanded for the first time
                if (showRecentCallsHint && isExpanded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header with title and close button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.recent_calls_hint_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                IconButton(
                                    onClick = onDismissRecentCallsHint,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss hint",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            // Call type icons with descriptions
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                // Incoming call
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallReceived,
                                        contentDescription = "Incoming call",
                                        tint = Color(0xFF81C784), // Subtle green
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Incoming call",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Outgoing call
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallMade,
                                        contentDescription = "Outgoing call",
                                        tint = Color(0xFF64B5F6), // Subtle blue
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Outgoing call",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Missed call
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallMissed,
                                        contentDescription = "Missed call",
                                        tint = Color(0xFFE57373), // Subtle red
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Missed call",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Rejected call
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallReceived,
                                        contentDescription = "Rejected call",
                                        tint = Color(0xFFE57373), // Subtle red (same as missed)
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Rejected call",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Content container with expansion animation but no data refresh animation when minimized
                AnimatedContent(
                    targetState = isExpanded,
                    transitionSpec = {
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        ) togetherWith slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        )
                    }
                ) { isExpanded ->
                    if (!isExpanded) {
                        // Collapsed view - show only first 2 items (no data refresh animation)
                        Column(
                            modifier = Modifier.offset(y = (-4).dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                recentCalls.take(2).forEach { contact ->
                                    RecentCallVerticalItem(
                                        contact = contact,
                                        onContactClick = onContactClick,
                                        onWhatsAppClick = onWhatsAppClick,
                                        onContactImageClick = onContactImageClick,
                                        defaultMessagingApp = defaultMessagingApp,
                                        modifier = Modifier.fillMaxWidth(),
                                        selectedContacts = selectedContacts,
                                        availableMessagingApps = availableMessagingApps,
                                        availableActions = availableActions,
                                        onExecuteAction = onExecuteAction,
                                        onAddToQuickList = onAddToQuickList,
                                        getLastShownPhoneNumber = getLastShownPhoneNumber,
                                        setLastShownPhoneNumber = setLastShownPhoneNumber
                                    )
                                }
                            }
                        }
                    } else {
                        // Expanded view - show all items with data refresh animation
                        AnimatedContent(
                            targetState = recentCalls,
                            transitionSpec = {
                                fadeIn(
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                ) togetherWith fadeOut(
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                )
                            }
                        ) { currentRecentCalls ->
                            Column(
                                modifier = Modifier.offset(y = (-4).dp)
                            ) {
                                LazyColumn(
                                    contentPadding = PaddingValues(vertical = 0.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    items(currentRecentCalls.size) { index ->
                                        val contact = currentRecentCalls[index]
                                        Column {
                                            RecentCallVerticalItem(
                                                contact = contact,
                                                onContactClick = onContactClick,
                                                onWhatsAppClick = onWhatsAppClick,
                                                onContactImageClick = onContactImageClick,
                                                defaultMessagingApp = defaultMessagingApp,
                                                modifier = Modifier.fillMaxWidth(),
                                                selectedContacts = selectedContacts,
                                                availableMessagingApps = availableMessagingApps,
                                                availableActions = availableActions,
                                                onExecuteAction = onExecuteAction,
                                                onAddToQuickList = onAddToQuickList,
                                                getLastShownPhoneNumber = getLastShownPhoneNumber,
                                                setLastShownPhoneNumber = setLastShownPhoneNumber
                                            )
                                            // Add subtle divider between items (except after the last item)
                                            if (index < currentRecentCalls.size - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                                    thickness = 0.5.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 