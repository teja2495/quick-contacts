package com.tk.quickcontacts

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.models.CustomActions
import com.tk.quickcontacts.repository.PreferencesRepository
import com.tk.quickcontacts.services.ContactService
import com.tk.quickcontacts.services.MessagingService
import com.tk.quickcontacts.services.PhoneService

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    // Services
    private val preferencesRepository = PreferencesRepository(application)
    private val contactService = ContactService()
    private val messagingService = MessagingService()
    private val phoneService = PhoneService()
    
    // State flows
    private val _selectedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val selectedContacts: StateFlow<List<Contact>> = _selectedContacts

    private val _recentCalls = MutableStateFlow<List<Contact>>(emptyList())
    val recentCalls: StateFlow<List<Contact>> = _recentCalls
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _filteredSelectedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val filteredSelectedContacts: StateFlow<List<Contact>> = _filteredSelectedContacts
    
    private val _filteredRecentCalls = MutableStateFlow<List<Contact>>(emptyList())
    val filteredRecentCalls: StateFlow<List<Contact>> = _filteredRecentCalls
    
    private val _searchResults = MutableStateFlow<List<Contact>>(emptyList())
    val searchResults: StateFlow<List<Contact>> = _searchResults

    // Action preferences
    private val _actionPreferences = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val actionPreferences: StateFlow<Map<String, Boolean>> = _actionPreferences

    // Custom action preferences
    private val _customActionPreferences = MutableStateFlow<Map<String, CustomActions>>(emptyMap())
    val customActionPreferences: StateFlow<Map<String, CustomActions>> = _customActionPreferences

    // Settings preferences
    private val _isInternationalDetectionEnabled = MutableStateFlow(false)
    val isInternationalDetectionEnabled: StateFlow<Boolean> = _isInternationalDetectionEnabled
    
    private val _isRecentCallsVisible = MutableStateFlow(true)
    val isRecentCallsVisible: StateFlow<Boolean> = _isRecentCallsVisible
    
    private val _defaultMessagingApp = MutableStateFlow(MessagingApp.WHATSAPP)
    val defaultMessagingApp: StateFlow<MessagingApp> = _defaultMessagingApp
    
    private val _availableMessagingApps = MutableStateFlow<Set<MessagingApp>>(emptySet())
    val availableMessagingApps: StateFlow<Set<MessagingApp>> = _availableMessagingApps
    
    // Backward compatibility
    val useWhatsAppAsDefault: StateFlow<Boolean> = _defaultMessagingApp.map { it == MessagingApp.WHATSAPP }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main),
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    // Search debouncing
    private var searchJob: kotlinx.coroutines.Job? = null
    private val searchDebounceDelay = 300L // 300ms debounce

    init {
        loadContacts()
        loadActionPreferences()
        loadCustomActionPreferences()
        loadSettings()
        checkAvailableMessagingApps()
        // Initialize filtered lists
        _filteredSelectedContacts.value = _selectedContacts.value
        _filteredRecentCalls.value = _recentCalls.value
    }
    
    // First launch and testing methods
    fun checkAndLoadFavoriteContactsOnFirstLaunch(context: Context) {
        if (preferencesRepository.isFirstTimeLaunch() && _selectedContacts.value.isEmpty()) {
            android.util.Log.d("QuickContacts", "First time launch detected, loading favorite contacts...")
            loadFavoriteContactsOnFirstLaunch(context)
        }
    }
    
    fun loadFavoriteContactsForTesting(context: Context) {
        android.util.Log.d("QuickContacts", "Manually loading favorite contacts for testing...")
        val favoriteContacts = contactService.getFavoriteContacts(context)
        android.util.Log.d("QuickContacts", "Found ${favoriteContacts.size} favorite contacts for testing")
        
        if (favoriteContacts.isNotEmpty()) {
            val contactsToAdd = favoriteContacts.take(5)
            contactsToAdd.forEach { contact ->
                android.util.Log.d("QuickContacts", "Adding favorite contact for testing: ${contact.name} - ${contact.phoneNumber}")
                addContact(contact)
            }
        }
    }
    
    fun resetFirstLaunchFlag() {
        preferencesRepository.resetFirstLaunchFlag()
        android.util.Log.d("QuickContacts", "First launch flag reset for testing")
    }
    
    fun hasShownEditHint(): Boolean {
        return preferencesRepository.hasShownEditHint()
    }
    
    fun markEditHintShown() {
        preferencesRepository.markEditHintShown()
        android.util.Log.d("QuickContacts", "Edit hint marked as shown")
    }
    
    fun resetEditHintFlag() {
        preferencesRepository.resetEditHintFlag()
        android.util.Log.d("QuickContacts", "Edit hint flag reset for testing")
    }
    
    // Private helper methods
    private fun loadFavoriteContactsOnFirstLaunch(context: Context) {
        try {
            android.util.Log.d("QuickContacts", "Loading favorite contacts...")
            val favoriteContacts = contactService.getFavoriteContacts(context)
            android.util.Log.d("QuickContacts", "Found ${favoriteContacts.size} favorite contacts")
            
            if (favoriteContacts.isNotEmpty()) {
                val contactsToAdd = favoriteContacts.take(5)
                android.util.Log.d("QuickContacts", "Adding ${contactsToAdd.size} favorite contacts to quick list")
                contactsToAdd.forEach { contact ->
                    android.util.Log.d("QuickContacts", "Adding favorite contact: ${contact.name} - ${contact.phoneNumber}")
                    addContact(contact)
                }
                preferencesRepository.markFirstLaunchComplete()
                android.util.Log.d("QuickContacts", "First launch setup completed")
            } else {
                android.util.Log.d("QuickContacts", "No favorite contacts found, marking first launch complete")
                preferencesRepository.markFirstLaunchComplete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("QuickContacts", "Error loading favorite contacts: ${e.message}")
            preferencesRepository.markFirstLaunchComplete()
        }
    }
    
    private fun checkAvailableMessagingApps() {
        val packageManager = getApplication<Application>().packageManager
        val availableApps = messagingService.checkAvailableMessagingApps(packageManager)
        
        android.util.Log.d("QuickContacts", "Available messaging apps: $availableApps")
        _availableMessagingApps.value = availableApps
        
        // If current default app is not available, switch to SMS
        if (!availableApps.contains(_defaultMessagingApp.value)) {
            android.util.Log.d("QuickContacts", "Current default app ${_defaultMessagingApp.value} not available, switching to SMS")
            _defaultMessagingApp.value = MessagingApp.SMS
            saveSettings()
        }
    }
    
    // Search and filtering
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterContacts()
        
        // Cancel previous search job
        searchJob?.cancel()
        
        // Debounce search for all contacts
        if (query.isNotEmpty()) {
            searchJob = CoroutineScope(Dispatchers.IO).launch {
                delay(searchDebounceDelay)
                if (query == _searchQuery.value) { // Only search if query hasn't changed
                    val results = contactService.searchContacts(getApplication(), query)
                    _searchResults.value = results
                }
            }
        } else {
            _searchResults.value = emptyList()
        }
    }
    
    private fun filterContacts() {
        val query = _searchQuery.value.lowercase().trim()
        
        if (query.isEmpty()) {
            _filteredSelectedContacts.value = _selectedContacts.value
            _filteredRecentCalls.value = _recentCalls.value
        } else {
            _filteredSelectedContacts.value = _selectedContacts.value.filter { contact ->
                contact.name.lowercase().contains(query)
            }
            
            _filteredRecentCalls.value = _recentCalls.value.filter { contact ->
                contact.name.lowercase().contains(query)
            }
        }
    }
    
    fun searchAllContacts(context: Context, query: String) {
        // This method is now handled by updateSearchQuery with debouncing
        // Keeping for backward compatibility but it's no longer needed
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
        }
    }
    
    // Data loading and saving
    private fun loadContacts() {
        val contacts = preferencesRepository.loadContacts()
        _selectedContacts.value = contacts
        filterContacts()
    }

    private fun saveContacts() {
        preferencesRepository.saveContacts(_selectedContacts.value)
    }

    private fun loadActionPreferences() {
        val preferences = preferencesRepository.loadActionPreferences()
        _actionPreferences.value = preferences
    }

    private fun saveActionPreferences() {
        preferencesRepository.saveActionPreferences(_actionPreferences.value)
    }

    private fun loadCustomActionPreferences() {
        val preferences = preferencesRepository.loadCustomActionPreferences()
        _customActionPreferences.value = preferences
    }

    private fun saveCustomActionPreferences() {
        preferencesRepository.saveCustomActionPreferences(_customActionPreferences.value)
    }

    private fun loadSettings() {
        val (isInternationalDetectionEnabled, isRecentCallsVisible, defaultMessagingApp) = preferencesRepository.loadSettings()
        _isInternationalDetectionEnabled.value = isInternationalDetectionEnabled
        _isRecentCallsVisible.value = isRecentCallsVisible
        _defaultMessagingApp.value = defaultMessagingApp
    }

    private fun saveSettings() {
        preferencesRepository.saveSettings(
            _isInternationalDetectionEnabled.value,
            _isRecentCallsVisible.value,
            _defaultMessagingApp.value
        )
    }
    
    // Public methods for preferences
    fun toggleActionPreference(contactId: String) {
        val currentPreferences = _actionPreferences.value.toMutableMap()
        currentPreferences[contactId] = !currentPreferences.getOrDefault(contactId, false)
        _actionPreferences.value = currentPreferences
        saveActionPreferences()
    }

    fun setCustomActions(contactId: String, primaryAction: String, secondaryAction: String) {
        val currentPreferences = _customActionPreferences.value.toMutableMap()
        currentPreferences[contactId] = CustomActions(primaryAction, secondaryAction)
        _customActionPreferences.value = currentPreferences
        saveCustomActionPreferences()
    }

    fun removeCustomActions(contactId: String) {
        val currentPreferences = _customActionPreferences.value.toMutableMap()
        currentPreferences.remove(contactId)
        _customActionPreferences.value = currentPreferences
        saveCustomActionPreferences()
    }

    fun toggleInternationalDetection() {
        _isInternationalDetectionEnabled.value = !_isInternationalDetectionEnabled.value
        saveSettings()
    }

    fun toggleRecentCallsVisibility() {
        _isRecentCallsVisible.value = !_isRecentCallsVisible.value
        saveSettings()
    }

    fun setMessagingApp(app: MessagingApp) {
        _defaultMessagingApp.value = app
        saveSettings()
    }
    
    fun toggleMessagingApp() {
        _defaultMessagingApp.value = when (_defaultMessagingApp.value) {
            MessagingApp.WHATSAPP -> MessagingApp.SMS
            MessagingApp.SMS -> MessagingApp.WHATSAPP
            MessagingApp.TELEGRAM -> MessagingApp.WHATSAPP
        }
        saveSettings()
    }

    // Contact management
    fun addContact(contact: Contact) {
        val currentList = _selectedContacts.value.toMutableList()
        if (!currentList.any { it.id == contact.id }) {
            currentList.add(contact)
            _selectedContacts.value = currentList
            saveContacts()
            filterContacts()
        }
    }

    fun removeContact(contact: Contact) {
        val currentList = _selectedContacts.value.toMutableList()
        currentList.removeAll { it.id == contact.id }
        _selectedContacts.value = currentList
        saveContacts()
        filterContacts()
    }

    fun moveContact(fromIndex: Int, toIndex: Int) {
        val currentList = _selectedContacts.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val contact = currentList.removeAt(fromIndex)
            currentList.add(toIndex, contact)
            _selectedContacts.value = currentList
            saveContacts()
            filterContacts()
        }
    }

    // Action execution
    fun makePhoneCall(context: Context, phoneNumber: String) {
        phoneService.makePhoneCall(context, phoneNumber)
    }
    
    fun openDialer(context: Context) {
        phoneService.openDialer(context)
    }
    
    fun openWhatsAppChat(context: Context, phoneNumber: String) {
        messagingService.openWhatsAppChat(context, phoneNumber)
    }

    fun openSmsApp(context: Context, phoneNumber: String) {
        messagingService.openSmsApp(context, phoneNumber)
    }

    fun openTelegramChat(context: Context, phoneNumber: String) {
        messagingService.openTelegramChat(context, phoneNumber)
    }

    fun openMessagingApp(context: Context, phoneNumber: String) {
        messagingService.openMessagingApp(context, phoneNumber, _defaultMessagingApp.value)
    }

    fun executeAction(context: Context, action: String, phoneNumber: String) {
        when (action) {
            "Call" -> makePhoneCall(context, phoneNumber)
            "WhatsApp" -> openWhatsAppChat(context, phoneNumber)
            "SMS" -> openSmsApp(context, phoneNumber)
            "Telegram" -> openTelegramChat(context, phoneNumber)
        }
    }

    fun openContactInContactsApp(context: Context, contact: Contact) {
        phoneService.openContactInContactsApp(context, contact)
    }

    fun loadRecentCalls(context: Context) {
        val recentCallsList = contactService.loadRecentCalls(context, _selectedContacts.value)
        _recentCalls.value = recentCallsList
        filterContacts()
    }
    
    fun refreshAvailableMessagingApps() {
        checkAvailableMessagingApps()
    }
    
    fun formatPhoneNumber(phoneNumber: String): String {
        return phoneService.formatPhoneNumber(phoneNumber)
    }
} 