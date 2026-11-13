package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quickcontacts.R

private val ActionButtonWidth = 90.dp
private val ActionButtonHeight = 84.dp
private val ActionButtonShape = RoundedCornerShape(20.dp)
private val LargeIconSize = 24.dp

@Composable
private fun actionTint(action: String): Color {
    val primary = MaterialTheme.colorScheme.primary
    return when (action) {
        QuickContactAction.CALL,
        QuickContactAction.MESSAGE -> primary
        QuickContactAction.WHATSAPP_CHAT,
        QuickContactAction.WHATSAPP_VOICE_CALL,
        QuickContactAction.WHATSAPP_VIDEO_CALL -> Color(0xFF25D366)
        QuickContactAction.TELEGRAM_CHAT,
        QuickContactAction.TELEGRAM_VOICE_CALL,
        QuickContactAction.TELEGRAM_VIDEO_CALL -> Color(0xFF0088CC)
        QuickContactAction.SIGNAL_CHAT,
        QuickContactAction.SIGNAL_VOICE_CALL,
        QuickContactAction.SIGNAL_VIDEO_CALL -> Color(0xFF3A76F0)
        QuickContactAction.GOOGLE_MEET -> Color(0xFF00897B)
        QuickContactAction.ALL_OPTIONS,
        QuickContactAction.NONE -> primary
        else -> primary
    }
}

private fun actionButtonLabel(action: String): String = when (action) {
    QuickContactAction.CALL -> "Call"
    QuickContactAction.MESSAGE -> "Message"
    QuickContactAction.GOOGLE_MEET -> "Meet"
    QuickContactAction.WHATSAPP_CHAT,
    QuickContactAction.TELEGRAM_CHAT,
    QuickContactAction.SIGNAL_CHAT -> "Chat"
    QuickContactAction.WHATSAPP_VOICE_CALL,
    QuickContactAction.TELEGRAM_VOICE_CALL,
    QuickContactAction.SIGNAL_VOICE_CALL -> "Voice Call"
    QuickContactAction.WHATSAPP_VIDEO_CALL,
    QuickContactAction.TELEGRAM_VIDEO_CALL,
    QuickContactAction.SIGNAL_VIDEO_CALL -> "Video Call"
    QuickContactAction.ALL_OPTIONS -> "All Options"
    QuickContactAction.NONE -> "None"
    else -> action
}

@Composable
fun ContactActionButton(
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useFullWidth: Boolean = false,
    shape: Shape = ActionButtonShape
) {
    val tint = actionTint(action)
    Surface(
        modifier = modifier
            .then(if (useFullWidth) Modifier.fillMaxWidth() else Modifier.width(ActionButtonWidth))
            .height(ActionButtonHeight)
            .clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .then(if (action == QuickContactAction.MESSAGE) Modifier.padding(top = 3.dp) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ContactActionButtonIcon(action = action, tint = tint)
            Text(
                text = actionButtonLabel(action),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ContactActionButtonIcon(
    action: String,
    tint: Color
) {
    when (action) {
        QuickContactAction.CALL -> Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.MESSAGE -> Icon(
            imageVector = Icons.Rounded.Sms,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(LargeIconSize * 0.9f)
        )
        QuickContactAction.WHATSAPP_CHAT -> Icon(
            painter = painterResource(id = R.drawable.whatsapp_icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.WHATSAPP_VOICE_CALL -> Icon(
            painter = painterResource(id = R.drawable.whatsapp_voice_call_icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.WHATSAPP_VIDEO_CALL -> Icon(
            painter = painterResource(id = R.drawable.whatsapp_video_call_icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.TELEGRAM_CHAT -> Icon(
            painter = painterResource(id = R.drawable.telegram_icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.TELEGRAM_VOICE_CALL -> Icon(
            painter = painterResource(id = R.drawable.telegram_voice_call_icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.TELEGRAM_VIDEO_CALL -> Icon(
            painter = painterResource(id = R.drawable.telegram_video_call_icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.SIGNAL_VOICE_CALL -> Icon(
            painter = painterResource(id = R.drawable.signal_voice_call_icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.SIGNAL_CHAT -> Icon(
            painter = painterResource(id = R.drawable.signal_icon),
            contentDescription = null,
            tint = Color(0xFF3A76F0),
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.SIGNAL_VIDEO_CALL -> Icon(
            painter = painterResource(id = R.drawable.signal_video_call_icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.GOOGLE_MEET -> Icon(
            painter = painterResource(id = R.drawable.google_meet_icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.ALL_OPTIONS -> Icon(
            imageVector = Icons.Default.UnfoldMore,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(LargeIconSize)
        )
        QuickContactAction.NONE -> Icon(
            imageVector = Icons.Default.DoNotDisturb,
            contentDescription = null,
            tint = tint.copy(alpha = 0.5f),
            modifier = Modifier.size(LargeIconSize)
        )
        else -> Icon(
            imageVector = Icons.Rounded.Sms,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(LargeIconSize)
        )
    }
}

@Composable
fun QuickContactActionIcon(
    action: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    when (action) {
        QuickContactAction.CALL -> {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
        QuickContactAction.WHATSAPP_CHAT -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp_icon),
                contentDescription = contentDescription,
                tint = Color(0xFF25D366),
                modifier = modifier
            )
        }
        QuickContactAction.WHATSAPP_VOICE_CALL -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp_voice_call_icon),
                contentDescription = contentDescription,
                tint = Color.Unspecified,
                modifier = modifier
            )
        }
        QuickContactAction.WHATSAPP_VIDEO_CALL -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp_video_call_icon),
                contentDescription = contentDescription,
                tint = Color.Unspecified,
                modifier = modifier
            )
        }
        QuickContactAction.TELEGRAM_CHAT -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram_icon),
                contentDescription = contentDescription,
                tint = Color(0xFF0088CC),
                modifier = modifier
            )
        }
        QuickContactAction.TELEGRAM_VOICE_CALL -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram_voice_call_icon),
                contentDescription = contentDescription,
                tint = Color.Unspecified,
                modifier = modifier
            )
        }
        QuickContactAction.TELEGRAM_VIDEO_CALL -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram_video_call_icon),
                contentDescription = contentDescription,
                tint = Color.Unspecified,
                modifier = modifier
            )
        }
        QuickContactAction.SIGNAL_VOICE_CALL -> {
            Icon(
                painter = painterResource(id = R.drawable.signal_voice_call_icon),
                contentDescription = contentDescription,
                tint = Color.Unspecified,
                modifier = modifier
            )
        }
        QuickContactAction.SIGNAL_CHAT -> {
            Icon(
                painter = painterResource(id = R.drawable.signal_icon),
                contentDescription = contentDescription,
                tint = Color(0xFF3A76F0),
                modifier = modifier
            )
        }
        QuickContactAction.SIGNAL_VIDEO_CALL -> {
            Icon(
                painter = painterResource(id = R.drawable.signal_video_call_icon),
                contentDescription = contentDescription,
                tint = Color.Unspecified,
                modifier = modifier
            )
        }
        QuickContactAction.GOOGLE_MEET -> {
            Icon(
                painter = painterResource(id = R.drawable.google_meet_icon),
                contentDescription = contentDescription,
                tint = Color.Unspecified,
                modifier = modifier
            )
        }
        QuickContactAction.ALL_OPTIONS -> {
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Rounded.Sms,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
    }
}

/**
 * Shape for a tile in a grid of action tiles: only the four outer corners of the
 * whole grid are fully rounded, so the tiles read as one grouped surface.
 */
fun actionGridTileShape(
    index: Int,
    count: Int,
    columns: Int,
    outer: Dp = 24.dp,
    inner: Dp = 6.dp
): Shape {
    val row = index / columns
    val col = index % columns
    val lastRow = (count - 1) / columns
    val lastColInRow = if (row == lastRow) (count - 1) % columns else columns - 1
    val lastColInRowAbove = columns - 1
    val topStart = row == 0 && col == 0
    val topEnd = row == 0 && col == lastColInRow
    val bottomStart = row == lastRow && col == 0
    // The last tile of a short final row has nothing below or beside it on the end side.
    val bottomEnd = (row == lastRow && col == lastColInRow) ||
        (row == lastRow - 1 && col == lastColInRowAbove && (count - 1) % columns < col)
    return RoundedCornerShape(
        topStart = if (topStart) outer else inner,
        topEnd = if (topEnd) outer else inner,
        bottomStart = if (bottomStart) outer else inner,
        bottomEnd = if (bottomEnd) outer else inner,
    )
}
