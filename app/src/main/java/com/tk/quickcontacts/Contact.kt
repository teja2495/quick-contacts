package com.tk.quickcontacts

import android.graphics.Bitmap

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String, // Primary phone number for compatibility
    val phoneNumbers: List<String> = listOf(phoneNumber), // All phone numbers
    val photo: Bitmap? = null,
    val photoUri: String? = null
) 