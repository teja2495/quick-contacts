package com.tk.quickcontacts.models

enum class MessagingApp {
    WHATSAPP,
    SMS,
    TELEGRAM
}

data class CustomActions(
    val primaryAction: String,
    val secondaryAction: String
) 