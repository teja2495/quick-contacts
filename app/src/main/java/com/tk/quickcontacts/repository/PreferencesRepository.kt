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
        defaultMessagingApp: MessagingApp,
        isDirectDialEnabled: Boolean = true
    ) {
        try {
            sharedPreferences.edit()
                .putBoolean("international_detection_enabled", isInternationalDetectionEnabled)
                .putBoolean("recent_calls_visible", isRecentCallsVisible)
                .putString("default_messaging_app", defaultMessagingApp.name)
                .putBoolean("direct_dial_enabled", isDirectDialEnabled)
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
    
    // Direct dial setting
    fun saveDirectDialEnabled(isEnabled: Boolean) {
        try {
            sharedPreferences.edit().putBoolean("direct_dial_enabled", isEnabled).apply()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error saving direct dial setting", e)
        }
    }
    
    fun loadDirectDialEnabled(): Boolean {
        return try {
            sharedPreferences.getBoolean("direct_dial_enabled", true) // Default is true (enabled)
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error loading direct dial setting", e)
            true // Default to enabled if error
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
    
    fun hasShownRecentCallsHint(): Boolean {
        return try {
            sharedPreferences.getBoolean("has_shown_recent_calls_hint", false)
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error checking recent calls hint", e)
            false // Default to not shown if error
        }
    }

    fun markRecentCallsHintShown() {
        try {
            sharedPreferences.edit().putBoolean("has_shown_recent_calls_hint", true).apply()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error marking recent calls hint shown", e)
        }
    }

    fun resetRecentCallsHintFlag() {
        try {
            sharedPreferences.edit().putBoolean("has_shown_recent_calls_hint", false).apply()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error resetting recent calls hint flag", e)
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
    
    // Home country code persistence
    fun saveHomeCountryCode(code: String) {
        try {
            sharedPreferences.edit().putString("home_country_code", code).apply()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error saving home country code", e)
        }
    }

    fun loadHomeCountryCode(): String? {
        return try {
            sharedPreferences.getString("home_country_code", null)
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error loading home country code", e)
            null
        }
    }

    fun clearHomeCountryCode() {
        try {
            sharedPreferences.edit().remove("home_country_code").apply()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error clearing home country code", e)
        }
    }
    
    // Recent calls persistence
    fun saveRecentCalls(recentCalls: List<Contact>) {
        try {
            android.util.Log.d("PreferencesRepository", "Saving ${recentCalls.size} recent calls to storage")
            
            // Validate contacts before saving
            val validRecentCalls = recentCalls.filter { ContactUtils.isValidContact(it) }
            
            if (validRecentCalls.size != recentCalls.size) {
                android.util.Log.w("PreferencesRepository", "Removing ${recentCalls.size - validRecentCalls.size} invalid recent calls before saving")
            }
            
            val json = gson.toJson(validRecentCalls)
            android.util.Log.d("PreferencesRepository", "Serialized recent calls to JSON: ${json.length} characters")
            
            sharedPreferences.edit().putString("recent_calls", json).apply()
            
            android.util.Log.d("PreferencesRepository", "Successfully saved ${validRecentCalls.size} recent calls to SharedPreferences")
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error saving recent calls", e)
        }
    }

    fun loadRecentCalls(): List<Contact> {
        return try {
            android.util.Log.d("PreferencesRepository", "Loading recent calls from SharedPreferences")
            val json = sharedPreferences.getString("recent_calls", null)
            if (json != null) {
                android.util.Log.d("PreferencesRepository", "Found JSON data: ${json.length} characters")
                val type = object : TypeToken<List<Contact>>() {}.type
                val recentCalls = gson.fromJson<List<Contact>>(json, type) ?: emptyList()
                
                android.util.Log.d("PreferencesRepository", "Deserialized ${recentCalls.size} recent calls from JSON")
                
                // Validate loaded recent calls
                val validRecentCalls = recentCalls.filter { ContactUtils.isValidContact(it) }
                
                if (validRecentCalls.size != recentCalls.size) {
                    android.util.Log.w("PreferencesRepository", "Removing ${recentCalls.size - validRecentCalls.size} invalid recent calls from storage")
                    // Save the cleaned list back
                    saveRecentCalls(validRecentCalls)
                }
                
                android.util.Log.d("PreferencesRepository", "Successfully loaded ${validRecentCalls.size} valid recent calls")
                validRecentCalls
            } else {
                android.util.Log.d("PreferencesRepository", "No recent calls found in SharedPreferences")
                emptyList()
            }
        } catch (e: JsonSyntaxException) {
            android.util.Log.e("PreferencesRepository", "Error parsing recent calls JSON", e)
            // Clear corrupted data
            sharedPreferences.edit().remove("recent_calls").apply()
            emptyList()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error loading recent calls", e)
            emptyList()
        }
    }
    
    fun clearRecentCalls() {
        try {
            sharedPreferences.edit().remove("recent_calls").apply()
            android.util.Log.d("PreferencesRepository", "Cleared saved recent calls")
        } catch (e: Exception) {
            android.util.Log.e("PreferencesRepository", "Error clearing recent calls", e)
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