package com.tk.quickcontacts.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.tk.quickcontacts.Contact

/**
 * Utility functions for contact operations
 */
object ContactUtils {
    
    /**
     * Validate contact data integrity
     */
    fun isValidContact(contact: Contact): Boolean {
        return try {
            // Check required fields
            if (contact.id.isBlank() || contact.name.isBlank() || contact.phoneNumber.isBlank()) {
                return false
            }
            
            // Validate phone number
            if (!PhoneNumberUtils.isValidPhoneNumber(contact.phoneNumber)) {
                return false
            }
            
            // Check phone numbers list consistency
            if (contact.phoneNumbers.isEmpty()) {
                return false
            }
            
            // Validate all phone numbers in the list
            contact.phoneNumbers.all { phoneNumber ->
                phoneNumber.isNotBlank() && PhoneNumberUtils.isValidPhoneNumber(phoneNumber)
            }
        } catch (e: Exception) {
            android.util.Log.w("ContactUtils", "Error validating contact: ${contact.id}", e)
            false
        }
    }
    
    /**
     * Sanitize contact data to prevent crashes
     */
    fun sanitizeContact(contact: Contact): Contact? {
        return try {
            // Validate basic structure
            if (contact.id.isBlank() || contact.name.isBlank()) {
                return null
            }
            
            // Clean and validate phone numbers
            val validPhoneNumbers = contact.phoneNumbers.filter { phoneNumber ->
                phoneNumber.isNotBlank() && PhoneNumberUtils.isValidPhoneNumber(phoneNumber)
            }
            
            if (validPhoneNumbers.isEmpty()) {
                return null
            }
            
            // Use first valid phone number as primary
            val primaryPhoneNumber = validPhoneNumbers.first()
            
            // Clean name (remove excessive whitespace, etc.)
            val cleanName = contact.name.trim().replace(Regex("\\s+"), " ")
            
            if (cleanName.isBlank()) {
                return null
            }
            
            Contact(
                id = contact.id.trim(),
                name = cleanName,
                phoneNumber = primaryPhoneNumber,
                phoneNumbers = validPhoneNumbers,
                photo = contact.photo,
                photoUri = contact.photoUri?.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            android.util.Log.w("ContactUtils", "Error sanitizing contact: ${contact.id}", e)
            null
        }
    }
    
    /**
     * Get contact photo URI for a given contact ID with validation
     */
    fun getContactPhotoUri(context: Context, contactId: String): String? {
        if (contactId.isBlank()) {
            return null
        }
        
        return try {
            val photoUri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_URI,
                contactId
            ).let { contactUri ->
                Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
            }
            
            // Check if photo exists by trying to open an input stream
            val inputStream = context.contentResolver.openInputStream(photoUri)
            if (inputStream != null) {
                inputStream.close()
                photoUri.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            // Photo doesn't exist or can't be accessed
            android.util.Log.d("ContactUtils", "Contact photo not available for ID: $contactId")
            null
        }
    }
    
    /**
     * Parse contact from URI with validation
     */
    fun parseContactFromUri(context: Context, contactUri: Uri): Contact? {
        if (contactUri == Uri.EMPTY) {
            return null
        }
        
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                contactUri,
                projection,
                null,
                null,
                null
            )

            cursor?.let {
                if (it.moveToFirst()) {
                    val idColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    if (idColumn >= 0 && nameColumn >= 0 && numberColumn >= 0) {
                        val id = it.getString(idColumn)
                        val name = it.getString(nameColumn)
                        val number = it.getString(numberColumn)

                        if (name != null && number != null && id != null && 
                            name.isNotBlank() && number.isNotBlank() && id.isNotBlank() &&
                            PhoneNumberUtils.isValidPhoneNumber(number)) {
                            
                            // Get the contact photo URI using the contact ID
                            val photoUri = getContactPhotoUri(context, id)
                            
                            val contact = Contact(
                                id = id,
                                name = name,
                                phoneNumber = number,
                                phoneNumbers = listOf(number),
                                photoUri = photoUri
                            )
                            
                            // Validate the created contact
                            if (isValidContact(contact)) {
                                return contact
                            } else {
                                return null
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactUtils", "Error parsing contact from URI: $contactUri", e)
        } finally {
            cursor?.close()
        }

        return null
    }
    
    /**
     * Get contact by phone number lookup with validation
     */
    fun getContactByPhoneNumber(context: Context, phoneNumber: String, cachedName: String? = null): Contact? {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            return null
        }
        
        return try {
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
                        val id = it.getString(idColumnIndex)
                        val name = it.getString(nameColumnIndex)
                        val number = it.getString(numberColumnIndex)
                        
                        if (id != null && name != null && number != null && 
                            id.isNotBlank() && name.isNotBlank() && number.isNotBlank() &&
                            PhoneNumberUtils.isValidPhoneNumber(number)) {
                            
                            val photoUri = getContactPhotoUri(context, id)
                            
                            val contact = Contact(
                                id = id,
                                name = name,
                                phoneNumber = number,
                                phoneNumbers = listOf(number),
                                photoUri = photoUri
                            )
                            
                            // Validate the created contact
                            if (isValidContact(contact)) {
                                return contact
                            } else {
                                return null
                            }
                        }
                    }
                }
            }
            
            // If we reach here, no contact was found
            null
        } catch (e: Exception) {
            android.util.Log.e("ContactUtils", "Error getting contact by phone number: $phoneNumber", e)
            null
        }
        
        // Fallback: create contact from cached name if available, otherwise use 'Unknown Number'
        val fallbackName = if (!cachedName.isNullOrBlank()) cachedName.trim() else "Unknown Number"
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber != null) {
            val fallbackContact = Contact(
                id = "call_history_${phoneNumber.hashCode().toString()}",
                name = fallbackName,
                phoneNumber = cleanNumber,
                phoneNumbers = listOf(cleanNumber),
                photoUri = null
            )
            
            if (isValidContact(fallbackContact)) {
                return fallbackContact
            } else {
                return null
            }
        }
        
        return null
    }
    
    /**
     * Get the primary phone number or first available with validation
     */
    fun getPrimaryPhoneNumber(contact: Contact): String {
        return try {
            if (contact.phoneNumbers.isNotEmpty()) {
                val primaryNumber = contact.phoneNumbers.first()
                if (PhoneNumberUtils.isValidPhoneNumber(primaryNumber)) {
                    return primaryNumber
                }
            }
            
            // Fallback to contact.phoneNumber
            if (PhoneNumberUtils.isValidPhoneNumber(contact.phoneNumber)) {
                return contact.phoneNumber
            }
            
            // Last resort: return empty string
            ""
        } catch (e: Exception) {
            android.util.Log.w("ContactUtils", "Error getting primary phone number for contact: ${contact.id}", e)
            ""
        }
    }
    
    /**
     * Validate contact ID format
     */
    fun isValidContactId(contactId: String): Boolean {
        return try {
            contactId.isNotBlank() && (
                contactId.all { it.isDigit() } || 
                contactId.startsWith("call_history_") ||
                contactId.contains("-")
            )
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extract actual contact ID from various formats
     */
    fun extractActualContactId(contactId: String): String? {
        return try {
            when {
                contactId.startsWith("call_history_") -> {
                    val extractedId = contactId.substringAfter("call_history_")
                    if (extractedId.all { it.isDigit() } || extractedId.contains("-")) {
                        extractedId
                    } else {
                        null
                    }
                }
                isValidContactId(contactId) -> contactId
                else -> null
            }
        } catch (e: Exception) {
            android.util.Log.w("ContactUtils", "Error extracting contact ID from: $contactId", e)
            null
        }
    }
} 