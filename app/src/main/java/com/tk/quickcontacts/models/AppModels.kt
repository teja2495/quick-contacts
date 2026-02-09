package com.tk.quickcontacts.models

import com.google.gson.annotations.Expose

enum class MessagingApp {
    WHATSAPP,
    SMS,
    TELEGRAM
}

data class CustomActions(
    @Expose
    val primaryAction: String,
    @Expose
    val secondaryAction: String,
    @Expose
    val cardLongPressAction: String? = null,
    @Expose
    val firstButtonLongPressAction: String? = null,
    @Expose
    val secondButtonTapAction: String? = null,
    @Expose
    val secondButtonLongPressAction: String? = null
)
