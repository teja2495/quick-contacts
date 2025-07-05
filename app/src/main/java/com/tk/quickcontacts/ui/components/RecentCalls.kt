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
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.models.MessagingApp

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
    selectedContacts: List<Contact> = emptyList()
) {
    if (recentCalls.isNotEmpty()) {
        var isExpanded by remember { mutableStateOf(false) }
        val rotationAngle by animateFloatAsState(
            targetValue = if (isExpanded) 180f else 0f,
            animationSpec = tween(300),
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
                        .padding(bottom = 0.dp)
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
                            onClick = { 
                                isExpanded = !isExpanded
                                onExpandedChange(isExpanded)
                            },
                            modifier = Modifier.padding(end = 5.dp)
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
                if (!isExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.offset(y = (-4).dp)
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
                                selectedContacts = selectedContacts
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .weight(1f)
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(recentCalls) { contact ->
                                RecentCallVerticalItem(
                                    contact = contact,
                                    onContactClick = onContactClick,
                                    onWhatsAppClick = onWhatsAppClick,
                                    onContactImageClick = onContactImageClick,
                                    isInternationalDetectionEnabled = isInternationalDetectionEnabled,
                                    defaultMessagingApp = defaultMessagingApp,
                                    modifier = Modifier.fillMaxWidth(),
                                    selectedContacts = selectedContacts
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 