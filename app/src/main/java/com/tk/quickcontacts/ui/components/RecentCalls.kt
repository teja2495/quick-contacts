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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
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
import androidx.compose.ui.res.stringResource
import com.tk.quickcontacts.R
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

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
    onRemoveFromQuickList: ((Contact) -> Unit)? = null,
    showRecentCallsHint: Boolean = false,
    onDismissRecentCallsHint: () -> Unit = {},
    getLastShownPhoneNumber: (String) -> String? = { null },
    setLastShownPhoneNumber: (String, String) -> Unit = { _, _ -> }
) {

    
    if (recentCalls.isNotEmpty()) {
        val displayedRecentCalls = if (isExpanded) recentCalls else recentCalls.take(2)
        val nowMs by rememberRecentCallsTimestampNow(displayedRecentCalls)

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
                                displayedRecentCalls.forEach { contact ->
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
                                        onRemoveFromQuickList = onRemoveFromQuickList,
                                        getLastShownPhoneNumber = getLastShownPhoneNumber,
                                        setLastShownPhoneNumber = setLastShownPhoneNumber,
                                        nowMs = nowMs
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
                                                onRemoveFromQuickList = onRemoveFromQuickList,
                                                getLastShownPhoneNumber = getLastShownPhoneNumber,
                                                setLastShownPhoneNumber = setLastShownPhoneNumber,
                                                nowMs = nowMs
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

@Composable
private fun rememberRecentCallsTimestampNow(recentCalls: List<Contact>): State<Long> {
    val nowMsState = remember { mutableStateOf(System.currentTimeMillis()) }
    val timestamps = remember(recentCalls) { recentCalls.mapNotNull { it.callTimestamp } }

    LaunchedEffect(timestamps) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            nowMsState.value = currentTime

            val nextDelayMs = computeNextRecentCallRefreshDelayMs(timestamps, currentTime) ?: break
            delay(nextDelayMs)
        }
    }

    return nowMsState
}

private fun computeNextRecentCallRefreshDelayMs(timestamps: List<Long>, nowMs: Long): Long? {
    val oneMinuteMs = 60_000L
    val oneHourMs = 3_600_000L

    val minDelayMs = timestamps
        .mapNotNull { timestamp ->
            val ageMs = (nowMs - timestamp).coerceAtLeast(0L)
            when {
                ageMs < oneMinuteMs -> oneMinuteMs - ageMs
                ageMs < oneHourMs -> {
                    val remainder = ageMs % oneMinuteMs
                    if (remainder == 0L) oneMinuteMs else oneMinuteMs - remainder
                }
                else -> null
            }
        }
        .minOrNull()

    return minDelayMs?.coerceAtLeast(1_000L)
}
