package com.tk.quickcontacts

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

enum class MessagingApp {
    WHATSAPP,
    SMS,
    TELEGRAM
}

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val _selectedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val selectedContacts: StateFlow<List<Contact>> = _selectedContacts

    private val _recentCalls = MutableStateFlow<List<Contact>>(emptyList())
    val recentCalls: StateFlow<List<Contact>> = _recentCalls
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _filteredSelectedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val filteredSelectedContacts: StateFlow<List<Contact>> = _filteredSelectedContacts
    
    private val _filteredRecentCalls = MutableStateFlow<List<Contact>>(emptyList())
    val filteredRecentCalls: StateFlow<List<Contact>> = _filteredRecentCalls
    
    private val _searchResults = MutableStateFlow<List<Contact>>(emptyList())
    val searchResults: StateFlow<List<Contact>> = _searchResults

    // Action preferences: Map<ContactId, Boolean> where true means actions are swapped
    private val _actionPreferences = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val actionPreferences: StateFlow<Map<String, Boolean>> = _actionPreferences

    // Settings preferences
    private val _isInternationalDetectionEnabled = MutableStateFlow(true)
    val isInternationalDetectionEnabled: StateFlow<Boolean> = _isInternationalDetectionEnabled
    
    // Messaging app preference
    private val _defaultMessagingApp = MutableStateFlow(MessagingApp.WHATSAPP)
    val defaultMessagingApp: StateFlow<MessagingApp> = _defaultMessagingApp
    
    // Backward compatibility - keep the old boolean property for existing UI
    val useWhatsAppAsDefault: StateFlow<Boolean> = _defaultMessagingApp.map { it == MessagingApp.WHATSAPP }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main),
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    private val sharedPreferences: SharedPreferences
    private val gson = Gson()

    init {
        sharedPreferences = application.getSharedPreferences("QuickContactsPrefs", Context.MODE_PRIVATE)
        loadContacts()
        loadActionPreferences()
        loadSettings()
        // Initialize filtered lists
        _filteredSelectedContacts.value = _selectedContacts.value
        _filteredRecentCalls.value = _recentCalls.value
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterContacts()
    }
    
    private fun filterContacts() {
        val query = _searchQuery.value.lowercase().trim()
        
        if (query.isEmpty()) {
            _filteredSelectedContacts.value = _selectedContacts.value
            _filteredRecentCalls.value = _recentCalls.value
            _searchResults.value = emptyList()
        } else {
            // Filter only by name, not phone number
            _filteredSelectedContacts.value = _selectedContacts.value.filter { contact ->
                contact.name.lowercase().contains(query)
            }
            
            _filteredRecentCalls.value = _recentCalls.value.filter { contact ->
                contact.name.lowercase().contains(query)
            }
        }
    }
    
    fun searchAllContacts(context: Context, query: String) {
        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            return
        }
        
        try {
            val searchResultsList = mutableListOf<Contact>()
            val seenContactsMap = mutableMapOf<String, Contact>()
            
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            
            // Search only by name, not phone number
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")
            
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            
            cursor?.use {
                val idColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                if (idColumn >= 0 && nameColumn >= 0 && numberColumn >= 0) {
                    while (it.moveToNext()) {
                        val id = it.getString(idColumn)
                        val name = it.getString(nameColumn)
                        val number = it.getString(numberColumn)
                        
                        if (id != null && name != null && number != null) {
                            
                            if (seenContactsMap.containsKey(id)) {
                                // Add phone number to existing contact (normalize to avoid duplicates)
                                val existingContact = seenContactsMap[id]!!
                                val updatedPhoneNumbers = existingContact.phoneNumbers.toMutableList()
                                val normalizedNewNumber = normalizePhoneNumber(number)
                                
                                // Check if this normalized number already exists
                                val alreadyExists = updatedPhoneNumbers.any { 
                                    normalizePhoneNumber(it) == normalizedNewNumber 
                                }
                                
                                if (!alreadyExists) {
                                    updatedPhoneNumbers.add(number)
                                    seenContactsMap[id] = existingContact.copy(phoneNumbers = updatedPhoneNumbers)
                                }
                            } else {
                                // Create new contact
                                val contact = Contact(
                                    id = id,
                                    name = name,
                                    phoneNumber = number, // Primary phone number
                                    phoneNumbers = listOf(number),
                                    photoUri = null // Not using photos in search results
                                )
                                seenContactsMap[id] = contact
                            }
                        }
                    }
                }
            }
            
            // Convert map to list and limit results
            _searchResults.value = seenContactsMap.values.take(20)
        } catch (e: Exception) {
            e.printStackTrace()
            _searchResults.value = emptyList()
        }
    }

    private fun loadContacts() {
        val json = sharedPreferences.getString("selected_contacts", null)
        if (json != null) {
            val type = object : TypeToken<List<Contact>>() {}.type
            _selectedContacts.value = gson.fromJson(json, type)
            filterContacts()
        }
    }

    private fun saveContacts() {
        val json = gson.toJson(_selectedContacts.value)
        sharedPreferences.edit().putString("selected_contacts", json).apply()
    }

    private fun loadActionPreferences() {
        val json = sharedPreferences.getString("action_preferences", null)
        if (json != null) {
            val type = object : TypeToken<Map<String, Boolean>>() {}.type
            _actionPreferences.value = gson.fromJson(json, type)
        }
    }

    private fun saveActionPreferences() {
        val json = gson.toJson(_actionPreferences.value)
        sharedPreferences.edit().putString("action_preferences", json).apply()
    }

    fun toggleActionPreference(contactId: String) {
        val currentPreferences = _actionPreferences.value.toMutableMap()
        currentPreferences[contactId] = !currentPreferences.getOrDefault(contactId, false)
        _actionPreferences.value = currentPreferences
        saveActionPreferences()
    }

    private fun loadSettings() {
        _isInternationalDetectionEnabled.value = sharedPreferences.getBoolean("international_detection_enabled", true)
        
        // Load messaging app preference with backward compatibility
        val messagingAppString = sharedPreferences.getString("default_messaging_app", null)
        if (messagingAppString != null) {
            try {
                _defaultMessagingApp.value = MessagingApp.valueOf(messagingAppString)
            } catch (e: IllegalArgumentException) {
                // If invalid enum value, fall back to WhatsApp
                _defaultMessagingApp.value = MessagingApp.WHATSAPP
            }
        } else {
            // Backward compatibility: check old boolean preference
            val useWhatsApp = sharedPreferences.getBoolean("use_whatsapp_as_default", true)
            _defaultMessagingApp.value = if (useWhatsApp) MessagingApp.WHATSAPP else MessagingApp.SMS
        }
    }

    private fun saveSettings() {
        sharedPreferences.edit()
            .putBoolean("international_detection_enabled", _isInternationalDetectionEnabled.value)
            .putString("default_messaging_app", _defaultMessagingApp.value.name)
            .apply()
    }

    fun toggleInternationalDetection() {
        _isInternationalDetectionEnabled.value = !_isInternationalDetectionEnabled.value
        saveSettings()
    }

    fun setMessagingApp(app: MessagingApp) {
        _defaultMessagingApp.value = app
        saveSettings()
    }
    
    // Keep the old toggle function for backward compatibility
    fun toggleMessagingApp() {
        _defaultMessagingApp.value = when (_defaultMessagingApp.value) {
            MessagingApp.WHATSAPP -> MessagingApp.SMS
            MessagingApp.SMS -> MessagingApp.WHATSAPP
            MessagingApp.TELEGRAM -> MessagingApp.WHATSAPP
        }
        saveSettings()
    }

    fun addContact(contact: Contact) {
        val currentList = _selectedContacts.value.toMutableList()
        if (!currentList.any { it.id == contact.id }) {
            currentList.add(contact)
            _selectedContacts.value = currentList
            saveContacts()
            filterContacts()
        }
    }

    fun removeContact(contact: Contact) {
        val currentList = _selectedContacts.value.toMutableList()
        currentList.removeAll { it.id == contact.id }
        _selectedContacts.value = currentList
        saveContacts()
        filterContacts()
    }

    fun moveContact(fromIndex: Int, toIndex: Int) {
        val currentList = _selectedContacts.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val contact = currentList.removeAt(fromIndex)
            currentList.add(toIndex, contact)
            _selectedContacts.value = currentList
            saveContacts()
            filterContacts()
        }
    }

    fun makePhoneCall(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    fun openDialer(context: Context) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
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

    fun openMessagingApp(context: Context, phoneNumber: String) {
        when (_defaultMessagingApp.value) {
            MessagingApp.WHATSAPP -> openWhatsAppChat(context, phoneNumber)
            MessagingApp.SMS -> openSmsApp(context, phoneNumber)
            MessagingApp.TELEGRAM -> openTelegramChat(context, phoneNumber)
        }
    }

    fun openContactInContactsApp(context: Context, contact: Contact) {
        try {
            // Extract actual contact ID for recent calls contacts
            val actualContactId = if (contact.id.startsWith("call_history_")) {
                val extractedId = contact.id.substringAfter("call_history_")
                // Check if it's a valid numeric contact ID (not a hashcode)
                if (extractedId.all { it.isDigit() } || extractedId.contains("-")) {
                    extractedId
                } else {
                    // It's likely a hashcode, so skip to phone number fallback
                    throw Exception("Invalid contact ID - likely a hashcode")
                }
            } else {
                contact.id
            }
            
            // Try to open the specific contact using contact ID
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, actualContactId)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback: search for contact by phone number
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contact.phoneNumber))
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e2: Exception) {
                // Final fallback: just open contacts app
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = ContactsContract.Contacts.CONTENT_URI
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }
    }

    fun loadRecentCalls(context: Context) {
        try {
            val recentCallsList = mutableListOf<Contact>()
            val seenNumbers = mutableSetOf<String>()
            
            // Get phone numbers from selected contacts to exclude them from recent calls
            val selectedContactNumbers = _selectedContacts.value.map { 
                normalizePhoneNumber(it.phoneNumber) 
            }.toSet()
            
            // Debug logging
            android.util.Log.d("QuickContacts", "Selected contact numbers (normalized): $selectedContactNumbers")
            
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE
            )
            
            val cursor: Cursor? = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )
            
            cursor?.use {
                val numberColumn = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameColumn = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                
                if (numberColumn >= 0 && nameColumn >= 0) {
                    while (it.moveToNext() && recentCallsList.size < 10) {
                        val number = it.getString(numberColumn)
                        val cachedName = it.getString(nameColumn)
                        
                        if (number != null && !seenNumbers.contains(number)) {
                            val normalizedNumber = normalizePhoneNumber(number)
                            
                            // Debug logging
                            android.util.Log.d("QuickContacts", "Call log number: $number -> normalized: $normalizedNumber")
                            android.util.Log.d("QuickContacts", "Is excluded: ${selectedContactNumbers.contains(normalizedNumber)}")
                            
                            // Skip if this number is already in selected contacts
                            if (!selectedContactNumbers.contains(normalizedNumber)) {
                                seenNumbers.add(number)
                                
                                // Try to get contact details from contacts provider
                                val contact = getContactByPhoneNumber(context, number, cachedName)
                                contact?.let { 
                                    // Double-check: also exclude by name if it matches a selected contact
                                    val isNameDuplicate = _selectedContacts.value.any { selectedContact ->
                                        selectedContact.name.equals(it.name, ignoreCase = true)
                                    }
                                    
                                    if (!isNameDuplicate) {
                                        android.util.Log.d("QuickContacts", "Adding to recent calls: ${it.name} - ${it.phoneNumber}")
                                        recentCallsList.add(it)
                                    } else {
                                        android.util.Log.d("QuickContacts", "Skipping duplicate by name: ${it.name}")
                                    }
                                }
                            } else {
                                android.util.Log.d("QuickContacts", "Skipping duplicate contact: $cachedName - $number")
                            }
                        }
                    }
                }
            }
            
            _recentCalls.value = recentCallsList
            filterContacts()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getContactByPhoneNumber(context: Context, phoneNumber: String, cachedName: String?): Contact? {
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            
            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.NUMBER
            )
            
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val idColumnIndex = it.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    val nameColumnIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val numberColumnIndex = it.getColumnIndex(ContactsContract.PhoneLookup.NUMBER)
                    
                    if (idColumnIndex >= 0 && nameColumnIndex >= 0 && numberColumnIndex >= 0) {
                        val contactId = it.getString(idColumnIndex)
                        val displayName = it.getString(nameColumnIndex)
                        val number = it.getString(numberColumnIndex)
                        
                        // Get contact photo URI
                        val photoUri = getContactPhotoUri(context, contactId)
                        
                        val finalName = displayName?.takeIf { it.isNotBlank() }
                            ?: cachedName?.takeIf { it.isNotBlank() }
                            ?: formatPhoneNumber(phoneNumber)
                        
                        return Contact(
                            id = "call_history_$contactId",
                            name = finalName,
                            phoneNumber = phoneNumber, // Use original call log number for consistency
                            phoneNumbers = listOf(phoneNumber),
                            photoUri = photoUri
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Fallback: create contact with cached name or formatted phone number
        val finalName = cachedName?.takeIf { it.isNotBlank() } ?: formatPhoneNumber(phoneNumber)
        
        return Contact(
            id = "call_history_${phoneNumber.hashCode()}",
            name = finalName,
            phoneNumber = phoneNumber,
            phoneNumbers = listOf(phoneNumber),
            photoUri = null
        )
    }
    
    private fun getContactPhotoUri(context: Context, contactId: String): String? {
        try {
            val photoUri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_URI,
                contactId
            ).let { contactUri ->
                Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
            }
            
            // Check if photo exists by trying to open an input stream
            context.contentResolver.openInputStream(photoUri)?.use {
                return photoUri.toString()
            }
        } catch (e: Exception) {
            // Photo doesn't exist or can't be accessed
        }
        
        return null
    }
    
    private fun normalizePhoneNumber(phoneNumber: String): String {
        // Remove all non-digit characters
        val digitsOnly = phoneNumber.replace(Regex("[^\\d]"), "")
        
        return when {
            // Empty or too short
            digitsOnly.length < 7 -> digitsOnly
            
            // US/Canada numbers: 11 digits starting with 1, or 10 digits
            digitsOnly.length == 11 && digitsOnly.startsWith("1") -> {
                // Remove leading 1 for US/Canada numbers to normalize to 10 digits
                digitsOnly.substring(1)
            }
            digitsOnly.length == 10 -> {
                // Already 10 digits, likely US/Canada without country code
                digitsOnly
            }
            
            // International numbers: keep as is but remove leading country codes for comparison
            digitsOnly.length > 11 -> {
                // Take last 10 digits for comparison (assumes international format)
                digitsOnly.takeLast(10)
            }
            
            // Other cases: return as is
            else -> digitsOnly
        }
    }
    
    private fun formatPhoneNumber(phoneNumber: String): String {
        val cleaned = phoneNumber.replace(Regex("[^+\\d]"), "")
        
        return when {
            // US/Canada number with +1 country code
            cleaned.startsWith("+1") && cleaned.length == 12 -> {
                val digits = cleaned.substring(2)
                "+1 (${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            }
            // US/Canada number without country code (10 digits)
            !cleaned.startsWith("+") && cleaned.length == 10 -> {
                "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
            }
            // US/Canada number with country code but no +
            cleaned.startsWith("1") && cleaned.length == 11 -> {
                val digits = cleaned.substring(1)
                "+1 (${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            }
            // International number with country code
            cleaned.startsWith("+") && cleaned.length > 7 -> {
                val countryCode = cleaned.substring(0, cleaned.length - 10)
                val localNumber = cleaned.substring(cleaned.length - 10)
                if (localNumber.length == 10) {
                    "$countryCode (${localNumber.substring(0, 3)}) ${localNumber.substring(3, 6)}-${localNumber.substring(6)}"
                } else {
                    cleaned // Fallback to cleaned number
                }
            }
            // Other formats - just return cleaned
            else -> cleaned
        }
    }
} 