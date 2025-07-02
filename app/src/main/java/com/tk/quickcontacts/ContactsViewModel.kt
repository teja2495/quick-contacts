package com.tk.quickcontacts

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val _selectedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val selectedContacts: StateFlow<List<Contact>> = _selectedContacts

    private val sharedPreferences: SharedPreferences
    private val gson = Gson()

    init {
        sharedPreferences = application.getSharedPreferences("QuickContactsPrefs", Context.MODE_PRIVATE)
        loadContacts()
    }

    private fun loadContacts() {
        val json = sharedPreferences.getString("selected_contacts", null)
        if (json != null) {
            val type = object : TypeToken<List<Contact>>() {}.type
            _selectedContacts.value = gson.fromJson(json, type)
        }
    }

    private fun saveContacts() {
        val json = gson.toJson(_selectedContacts.value)
        sharedPreferences.edit().putString("selected_contacts", json).apply()
    }

    fun addContact(contact: Contact) {
        val currentList = _selectedContacts.value.toMutableList()
        if (!currentList.any { it.id == contact.id }) {
            currentList.add(contact)
            _selectedContacts.value = currentList
            saveContacts()
        }
    }

    fun removeContact(contact: Contact) {
        val currentList = _selectedContacts.value.toMutableList()
        currentList.removeAll { it.id == contact.id }
        _selectedContacts.value = currentList
        saveContacts()
    }

    fun moveContact(fromIndex: Int, toIndex: Int) {
        val currentList = _selectedContacts.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val contact = currentList.removeAt(fromIndex)
            currentList.add(toIndex, contact)
            _selectedContacts.value = currentList
            saveContacts()
        }
    }

    fun makePhoneCall(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 