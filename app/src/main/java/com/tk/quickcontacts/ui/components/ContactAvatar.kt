package com.tk.quickcontacts.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.tk.quickcontacts.Contact

/**
 * Contact photo, or a tonal monogram when there is none. The monogram colour is
 * picked from the primary / secondary / tertiary container roles by name, so a
 * list of contacts without photos stays easy to scan.
 */
@Composable
fun ContactAvatar(
    contact: Contact,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    var imageLoadFailed by remember(contact.photoUri) { mutableStateOf(false) }
    val context = LocalContext.current
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val base = modifier
        .size(size)
        .clip(CircleShape)
        .then(clickModifier)

    if (contact.photoUri != null && !imageLoadFailed) {
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(contact.photoUri)
                    .crossfade(true)
                    .memoryCacheKey("contact_${contact.id}")
                    .build(),
                onError = { imageLoadFailed = true }
            ),
            contentDescription = "Contact photo",
            modifier = base,
            contentScale = ContentScale.Crop
        )
    } else {
        val (container, content) = monogramColors(contact.name)
        Box(
            modifier = base.background(container),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.firstOrNull()
                    ?.let { if (!it.isLetter()) "#" else it.uppercaseChar().toString() } ?: "?",
                color = content,
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun monogramColors(name: String): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (Math.floorMod(name.trim().lowercase().hashCode(), 3)) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
}

/** Theme-aware tint for a call-log entry's direction. */
@Composable
fun callTypeColor(callType: String?): Color {
    val scheme = MaterialTheme.colorScheme
    return when (callType) {
        "missed", "rejected" -> scheme.error
        "incoming" -> scheme.tertiary
        "outgoing" -> scheme.primary
        else -> scheme.onSurfaceVariant
    }
}
