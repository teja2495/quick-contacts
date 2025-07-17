package com.tk.quickcontacts.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.models.CustomActions
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.utils.ContactUtils

class PreferencesRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("QuickContactsPrefs", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder()
        .setLenient() // Allow lenient parsing for backward compatibility
        .excludeFieldsWithoutExposeAnnotation() // Only serialize fields with @Expose annotation
        .create()
    
    // Cache for frequently accessed data
    private var cachedContacts: List<Contact>? = null
    private var cachedActionPreferences: Map<String, Boolean>? = null
    private var cachedCustomActionPreferences: Map<String, CustomActions>? = null
    private var cachedSettings: Triple<Boolean, Boolean, MessagingApp>? = null

    // Contact management with validation
    fun saveContacts(contacts: List<Contact>) {
        try {
            android.util.Log.d("PreferencesRepository", "Saving ${contacts.size} contacts to storage")
            
            // Validate contacts before saving
            val validContacts = contacts.filter { ContactUtils.isValidContact(it) }
            
            if (validContacts.size != contacts.size) {
                android.util.Log.w("PreferencesRepository", "Removing ${contacts.size - validContacts.size} invalid contacts before saving")
            }
            
            val json = gson.toJson(validContacts)
            android.util.Log.d("PreferencesRepository", "Serialized contacts to JSON: ${json.length} characters")
            
            sharedPreferences.edit().putString("selected_contacts", json).apply()
            cachedContacts = validContacts
            
            android.util.Log.d("PreferencesRepository", "Successfully saved ${validContacts.size} contacts to SharedPreferences")
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error saving contacts", e)
        }
    }

    fun loadContacts(): List<Contact> {
        // Return cached value if available
        cachedContacts?.let { 
            android.util.Log.d("PreferencesRepository", "Returning ${it.size} cached contacts")
            return it 
        }
        
        return try {
            android.util.Log.d("PreferencesRepository", "Loading contacts from SharedPreferences")
            val json = sharedPreferences.getString("selected_contacts", null)
            if (json != null) {
                android.util.Log.d("PreferencesRepository", "Found JSON data: ${json.length} characters")
                val type = object : TypeToken<List<Contact>>() {}.type
                val contacts = gson.fromJson<List<Contact>>(json, type) ?: emptyList()
                
                android.util.Log.d("PreferencesRepository", "Deserialized ${contacts.size} contacts from JSON")
                
                // Validate loaded contacts
                val validContacts = contacts.filter { ContactUtils.isValidContact(it) }
                
                if (validContacts.size != contacts.size) {
                    android.util.Log.w("PreferencesRepository", "Removing ${contacts.size - validContacts.size} invalid contacts from storage")
                    // Save the cleaned list back
                    saveContacts(validContacts)
                }
                
                cachedContacts = validContacts
                android.util.Log.d("PreferencesRepository", "Successfully loaded ${validContacts.size} valid contacts")
                validContacts
            } else {
                android.util.Log.d("PreferencesRepository", "No contacts found in SharedPreferences")
                emptyList()
            }
        } catch (e: JsonSyntaxException) {
            android.util.Log.e("PreferencesRepository", "Error parsing contacts JSON", e)
            // Clear corrupted data
            sharedPreferences.edit().remove("selected_contacts").apply()
            emptyList()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error loading contacts", e)
            emptyList()
        }
    }

    // Action preferences with validation
    fun saveActionPreferences(preferences: Map<String, Boolean>) {
        try {
            val json = gson.toJson(preferences)
            sharedPreferences.edit().putString("action_preferences", json).apply()
            cachedActionPreferences = preferences
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error saving action preferences", e)
        }
    }

    fun loadActionPreferences(): Map<String, Boolean> {
        // Return cached value if available
        cachedActionPreferences?.let { return it }
        
        return try {
            val json = sharedPreferences.getString("action_preferences", null)
            if (json != null) {
                val type = object : TypeToken<Map<String, Boolean>>() {}.type
                val preferences = gson.fromJson<Map<String, Boolean>>(json, type) ?: emptyMap()
                cachedActionPreferences = preferences
                preferences
            } else {
                emptyMap()
            }
        } catch (e: JsonSyntaxException) {
            android.util.Log.e("PreferencesRepository", "Error parsing action preferences JSON", e)
            // Clear corrupted data
            sharedPreferences.edit().remove("action_preferences").apply()
            emptyMap()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error loading action preferences", e)
            emptyMap()
        }
    }

    // Custom action preferences with validation
    fun saveCustomActionPreferences(preferences: Map<String, CustomActions>) {
        try {
            val json = gson.toJson(preferences)
            sharedPreferences.edit().putString("custom_action_preferences", json).apply()
            cachedCustomActionPreferences = preferences
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error saving custom action preferences", e)
        }
    }

    fun loadCustomActionPreferences(): Map<String, CustomActions> {
        // Return cached value if available
        cachedCustomActionPreferences?.let { return it }
        
        return try {
            val json = sharedPreferences.getString("custom_action_preferences", null)
            if (json != null) {
                val type = object : TypeToken<Map<String, CustomActions>>() {}.type
                val preferences = gson.fromJson<Map<String, CustomActions>>(json, type) ?: emptyMap()
                cachedCustomActionPreferences = preferences
                preferences
            } else {
                emptyMap()
            }
        } catch (e: JsonSyntaxException) {
            android.util.Log.e("PreferencesRepository", "Error parsing custom action preferences JSON", e)
            // Clear corrupted data
            sharedPreferences.edit().remove("custom_action_preferences").apply()
            emptyMap()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error loading custom action preferences", e)
            emptyMap()
        }
    }

    // Settings with validation
    fun saveSettings(
        isInternationalDetectionEnabled: Boolean,
        isRecentCallsVisible: Boolean,
        defaultMessagingApp: MessagingApp
    ) {
        try {
            sharedPreferences.edit()
                .putBoolean("international_detection_enabled", isInternationalDetectionEnabled)
                .putBoolean("recent_calls_visible", isRecentCallsVisible)
                .putString("default_messaging_app", defaultMessagingApp.name)
                .apply()
            cachedSettings = Triple(isInternationalDetectionEnabled, isRecentCallsVisible, defaultMessagingApp)
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error saving settings", e)
        }
    }

    fun loadSettings(): Triple<Boolean, Boolean, MessagingApp> {
        // Return cached value if available
        cachedSettings?.let { return it }
        
        return try {
            val isInternationalDetectionEnabled = sharedPreferences.getBoolean("international_detection_enabled", false)
            val isRecentCallsVisible = sharedPreferences.getBoolean("recent_calls_visible", true)
            
            // Load messaging app preference with backward compatibility and validation
            val messagingAppString = sharedPreferences.getString("default_messaging_app", null)
            val defaultMessagingApp = if (messagingAppString != null) {
                try {
                    MessagingApp.valueOf(messagingAppString)
                } catch (e: IllegalArgumentException) {
                    android.util.Log.w("PreferencesRepository", "Invalid messaging app in preferences: $messagingAppString, using default")
                    MessagingApp.WHATSAPP
                }
            } else {
                // Backward compatibility: check old boolean preference
                val useWhatsApp = sharedPreferences.getBoolean("use_whatsapp_as_default", true)
                if (useWhatsApp) MessagingApp.WHATSAPP else MessagingApp.SMS
            }
            
            val settings = Triple(isInternationalDetectionEnabled, isRecentCallsVisible, defaultMessagingApp)
            cachedSettings = settings
            settings
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error loading settings", e)
            // Return default settings
            Triple(false, true, MessagingApp.WHATSAPP)
        }
    }

    // First launch and hints with validation
    fun isFirstTimeLaunch(): Boolean {
        return try {
            sharedPreferences.getBoolean("is_first_launch", true)
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error checking first launch", e)
            true // Default to first launch if error
        }
    }

    fun markFirstLaunchComplete() {
        try {
            sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error marking first launch complete", e)
        }
    }

    fun resetFirstLaunchFlag() {
        try {
            sharedPreferences.edit().putBoolean("is_first_launch", true).apply()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error resetting first launch flag", e)
        }
    }

    fun hasShownEditHint(): Boolean {
        return try {
            sharedPreferences.getBoolean("has_shown_edit_hint", false)
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error checking edit hint", e)
            false // Default to not shown if error
        }
    }

    fun markEditHintShown() {
        try {
            sharedPreferences.edit().putBoolean("has_shown_edit_hint", true).apply()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error marking edit hint shown", e)
        }
    }

    fun resetEditHintFlag() {
        try {
            sharedPreferences.edit().putBoolean("has_shown_edit_hint", false).apply()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error resetting edit hint flag", e)
        }
    }
    
    fun hasSeenCallWarning(): Boolean {
        return try {
            sharedPreferences.getBoolean("has_seen_call_warning", false)
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error checking call warning", e)
            false // Default to not shown if error
        }
    }

    fun markCallWarningSeen() {
        try {
            sharedPreferences.edit().putBoolean("has_seen_call_warning", true).apply()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error marking call warning seen", e)
        }
    }
    
    // Clear cache when needed (e.g., on app restart)
    fun clearCache() {
        try {
            cachedContacts = null
            cachedActionPreferences = null
            cachedCustomActionPreferences = null
            cachedSettings = null
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error clearing cache", e)
        }
    }
    
    // Clear all data (for debugging/testing)
    fun clearAllData() {
        try {
            sharedPreferences.edit().clear().apply()
            clearCache()
            android.util.Log.d("PreferencesRepository", "All data cleared")
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error clearing all data", e)
        }
    }
    
    // Validate all stored data
    fun validateStoredData(): Boolean {
        return try {
            // Test loading all data types
            loadContacts()
            loadActionPreferences()
            loadCustomActionPreferences()
            loadSettings()
            isFirstTimeLaunch()
            hasShownEditHint()
            
            android.util.Log.d("PreferencesRepository", "All stored data validated successfully")
            true
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Data validation failed", e)
            false
        }
    }
} 