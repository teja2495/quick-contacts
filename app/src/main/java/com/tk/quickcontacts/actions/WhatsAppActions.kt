package com.tk.quickcontacts.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.tk.quickcontacts.R
import com.tk.quickcontacts.utils.ContactMethodMimeTypes
import com.tk.quickcontacts.utils.PhoneNumberUtils

object WhatsAppActions {
    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_MESSAGE_MIME = ContactMethodMimeTypes.WHATSAPP_MESSAGE
    private const val WHATSAPP_VOICE_CALL_MIME = ContactMethodMimeTypes.WHATSAPP_VOICE_CALL
    private const val WHATSAPP_VIDEO_CALL_MIME = ContactMethodMimeTypes.WHATSAPP_VIDEO_CALL

    fun openWhatsAppChat(
        context: Context,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        if (dataId == null) {
            Log.w("WhatsAppActions", "No dataId provided for WhatsApp chat")
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    WHATSAPP_MESSAGE_MIME,
                )
                setPackage(WHATSAPP_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
            } else {
                Log.w("WhatsAppActions", "WhatsApp chat intent cannot be resolved")
                onShowToast?.invoke(R.string.error_whatsapp_not_installed)
            }
        } catch (e: Exception) {
            Log.e("WhatsAppActions", "Failed to open WhatsApp chat", e)
            onShowToast?.invoke(R.string.error_whatsapp_chat_failed)
        }
    }

    fun openWhatsAppChat(
        context: Context,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
        onSmsFallback: ((Context, String) -> Unit)? = null,
    ) {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            Log.w("WhatsAppActions", "Invalid phone number for WhatsApp: $phoneNumber")
            return
        }
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            Log.w("WhatsAppActions", "Could not clean phone number for WhatsApp: $phoneNumber")
            return
        }
        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanNumber")
                setPackage(WHATSAPP_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            Log.w("WhatsAppActions", "WhatsApp method 1 failed", e)
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra("jid", "$cleanNumber@s.whatsapp.net")
                    setPackage(WHATSAPP_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(sendIntent)
            } catch (e2: Exception) {
                Log.w("WhatsAppActions", "WhatsApp method 2 failed", e2)
                try {
                    val messageIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("sms:$cleanNumber")
                        setPackage(WHATSAPP_PACKAGE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(messageIntent)
                } catch (e3: Exception) {
                    Log.w("WhatsAppActions", "WhatsApp method 3 failed", e3)
                    onSmsFallback?.invoke(context, cleanNumber)
                }
            }
        }
    }

    fun openWhatsAppCall(
        context: Context,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) {
            Log.w("WhatsAppActions", "No dataId provided for WhatsApp call")
            return false
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    WHATSAPP_VOICE_CALL_MIME,
                )
                setPackage(WHATSAPP_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return true
            } else {
                Log.w("WhatsAppActions", "WhatsApp call intent cannot be resolved")
                onShowToast?.invoke(R.string.error_whatsapp_not_installed)
                return false
            }
        } catch (e: Exception) {
            Log.e("WhatsAppActions", "Failed to initiate WhatsApp call", e)
            onShowToast?.invoke(R.string.error_whatsapp_call_failed)
            return false
        }
    }

    fun openWhatsAppCall(
        context: Context,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
        onChatFallback: ((Context, String) -> Unit)? = null,
    ): Boolean {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) return false
        val dataId = resolveWhatsAppDataId(context, phoneNumber, WHATSAPP_VOICE_CALL_MIME)
        if (dataId != null) {
            return openWhatsAppCall(context, dataId, onShowToast)
        }
        onChatFallback?.invoke(context, phoneNumber)
        return false
    }

    fun openWhatsAppVideoCall(
        context: Context,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) {
            Log.w("WhatsAppActions", "No dataId provided for WhatsApp video call")
            return false
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    WHATSAPP_VIDEO_CALL_MIME,
                )
                setPackage(WHATSAPP_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                Log.w("WhatsAppActions", "No activity found to handle WhatsApp video call")
                onShowToast?.invoke(R.string.error_whatsapp_video_call_unavailable)
                false
            }
        } catch (e: Exception) {
            Log.e("WhatsAppActions", "Failed to open WhatsApp video call", e)
            onShowToast?.invoke(R.string.error_whatsapp_video_call_failed)
            false
        }
    }

    fun openWhatsAppVideoCall(
        context: Context,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
        onChatFallback: ((Context, String) -> Unit)? = null,
    ): Boolean {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) return false
        val dataId = resolveWhatsAppDataId(context, phoneNumber, WHATSAPP_VIDEO_CALL_MIME)
        if (dataId != null) {
            return openWhatsAppVideoCall(context, dataId, onShowToast)
        }
        onChatFallback?.invoke(context, phoneNumber)
        return false
    }

    private fun resolveWhatsAppDataId(context: Context, phoneNumber: String, mimeType: String): Long? {
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber) ?: return null
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
            if (contactId == null) return null
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId.toString(), mimeType),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.Data._ID)
                    if (idx >= 0) cursor.getLong(idx) else null
                } else null
            }
        } catch (e: Exception) {
            Log.w("WhatsAppActions", "Failed to resolve WhatsApp dataId for $phoneNumber", e)
            null
        }
    }
}
