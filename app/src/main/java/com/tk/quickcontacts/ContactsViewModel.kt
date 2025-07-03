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

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val _selectedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val selectedContacts: StateFlow<List<Contact>> = _selectedContacts

    private val _recentCalls = MutableStateFlow<List<Contact>>(emptyList())
    val recentCalls: StateFlow<List<Contact>> = _recentCalls

    private val sharedPreferences: SharedPreferences
    private val gson = Gson()

    init {
        sharedPreferences = application.getSharedPreferences("QuickContactsPrefs", Context.MODE_PRIVATE)
        loadContacts()
    }

    private fun loadContacts() {
        val json = sharedPreferences.getString("selected_contacts", null)
        if (json != null) {
            val type = object : TypeToken<List<Contact>>() {}.type
            _selectedContacts.value = gson.fromJson(json, type)
        }
    }

    private fun saveContacts() {
        val json = gson.toJson(_selectedContacts.value)
        sharedPreferences.edit().putString("selected_contacts", json).apply()
    }

    fun addContact(contact: Contact) {
        val currentList = _selectedContacts.value.toMutableList()
        if (!currentList.any { it.id == contact.id }) {
            currentList.add(contact)
            _selectedContacts.value = currentList
            saveContacts()
        }
    }

    fun removeContact(contact: Contact) {
        val currentList = _selectedContacts.value.toMutableList()
        currentList.removeAll { it.id == contact.id }
        _selectedContacts.value = currentList
        saveContacts()
    }

    fun moveContact(fromIndex: Int, toIndex: Int) {
        val currentList = _selectedContacts.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val contact = currentList.removeAt(fromIndex)
            currentList.add(toIndex, contact)
            _selectedContacts.value = currentList
            saveContacts()
        }
    }

    fun makePhoneCall(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun openWhatsAppChat(context: Context, phoneNumber: String) {
        try {
            // Format phone number for WhatsApp (remove all non-digits and ensure country code)
            val cleanNumber = phoneNumber.replace(Regex("[^\\d]"), "")
            val formattedNumber = if (cleanNumber.startsWith("1") && cleanNumber.length == 11) {
                cleanNumber // US/Canada number with country code
            } else if (cleanNumber.length == 10) {
                "1$cleanNumber" // Add US country code
            } else {
                cleanNumber // International number
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$formattedNumber")
                setPackage("com.whatsapp")
            }
            
            // Try to open WhatsApp, fall back to web if not installed
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // WhatsApp not installed, open in browser
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$formattedNumber")
                }
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                    while (it.moveToNext() && recentCallsList.size < 6) {
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