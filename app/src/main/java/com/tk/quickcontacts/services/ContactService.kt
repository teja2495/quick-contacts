package com.tk.quickcontacts.services

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.utils.ContactUtils
import com.tk.quickcontacts.utils.PhoneNumberUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ContactService {
    
    // Cache for search results to avoid repeated queries
    private val searchCache = ConcurrentHashMap<String, List<Contact>>()
    private val cacheMaxSize = 50
    private val cacheExpiryTime = 5 * 60 * 1000L // 5 minutes
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    
    // Scheduled executor for cache cleanup
    private val cacheCleanupExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    
    init {
        // Schedule cache cleanup every 10 minutes
        cacheCleanupExecutor.scheduleAtFixedRate({
            cleanupExpiredCache()
        }, 10, 10, TimeUnit.MINUTES)
    }
    
    /**
     * Clean up expired cache entries
     */
    private fun cleanupExpiredCache() {
        try {
            val currentTime = System.currentTimeMillis()
            val expiredKeys = mutableListOf<String>()
            
            // Find expired entries
            cacheTimestamps.forEach { (key, timestamp) ->
                if (currentTime - timestamp > cacheExpiryTime) {
                    expiredKeys.add(key)
                }
            }
            
            // Remove expired entries
            expiredKeys.forEach { key ->
                searchCache.remove(key)
                cacheTimestamps.remove(key)
            }
            
            // If cache is still too large, remove oldest entries
            if (searchCache.size > cacheMaxSize) {
                val sortedEntries = cacheTimestamps.entries.sortedBy { it.value }
                val entriesToRemove = searchCache.size - cacheMaxSize
                
                sortedEntries.take(entriesToRemove).forEach { entry ->
                    searchCache.remove(entry.key)
                    cacheTimestamps.remove(entry.key)
                }
            }
            
            android.util.Log.d("ContactService", "Cache cleanup completed. Size: ${searchCache.size}")
        } catch (e: Exception) {
            android.util.Log.e("ContactService", "Error during cache cleanup", e)
        }
    }
    
    /**
     * Clear all cache data
     */
    fun clearCache() {
        try {
            searchCache.clear()
            cacheTimestamps.clear()
            android.util.Log.d("ContactService", "Cache cleared")
        } catch (e: Exception) {
            android.util.Log.e("ContactService", "Error clearing cache", e)
        }
    }
    
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
                        if (contactId != null && contactId.isNotBlank()) {
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
                        
                        if (id != null && name != null && number != null && 
                            id.isNotBlank() && name.isNotBlank() && number.isNotBlank() &&
                            PhoneNumberUtils.isValidPhoneNumber(number)) {
                            
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
                                    val updatedContact = existingContact.copy(phoneNumbers = updatedPhoneNumbers)
                                    // Validate the updated contact
                                    if (ContactUtils.isValidContact(updatedContact)) {
                                        seenContactsMap[id] = updatedContact
                                    }
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
                                
                                // Validate the contact before adding
                                if (ContactUtils.isValidContact(contact)) {
                                    seenContactsMap[id] = contact
                                }
                            }
                        }
                    }
                }
            }
            
            // Convert map to list and sanitize contacts
            favoriteContacts.addAll(seenContactsMap.values.mapNotNull { contact ->
                ContactUtils.sanitizeContact(contact)
            })
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
        
        android.util.Log.d("ContactService", "Searching for query: '$query'")
        
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
            val contactRelevanceMap = mutableMapOf<String, Int>() // Track relevance score for each contact
            
            // Define search patterns with different relevance levels
            val searchPatterns = listOf(
                // Level 1: Exact match (highest priority)
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ? COLLATE NOCASE" to 100,
                // Level 2: Starts with query (high priority)
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? COLLATE NOCASE" to 80,
                // Level 3: Contains query (medium priority)
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? COLLATE NOCASE" to 60,
                // Level 4: Contains query with word boundaries (lower priority)
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? COLLATE NOCASE" to 40
            )
            
            val searchArgs = listOf(
                query, // Exact match
                "$query%", // Starts with
                "%$query%", // Contains
                "% $query %" // Word boundary
            )
            
            android.util.Log.d("ContactService", "Using multiple search patterns for better relevance")
            
            // Execute each search pattern
            searchPatterns.forEachIndexed { index, (pattern, relevance) ->
                val selectionArgs = arrayOf(searchArgs[index])
                
                android.util.Log.d("ContactService", "Pattern $index: $pattern (relevance: $relevance)")
                android.util.Log.d("ContactService", "Args: ${selectionArgs.joinToString()}")
                
                val cursor: Cursor? = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    phoneProjection,
                    pattern,
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
                            
                            if (id != null && name != null && number != null && 
                                id.isNotBlank() && name.isNotBlank() && number.isNotBlank() &&
                                PhoneNumberUtils.isValidPhoneNumber(number)) {
                                
                                // Only update relevance if this is a higher score or new contact
                                val currentRelevance = contactRelevanceMap[id] ?: 0
                                if (relevance > currentRelevance) {
                                    contactRelevanceMap[id] = relevance
                                    android.util.Log.d("ContactService", "Contact '$name' gets relevance score: $relevance")
                                }
                                
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
                                        val updatedContact = existingContact.copy(phoneNumbers = updatedPhoneNumbers)
                                        // Validate the updated contact
                                        if (ContactUtils.isValidContact(updatedContact)) {
                                            seenContactsMap[id] = updatedContact
                                        }
                                    }
                                } else {
                                    // Create new contact
                                    val photoUri = ContactUtils.getContactPhotoUri(context, id)
                                    val contact = Contact(
                                        id = id,
                                        name = name,
                                        phoneNumber = number,
                                        phoneNumbers = listOf(number),
                                        photoUri = photoUri
                                    )
                                    
                                    // Validate the contact before adding
                                    if (ContactUtils.isValidContact(contact)) {
                                        seenContactsMap[id] = contact
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            android.util.Log.d("ContactService", "Found ${seenContactsMap.size} unique contacts")
            
            // Convert map to list, sanitize contacts, and sort by relevance
            val sanitizedContacts = seenContactsMap.values.mapNotNull { contact ->
                val sanitized = ContactUtils.sanitizeContact(contact)
                if (sanitized == null) {
                    android.util.Log.w("ContactService", "Contact sanitization failed: ${contact.name}")
                }
                sanitized
            }
            
            // Sort by relevance score (highest first), then by name
            val sortedContacts = sanitizedContacts.sortedWith(
                compareByDescending<Contact> { contactRelevanceMap[it.id] ?: 0 }
                .thenBy { it.name.lowercase() }
            )
            
            searchResultsList.addAll(sortedContacts)
            
            android.util.Log.d("ContactService", "Final sorted results:")
            searchResultsList.forEachIndexed { index, contact ->
                val relevance = contactRelevanceMap[contact.id] ?: 0
                android.util.Log.d("ContactService", "${index + 1}. ${contact.name} (relevance: $relevance)")
            }
            
            // Cache the results
            searchCache[cacheKey] = searchResultsList
            cacheTimestamps[cacheKey] = currentTime
            
            android.util.Log.d("ContactService", "Search completed for '$query': ${searchResultsList.size} results")
            return searchResultsList
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("QuickContacts", "Error searching contacts: ${e.message}")
            return emptyList()
        }
    }
    
    fun loadRecentCalls(context: Context, selectedContacts: List<Contact>): List<Contact> {
        try {
            val recentCallsList = mutableListOf<Contact>()
            val seenNumbers = mutableSetOf<String>()

            // Get phone numbers from selected contacts to exclude them from recent calls
            val selectedContactNumbers = selectedContacts.mapNotNull { contact ->
                val primaryNumber = ContactUtils.getPrimaryPhoneNumber(contact)
                if (primaryNumber.isNotBlank()) {
                    PhoneNumberUtils.normalizePhoneNumber(primaryNumber)
                } else null
            }.toSet()

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

                        if (number != null && number.isNotBlank() && !seenNumbers.contains(number)) {
                            val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(number)

                            android.util.Log.d("QuickContacts", "Call log number: $number -> normalized: $normalizedNumber")
                            android.util.Log.d("QuickContacts", "Is excluded: ${selectedContactNumbers.contains(normalizedNumber)}")

                            // Skip if this number is already in selected contacts
                            if (!selectedContactNumbers.contains(normalizedNumber)) {
                                seenNumbers.add(number)

                                // Try to get contact details from contacts provider
                                val contact = ContactUtils.getContactByPhoneNumber(context, number, cachedName)
                                if (contact != null) {
                                    // Double-check: also exclude by name if it matches a selected contact
                                    val isNameDuplicate = selectedContacts.any { selectedContact ->
                                        selectedContact.name.equals(contact.name, ignoreCase = true)
                                    }
                                    if (!isNameDuplicate) {
                                        android.util.Log.d("QuickContacts", "Adding to recent calls: ${contact.name} - ${contact.phoneNumber}")
                                        recentCallsList.add(contact)
                                    } else {
                                        android.util.Log.d("QuickContacts", "Skipping duplicate by name: ${contact.name}")
                                    }
                                } else {
                                    // If contact is null, create a fallback contact for this number
                                    val formattedNumber = com.tk.quickcontacts.utils.PhoneNumberUtils.formatPhoneNumber(number)
                                    // Allow short service numbers (3-6 digits) as recent calls
                                    val digitsOnly = number.replace(Regex("[^\\d]"), "")
                                    val isShortServiceNumber = digitsOnly.length in 3..6
                                    if (isShortServiceNumber || com.tk.quickcontacts.utils.PhoneNumberUtils.isValidPhoneNumber(number)) {
                                        val fallbackContact = com.tk.quickcontacts.Contact(
                                            id = "call_history_${number.hashCode().toString()}",
                                            name = if (!cachedName.isNullOrBlank()) cachedName.trim() else formattedNumber,
                                            phoneNumber = number,
                                            phoneNumbers = listOf(number),
                                            photoUri = null
                                        )
                                        android.util.Log.d("QuickContacts", "Adding fallback to recent calls: ${fallbackContact.name} - ${fallbackContact.phoneNumber}")
                                        recentCallsList.add(fallbackContact)
                                    } else {
                                        android.util.Log.d("QuickContacts", "Skipping invalid number: $number")
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
            android.util.Log.e("QuickContacts", "Error loading recent calls: ${e.message}")
            return emptyList()
        }
    }
    
    fun getAllContacts(context: Context): List<Contact> {
        val allContacts = mutableListOf<Contact>()
        val seenContactsMap = mutableMapOf<String, Contact>()
        try {
            val phoneCursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                phoneProjection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            phoneCursor?.use {
                val idColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (idColumn >= 0 && nameColumn >= 0 && numberColumn >= 0) {
                    while (it.moveToNext()) {
                        val id = it.getString(idColumn)
                        val name = it.getString(nameColumn)
                        val number = it.getString(numberColumn)
                        if (id != null && name != null && number != null &&
                            id.isNotBlank() && name.isNotBlank() && number.isNotBlank() &&
                            PhoneNumberUtils.isValidPhoneNumber(number)) {
                            if (seenContactsMap.containsKey(id)) {
                                val existingContact = seenContactsMap[id]!!
                                val updatedPhoneNumbers = existingContact.phoneNumbers.toMutableList()
                                val normalizedNewNumber = PhoneNumberUtils.normalizePhoneNumber(number)
                                val alreadyExists = updatedPhoneNumbers.any {
                                    PhoneNumberUtils.normalizePhoneNumber(it) == normalizedNewNumber
                                }
                                if (!alreadyExists) {
                                    updatedPhoneNumbers.add(number)
                                    val updatedContact = existingContact.copy(phoneNumbers = updatedPhoneNumbers)
                                    if (ContactUtils.isValidContact(updatedContact)) {
                                        seenContactsMap[id] = updatedContact
                                    }
                                }
                            } else {
                                val photoUri = ContactUtils.getContactPhotoUri(context, id)
                                val contact = Contact(
                                    id = id,
                                    name = name,
                                    phoneNumber = number,
                                    phoneNumbers = listOf(number),
                                    photoUri = photoUri
                                )
                                if (ContactUtils.isValidContact(contact)) {
                                    seenContactsMap[id] = contact
                                }
                            }
                        }
                    }
                }
            }
            allContacts.addAll(seenContactsMap.values.mapNotNull { ContactUtils.sanitizeContact(it) })
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("QuickContacts", "Error getting all contacts: ${e.message}")
        }
        return allContacts
    }
    
    /**
     * Cleanup resources when service is no longer needed
     */
    fun cleanup() {
        try {
            cacheCleanupExecutor.shutdown()
            if (!cacheCleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cacheCleanupExecutor.shutdownNow()
            }
            clearCache()
        } catch (e: Exception) {
            android.util.Log.e("ContactService", "Error during cleanup", e)
        }
    }
} 