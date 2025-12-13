package com.tk.quickcontacts.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.utils.ContactUtils
import com.tk.quickcontacts.utils.PhoneNumberUtils

class PhoneService {
    
    fun makePhoneCall(context: Context, phoneNumber: String, directDial: Boolean = true) {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            android.util.Log.w("PhoneService", "Invalid phone number for call: $phoneNumber")
            return
        }
        
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            android.util.Log.w("PhoneService", "Could not clean phone number: $phoneNumber")
            return
        }
        
        // Check if CALL_PHONE permission is granted
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        
        try {
            if (directDial && hasCallPermission) {
                // Direct dial - make the call immediately (requires CALL_PHONE permission)
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } else {
                // Open dialer with pre-filled number (doesn't require CALL_PHONE permission)
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("PhoneService", "Error making phone call to: $cleanNumber", e)
            // Fallback to dialer with number
            openDialerWithNumber(context, cleanNumber)
        }
    }
    
    fun openDialer(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PhoneService", "Error opening dialer", e)
        }
    }
    
    fun openDialerWithNumber(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PhoneService", "Error opening dialer with number: $phoneNumber", e)
        }
    }
    
    fun openPhoneApp(context: Context) {
        openDialer(context)
    }
    
    fun openContactInContactsApp(context: Context, contact: Contact) {
        if (!ContactUtils.isValidContact(contact)) {
            android.util.Log.w("PhoneService", "Invalid contact for opening in contacts app: ${contact.id}")
            return
        }
        
        try {
            // Extract actual contact ID for recent calls contacts
            val actualContactId = ContactUtils.extractActualContactId(contact.id)
            
            if (actualContactId != null) {
                // Try to open the specific contact using contact ID
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, actualContactId)
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            android.util.Log.w("PhoneService", "Error opening contact by ID: ${contact.id}", e)
        }
        
        try {
            // Fallback: search for contact by phone number
            val primaryNumber = ContactUtils.getPrimaryPhoneNumber(contact)
            if (primaryNumber.isNotBlank() && PhoneNumberUtils.isValidPhoneNumber(primaryNumber)) {
                val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(primaryNumber)
                if (cleanNumber != null) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(cleanNumber))
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    return
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("PhoneService", "Error opening contact by phone number: ${contact.phoneNumber}", e)
        }
        
        try {
            // Final fallback: just open contacts app
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = ContactsContract.Contacts.CONTENT_URI
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PhoneService", "Error opening contacts app", e)
        }
    }
    
    fun formatPhoneNumber(phoneNumber: String): String {
        return PhoneNumberUtils.formatPhoneNumber(phoneNumber)
    }
    
    fun addNewContact(context: Context, phoneNumber: String) {
        try {
            // For adding contacts, allow any number format
            // Just clean it by removing non-digit/non-plus characters
            val cleanNumber = phoneNumber.replace(Regex("[^+\\d]"), "")
            
            if (cleanNumber.isEmpty()) {
                android.util.Log.w("PhoneService", "Phone number is empty after cleaning: $phoneNumber")
                return
            }
            
            val intent = Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, cleanNumber)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PhoneService", "Error adding new contact with number: $phoneNumber", e)
        }
    }
} 