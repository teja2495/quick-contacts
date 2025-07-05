package com.tk.quickcontacts.services

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.tk.quickcontacts.models.MessagingApp

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
        val cleanNumber = phoneNumber.replace("[^\\d+]".toRegex(), "")
        
        try {
            // Method 1: Use ACTION_SENDTO with smsto scheme for direct chat
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanNumber")
                setPackage("com.whatsapp")
            }
            smsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(smsIntent)
        } catch (e: Exception) {
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
                try {
                    // Method 3: Try standard messaging intent
                    val messageIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("sms:$cleanNumber")
                        setPackage("com.whatsapp")
                    }
                    messageIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(messageIntent)
                } catch (e3: Exception) {
                    try {
                        // Method 4: Final fallback to web API
                        val webIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")
                        }
                        webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(webIntent)
                    } catch (e4: Exception) {
                        // WhatsApp not installed or no browser available
                        android.util.Log.e("QuickContacts", "Unable to open WhatsApp: ${e4.message}")
                    }
                }
            }
        }
    }
    
    fun openSmsApp(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.replace("[^\\d+]".toRegex(), "")
        
        try {
            // Use ACTION_SENDTO with sms scheme to open default SMS app
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("sms:$cleanNumber")
            }
            smsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            try {
                // Fallback: Use ACTION_VIEW with sms scheme
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$cleanNumber")
                }
                viewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(viewIntent)
            } catch (e2: Exception) {
                // Final fallback: open generic messaging app
                try {
                    val messageIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "")
                    }
                    messageIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(Intent.createChooser(messageIntent, "Send message"))
                } catch (e3: Exception) {
                    android.util.Log.e("QuickContacts", "Unable to open SMS app: ${e3.message}")
                }
            }
        }
    }
    
    fun openTelegramChat(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.replace("[^\\d+]".toRegex(), "")
        
        try {
            // Method 1: Try to open direct chat using phone number
            val telegramIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("tg://resolve?phone=$cleanNumber")
                setPackage("org.telegram.messenger")
            }
            telegramIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(telegramIntent)
        } catch (e: Exception) {
            try {
                // Method 2: Try with t.me link
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://t.me/$cleanNumber")
                }
                webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                try {
                    // Method 3: Open Telegram app directly
                    val appIntent = Intent(Intent.ACTION_MAIN).apply {
                        setPackage("org.telegram.messenger")
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(appIntent)
                } catch (e3: Exception) {
                    // Telegram not installed
                    android.util.Log.e("QuickContacts", "Unable to open Telegram: ${e3.message}")
                }
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
} 