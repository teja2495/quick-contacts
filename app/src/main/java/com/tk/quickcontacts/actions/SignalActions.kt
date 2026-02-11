package com.tk.quickcontacts.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.tk.quickcontacts.R
import com.tk.quickcontacts.utils.ContactMethodMimeTypes
import com.tk.quickcontacts.utils.PhoneNumberUtils

object SignalActions {
    private const val TAG = "SignalActions"
    private const val SIGNAL_PACKAGE = "org.thoughtcrime.securesms"
    private const val CONTACT_DATA_URI_PREFIX = "content://com.android.contacts/data/"
    private const val EXTRA_IS_VIDEO_CALL = "is_video_call"
    private const val EXTRA_IS_VIDEO_OFF = "is_video_off"

    fun openSignalChat(
        context: Context,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) return false
        return launchContactDataIntent(
            context = context,
            dataId = dataId,
            mimeType = ContactMethodMimeTypes.SIGNAL_MESSAGE,
            errorResId = R.string.error_signal_chat_failed,
            onShowToast = onShowToast,
        )
    }

    fun openSignalChat(
        context: Context,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) return false
        val dataId = resolveSignalDataId(context, phoneNumber, ContactMethodMimeTypes.SIGNAL_MESSAGE)
        if (dataId != null && openSignalChat(context, dataId, onShowToast)) return true
        return try {
            val cleaned = PhoneNumberUtils.cleanPhoneNumber(phoneNumber) ?: return false
            val uri = Uri.parse("smsto:${Uri.encode(cleaned)}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                setPackage(SIGNAL_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                onShowToast?.invoke(R.string.error_signal_not_installed)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Signal chat", e)
            onShowToast?.invoke(R.string.error_signal_chat_failed)
            false
        }
    }

    fun openSignalCall(
        context: Context,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) return false
        return launchContactDataIntent(
            context = context,
            dataId = dataId,
            mimeType = ContactMethodMimeTypes.SIGNAL_CALL,
            errorResId = R.string.error_signal_call_failed,
            onShowToast = onShowToast,
        )
    }

    fun openSignalCall(
        context: Context,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) return false
        val dataId = resolveSignalDataId(context, phoneNumber, ContactMethodMimeTypes.SIGNAL_CALL)
        if (dataId != null && openSignalCall(context, dataId, onShowToast)) return true
        openSignalChat(context, phoneNumber, onShowToast)
        return false
    }

    fun openSignalVideoCall(
        context: Context,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) return false
        val contactDataUri = Uri.parse("$CONTACT_DATA_URI_PREFIX$dataId")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contactDataUri, ContactMethodMimeTypes.SIGNAL_VIDEO_CALL)
            setPackage(SIGNAL_PACKAGE)
            putExtra(EXTRA_IS_VIDEO_CALL, true)
            putExtra(EXTRA_IS_VIDEO_OFF, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchIntentIfResolvable(context, intent)) return true
        Log.w(TAG, "Signal video call could not be initiated")
        if (isSignalInstalled(context)) {
            onShowToast?.invoke(R.string.error_signal_video_call_failed)
        } else {
            onShowToast?.invoke(R.string.error_signal_not_installed)
        }
        return false
    }

    fun openSignalVideoCall(
        context: Context,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) return false
        val dataId = resolveSignalDataId(context, phoneNumber, ContactMethodMimeTypes.SIGNAL_VIDEO_CALL)
        if (dataId != null && openSignalVideoCall(context, dataId, onShowToast)) return true
        openSignalChat(context, phoneNumber, onShowToast)
        return false
    }

    private fun launchContactDataIntent(
        context: Context,
        dataId: Long,
        mimeType: String,
        errorResId: Int,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean =
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("$CONTACT_DATA_URI_PREFIX$dataId"), mimeType)
                setPackage(SIGNAL_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntentIfResolvable(context, intent)) {
                true
            } else {
                if (isSignalInstalled(context)) {
                    onShowToast?.invoke(errorResId)
                } else {
                    onShowToast?.invoke(R.string.error_signal_not_installed)
                }
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Signal intent failed for mimeType=$mimeType", e)
            onShowToast?.invoke(errorResId)
            false
        }

    private fun launchIntentIfResolvable(context: Context, intent: Intent): Boolean {
        val canResolve = intent.resolveActivity(context.packageManager) != null
        if (!canResolve) return false
        context.startActivity(intent)
        return true
    }

    private fun isSignalInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(SIGNAL_PACKAGE) != null

    private fun resolveSignalDataId(context: Context, phoneNumber: String, mimeType: String): Long? {
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
            Log.w(TAG, "Failed to resolve Signal dataId for $phoneNumber", e)
            null
        }
    }
}
