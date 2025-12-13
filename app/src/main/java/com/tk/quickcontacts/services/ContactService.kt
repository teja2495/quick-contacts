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

/**
 * Convert call log type integer to readable string
 */
private fun getCallTypeString(callType: Int): String {
    return when (callType) {
        CallLog.Calls.INCOMING_TYPE -> "incoming"
        CallLog.Calls.OUTGOING_TYPE -> "outgoing"
        CallLog.Calls.MISSED_TYPE -> "missed"
        CallLog.Calls.REJECTED_TYPE -> "rejected"
        CallLog.Calls.BLOCKED_TYPE -> "blocked"
        else -> "unknown"
    }
}

class ContactService {
    
    // Common constant for recent calls limit
    companion object {
        const val RECENT_CALLS_LIMIT = 50
    }
    
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
        CallLog.Calls.DATE,
        CallLog.Calls.TYPE
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
            val contactMatchTypeMap = mutableMapOf<String, Int>() // Track match type (priority) for each contact
            
            // Normalized query for case-insensitive comparison
            val normalizedQuery = query.lowercase().trim()
            
            // Define priorities based on requirements
            val EXACT_MATCH_PRIORITY = 1000
            val STARTS_WITH_SINGLE_WORD_PRIORITY = 900
            val STARTS_WITH_MULTI_WORD_PRIORITY = 800
            val SECOND_WORD_STARTS_WITH_PRIORITY = 700
            val THIRD_WORD_STARTS_WITH_PRIORITY = 600
            val FOURTH_WORD_STARTS_WITH_PRIORITY = 500
            val PHONE_NUMBER_MATCH_PRIORITY = 400
            val CONTAINS_PRIORITY = 100
            
            // Normalize query for phone number search (remove non-digit characters)
            val normalizedPhoneQuery = query.replace(Regex("[^\\d]"), "")
            // Check if query looks like a phone number (mostly digits with optional formatting)
            val hasDigits = normalizedPhoneQuery.isNotEmpty()
            val hasLetters = query.replace(Regex("[\\d\\s\\-\\(\\)\\+]"), "").isNotEmpty()
            val isPhoneNumberQuery = hasDigits && !hasLetters && normalizedPhoneQuery.length >= 3
            
            // Execute search query to get all potential matches
            // If it's a phone number query, get all contacts and filter in Kotlin for better matching
            // Otherwise, search by name (and optionally by formatted phone number)
            val cursor: Cursor? = if (isPhoneNumberQuery) {
                // Get all contacts for phone number matching (we'll filter in Kotlin)
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    phoneProjection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )
            } else {
                android.util.Log.d("ContactService", "Searching by name: $normalizedQuery")
                // Search by name
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    phoneProjection,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? COLLATE NOCASE",
                    arrayOf("%$normalizedQuery%"),
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )
            }
            
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
                            
                            // Determine match type and priority
                            val normalizedName = name.lowercase().trim()
                            val nameParts = normalizedName.split(Regex("\\s+"))
                            
                            // Normalize phone number for comparison (remove non-digit characters)
                            val normalizedPhoneNumber = number.replace(Regex("[^\\d]"), "")
                            val phoneNumberMatchesQuery = normalizedPhoneQuery.isNotEmpty() && normalizedPhoneNumber.contains(normalizedPhoneQuery)
                            // Only check name matching if we're not in phone number query mode
                            val nameMatchesQuery = !isPhoneNumberQuery && normalizedName.contains(normalizedQuery)
                            
                            // Skip if neither name nor phone number matches
                            if (!nameMatchesQuery && !phoneNumberMatchesQuery) {
                                continue
                            }
                            
                            var priority = CONTAINS_PRIORITY
                            var matchType = 8 // Default: contains match (lowest priority)
                            
                            // Check phone number match first if in phone query mode
                            if (phoneNumberMatchesQuery && isPhoneNumberQuery) {
                                priority = PHONE_NUMBER_MATCH_PRIORITY
                                matchType = 7
                            }
                            // Name-based matching
                            else if (normalizedName == normalizedQuery) {
                                priority = EXACT_MATCH_PRIORITY
                                matchType = 1
                            } 
                            else if (normalizedName.startsWith(normalizedQuery) && nameParts.size == 1) {
                                priority = STARTS_WITH_SINGLE_WORD_PRIORITY
                                matchType = 2
                            }
                            else if (normalizedName.startsWith(normalizedQuery) && nameParts.size > 1) {
                                priority = STARTS_WITH_MULTI_WORD_PRIORITY
                                matchType = 3
                            }
                            else if (nameParts.size >= 2 && nameParts[1].startsWith(normalizedQuery)) {
                                priority = SECOND_WORD_STARTS_WITH_PRIORITY
                                matchType = 4
                            }
                            else if (nameParts.size >= 3 && nameParts[2].startsWith(normalizedQuery)) {
                                priority = THIRD_WORD_STARTS_WITH_PRIORITY
                                matchType = 5
                            }
                            else if (nameParts.size >= 4 && nameParts[3].startsWith(normalizedQuery)) {
                                priority = FOURTH_WORD_STARTS_WITH_PRIORITY
                                matchType = 6
                            }
                            // Phone number match as fallback for non-phone queries
                            else if (phoneNumberMatchesQuery) {
                                priority = PHONE_NUMBER_MATCH_PRIORITY
                                matchType = 7
                            }
                            
                            // Update contact's priority if this is a higher score or new contact
                            val currentPriority = contactRelevanceMap[id] ?: 0
                            val currentMatchType = contactMatchTypeMap[id] ?: Int.MAX_VALUE
                            
                            if (matchType < currentMatchType || (matchType == currentMatchType && priority > currentPriority)) {
                                contactRelevanceMap[id] = priority
                                contactMatchTypeMap[id] = matchType
                                android.util.Log.d("ContactService", "Contact '$name' gets match type: $matchType, priority: $priority")
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
            
            android.util.Log.d("ContactService", "Found ${seenContactsMap.size} unique contacts")
            
            // Convert map to list, sanitize contacts
            val sanitizedContacts = seenContactsMap.values.mapNotNull { contact ->
                val sanitized = ContactUtils.sanitizeContact(contact)
                if (sanitized == null) {
                    android.util.Log.w("ContactService", "Contact sanitization failed: ${contact.name}")
                }
                sanitized
            }
            
            // Sort by match type (lowest first = highest priority), then by priority score (highest first), 
            // then alphabetically by name for contacts with the same priority
            val sortedContacts = sanitizedContacts.sortedWith(
                compareBy<Contact> { contactMatchTypeMap[it.id] ?: Int.MAX_VALUE }
                .thenByDescending { contactRelevanceMap[it.id] ?: 0 }
                .thenBy { it.name.lowercase() }
            )
            
            searchResultsList.addAll(sortedContacts)
            
            android.util.Log.d("ContactService", "Final sorted results:")
            searchResultsList.forEachIndexed { index, contact ->
                val matchType = contactMatchTypeMap[contact.id] ?: Int.MAX_VALUE
                val priority = contactRelevanceMap[contact.id] ?: 0
                android.util.Log.d("ContactService", "${index + 1}. ${contact.name} (match type: $matchType, priority: $priority)")
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
        return loadRecentCallsInternal(context, selectedContacts, filterQuickContacts = true)
    }
    
    fun loadAllRecentCalls(context: Context): List<Contact> {
        return loadRecentCallsInternal(context, emptyList(), filterQuickContacts = false)
    }
    
    private fun loadRecentCallsInternal(context: Context, selectedContacts: List<Contact>, filterQuickContacts: Boolean): List<Contact> {
        try {
            val recentCallsList = mutableListOf<Contact>()
            val seenContactIds = mutableSetOf<String>()
            val contactCache = mutableMapOf<String, Contact>() // Cache for contact lookups

            // Get contact IDs from selected contacts to exclude them from recent calls (only if filtering is enabled)
            val selectedContactIds = if (filterQuickContacts) selectedContacts.map { it.id }.toSet() else emptySet()

            android.util.Log.d("QuickContacts", "Selected contact IDs: $selectedContactIds, filterQuickContacts: $filterQuickContacts")

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
                val typeColumn = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateColumn = it.getColumnIndex(CallLog.Calls.DATE)

                if (numberColumn >= 0 && nameColumn >= 0 && typeColumn >= 0) {
                    while (it.moveToNext() && recentCallsList.size < RECENT_CALLS_LIMIT) {
                        val number = it.getString(numberColumn)
                        val cachedName = it.getString(nameColumn)
                        val callType = it.getInt(typeColumn)
                        val callTimestamp = if (dateColumn >= 0) it.getLong(dateColumn) else null

                        // Skip blocked calls
                        if (callType == CallLog.Calls.BLOCKED_TYPE) {
                            android.util.Log.d("QuickContacts", "Skipping blocked call: $number")
                            continue
                        }

                        // Skip voicemails (call type 4)
                        if (callType == 4) {
                            android.util.Log.d("QuickContacts", "Skipping voicemail: $number")
                            continue
                        }

                        if (number != null && number.isNotBlank()) {
                            val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(number)

                            android.util.Log.d("QuickContacts", "Call log number: $number -> normalized: $normalizedNumber")

                            // Check cache first
                            val contact = contactCache[normalizedNumber] ?: run {
                                // Try to get contact details from contacts provider using optimized method
                                val foundContact = ContactUtils.getContactByPhoneNumberForRecentCalls(context, number, cachedName)
                                if (foundContact != null) {
                                    contactCache[normalizedNumber] = foundContact
                                }
                                foundContact
                            }

                            if (contact != null) {
                                // Skip if this contact is already in selected contacts or already seen
                                if (!selectedContactIds.contains(contact.id) && !seenContactIds.contains(contact.id)) {
                                    seenContactIds.add(contact.id)
                                    // Add call type and timestamp to the contact
                                    val contactWithCallData = contact.copy(
                                        callType = getCallTypeString(callType),
                                        callTimestamp = callTimestamp
                                    )
                                    android.util.Log.d("QuickContacts", "Adding to recent calls: ${contactWithCallData.name} (ID: ${contactWithCallData.id}) - ${contactWithCallData.phoneNumber} - ${contactWithCallData.callType} - ${contactWithCallData.callTimestamp}")
                                    recentCallsList.add(contactWithCallData)
                                } else {
                                    android.util.Log.d("QuickContacts", "Skipping duplicate contact: ${contact.name} (ID: ${contact.id})")
                                }
                            } else {
                                // If contact is null, create a fallback contact for this number
                                val formattedNumber = com.tk.quickcontacts.utils.PhoneNumberUtils.formatPhoneNumber(number)
                                // Allow short service numbers (3-6 digits) as recent calls
                                val digitsOnly = number.replace(Regex("[^\\d]"), "")
                                val isShortServiceNumber = digitsOnly.length in 3..6
                                if (isShortServiceNumber || com.tk.quickcontacts.utils.PhoneNumberUtils.isValidPhoneNumber(number)) {
                                    val fallbackContactId = "call_history_${number.hashCode().toString()}"
                                    // Skip if this fallback contact is already seen
                                    if (!seenContactIds.contains(fallbackContactId)) {
                                        seenContactIds.add(fallbackContactId)
                                        val fallbackContact = com.tk.quickcontacts.Contact(
                                            id = fallbackContactId,
                                            name = if (!cachedName.isNullOrBlank()) cachedName.trim() else formattedNumber,
                                            phoneNumber = number,
                                            phoneNumbers = listOf(number),
                                            photoUri = null,
                                            callType = getCallTypeString(callType),
                                            callTimestamp = callTimestamp
                                        )
                                        android.util.Log.d("QuickContacts", "Adding fallback to recent calls: ${fallbackContact.name} - ${fallbackContact.phoneNumber} - ${fallbackContact.callType}")
                                        recentCallsList.add(fallbackContact)
                                    } else {
                                        android.util.Log.d("QuickContacts", "Skipping duplicate fallback contact: $formattedNumber")
                                    }
                                } else {
                                    android.util.Log.d("QuickContacts", "Skipping invalid number: $number")
                                }
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
     * Get the latest call activity for a specific contact by phone number
     * Only checks the latest 25 items in the call log for better performance
     * Returns a Contact with callType and callTimestamp if found, null otherwise
     */
    fun getLatestCallActivityForContact(context: Context, phoneNumbers: List<String>): Contact? {
        try {
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
                val typeColumn = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateColumn = it.getColumnIndex(CallLog.Calls.DATE)

                if (numberColumn >= 0 && nameColumn >= 0 && typeColumn >= 0) {
                    var checkedItems = 0
                    while (it.moveToNext() && checkedItems < RECENT_CALLS_LIMIT) {
                        checkedItems++
                        val number = it.getString(numberColumn)
                        val cachedName = it.getString(nameColumn)
                        val callType = it.getInt(typeColumn)
                        val callTimestamp = if (dateColumn >= 0) it.getLong(dateColumn) else null

                        if (number != null && number.isNotBlank()) {
                            // Check if this number matches any of the contact's phone numbers
                            val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(number)
                            val matchesContact = phoneNumbers.any { contactNumber ->
                                PhoneNumberUtils.normalizePhoneNumber(contactNumber) == normalizedNumber
                            }

                            if (matchesContact) {
                                // Found a match, return the contact with call data
                                val contact = ContactUtils.getContactByPhoneNumberForRecentCalls(context, number, cachedName)
                                return contact?.copy(
                                    callType = getCallTypeString(callType),
                                    callTimestamp = callTimestamp
                                )
                            }
                        }
                    }
                }
            }

            return null
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("QuickContacts", "Error getting latest call activity: ${e.message}")
            return null
        }
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