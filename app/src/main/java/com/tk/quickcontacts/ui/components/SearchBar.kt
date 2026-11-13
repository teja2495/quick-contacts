package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.tk.quickcontacts.ui.theme.ExpressiveMotion
import kotlin.math.abs
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tk.quickcontacts.R
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.services.MessagingService
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.shape.RoundedCornerShape

private enum class OpenAppItem { Phone, Sms, WhatsApp, Telegram, Signal, GoogleMeet }

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
        placeholder = {
            Text(
                text = stringResource(R.string.search_placeholder),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.cd_search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.action_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent
        )
    )
}

/**
 * Home-screen search pill. Tapping it opens search; a horizontal swipe in either
 * direction flips it to a row of app shortcuts, and another swipe flips it back.
 */
@Composable
fun FakeSearchBar(
    onClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messagingService = remember { MessagingService() }
    val phoneService = remember { com.tk.quickcontacts.services.PhoneService() }
    val availableMessagingApps = remember {
        messagingService.checkAvailableMessagingApps(context.packageManager)
    }
    val isGoogleMeetInstalled = remember {
        context.packageManager.getLaunchIntentForPackage(GOOGLE_MEET_PACKAGE) != null
    }
    val appItems = remember(availableMessagingApps, isGoogleMeetInstalled) {
        buildList {
            add(OpenAppItem.Phone)
            add(OpenAppItem.Sms)
            if (availableMessagingApps.contains(MessagingApp.WHATSAPP)) add(OpenAppItem.WhatsApp)
            if (availableMessagingApps.contains(MessagingApp.TELEGRAM)) add(OpenAppItem.Telegram)
            if (availableMessagingApps.contains(MessagingApp.SIGNAL)) add(OpenAppItem.Signal)
            if (isGoogleMeetInstalled) add(OpenAppItem.GoogleMeet)
        }
    }

    var showApps by rememberSaveable { mutableStateOf(false) }
    // -1 when the last swipe went left, +1 when it went right; drives slide direction.
    var swipeDirection by remember { mutableIntStateOf(-1) }
    val swipeThreshold = with(LocalDensity.current) { 40.dp.toPx() }

    Surface(
        modifier = modifier
            .height(64.dp)
            .pointerInput(swipeThreshold) {
                var dragTotal = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragTotal += dragAmount
                    },
                    onDragEnd = {
                        if (abs(dragTotal) >= swipeThreshold) {
                            swipeDirection = if (dragTotal < 0) -1 else 1
                            showApps = !showApps
                        }
                    }
                )
            },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        AnimatedContent(
            targetState = showApps,
            transitionSpec = {
                val direction = swipeDirection
                (slideInHorizontally(ExpressiveMotion.defaultSpatialNoBounce()) { width -> -direction * width } +
                    fadeIn(ExpressiveMotion.defaultEffects())) togetherWith
                    (slideOutHorizontally(ExpressiveMotion.defaultSpatialNoBounce()) { width -> direction * width } +
                        fadeOut(ExpressiveMotion.fastEffects())) using
                    SizeTransform(clip = true)
            },
            label = "search_bar_face"
        ) { appsVisible ->
            if (appsVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    appItems.forEach { item ->
                        OpenAppButton(
                            item = item,
                            onClick = { openApp(context, item, phoneService, messagingService) }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onClick() }
                        .padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private const val GOOGLE_MEET_PACKAGE = "com.google.android.apps.tachyon"

private fun openApp(
    context: Context,
    item: OpenAppItem,
    phoneService: com.tk.quickcontacts.services.PhoneService,
    messagingService: MessagingService
) {
    fun launch(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(it)
        }
    }
    when (item) {
        OpenAppItem.Phone -> phoneService.openPhoneApp(context)
        OpenAppItem.Sms -> messagingService.openSmsAppDirectly(context)
        OpenAppItem.WhatsApp -> launch("com.whatsapp")
        OpenAppItem.Telegram -> launch("org.telegram.messenger")
        OpenAppItem.Signal -> launch("org.thoughtcrime.securesms")
        OpenAppItem.GoogleMeet -> launch(GOOGLE_MEET_PACKAGE)
    }
}

@Composable
private fun OpenAppButton(
    item: OpenAppItem,
    onClick: () -> Unit
) {
    val label = when (item) {
        OpenAppItem.Phone -> stringResource(R.string.menu_phone_default)
        OpenAppItem.Sms -> stringResource(R.string.menu_sms_default)
        OpenAppItem.WhatsApp -> stringResource(R.string.menu_whatsapp)
        OpenAppItem.Telegram -> stringResource(R.string.menu_telegram)
        OpenAppItem.Signal -> stringResource(R.string.messaging_app_signal)
        OpenAppItem.GoogleMeet -> stringResource(R.string.menu_google_meet)
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (item) {
            OpenAppItem.Phone -> Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            OpenAppItem.Sms -> Icon(
                imageVector = Icons.Rounded.Sms,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            OpenAppItem.WhatsApp -> Icon(
                painter = painterResource(R.drawable.whatsapp_icon),
                contentDescription = label,
                tint = Color(0xFF25D366),
                modifier = Modifier.size(24.dp)
            )
            OpenAppItem.Telegram -> Icon(
                painter = painterResource(R.drawable.telegram_icon),
                contentDescription = label,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
            OpenAppItem.Signal -> Icon(
                painter = painterResource(R.drawable.signal_icon),
                contentDescription = label,
                tint = Color(0xFF3A76F0),
                modifier = Modifier.size(22.dp)
            )
            OpenAppItem.GoogleMeet -> Icon(
                painter = painterResource(R.drawable.google_meet_icon),
                contentDescription = label,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
