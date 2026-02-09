package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun FakeSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val messagingService = remember { MessagingService() }
    val phoneService = remember { com.tk.quickcontacts.services.PhoneService() }
    val availableMessagingApps = remember {
        mutableStateOf(messagingService.checkAvailableMessagingApps(context.packageManager))
    }
    val isGoogleMeetInstalled = remember {
        context.packageManager.getLaunchIntentForPackage("com.google.android.apps.tachyon") != null
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search area - clickable to open search
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Apps menu
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = stringResource(R.string.cd_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { showMenu = true }
            )
        }
    }
    
    if (showMenu) {
        Dialog(
            onDismissRequest = { showMenu = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { showMenu = false }
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 160.dp)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.menu_open_app),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            IconButton(onClick = { showMenu = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_close)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        val appItems = buildList {
                            add(OpenAppItem.Phone)
                            add(OpenAppItem.Sms)
                            if (availableMessagingApps.value.contains(MessagingApp.WHATSAPP)) add(OpenAppItem.WhatsApp)
                            if (availableMessagingApps.value.contains(MessagingApp.TELEGRAM)) add(OpenAppItem.Telegram)
                            if (availableMessagingApps.value.contains(MessagingApp.SIGNAL)) add(OpenAppItem.Signal)
                            if (isGoogleMeetInstalled) add(OpenAppItem.GoogleMeet)
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (row in appItems.chunked(3)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (item in row) {
                                        OpenAppCard(
                                            item = item,
                                            onClick = {
                                                when (item) {
                                                    OpenAppItem.Phone -> phoneService.openPhoneApp(context)
                                                    OpenAppItem.Sms -> messagingService.openSmsAppDirectly(context)
                                                    OpenAppItem.WhatsApp -> {
                                                        val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                                                        intent?.let { it.flags = Intent.FLAG_ACTIVITY_NEW_TASK; context.startActivity(it) }
                                                    }
                                                    OpenAppItem.Telegram -> {
                                                        val intent = context.packageManager.getLaunchIntentForPackage("org.telegram.messenger")
                                                        intent?.let { it.flags = Intent.FLAG_ACTIVITY_NEW_TASK; context.startActivity(it) }
                                                    }
                                                    OpenAppItem.Signal -> {
                                                        val intent = context.packageManager.getLaunchIntentForPackage("org.thoughtcrime.securesms")
                                                        intent?.let { it.flags = Intent.FLAG_ACTIVITY_NEW_TASK; context.startActivity(it) }
                                                    }
                                                    OpenAppItem.GoogleMeet -> {
                                                        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.tachyon")
                                                        intent?.let { it.flags = Intent.FLAG_ACTIVITY_NEW_TASK; context.startActivity(it) }
                                                    }
                                                }
                                                showMenu = false
                                            },
                                            modifier = Modifier.weight(1f)
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

@Composable
private fun OpenAppCard(
    item: OpenAppItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (item) {
                OpenAppItem.Phone -> Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                OpenAppItem.Sms -> Icon(
                    imageVector = Icons.Rounded.Sms,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                OpenAppItem.WhatsApp -> Icon(
                    painter = painterResource(R.drawable.whatsapp_icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
                OpenAppItem.Telegram -> Icon(
                    painter = painterResource(R.drawable.telegram_icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                OpenAppItem.Signal -> Icon(
                    painter = painterResource(R.drawable.signal_icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                OpenAppItem.GoogleMeet -> Icon(
                    painter = painterResource(R.drawable.google_meet_icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = when (item) {
                    OpenAppItem.Phone -> stringResource(R.string.menu_phone_default)
                    OpenAppItem.Sms -> stringResource(R.string.menu_sms_default)
                    OpenAppItem.WhatsApp -> stringResource(R.string.menu_whatsapp)
                    OpenAppItem.Telegram -> stringResource(R.string.menu_telegram)
                    OpenAppItem.Signal -> stringResource(R.string.messaging_app_signal)
                    OpenAppItem.GoogleMeet -> stringResource(R.string.menu_google_meet)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
} 