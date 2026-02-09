package com.tk.quickcontacts.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.tk.quickcontacts.R
import com.tk.quickcontacts.utils.PhoneNumberUtils

object GoogleMeetActions {
    private const val MEET_PACKAGE = "com.google.android.apps.tachyon"
    private const val MEET_CALL_ACTION = "com.google.android.apps.tachyon.action.CALL"

    fun openGoogleMeet(
        context: Context,
        dataId: Long,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        return try {
            val pm = context.packageManager
            if (pm.getLaunchIntentForPackage(MEET_PACKAGE) == null) {
                onShowToast?.invoke(R.string.error_google_meet_not_installed)
                return false
            }

            var phoneNumber: String? = null
            try {
                val phoneUri = Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, dataId.toString())
                val phoneCursor = context.contentResolver.query(
                    phoneUri,
                    arrayOf(ContactsContract.Data.DATA1),
                    null,
                    null,
                    null,
                )
                phoneCursor?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        phoneNumber = cursor.getString(0)
                    }
                }
            } catch (e: Exception) {
                Log.w("GoogleMeetActions", "Failed to extract phone number from dataId", e)
            }

            if (phoneNumber.isNullOrBlank()) {
                Log.w("GoogleMeetActions", "No phone number found for dataId $dataId")
                onShowToast?.invoke(R.string.error_google_meet_no_phone)
                return false
            }

            openGoogleMeetWithNumber(context, phoneNumber!!, onShowToast)
        } catch (e: Exception) {
            Log.e("GoogleMeetActions", "Failed to open Google Meet video call", e)
            onShowToast?.invoke(R.string.error_google_meet_video_call_failed)
            false
        }
    }

    fun openGoogleMeet(
        context: Context,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            Log.w("GoogleMeetActions", "Invalid phone number for Google Meet: $phoneNumber")
            onShowToast?.invoke(R.string.error_google_meet_no_phone)
            return false
        }
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber.isNullOrBlank()) {
            Log.w("GoogleMeetActions", "Could not clean phone number: $phoneNumber")
            onShowToast?.invoke(R.string.error_google_meet_no_phone)
            return false
        }
        return openGoogleMeetWithNumber(context, cleanNumber, onShowToast)
    }

    private fun openGoogleMeetWithNumber(
        context: Context,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        return try {
            val pm = context.packageManager
            if (pm.getLaunchIntentForPackage(MEET_PACKAGE) == null) {
                onShowToast?.invoke(R.string.error_google_meet_not_installed)
                return false
            }

            val callIntent = Intent(MEET_CALL_ACTION).apply {
                data = Uri.parse("tel:$phoneNumber")
                setPackage(MEET_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (callIntent.resolveActivity(pm) != null) {
                context.startActivity(callIntent)
                true
            } else {
                Log.w("GoogleMeetActions", "Google Meet call action not resolved")
                onShowToast?.invoke(R.string.error_google_meet_call_failed)
                false
            }
        } catch (e: Exception) {
            Log.e("GoogleMeetActions", "Failed to open Google Meet video call", e)
            onShowToast?.invoke(R.string.error_google_meet_video_call_failed)
            false
        }
    }
}
