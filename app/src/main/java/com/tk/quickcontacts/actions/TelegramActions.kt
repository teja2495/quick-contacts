package com.tk.quickcontacts.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tk.quickcontacts.R
import com.tk.quickcontacts.utils.ContactMethodMimeTypes
import com.tk.quickcontacts.utils.PhoneNumberUtils

object TelegramActions {
    private const val TELEGRAM_PACKAGE = "org.telegram.messenger"

    fun openTelegramChat(
        context: Context,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        if (dataId == null) {
            Log.w("TelegramActions", "No dataId provided for Telegram chat")
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    ContactMethodMimeTypes.TELEGRAM_MESSAGE,
                )
                setPackage(TELEGRAM_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
            } else {
                Log.w("TelegramActions", "Telegram chat intent cannot be resolved")
                onShowToast?.invoke(R.string.error_telegram_not_installed)
            }
        } catch (e: Exception) {
            Log.e("TelegramActions", "Failed to open Telegram chat", e)
            onShowToast?.invoke(R.string.error_telegram_chat_failed)
        }
    }

    fun openTelegramChat(
        context: Context,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            Log.w("TelegramActions", "Invalid phone number for Telegram: $phoneNumber")
            return
        }
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            Log.w("TelegramActions", "Could not clean phone number for Telegram: $phoneNumber")
            return
        }
        try {
            val tgUri = Uri.parse("tg://resolve?phone=${Uri.encode(cleanNumber)}")
            val intent = Intent(Intent.ACTION_VIEW, tgUri).apply {
                setPackage(TELEGRAM_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return
            }
            Log.w("TelegramActions", "Telegram app not installed, trying web fallback")
        } catch (e: Exception) {
            Log.w("TelegramActions", "Telegram tg:// method failed", e)
        }
        try {
            val webUri = Uri.parse("https://t.me/${Uri.encode(cleanNumber)}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Log.w("TelegramActions", "Telegram web fallback failed", e)
        }
    }

    fun openTelegramCall(
        context: Context,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) {
            Log.w("TelegramActions", "No dataId provided for Telegram call")
            return false
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    ContactMethodMimeTypes.TELEGRAM_CALL,
                )
                setPackage(TELEGRAM_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return true
            }
            Log.w("TelegramActions", "Telegram call intent cannot be resolved")
            onShowToast?.invoke(R.string.error_telegram_not_installed)
            return false
        } catch (e: Exception) {
            Log.e("TelegramActions", "Failed to initiate Telegram call", e)
            onShowToast?.invoke(R.string.error_telegram_call_failed)
            return false
        }
    }

    fun openTelegramCall(
        context: Context,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        if (phoneNumber.isBlank()) return
        val normalizedNumber = phoneNumber
            .trim()
            .replace(" ", "")
            .replace("-", "")
            .let { if (it.startsWith("+")) it else "+$it" }
        val tgUri = Uri.parse("tg://resolve?phone=${Uri.encode(normalizedNumber)}&call=1")
        val intent = Intent(Intent.ACTION_VIEW, tgUri).apply {
            setPackage(TELEGRAM_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            onShowToast?.invoke(R.string.error_telegram_not_installed)
        }
    }

    fun openTelegramVideoCall(
        context: Context,
        dataId: Long?,
        phoneNumber: String? = null,
    ): Boolean {
        if (dataId == null) {
            Log.w("TelegramActions", "No dataId provided for Telegram video call")
            return false
        }
        return try {
            val contactDataUri = Uri.parse("content://com.android.contacts/data/$dataId")
            val intent = Intent(Intent.ACTION_VIEW, contactDataUri).apply {
                setPackage(TELEGRAM_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                Log.w("TelegramActions", "No activity found to handle Telegram video call intent")
                false
            }
        } catch (e: Exception) {
            Log.w("TelegramActions", "Telegram video call intent failed", e)
            false
        }
    }

    fun openTelegramVideoCall(
        context: Context,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
        onChatFallback: ((Context, String) -> Unit)? = null,
    ): Boolean {
        if (phoneNumber.isBlank()) return false
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber) ?: return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("tg://resolve?phone=${Uri.encode(cleanNumber)}&videochat=true")
                setPackage(TELEGRAM_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                onShowToast?.invoke(R.string.error_telegram_not_installed)
                onChatFallback?.invoke(context, phoneNumber)
                false
            }
        } catch (e: Exception) {
            Log.w("TelegramActions", "Telegram video call deep-link failed", e)
            onChatFallback?.invoke(context, phoneNumber)
            false
        }
    }
}
