package com.tk.quickcontacts.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.models.CustomActions
import com.tk.quickcontacts.models.MessagingApp

class PreferencesRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("QuickContactsPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Contact management
    fun saveContacts(contacts: List<Contact>) {
        val json = gson.toJson(contacts)
        sharedPreferences.edit().putString("selected_contacts", json).apply()
    }

    fun loadContacts(): List<Contact> {
        val json = sharedPreferences.getString("selected_contacts", null)
        return if (json != null) {
            val type = object : TypeToken<List<Contact>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    // Action preferences
    fun saveActionPreferences(preferences: Map<String, Boolean>) {
        val json = gson.toJson(preferences)
        sharedPreferences.edit().putString("action_preferences", json).apply()
    }

    fun loadActionPreferences(): Map<String, Boolean> {
        val json = sharedPreferences.getString("action_preferences", null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, Boolean>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyMap()
        }
    }

    // Custom action preferences
    fun saveCustomActionPreferences(preferences: Map<String, CustomActions>) {
        val json = gson.toJson(preferences)
        sharedPreferences.edit().putString("custom_action_preferences", json).apply()
    }

    fun loadCustomActionPreferences(): Map<String, CustomActions> {
        val json = sharedPreferences.getString("custom_action_preferences", null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, CustomActions>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyMap()
        }
    }

    // Settings
    fun saveSettings(
        isInternationalDetectionEnabled: Boolean,
        isRecentCallsVisible: Boolean,
        defaultMessagingApp: MessagingApp
    ) {
        sharedPreferences.edit()
            .putBoolean("international_detection_enabled", isInternationalDetectionEnabled)
            .putBoolean("recent_calls_visible", isRecentCallsVisible)
            .putString("default_messaging_app", defaultMessagingApp.name)
            .apply()
    }

    fun loadSettings(): Triple<Boolean, Boolean, MessagingApp> {
        val isInternationalDetectionEnabled = sharedPreferences.getBoolean("international_detection_enabled", false)
        val isRecentCallsVisible = sharedPreferences.getBoolean("recent_calls_visible", true)
        
        // Load messaging app preference with backward compatibility
        val messagingAppString = sharedPreferences.getString("default_messaging_app", null)
        val defaultMessagingApp = if (messagingAppString != null) {
            try {
                MessagingApp.valueOf(messagingAppString)
            } catch (e: IllegalArgumentException) {
                MessagingApp.WHATSAPP
            }
        } else {
            // Backward compatibility: check old boolean preference
            val useWhatsApp = sharedPreferences.getBoolean("use_whatsapp_as_default", true)
            if (useWhatsApp) MessagingApp.WHATSAPP else MessagingApp.SMS
        }
        
        return Triple(isInternationalDetectionEnabled, isRecentCallsVisible, defaultMessagingApp)
    }

    // First launch and hints
    fun isFirstTimeLaunch(): Boolean {
        return sharedPreferences.getBoolean("is_first_launch", true)
    }

    fun markFirstLaunchComplete() {
        sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
    }

    fun resetFirstLaunchFlag() {
        sharedPreferences.edit().putBoolean("is_first_launch", true).apply()
    }

    fun hasShownEditHint(): Boolean {
        return sharedPreferences.getBoolean("has_shown_edit_hint", false)
    }

    fun markEditHintShown() {
        sharedPreferences.edit().putBoolean("has_shown_edit_hint", true).apply()
    }

    fun resetEditHintFlag() {
        sharedPreferences.edit().putBoolean("has_shown_edit_hint", false).apply()
    }
} 