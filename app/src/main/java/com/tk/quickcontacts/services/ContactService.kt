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

    companion object {
        const val RECENT_CALLS_LIMIT = 50

        private const val ENABLE_SEARCH_INDEX = true

        private const val EXACT_MATCH_PRIORITY = 1000
        private const val STARTS_WITH_SINGLE_WORD_PRIORITY = 900
        private const val STARTS_WITH_MULTI_WORD_PRIORITY = 800
        private const val SECOND_WORD_STARTS_WITH_PRIORITY = 700
        private const val THIRD_WORD_STARTS_WITH_PRIORITY = 600
        private const val FOURTH_WORD_STARTS_WITH_PRIORITY = 500
        private const val PHONE_NUMBER_MATCH_PRIORITY = 400
        private const val CONTAINS_PRIORITY = 100

        private const val DEFAULT_MATCH_TYPE = 8

        private val NON_DIGIT_REGEX = Regex("[^\\d]")
        private val NON_PHONE_QUERY_CHARS_REGEX = Regex("[\\d\\s\\-\\(\\)\\+]")
        private val WHITESPACE_REGEX = Regex("\\s+")
    }

    private data class SearchRow(
        val id: String,
        val name: String,
        val number: String,
        val normalizedName: String,
        val nameParts: List<String>,
        val normalizedPhoneNumber: String,
        val comparablePhoneNumber: String,
        val photoUri: String?
    )

    // Cache for search results to avoid repeated queries
    private val searchCache = ConcurrentHashMap<String, List<Contact>>()
    private val cacheMaxSize = 50
    private val cacheExpiryTime = 5 * 60 * 1000L // 5 minutes
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()

    // Lazy hybrid search index
    @Volatile
    private var searchIndex: List<SearchRow>? = null
    private val searchIndexLock = Any()

    // Scheduled executor for cache cleanup
    private val cacheCleanupExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

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

            cacheTimestamps.forEach { (key, timestamp) ->
                if (currentTime - timestamp > cacheExpiryTime) {
                    expiredKeys.add(key)
                }
            }

            expiredKeys.forEach { key ->
                searchCache.remove(key)
                cacheTimestamps.remove(key)
            }

            if (searchCache.size > cacheMaxSize) {
                val sortedEntries = cacheTimestamps.entries.sortedBy { it.value }
                val entriesToRemove = searchCache.size - cacheMaxSize

                sortedEntries.take(entriesToRemove).forEach { entry ->
                    searchCache.remove(entry.key)
                    cacheTimestamps.remove(entry.key)
                }
            }
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
            synchronized(searchIndexLock) {
                searchIndex = null
            }
            android.util.Log.d("ContactService", "Cache cleared")
        } catch (e: Exception) {
            android.util.Log.e("ContactService", "Error clearing cache", e)
        }
    }

    private fun toDigits(value: String): String = value.replace(NON_DIGIT_REGEX, "")

    private fun createSearchRow(
        id: String?,
        name: String?,
        number: String?,
        photoUri: String?
    ): SearchRow? {
        if (id.isNullOrBlank() || name.isNullOrBlank() || number.isNullOrBlank()) {
            return null
        }
        if (!PhoneNumberUtils.isValidPhoneNumber(number)) {
            return null
        }

        val normalizedName = name.lowercase().trim()
        if (normalizedName.isBlank()) {
            return null
        }

        return SearchRow(
            id = id,
            name = name,
            number = number,
            normalizedName = normalizedName,
            nameParts = normalizedName.split(WHITESPACE_REGEX),
            normalizedPhoneNumber = toDigits(number),
            comparablePhoneNumber = PhoneNumberUtils.normalizePhoneNumber(number),
            photoUri = photoUri
        )
    }

    private fun queryPhoneRows(
        context: Context,
        selection: String?,
        selectionArgs: Array<String>?
    ): List<SearchRow> {
        val rows = mutableListOf<SearchRow>()
        val photoUriByContactId = mutableMapOf<String, String?>()

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

                    val row = createSearchRow(
                        id = id,
                        name = name,
                        number = number,
                        photoUri = if (id.isNullOrBlank()) null else photoUriByContactId.getOrPut(id) {
                            ContactUtils.getContactPhotoUri(context, id)
                        }
                    )
                    if (row != null) {
                        rows.add(row)
                    }
                }
            }
        }

        return rows
    }

    private fun getOrBuildSearchIndex(context: Context): List<SearchRow>? {
        if (!ENABLE_SEARCH_INDEX) {
            return null
        }

        searchIndex?.let { return it }

        synchronized(searchIndexLock) {
            searchIndex?.let { return it }

            return try {
                val rows = queryPhoneRows(
                    context = context,
                    selection = null,
                    selectionArgs = null
                )
                searchIndex = rows
                rows
            } catch (e: Exception) {
                android.util.Log.e("ContactService", "Failed to build search index", e)
                null
            }
        }
    }

    fun prewarmSearchIndex(context: Context) {
        try {
            getOrBuildSearchIndex(context)
        } catch (e: Exception) {
            android.util.Log.e("ContactService", "Failed to prewarm search index", e)
        }
    }

    private fun searchRowsFromProvider(context: Context, normalizedQuery: String, isPhoneNumberQuery: Boolean): List<SearchRow> {
        return if (isPhoneNumberQuery) {
            queryPhoneRows(context, selection = null, selectionArgs = null)
        } else {
            queryPhoneRows(
                context = context,
                selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? COLLATE NOCASE",
                selectionArgs = arrayOf("%$normalizedQuery%")
            )
        }
    }

    fun getFavoriteContacts(context: Context): List<Contact> {
        val starredContactIds = mutableSetOf<String>()

        try {
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
                if (idColumn >= 0) {
                    while (it.moveToNext()) {
                        val contactId = it.getString(idColumn)
                        if (!contactId.isNullOrBlank()) {
                            starredContactIds.add(contactId)
                        }
                    }
                }
            }

            if (starredContactIds.isEmpty()) {
                return emptyList()
            }

            val placeholders = starredContactIds.joinToString(",") { "?" }
            val phoneSelection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN ($placeholders)"
            val phoneSelectionArgs = starredContactIds.toTypedArray()

            val rows = queryPhoneRows(context, phoneSelection, phoneSelectionArgs)
            return aggregateRowsToContacts(rows)
        } catch (e: Exception) {
            android.util.Log.e("QuickContacts", "Error getting favorite contacts: ${e.message}")
            return emptyList()
        }
    }

    fun searchContacts(context: Context, query: String): List<Contact> {
        if (query.trim().isEmpty()) {
            return emptyList()
        }

        val cacheKey = query.lowercase().trim()
        val currentTime = System.currentTimeMillis()
        val cachedTimestamp = cacheTimestamps[cacheKey]

        if (cachedTimestamp != null && (currentTime - cachedTimestamp) < cacheExpiryTime) {
            searchCache[cacheKey]?.let { return it }
        }

        try {
            val normalizedQuery = query.lowercase().trim()
            val normalizedPhoneQuery = toDigits(query)
            val hasDigits = normalizedPhoneQuery.isNotEmpty()
            val hasLetters = query.replace(NON_PHONE_QUERY_CHARS_REGEX, "").isNotEmpty()
            val isPhoneNumberQuery = hasDigits && !hasLetters && normalizedPhoneQuery.length >= 3

            val rows = if (ENABLE_SEARCH_INDEX) {
                val indexedRows = getOrBuildSearchIndex(context)
                if (indexedRows != null) {
                    if (isPhoneNumberQuery) indexedRows else indexedRows.filter { it.normalizedName.contains(normalizedQuery) }
                } else {
                    searchRowsFromProvider(context, normalizedQuery, isPhoneNumberQuery)
                }
            } else {
                searchRowsFromProvider(context, normalizedQuery, isPhoneNumberQuery)
            }

            val seenContactsMap = mutableMapOf<String, Contact>()
            val contactRelevanceMap = mutableMapOf<String, Int>()
            val contactMatchTypeMap = mutableMapOf<String, Int>()
            val normalizedNumbersByContact = mutableMapOf<String, MutableSet<String>>()

            for (row in rows) {
                val phoneNumberMatchesQuery =
                    normalizedPhoneQuery.isNotEmpty() && row.normalizedPhoneNumber.contains(normalizedPhoneQuery)
                val nameMatchesQuery = !isPhoneNumberQuery && row.normalizedName.contains(normalizedQuery)

                if (!nameMatchesQuery && !phoneNumberMatchesQuery) {
                    continue
                }

                var priority = CONTAINS_PRIORITY
                var matchType = DEFAULT_MATCH_TYPE

                if (phoneNumberMatchesQuery && isPhoneNumberQuery) {
                    priority = PHONE_NUMBER_MATCH_PRIORITY
                    matchType = 7
                } else if (row.normalizedName == normalizedQuery) {
                    priority = EXACT_MATCH_PRIORITY
                    matchType = 1
                } else if (row.normalizedName.startsWith(normalizedQuery) && row.nameParts.size == 1) {
                    priority = STARTS_WITH_SINGLE_WORD_PRIORITY
                    matchType = 2
                } else if (row.normalizedName.startsWith(normalizedQuery) && row.nameParts.size > 1) {
                    priority = STARTS_WITH_MULTI_WORD_PRIORITY
                    matchType = 3
                } else if (row.nameParts.size >= 2 && row.nameParts[1].startsWith(normalizedQuery)) {
                    priority = SECOND_WORD_STARTS_WITH_PRIORITY
                    matchType = 4
                } else if (row.nameParts.size >= 3 && row.nameParts[2].startsWith(normalizedQuery)) {
                    priority = THIRD_WORD_STARTS_WITH_PRIORITY
                    matchType = 5
                } else if (row.nameParts.size >= 4 && row.nameParts[3].startsWith(normalizedQuery)) {
                    priority = FOURTH_WORD_STARTS_WITH_PRIORITY
                    matchType = 6
                } else if (phoneNumberMatchesQuery) {
                    priority = PHONE_NUMBER_MATCH_PRIORITY
                    matchType = 7
                }

                val currentPriority = contactRelevanceMap[row.id] ?: 0
                val currentMatchType = contactMatchTypeMap[row.id] ?: Int.MAX_VALUE

                if (matchType < currentMatchType || (matchType == currentMatchType && priority > currentPriority)) {
                    contactRelevanceMap[row.id] = priority
                    contactMatchTypeMap[row.id] = matchType
                }

                val existingContact = seenContactsMap[row.id]
                if (existingContact == null) {
                    val newContact = Contact(
                        id = row.id,
                        name = row.name,
                        phoneNumber = row.number,
                        phoneNumbers = listOf(row.number),
                        photoUri = row.photoUri
                    )
                    if (ContactUtils.isValidContact(newContact)) {
                        seenContactsMap[row.id] = newContact
                        normalizedNumbersByContact[row.id] = mutableSetOf(row.comparablePhoneNumber)
                    }
                } else {
                    val normalizedSet = normalizedNumbersByContact.getOrPut(row.id) {
                        existingContact.phoneNumbers.map { PhoneNumberUtils.normalizePhoneNumber(it) }.toMutableSet()
                    }
                    if (normalizedSet.add(row.comparablePhoneNumber)) {
                        val updatedContact = existingContact.copy(
                            phoneNumbers = existingContact.phoneNumbers + row.number
                        )
                        if (ContactUtils.isValidContact(updatedContact)) {
                            seenContactsMap[row.id] = updatedContact
                        }
                    }
                }
            }

            val sanitizedContacts = seenContactsMap.values.mapNotNull { ContactUtils.sanitizeContact(it) }
            val sortedContacts = sanitizedContacts.sortedWith(
                compareBy<Contact> { contactMatchTypeMap[it.id] ?: Int.MAX_VALUE }
                    .thenByDescending { contactRelevanceMap[it.id] ?: 0 }
                    .thenBy { it.name.lowercase() }
            )

            searchCache[cacheKey] = sortedContacts
            cacheTimestamps[cacheKey] = currentTime
            return sortedContacts
        } catch (e: Exception) {
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
            val contactCache = mutableMapOf<String, Contact>()

            val selectedContactIds = if (filterQuickContacts) selectedContacts.map { it.id }.toSet() else emptySet()

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

                        if (callType == CallLog.Calls.BLOCKED_TYPE || callType == 4) {
                            continue
                        }

                        if (!number.isNullOrBlank()) {
                            val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(number)

                            val contact = contactCache[normalizedNumber] ?: run {
                                val foundContact = ContactUtils.getContactByPhoneNumberForRecentCalls(context, number, cachedName)
                                if (foundContact != null) {
                                    contactCache[normalizedNumber] = foundContact
                                }
                                foundContact
                            }

                            if (contact != null) {
                                if (!selectedContactIds.contains(contact.id) && !seenContactIds.contains(contact.id)) {
                                    seenContactIds.add(contact.id)
                                    recentCallsList.add(
                                        contact.copy(
                                            callType = getCallTypeString(callType),
                                            callTimestamp = callTimestamp
                                        )
                                    )
                                }
                            } else {
                                val formattedNumber = PhoneNumberUtils.formatPhoneNumber(number)
                                val digitsOnly = toDigits(number)
                                val isShortServiceNumber = digitsOnly.length in 3..6
                                if (isShortServiceNumber || PhoneNumberUtils.isValidPhoneNumber(number)) {
                                    val fallbackContactId = "call_history_${number.hashCode()}"
                                    if (!seenContactIds.contains(fallbackContactId)) {
                                        seenContactIds.add(fallbackContactId)
                                        recentCallsList.add(
                                            Contact(
                                                id = fallbackContactId,
                                                name = if (!cachedName.isNullOrBlank()) cachedName.trim() else formattedNumber,
                                                phoneNumber = number,
                                                phoneNumbers = listOf(number),
                                                photoUri = null,
                                                callType = getCallTypeString(callType),
                                                callTimestamp = callTimestamp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return recentCallsList
        } catch (e: Exception) {
            android.util.Log.e("QuickContacts", "Error loading recent calls: ${e.message}")
            return emptyList()
        }
    }

    private fun aggregateRowsToContacts(rows: List<SearchRow>): List<Contact> {
        val seenContactsMap = mutableMapOf<String, Contact>()
        val normalizedNumbersByContact = mutableMapOf<String, MutableSet<String>>()

        for (row in rows) {
            val existingContact = seenContactsMap[row.id]
            if (existingContact == null) {
                val contact = Contact(
                    id = row.id,
                    name = row.name,
                    phoneNumber = row.number,
                    phoneNumbers = listOf(row.number),
                    photoUri = row.photoUri
                )
                if (ContactUtils.isValidContact(contact)) {
                    seenContactsMap[row.id] = contact
                    normalizedNumbersByContact[row.id] = mutableSetOf(row.comparablePhoneNumber)
                }
            } else {
                val normalizedSet = normalizedNumbersByContact.getOrPut(row.id) {
                    existingContact.phoneNumbers.map { PhoneNumberUtils.normalizePhoneNumber(it) }.toMutableSet()
                }
                if (normalizedSet.add(row.comparablePhoneNumber)) {
                    val updatedContact = existingContact.copy(phoneNumbers = existingContact.phoneNumbers + row.number)
                    if (ContactUtils.isValidContact(updatedContact)) {
                        seenContactsMap[row.id] = updatedContact
                    }
                }
            }
        }

        return seenContactsMap.values.mapNotNull { ContactUtils.sanitizeContact(it) }
    }

    fun getAllContacts(context: Context): List<Contact> {
        return try {
            val rows = queryPhoneRows(context, selection = null, selectionArgs = null)
            aggregateRowsToContacts(rows)
        } catch (e: Exception) {
            android.util.Log.e("QuickContacts", "Error getting all contacts: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get the latest call activity for a specific contact by phone number.
     */
    fun getLatestCallActivityForContact(context: Context, phoneNumbers: List<String>): Contact? {
        try {
            val targetNumbers = phoneNumbers
                .map { PhoneNumberUtils.normalizePhoneNumber(it) }
                .toSet()

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

                        if (!number.isNullOrBlank()) {
                            val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(number)
                            if (targetNumbers.contains(normalizedNumber)) {
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
            android.util.Log.e("QuickContacts", "Error getting latest call activity: ${e.message}")
            return null
        }
    }

    /**
     * Single-query variant used for quick-list call activity refresh.
     */
    fun getLatestCallActivityForContacts(context: Context, contacts: List<Contact>): Map<String, Contact> {
        if (contacts.isEmpty()) {
            return emptyMap()
        }

        val numberToContacts = mutableMapOf<String, MutableList<Contact>>()
        contacts.forEach { contact ->
            contact.phoneNumbers.forEach { number ->
                val normalized = PhoneNumberUtils.normalizePhoneNumber(number)
                if (normalized.isNotBlank()) {
                    numberToContacts.getOrPut(normalized) { mutableListOf() }.add(contact)
                }
            }
        }

        if (numberToContacts.isEmpty()) {
            return emptyMap()
        }

        val result = mutableMapOf<String, Contact>()

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
                val typeColumn = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateColumn = it.getColumnIndex(CallLog.Calls.DATE)

                if (numberColumn >= 0 && typeColumn >= 0) {
                    var checkedItems = 0
                    while (it.moveToNext() && checkedItems < RECENT_CALLS_LIMIT) {
                        checkedItems++

                        val number = it.getString(numberColumn)
                        if (number.isNullOrBlank()) {
                            continue
                        }

                        val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(number)
                        val matchedContacts = numberToContacts[normalizedNumber] ?: continue

                        val callType = it.getInt(typeColumn)
                        val callTimestamp = if (dateColumn >= 0) it.getLong(dateColumn) else null

                        for (contact in matchedContacts) {
                            if (result.containsKey(contact.id)) {
                                continue
                            }
                            result[contact.id] = contact.copy(
                                callType = getCallTypeString(callType),
                                callTimestamp = callTimestamp
                            )
                        }

                        if (result.size == contacts.size) {
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("QuickContacts", "Error getting latest call activity for contacts: ${e.message}")
        }

        return result
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
