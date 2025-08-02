package com.tk.quickcontacts

import android.graphics.Bitmap
import com.google.gson.annotations.Expose

data class Contact(
    @Expose
    val id: String,
    @Expose
    val name: String,
    @Expose
    val phoneNumber: String, // Primary phone number for compatibility
    @Expose
    val phoneNumbers: List<String> = listOf(phoneNumber), // All phone numbers
    @Expose(serialize = false, deserialize = false)
    val photo: Bitmap? = null,
    @Expose
    val photoUri: String? = null,
    @Expose
    val callType: String? = null, // Call type: "missed", "rejected", "incoming", "outgoing", or null for non-call contacts
    @Expose
    val callTimestamp: Long? = null // Timestamp of the call in milliseconds
) 