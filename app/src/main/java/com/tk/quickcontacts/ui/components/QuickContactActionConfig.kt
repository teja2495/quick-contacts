package com.tk.quickcontacts.ui.components

import com.tk.quickcontacts.models.CustomActions
import com.tk.quickcontacts.models.MessagingApp

object QuickContactAction {
    const val NONE = "None"
    const val ALL_OPTIONS = "All Options"
    const val CALL = "Call"
    const val MESSAGE = "Message"
    const val GOOGLE_MEET = "Google Meet"
    const val WHATSAPP_CHAT = "WhatsApp Chat"
    const val WHATSAPP_VOICE_CALL = "WhatsApp Voice Call"
    const val WHATSAPP_VIDEO_CALL = "WhatsApp Video Call"
    const val TELEGRAM_CHAT = "Telegram Chat"
    const val TELEGRAM_VOICE_CALL = "Telegram Voice Call"
    const val TELEGRAM_VIDEO_CALL = "Telegram Video Call"
    const val SIGNAL_CHAT = "Signal Chat"
    const val SIGNAL_VOICE_CALL = "Signal Voice Call"
    const val SIGNAL_VIDEO_CALL = "Signal Video Call"

    val editableOptions: List<String> = listOf(
        CALL,
        MESSAGE,
        GOOGLE_MEET,
        WHATSAPP_CHAT,
        WHATSAPP_VOICE_CALL,
        WHATSAPP_VIDEO_CALL,
        TELEGRAM_CHAT,
        TELEGRAM_VOICE_CALL,
        TELEGRAM_VIDEO_CALL,
        SIGNAL_CHAT,
        SIGNAL_VOICE_CALL,
        SIGNAL_VIDEO_CALL,
        ALL_OPTIONS,
        NONE
    )

    val executableOptions: List<String> = editableOptions.filter {
        it != NONE && it != ALL_OPTIONS
    }

    fun editableOptionsFiltered(availableActions: Set<String>): List<String> {
        val alwaysShown = setOf(CALL, MESSAGE, ALL_OPTIONS, NONE)
        return editableOptions.filter { it in alwaysShown || it in availableActions }
    }

    fun executableOptionsFiltered(availableActions: Set<String>): List<String> {
        return editableOptionsFiltered(availableActions).filter {
            it != NONE && it != ALL_OPTIONS
        }
    }
}

enum class QuickContactActionSlot(
    val sectionTitle: String,
    val interactionTitle: String
) {
    CARD_TAP("Card", "Tap"),
    CARD_LONG_PRESS("Card", "Tap and Hold"),
    FIRST_BUTTON_TAP("Button 1", "Tap"),
    FIRST_BUTTON_LONG_PRESS("Button 1", "Tap and Hold"),
    SECOND_BUTTON_TAP("Button 2", "Tap"),
    SECOND_BUTTON_LONG_PRESS("Button 2", "Tap and Hold")
}

data class ResolvedQuickContactActions(
    val cardTapAction: String,
    val cardLongPressAction: String,
    val firstButtonTapAction: String,
    val firstButtonLongPressAction: String,
    val secondButtonTapAction: String,
    val secondButtonLongPressAction: String
) {
    fun actionFor(slot: QuickContactActionSlot): String {
        return when (slot) {
            QuickContactActionSlot.CARD_TAP -> cardTapAction
            QuickContactActionSlot.CARD_LONG_PRESS -> cardLongPressAction
            QuickContactActionSlot.FIRST_BUTTON_TAP -> firstButtonTapAction
            QuickContactActionSlot.FIRST_BUTTON_LONG_PRESS -> firstButtonLongPressAction
            QuickContactActionSlot.SECOND_BUTTON_TAP -> secondButtonTapAction
            QuickContactActionSlot.SECOND_BUTTON_LONG_PRESS -> secondButtonLongPressAction
        }
    }

    fun update(slot: QuickContactActionSlot, action: String): ResolvedQuickContactActions {
        return when (slot) {
            QuickContactActionSlot.CARD_TAP -> copy(cardTapAction = action)
            QuickContactActionSlot.CARD_LONG_PRESS -> copy(cardLongPressAction = action)
            QuickContactActionSlot.FIRST_BUTTON_TAP -> copy(firstButtonTapAction = action)
            QuickContactActionSlot.FIRST_BUTTON_LONG_PRESS -> copy(firstButtonLongPressAction = action)
            QuickContactActionSlot.SECOND_BUTTON_TAP -> copy(secondButtonTapAction = action)
            QuickContactActionSlot.SECOND_BUTTON_LONG_PRESS -> copy(secondButtonLongPressAction = action)
        }
    }

    fun toCustomActions(): CustomActions {
        return CustomActions(
            primaryAction = cardTapAction,
            secondaryAction = firstButtonTapAction,
            cardLongPressAction = cardLongPressAction,
            firstButtonLongPressAction = firstButtonLongPressAction,
            secondButtonTapAction = secondButtonTapAction,
            secondButtonLongPressAction = secondButtonLongPressAction
        )
    }
}

fun resolveQuickContactActions(
    customActions: CustomActions?,
    defaultMessagingApp: MessagingApp
): ResolvedQuickContactActions {
    val defaultCardTapAction = QuickContactAction.CALL
    val defaultFirstButtonTapAction = defaultMessagingAction(defaultMessagingApp)

    return ResolvedQuickContactActions(
        cardTapAction = normalizeQuickContactAction(customActions?.primaryAction, defaultCardTapAction),
        cardLongPressAction = normalizeQuickContactAction(customActions?.cardLongPressAction, QuickContactAction.ALL_OPTIONS),
        firstButtonTapAction = normalizeQuickContactAction(customActions?.secondaryAction, defaultFirstButtonTapAction),
        firstButtonLongPressAction = normalizeQuickContactAction(customActions?.firstButtonLongPressAction, QuickContactAction.ALL_OPTIONS),
        secondButtonTapAction = normalizeQuickContactAction(customActions?.secondButtonTapAction, QuickContactAction.ALL_OPTIONS),
        secondButtonLongPressAction = normalizeQuickContactAction(customActions?.secondButtonLongPressAction, QuickContactAction.ALL_OPTIONS)
    )
}

fun actionNeedsPhoneNumber(action: String): Boolean {
    return action != QuickContactAction.NONE &&
        action != QuickContactAction.ALL_OPTIONS
}

fun normalizeQuickContactAction(action: String?, fallbackAction: String): String {
    val normalized = when (action?.trim()) {
        null, "" -> fallbackAction
        "SMS", "Messages" -> QuickContactAction.MESSAGE
        "WhatsApp" -> QuickContactAction.WHATSAPP_CHAT
        "Telegram" -> QuickContactAction.TELEGRAM_CHAT
        "Signal" -> QuickContactAction.SIGNAL_CHAT
        else -> action
    }
    return if (QuickContactAction.editableOptions.contains(normalized)) {
        normalized
    } else {
        fallbackAction
    }
}

private fun defaultMessagingAction(defaultMessagingApp: MessagingApp): String {
    return when (defaultMessagingApp) {
        MessagingApp.WHATSAPP -> QuickContactAction.WHATSAPP_CHAT
        MessagingApp.SMS -> QuickContactAction.MESSAGE
        MessagingApp.TELEGRAM -> QuickContactAction.TELEGRAM_CHAT
        MessagingApp.SIGNAL -> QuickContactAction.SIGNAL_CHAT
    }
}
