package com.tk.quickcontacts.services

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.utils.ContactUtils
import com.tk.quickcontacts.utils.PhoneNumberUtils
import java.util.concurrent.ConcurrentHashMap

class ContactService {
    
    // Cache for search results to avoid repeated queries
    private val searchCache = ConcurrentHashMap<String, List<Contact>>()
    private val cacheMaxSize = 50
    private val cacheExpiryTime = 5 * 60 * 1000L // 5 minutes
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    
    // Optimized projections for better performance
    private val contactProjection = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME,
        ContactsContract.Contacts.STARRED
    )
    
    private val phoneProjection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    
    private val callLogProjection = arrayOf(
        CallLog.Calls.NUMBER,
        CallLog.Calls.CACHED_NAME,
        CallLog.Calls.DATE
    )
    
    fun getFavoriteContacts(context: Context): List<Contact> {
        val favoriteContacts = mutableListOf<Contact>()
        val seenContactsMap = mutableMapOf<String, Contact>()
        
        try {
            android.util.Log.d("QuickContacts", "Querying for starred contacts...")
            // First, get all starred contact IDs
            val starredContactIds = mutableSetOf<String>()
            val contactsSelection = "${ContactsContract.Contacts.STARRED} = 1"
            
            val contactsCursor: Cursor? = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                contactProjection,
                contactsSelection,
                null,
                null
            )
            
            contactsCursor?.use {
                val idColumn = it.getColumnIndex(ContactsContract.Contacts._ID)
                android.util.Log.d("QuickContacts", "Contacts cursor count: ${it.count}")
                if (idColumn >= 0) {
                    while (it.moveToNext()) {
                        val contactId = it.getString(idColumn)
                        if (contactId != null) {
                            starredContactIds.add(contactId)
                            android.util.Log.d("QuickContacts", "Found starred contact ID: $contactId")
                        }
                    }
                } else {
                    android.util.Log.e("QuickContacts", "ID column not found in contacts cursor")
                }
            }
            
            // If no starred contacts found, return empty list
            if (starredContactIds.isEmpty()) {
                android.util.Log.d("QuickContacts", "No starred contacts found")
                return favoriteContacts
            }
            
            android.util.Log.d("QuickContacts", "Found ${starredContactIds.size} starred contact IDs")
            
            // Now get phone numbers for starred contacts
            // Create selection for starred contact IDs
            val placeholders = starredContactIds.joinToString(",") { "?" }
            val phoneSelection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN ($placeholders)"
            val phoneSelectionArgs = starredContactIds.toTypedArray()
            
            val phoneCursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                phoneProjection,
                phoneSelection,
                phoneSelectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            
            phoneCursor?.use {
                val idColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                android.util.Log.d("QuickContacts", "Phone cursor count: ${it.count}")
                android.util.Log.d("QuickContacts", "Phone selection: $phoneSelection")
                android.util.Log.d("QuickContacts", "Phone selection args: ${phoneSelectionArgs.joinToString()}")
                
                if (idColumn >= 0 && nameColumn >= 0 && numberColumn >= 0) {
                    while (it.moveToNext()) {
                        val id = it.getString(idColumn)
                        val name = it.getString(nameColumn)
                        val number = it.getString(numberColumn)
                        
                        if (id != null && name != null && number != null) {
                            android.util.Log.d("QuickContacts", "Processing contact: $name ($id) - $number")
                            if (seenContactsMap.containsKey(id)) {
                                // Add phone number to existing contact
                                val existingContact = seenContactsMap[id]!!
                                val updatedPhoneNumbers = existingContact.phoneNumbers.toMutableList()
                                val normalizedNewNumber = PhoneNumberUtils.normalizePhoneNumber(number)
                                
                                // Check if this normalized number already exists
                                val alreadyExists = updatedPhoneNumbers.any { 
                                    PhoneNumberUtils.normalizePhoneNumber(it) == normalizedNewNumber 
                                }
                                
                                if (!alreadyExists) {
                                    updatedPhoneNumbers.add(number)
                                    seenContactsMap[id] = existingContact.copy(phoneNumbers = updatedPhoneNumbers)
                                }
                            } else {
                                // Create new contact
                                val photoUri = ContactUtils.getContactPhotoUri(context, id)
                                val contact = Contact(
                                    id = id,
                                    name = name,
                                    phoneNumber = number, // Primary phone number
                                    phoneNumbers = listOf(number),
                                    photoUri = photoUri
                                )
                                seenContactsMap[id] = contact
                            }
                        }
                    }
                }
            }
            
            // Convert map to list
            favoriteContacts.addAll(seenContactsMap.values)
            android.util.Log.d("QuickContacts", "Processed ${favoriteContacts.size} favorite contacts with phone numbers")
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("QuickContacts", "Error getting favorite contacts: ${e.message}")
        }
        
        return favoriteContacts
    }
    
    fun searchContacts(context: Context, query: String): List<Contact> {
        if (query.trim().isEmpty()) {
            return emptyList()
        }
        
        // Check cache first
        val cacheKey = query.lowercase().trim()
        val currentTime = System.currentTimeMillis()
        val cachedTimestamp = cacheTimestamps[cacheKey]
        
        if (cachedTimestamp != null && (currentTime - cachedTimestamp) < cacheExpiryTime) {
            val cachedResults = searchCache[cacheKey]
            if (cachedResults != null) {
                android.util.Log.d("QuickContacts", "Returning cached search results for: $query")
                return cachedResults
            }
        }
        
        try {
            val searchResultsList = mutableListOf<Contact>()
            val seenContactsMap = mutableMapOf<String, Contact>()
            
            // Search only by name, not phone number
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")
            
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                phoneProjection,
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
                                val normalizedNewNumber = PhoneNumberUtils.normalizePhoneNumber(number)
                                
                                // Check if this normalized number already exists
                                val alreadyExists = updatedPhoneNumbers.any { 
                                    PhoneNumberUtils.normalizePhoneNumber(it) == normalizedNewNumber 
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
            val results = seenContactsMap.values.take(20)
            
            // Cache the results
            cacheResults(cacheKey, results)
            
            return results
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
    
    private fun cacheResults(query: String, results: List<Contact>) {
        // Clean up old cache entries if cache is too large
        if (searchCache.size >= cacheMaxSize) {
            val oldestEntry = cacheTimestamps.minByOrNull { it.value }
            oldestEntry?.let {
                searchCache.remove(it.key)
                cacheTimestamps.remove(it.key)
            }
        }
        
        searchCache[query] = results
        cacheTimestamps[query] = System.currentTimeMillis()
    }
    
    fun loadRecentCalls(context: Context, selectedContacts: List<Contact>): List<Contact> {
        try {
            val recentCallsList = mutableListOf<Contact>()
            val seenNumbers = mutableSetOf<String>()
            
            // Get phone numbers from selected contacts to exclude them from recent calls
            val selectedContactNumbers = selectedContacts.map { 
                PhoneNumberUtils.normalizePhoneNumber(it.phoneNumber) 
            }.toSet()
            
            // Debug logging
            android.util.Log.d("QuickContacts", "Selected contact numbers (normalized): $selectedContactNumbers")
            
            val cursor: Cursor? = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                callLogProjection,
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
                            val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(number)
                            
                            // Debug logging
                            android.util.Log.d("QuickContacts", "Call log number: $number -> normalized: $normalizedNumber")
                            android.util.Log.d("QuickContacts", "Is excluded: ${selectedContactNumbers.contains(normalizedNumber)}")
                            
                            // Skip if this number is already in selected contacts
                            if (!selectedContactNumbers.contains(normalizedNumber)) {
                                seenNumbers.add(number)
                                
                                // Try to get contact details from contacts provider
                                val contact = ContactUtils.getContactByPhoneNumber(context, number, cachedName)
                                contact?.let { 
                                    // Double-check: also exclude by name if it matches a selected contact
                                    val isNameDuplicate = selectedContacts.any { selectedContact ->
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
            
            return recentCallsList
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
} 