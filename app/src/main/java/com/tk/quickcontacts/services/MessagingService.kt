package com.tk.quickcontacts.services

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.tk.quickcontacts.actions.GoogleMeetActions
import com.tk.quickcontacts.actions.SignalActions
import com.tk.quickcontacts.actions.TelegramActions
import com.tk.quickcontacts.actions.WhatsAppActions
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.utils.PhoneNumberUtils

class MessagingService {
    
    fun checkAvailableMessagingApps(packageManager: PackageManager): Set<MessagingApp> {
        val availableApps = mutableSetOf<MessagingApp>()
        
        // SMS is always available
        availableApps.add(MessagingApp.SMS)
        
        // Check if WhatsApp is installed
        if (isWhatsAppInstalled(packageManager)) {
            availableApps.add(MessagingApp.WHATSAPP)
        }
        
        // Check if Telegram is installed
        if (isTelegramInstalled(packageManager)) {
            availableApps.add(MessagingApp.TELEGRAM)
        }
        if (isSignalInstalled(packageManager)) {
            availableApps.add(MessagingApp.SIGNAL)
        }
        
        // Debug logging
        android.util.Log.d("QuickContacts", "Available messaging apps: $availableApps")
        
        return availableApps
    }
    
    private fun isWhatsAppInstalled(packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    private fun isTelegramInstalled(packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo("org.telegram.messenger", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isSignalInstalled(packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo("org.thoughtcrime.securesms", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isGoogleMeetInstalled(packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo("com.google.android.apps.tachyon", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun checkAvailableActions(packageManager: PackageManager): Set<String> {
        val actions = mutableSetOf<String>()
        actions.add("Call")
        actions.add("Message")
        if (isWhatsAppInstalled(packageManager)) {
            actions.add("WhatsApp Chat")
            actions.add("WhatsApp Voice Call")
            actions.add("WhatsApp Video Call")
        }
        if (isTelegramInstalled(packageManager)) {
            actions.add("Telegram Chat")
            actions.add("Telegram Voice Call")
            actions.add("Telegram Video Call")
        }
        if (isSignalInstalled(packageManager)) {
            actions.add("Signal Chat")
            actions.add("Signal Voice Call")
            actions.add("Signal Video Call")
        }
        if (isGoogleMeetInstalled(packageManager)) {
            actions.add("Google Meet")
        }
        return actions
    }

    fun openWhatsAppChat(context: Context, phoneNumber: String) {
        WhatsAppActions.openWhatsAppChat(context, phoneNumber, onSmsFallback = { ctx, num -> openSmsApp(ctx, num) })
    }

    fun openWhatsAppChat(context: Context, dataId: Long?) {
        WhatsAppActions.openWhatsAppChat(context, dataId)
    }
    
    fun openSmsApp(context: Context, phoneNumber: String) {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            android.util.Log.w("MessagingService", "Invalid phone number for SMS: $phoneNumber")
            return
        }
        
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            android.util.Log.w("MessagingService", "Could not clean phone number for SMS: $phoneNumber")
            return
        }
        
        try {
            // Use ACTION_SENDTO with sms scheme to open default SMS app
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("sms:$cleanNumber")
            }
            smsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            android.util.Log.w("MessagingService", "SMS method 1 failed", e)
            try {
                // Fallback: Use ACTION_VIEW with sms scheme
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$cleanNumber")
                }
                viewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(viewIntent)
            } catch (e2: Exception) {
                android.util.Log.w("MessagingService", "SMS method 2 failed", e2)
                // Final fallback: open generic messaging app
                try {
                    val messageIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "")
                    }
                    messageIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(Intent.createChooser(messageIntent, "Send message"))
                } catch (e3: Exception) {
                    android.util.Log.e("MessagingService", "All SMS methods failed", e3)
                }
            }
        }
    }
    
    fun openTelegramChat(context: Context, phoneNumber: String) {
        TelegramActions.openTelegramChat(context, phoneNumber)
    }

    fun openTelegramChat(context: Context, dataId: Long?) {
        TelegramActions.openTelegramChat(context, dataId)
    }

    fun openWhatsAppVoiceCall(context: Context, phoneNumber: String) {
        WhatsAppActions.openWhatsAppCall(
            context,
            phoneNumber,
            onChatFallback = { ctx, num -> WhatsAppActions.openWhatsAppChat(ctx, num, onSmsFallback = { c, n -> openSmsApp(c, n) }) },
        )
    }

    fun openWhatsAppVoiceCall(context: Context, dataId: Long?): Boolean {
        return WhatsAppActions.openWhatsAppCall(context, dataId)
    }

    fun openWhatsAppVideoCall(context: Context, phoneNumber: String) {
        WhatsAppActions.openWhatsAppVideoCall(
            context,
            phoneNumber,
            onChatFallback = { ctx, num -> WhatsAppActions.openWhatsAppChat(ctx, num, onSmsFallback = { c, n -> openSmsApp(c, n) }) },
        )
    }

    fun openWhatsAppVideoCall(context: Context, dataId: Long?): Boolean {
        return WhatsAppActions.openWhatsAppVideoCall(context, dataId)
    }

    fun openTelegramVoiceCall(context: Context, phoneNumber: String) {
        TelegramActions.openTelegramCall(
            context,
            phoneNumber,
            onShowToast = { resId -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() },
        )
    }

    fun openTelegramVoiceCall(context: Context, dataId: Long?): Boolean {
        return TelegramActions.openTelegramCall(
            context,
            dataId,
            onShowToast = { resId -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() },
        )
    }

    fun openTelegramVideoCall(context: Context, phoneNumber: String) {
        TelegramActions.openTelegramVideoCall(
            context,
            phoneNumber,
            onShowToast = { resId -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() },
            onChatFallback = { ctx, num -> openTelegramChat(ctx, num) },
        )
    }

    fun openTelegramVideoCall(context: Context, dataId: Long?): Boolean {
        return TelegramActions.openTelegramVideoCall(context, dataId)
    }

    fun openSignalChat(context: Context, phoneNumber: String) {
        SignalActions.openSignalChat(
            context,
            phoneNumber,
            onShowToast = { resId -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() },
        )
    }

    fun openSignalChat(context: Context, dataId: Long?) {
        SignalActions.openSignalChat(
            context,
            dataId,
            onShowToast = { resId -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() },
        )
    }

    fun openSignalVoiceCall(context: Context, phoneNumber: String): Boolean {
        return SignalActions.openSignalCall(
            context,
            phoneNumber,
            onShowToast = { resId -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() },
        )
    }

    fun openSignalVoiceCall(context: Context, dataId: Long?): Boolean {
        return SignalActions.openSignalCall(
            context,
            dataId,
            onShowToast = { resId -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() },
        )
    }

    fun openSignalVideoCall(context: Context, phoneNumber: String): Boolean {
        return SignalActions.openSignalVideoCall(
            context,
            phoneNumber,
            onShowToast = { resId -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() },
        )
    }

    fun openSignalVideoCall(context: Context, dataId: Long?): Boolean {
        return SignalActions.openSignalVideoCall(
            context,
            dataId,
            onShowToast = { resId -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() },
        )
    }

    fun openGoogleMeet(context: Context, phoneNumber: String): Boolean {
        return GoogleMeetActions.openGoogleMeet(
            context,
            phoneNumber,
            onShowToast = { resId -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() }
        )
    }

    fun openGoogleMeet(context: Context, dataId: Long?): Boolean {
        if (dataId == null) return false
        return GoogleMeetActions.openGoogleMeet(
            context,
            dataId,
            onShowToast = { resId -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() }
        )
    }
    
    fun openMessagingApp(context: Context, phoneNumber: String, defaultApp: MessagingApp) {
        when (defaultApp) {
            MessagingApp.WHATSAPP -> openWhatsAppChat(context, phoneNumber)
            MessagingApp.SMS -> openSmsApp(context, phoneNumber)
            MessagingApp.TELEGRAM -> openTelegramChat(context, phoneNumber)
            MessagingApp.SIGNAL -> openSignalChat(context, phoneNumber)
        }
    }
    
    fun openSmsAppDirectly(context: Context) {
        try {
            // Always use ACTION_SENDTO with sms: URI to open the default SMS app's compose screen
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("sms:")
            }
            smsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            android.util.Log.w("MessagingService", "Error opening SMS app directly, trying alternative method", e)
            try {
                // Alternative: try to open with ACTION_MAIN and CATEGORY_APP_MESSAGING
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e2: Exception) {
                android.util.Log.e("MessagingService", "All SMS app opening methods failed", e2)
            }
        }
    }

} 
