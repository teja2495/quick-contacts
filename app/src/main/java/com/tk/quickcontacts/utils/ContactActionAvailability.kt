package com.tk.quickcontacts.utils

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.content.pm.PackageManager

object ContactActionAvailability {

    private val CONTACT_REQUIRED_ACTIONS = listOf(
        "WhatsApp Voice Call" to ContactMethodMimeTypes.WHATSAPP_VOICE_CALL,
        "WhatsApp Video Call" to ContactMethodMimeTypes.WHATSAPP_VIDEO_CALL,
        "Telegram Voice Call" to ContactMethodMimeTypes.TELEGRAM_CALL,
        "Telegram Video Call" to ContactMethodMimeTypes.TELEGRAM_VIDEO_CALL,
        "Signal Voice Call" to ContactMethodMimeTypes.SIGNAL_CALL,
        "Signal Video Call" to ContactMethodMimeTypes.SIGNAL_VIDEO_CALL
    )

    private val CONTACT_ACTIONS_BY_MIMETYPE = listOf(
        "WhatsApp Chat" to ContactMethodMimeTypes.WHATSAPP_MESSAGE,
        "WhatsApp Voice Call" to ContactMethodMimeTypes.WHATSAPP_VOICE_CALL,
        "WhatsApp Video Call" to ContactMethodMimeTypes.WHATSAPP_VIDEO_CALL,
        "Telegram Chat" to ContactMethodMimeTypes.TELEGRAM_MESSAGE,
        "Telegram Voice Call" to ContactMethodMimeTypes.TELEGRAM_CALL,
        "Telegram Video Call" to ContactMethodMimeTypes.TELEGRAM_VIDEO_CALL,
        "Signal Chat" to ContactMethodMimeTypes.SIGNAL_MESSAGE,
        "Signal Voice Call" to ContactMethodMimeTypes.SIGNAL_CALL,
        "Signal Video Call" to ContactMethodMimeTypes.SIGNAL_VIDEO_CALL
    )

    fun getContactAvailableActions(context: Context, phoneNumber: String?): Set<String> {
        val result = mutableSetOf<String>()
        if (phoneNumber.isNullOrBlank()) return result
        result.add("Call")
        result.add("Message")
        for ((actionName, mimeType) in CONTACT_ACTIONS_BY_MIMETYPE) {
            if (hasContactDataRow(context, phoneNumber, mimeType)) {
                result.add(actionName)
            }
        }
        if (isGoogleMeetInstalled(context)) {
            result.add("Google Meet")
        }
        return result
    }

    private fun isGoogleMeetInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.apps.tachyon", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

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
