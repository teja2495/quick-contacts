package com.tk.quickcontacts.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.tk.quickcontacts.Contact

class PhoneService {
    
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
    
    fun formatPhoneNumber(phoneNumber: String): String {
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