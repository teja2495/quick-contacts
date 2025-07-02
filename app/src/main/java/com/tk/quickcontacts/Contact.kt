package com.tk.quickcontacts

import android.graphics.Bitmap

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photo: Bitmap? = null,
    val photoUri: String? = null
) 