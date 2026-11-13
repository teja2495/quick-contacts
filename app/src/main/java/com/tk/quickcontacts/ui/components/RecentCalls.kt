package com.tk.quickcontacts.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.ui.theme.ExpressiveMotion
import com.tk.quickcontacts.ui.theme.SegmentedList
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
    setLastShownPhoneNumber: (String, String) -> Unit = { _, _ -> },
    expandedRecentCalls: List<Contact> = recentCalls
) {
    // Each state keeps its own list so the outgoing content doesn't swap data
    // mid-transition (which is what made the old animation jump).
    val collapsedCalls = remember(recentCalls) { recentCalls.take(COLLAPSED_COUNT) }
    val allCalls = expandedRecentCalls.ifEmpty { recentCalls }
    val canExpand = allCalls.size > COLLAPSED_COUNT || recentCalls.size > COLLAPSED_COUNT

    if (collapsedCalls.isEmpty() && allCalls.isEmpty()) return

    val nowMs by rememberRecentCallsTimestampNow(if (isExpanded) allCalls else collapsedCalls)

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "recent_calls_arrow"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable(enabled = canExpand) { onExpandedChange(!isExpanded) }
                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = "Recent Calls",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (canExpand) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse recent calls" else "Expand recent calls",
                    tint = if (canExpand) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        AnimatedContent(
            targetState = isExpanded,
            contentAlignment = Alignment.TopCenter,
            transitionSpec = {
                (fadeIn(animationSpec = ExpressiveMotion.defaultEffects()) togetherWith
                    fadeOut(animationSpec = ExpressiveMotion.fastEffects()))
                    .using(
                        SizeTransform(clip = true) { _, _ ->
                            ExpressiveMotion.defaultSpatialNoBounce()
                        }
                    )
            },
            label = "recent_calls_expand"
        ) { expanded ->
            val calls = if (expanded) allCalls else collapsedCalls
            val itemContent: @Composable (Int, Contact) -> Unit = { index, contact ->
                RecentCallVerticalItem(
                    contact = contact,
                    onContactClick = onContactClick,
                    onWhatsAppClick = onWhatsAppClick,
                    onContactImageClick = onContactImageClick,
                    defaultMessagingApp = defaultMessagingApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SegmentedList.shape(index, calls.size))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
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

            if (expanded) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(SegmentedList.Gap),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(calls) { index, contact -> itemContent(index, contact) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(SegmentedList.Gap)) {
                    calls.forEachIndexed { index, contact -> itemContent(index, contact) }
                }
            }
        }
    }
}

private const val COLLAPSED_COUNT = 2

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
