package com.tk.quickcontacts.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Receives WhatsApp call notifications after the user grants Notification Access. */
class WhatsAppCallNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!WhatsAppCallStore.isEnabled(this) || sbn.packageName != WHATSAPP_PACKAGE) return

        val notification = sbn.notification
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            ?.trim()
            .orEmpty()
        val content = listOf(
            notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        ).filterNotNull().joinToString(" ").lowercase()
        val callType = content.toCallType() ?: return
        if (title.isBlank()) return

        WhatsAppCallStore.record(
            this,
            WhatsAppCallStore.Event(
                notificationKey = sbn.key,
                name = title,
                phoneNumber = PHONE_NUMBER_REGEX.find("$title $content")?.value,
                callType = callType,
                timestamp = sbn.postTime,
            )
        )
    }

    private fun String.toCallType(): String? = when {
        "missed" in this -> "missed"
        "declined" in this || "rejected" in this -> "rejected"
        "outgoing" in this || "calling" in this -> "outgoing"
        "incoming" in this || "voice call" in this || "video call" in this -> "incoming"
        else -> null
    }

    private companion object {
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        val PHONE_NUMBER_REGEX = Regex("\\+?[0-9][0-9()\\- ]{5,}[0-9]")
    }
}
