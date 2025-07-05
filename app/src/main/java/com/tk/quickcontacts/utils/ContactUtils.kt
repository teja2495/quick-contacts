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
     * Get contact photo URI for a given contact ID
     */
    fun getContactPhotoUri(context: Context, contactId: String): String? {
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
    
    /**
     * Parse contact from URI
     */
    fun parseContactFromUri(context: Context, contactUri: Uri): Contact? {
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

                        if (name != null && number != null) {
                            // Get the contact photo URI using the contact ID
                            val photoUri = getContactPhotoUri(context, id)
                            
                            return Contact(
                                id = id,
                                name = name,
                                phoneNumber = number,
                                photoUri = photoUri
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return null
    }
    
    /**
     * Get contact by phone number lookup
     */
    fun getContactByPhoneNumber(context: Context, phoneNumber: String, cachedName: String?): Contact? {
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
                        val id = it.getString(idColumnIndex)
                        val name = it.getString(nameColumnIndex)
                        val number = it.getString(numberColumnIndex)
                        
                        if (id != null && name != null && number != null) {
                            val photoUri = getContactPhotoUri(context, id)
                            
                            return Contact(
                                id = id,
                                name = name,
                                phoneNumber = number,
                                photoUri = photoUri
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return null
    }
    
    /**
     * Get the primary phone number or first available
     */
    fun getPrimaryPhoneNumber(contact: Contact): String {
        return if (contact.phoneNumbers.isNotEmpty()) contact.phoneNumbers.first() else contact.phoneNumber
    }
} 