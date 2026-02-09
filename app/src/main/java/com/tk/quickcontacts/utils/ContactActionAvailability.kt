package com.tk.quickcontacts.utils

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactActionAvailability {

    private val CONTACT_REQUIRED_ACTIONS = listOf(
        "WhatsApp Voice Call" to ContactMethodMimeTypes.WHATSAPP_VOICE_CALL,
        "WhatsApp Video Call" to ContactMethodMimeTypes.WHATSAPP_VIDEO_CALL,
        "Telegram Voice Call" to ContactMethodMimeTypes.TELEGRAM_CALL,
        "Telegram Video Call" to ContactMethodMimeTypes.TELEGRAM_VIDEO_CALL,
        "Signal Voice Call" to ContactMethodMimeTypes.SIGNAL_CALL,
        "Signal Video Call" to ContactMethodMimeTypes.SIGNAL_VIDEO_CALL
    )

    fun resolveContactAvailableActions(
        context: Context,
        phoneNumber: String?,
        appAvailableActions: Set<String>
    ): Set<String> {
        if (phoneNumber.isNullOrBlank()) return appAvailableActions
        val result = appAvailableActions.toMutableSet()
        for ((actionName, mimeType) in CONTACT_REQUIRED_ACTIONS) {
            if (actionName in result && !hasContactDataRow(context, phoneNumber, mimeType)) {
                result.remove(actionName)
            }
        }
        return result
    }

    private fun hasContactDataRow(context: Context, phoneNumber: String, mimeType: String): Boolean {
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber) ?: return false
        return try {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(cleanNumber)
            )
            val contactId: Long? = context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    if (idx >= 0) cursor.getLong(idx) else null
                } else null
            }
            if (contactId == null) return false
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId.toString(), mimeType),
                null
            )?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        } catch (_: Exception) {
            false
        }
    }
}
