package com.tk.quickcontacts.services

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
    
    fun openWhatsAppChat(context: Context, phoneNumber: String) {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            android.util.Log.w("MessagingService", "Invalid phone number for WhatsApp: $phoneNumber")
            return
        }
        
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            android.util.Log.w("MessagingService", "Could not clean phone number for WhatsApp: $phoneNumber")
            return
        }
        
        try {
            // Method 1: Use ACTION_SENDTO with smsto scheme for direct chat
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanNumber")
                setPackage("com.whatsapp")
            }
            smsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            android.util.Log.w("MessagingService", "WhatsApp method 1 failed", e)
            try {
                // Method 2: Use ACTION_SEND with WhatsApp package
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra("jid", "$cleanNumber@s.whatsapp.net")
                    setPackage("com.whatsapp")
                }
                sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(sendIntent)
            } catch (e2: Exception) {
                android.util.Log.w("MessagingService", "WhatsApp method 2 failed", e2)
                try {
                    // Method 3: Try standard messaging intent
                    val messageIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("sms:$cleanNumber")
                        setPackage("com.whatsapp")
                    }
                    messageIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(messageIntent)
                } catch (e3: Exception) {
                    android.util.Log.w("MessagingService", "WhatsApp method 3 failed", e3)
                    // Final fallback to SMS
                    openSmsApp(context, phoneNumber)
                }
            }
        }
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
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            android.util.Log.w("MessagingService", "Invalid phone number for Telegram: $phoneNumber")
            return
        }
        
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            android.util.Log.w("MessagingService", "Could not clean phone number for Telegram: $phoneNumber")
            return
        }
        
        try {
            // Method 1: Try to open direct chat using phone number
            val telegramIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("tg://resolve?phone=$cleanNumber")
                setPackage("org.telegram.messenger")
            }
            telegramIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(telegramIntent)
        } catch (e: Exception) {
            android.util.Log.w("MessagingService", "Telegram method 1 failed", e)
            try {
                // Method 2: Open Telegram app directly
                val appIntent = Intent(Intent.ACTION_MAIN).apply {
                    setPackage("org.telegram.messenger")
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(appIntent)
            } catch (e3: Exception) {
                android.util.Log.e("MessagingService", "All Telegram methods failed", e3)
                // Fallback to SMS
                openSmsApp(context, phoneNumber)
            }
        }
    }
    
    fun openMessagingApp(context: Context, phoneNumber: String, defaultApp: MessagingApp) {
        when (defaultApp) {
            MessagingApp.WHATSAPP -> openWhatsAppChat(context, phoneNumber)
            MessagingApp.SMS -> openSmsApp(context, phoneNumber)
            MessagingApp.TELEGRAM -> openTelegramChat(context, phoneNumber)
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