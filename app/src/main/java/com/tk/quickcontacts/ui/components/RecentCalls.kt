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

@Composable
fun RecentCallsSection(
    recentCalls: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit = {},
    onContactImageClick: (Contact) -> Unit = {},
    onExpandedChange: (Boolean) -> Unit = {},
    isExpanded: Boolean = false,
    isInternationalDetectionEnabled: Boolean = true,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier,
    selectedContacts: List<Contact> = emptyList(),
    availableMessagingApps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS),
    onExecuteAction: (Context, String, String) -> Unit,
    homeCountryCode: String? = null
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
                
                // Content container - only animate data changes when expanded
                if (isExpanded) {
                    // Animated content for expanded state
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
                            // Expanded view - show all items in LazyColumn
                            LazyColumn(
                                contentPadding = PaddingValues(vertical = 0.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(currentRecentCalls) { contact ->
                                    RecentCallVerticalItem(
                                        contact = contact,
                                        onContactClick = onContactClick,
                                        onWhatsAppClick = onWhatsAppClick,
                                        onContactImageClick = onContactImageClick,
                                        isInternationalDetectionEnabled = isInternationalDetectionEnabled,
                                        defaultMessagingApp = defaultMessagingApp,
                                        modifier = Modifier.fillMaxWidth(),
                                        selectedContacts = selectedContacts,
                                        availableMessagingApps = availableMessagingApps,
                                        onExecuteAction = onExecuteAction,
                                        homeCountryCode = homeCountryCode
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Non-animated content for collapsed state
                    Column(
                        modifier = Modifier.offset(y = (-4).dp)
                    ) {
                        // Collapsed view - show only first 2 items
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            recentCalls.take(2).forEach { contact ->
                                RecentCallVerticalItem(
                                    contact = contact,
                                    onContactClick = onContactClick,
                                    onWhatsAppClick = onWhatsAppClick,
                                    onContactImageClick = onContactImageClick,
                                    isInternationalDetectionEnabled = isInternationalDetectionEnabled,
                                    defaultMessagingApp = defaultMessagingApp,
                                    modifier = Modifier.fillMaxWidth(),
                                    selectedContacts = selectedContacts,
                                    availableMessagingApps = availableMessagingApps,
                                    onExecuteAction = onExecuteAction,
                                    homeCountryCode = homeCountryCode
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 