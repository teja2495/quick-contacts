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

    fun loadRecentCalls(context: Context) {
        try {
            val recentCallsList = mutableListOf<Contact>()
            val seenNumbers = mutableSetOf<String>()
            
            // Get phone numbers from selected contacts to exclude them from recent calls
            val selectedContactNumbers = _selectedContacts.value.map { 
                normalizePhoneNumber(it.phoneNumber) 
            }.toSet()
            
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
                    while (it.moveToNext() && recentCallsList.size < 3) {
                        val number = it.getString(numberColumn)
                        val cachedName = it.getString(nameColumn)
                        
                        if (number != null && !seenNumbers.contains(number)) {
                            val normalizedNumber = normalizePhoneNumber(number)
                            
                            // Skip if this number is already in selected contacts
                            if (!selectedContactNumbers.contains(normalizedNumber)) {
                                seenNumbers.add(number)
                                
                                // Try to get contact details from contacts provider
                                val contact = getContactByPhoneNumber(context, number, cachedName)
                                contact?.let { recentCallsList.add(it) }
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
                        
                        return Contact(
                            id = "call_history_$contactId",
                            name = displayName ?: cachedName ?: "Unknown",
                            phoneNumber = number ?: phoneNumber,
                            photoUri = photoUri
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Fallback: create contact with cached name or unknown
        return Contact(
            id = "call_history_${phoneNumber.hashCode()}",
            name = cachedName ?: "Unknown",
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
        // Remove all non-digit characters except '+'
        return phoneNumber.replace(Regex("[^+\\d]"), "")
            .let { cleaned ->
                // If number starts with country code, keep it; otherwise just return the digits
                when {
                    cleaned.startsWith("+") -> cleaned
                    cleaned.length > 10 -> cleaned // Assume it includes country code
                    else -> cleaned // Local number
                }
            }
    }
} 