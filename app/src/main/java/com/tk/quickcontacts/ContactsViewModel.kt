package com.tk.quickcontacts

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.quickcontacts.models.CustomActions
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.repository.PreferencesRepository
import com.tk.quickcontacts.services.ContactService
import com.tk.quickcontacts.services.MessagingService
import com.tk.quickcontacts.services.PhoneService
import com.tk.quickcontacts.utils.ContactUtils
import com.tk.quickcontacts.utils.PhoneNumberUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    // Services
    private val preferencesRepository = PreferencesRepository(application)
    private val contactService = ContactService()
    private val messagingService = MessagingService()
    private val phoneService = PhoneService()
    
    // State flows
    private val _selectedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val selectedContacts: StateFlow<List<Contact>> = _selectedContacts.asStateFlow()

    private val _recentCalls = MutableStateFlow<List<Contact>>(emptyList())
    val recentCalls: StateFlow<List<Contact>> = _recentCalls.asStateFlow()
    
    // New state flow for cached recent calls
    private val _cachedRecentCalls = MutableStateFlow<List<Contact>>(emptyList())
    val cachedRecentCalls: StateFlow<List<Contact>> = _cachedRecentCalls.asStateFlow()
    
    // New state flow for cached all recent calls
    private val _cachedAllRecentCalls = MutableStateFlow<List<Contact>>(emptyList())
    val cachedAllRecentCalls: StateFlow<List<Contact>> = _cachedAllRecentCalls.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _filteredSelectedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val filteredSelectedContacts: StateFlow<List<Contact>> = _filteredSelectedContacts.asStateFlow()
    
    private val _filteredRecentCalls = MutableStateFlow<List<Contact>>(emptyList())
    val filteredRecentCalls: StateFlow<List<Contact>> = _filteredRecentCalls.asStateFlow()
    
    private val _allRecentCalls = MutableStateFlow<List<Contact>>(emptyList())
    val allRecentCalls: StateFlow<List<Contact>> = _allRecentCalls.asStateFlow()
    
    private val _isLoadingRecentCalls = MutableStateFlow(false)
    val isLoadingRecentCalls: StateFlow<Boolean> = _isLoadingRecentCalls.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<Contact>>(emptyList())
    val searchResults: StateFlow<List<Contact>> = _searchResults.asStateFlow()

    private val _allContacts = MutableStateFlow<List<Contact>>(emptyList())
    val allContacts: StateFlow<List<Contact>> = _allContacts.asStateFlow()

    // Action preferences
    private val _actionPreferences = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val actionPreferences: StateFlow<Map<String, Boolean>> = _actionPreferences.asStateFlow()

    // Custom action preferences
    private val _customActionPreferences = MutableStateFlow<Map<String, CustomActions>>(emptyMap())
    val customActionPreferences: StateFlow<Map<String, CustomActions>> = _customActionPreferences.asStateFlow()

    // Settings preferences
    private val _isInternationalDetectionEnabled = MutableStateFlow(false)
    val isInternationalDetectionEnabled: StateFlow<Boolean> = _isInternationalDetectionEnabled.asStateFlow()
    
    private val _isRecentCallsVisible = MutableStateFlow(true)
    val isRecentCallsVisible: StateFlow<Boolean> = _isRecentCallsVisible.asStateFlow()
    
    private val _defaultMessagingApp = MutableStateFlow(MessagingApp.WHATSAPP)
    val defaultMessagingApp: StateFlow<MessagingApp> = _defaultMessagingApp.asStateFlow()
    
    private val _availableMessagingApps = MutableStateFlow<Set<MessagingApp>>(emptySet())
    val availableMessagingApps: StateFlow<Set<MessagingApp>> = _availableMessagingApps.asStateFlow()
    
    // Home country code state
    private val _homeCountryCode = MutableStateFlow<String?>(null)
    val homeCountryCode: StateFlow<String?> = _homeCountryCode.asStateFlow()
    
    // Call activity data for quick list contacts
    private val _callActivityMap = MutableStateFlow<Map<String, Contact>>(emptyMap())
    val callActivityMap: StateFlow<Map<String, Contact>> = _callActivityMap.asStateFlow()
    
    // Backward compatibility
    val useWhatsAppAsDefault: StateFlow<Boolean> = _defaultMessagingApp.map { it == MessagingApp.WHATSAPP }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main),
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    // Call warning state
    private val _hasSeenCallWarning = MutableStateFlow(preferencesRepository.hasSeenCallWarning())
    val hasSeenCallWarning: StateFlow<Boolean> = _hasSeenCallWarning.asStateFlow()

    fun checkCallWarningSeen() {
        _hasSeenCallWarning.value = preferencesRepository.hasSeenCallWarning()
    }

    fun markCallWarningSeen() {
        preferencesRepository.markCallWarningSeen()
        _hasSeenCallWarning.value = true
    }

    // Search debouncing
    private var searchJob: kotlinx.coroutines.Job? = null
    private val searchDebounceDelay = 300L // 300ms debounce

    init {
        loadContacts()
        loadActionPreferences()
        loadCustomActionPreferences()
        loadSettings()
        checkAvailableMessagingApps()
        // Load saved recent calls (this will populate both recent calls and cached calls)
        loadSavedRecentCalls()
        // Initialize filtered lists
        _filteredSelectedContacts.value = _selectedContacts.value
        _filteredRecentCalls.value = _recentCalls.value
        // Load home country code
        _homeCountryCode.value = preferencesRepository.loadHomeCountryCode()
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
    
    fun hasShownRecentCallsHint(): Boolean {
        return preferencesRepository.hasShownRecentCallsHint()
    }
    
    fun markRecentCallsHintShown() {
        preferencesRepository.markRecentCallsHintShown()
        android.util.Log.d("QuickContacts", "Recent calls hint marked as shown")
    }
    
    fun resetRecentCallsHintFlag() {
        preferencesRepository.resetRecentCallsHintFlag()
        android.util.Log.d("QuickContacts", "Recent calls hint flag reset for testing")
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
        android.util.Log.d("ContactsViewModel", "Updating search query: '$query'")
        _searchQuery.value = query
        filterContacts()
        
        // Cancel any ongoing search job
        searchJob?.cancel()
        
        val numberLikeRegex = Regex("^[\\d\\s+\\-()]+$")
        
        if (query.isNotEmpty()) {
            // If the query looks like a number, show only the dummy contact
            if (numberLikeRegex.matches(query)) {
                val normalizedQuery = query.trim()
                val formattedName = PhoneNumberUtils.formatPhoneNumber(query)
                val dummyContact = Contact(
                    id = "search_number_${normalizedQuery}",
                    name = formattedName,
                    phoneNumber = query,
                    phoneNumbers = listOf(query),
                    photo = null,
                    photoUri = null,
                    callType = null
                )
                _searchResults.value = listOf(dummyContact)
                android.util.Log.d("ContactsViewModel", "Query looks like a number, showing only dummy contact")
            } else {
                // Use debouncing to avoid excessive database queries
                searchJob = viewModelScope.launch {
                    delay(searchDebounceDelay) // Wait for user to finish typing
                    
                    // Use the ContactService for advanced search functionality
                    val context = getApplication<Application>().applicationContext
                    val searchResults = contactService.searchContacts(context, query)
                    
                    val mutableResults = searchResults.toMutableList()
                    _searchResults.value = mutableResults
                    android.util.Log.d("ContactsViewModel", "Search completed: ${mutableResults.size} contacts found")
                }
            }
        } else {
            _searchResults.value = emptyList()
            android.util.Log.d("ContactsViewModel", "Empty query, clearing search results")
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
        try {
            val contacts = preferencesRepository.loadContacts()
            
            // Validate and sanitize loaded contacts
            val validContacts = contacts.mapNotNull { contact ->
                if (ContactUtils.isValidContact(contact)) {
                    ContactUtils.sanitizeContact(contact)
                } else {
                    android.util.Log.w("ContactsViewModel", "Removing invalid contact from storage: ${contact.id}")
                    null
                }
            }
            
            _selectedContacts.value = validContacts
            android.util.Log.d("ContactsViewModel", "Loaded ${validContacts.size} valid contacts")
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error loading contacts", e)
            _selectedContacts.value = emptyList()
        }
    }

    private fun saveContacts() {
        try {
            val contacts = _selectedContacts.value
            
            // Validate all contacts before saving
            val validContacts = contacts.filter { ContactUtils.isValidContact(it) }
            
            if (validContacts.size != contacts.size) {
                android.util.Log.w("ContactsViewModel", "Removing ${contacts.size - validContacts.size} invalid contacts before saving")
                _selectedContacts.value = validContacts
            }
            
            preferencesRepository.saveContacts(validContacts)
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error saving contacts", e)
        }
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

    fun enableRecentCallsVisibility() {
        _isRecentCallsVisible.value = true
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
        try {
            // Validate contact before adding
            if (!ContactUtils.isValidContact(contact)) {
                android.util.Log.w("ContactsViewModel", "Invalid contact cannot be added: ${contact.id}")
                return
            }
            val sanitizedContact = ContactUtils.sanitizeContact(contact)
            if (sanitizedContact == null) {
                android.util.Log.w("ContactsViewModel", "Contact sanitization failed: ${contact.id}")
                return
            }
            val currentList = _selectedContacts.value.toMutableList()
            // Remove any contact with the same id (regardless of number)
            val removed = currentList.removeAll { it.id == sanitizedContact.id }
            currentList.add(sanitizedContact)
            _selectedContacts.value = currentList
            saveContacts()
            filterContacts()
            // Validate state consistency
            validateStateConsistency()
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error adding contact: ${contact.id}", e)
        }
    }

    fun removeContact(contact: Contact) {
        try {
            val currentList = _selectedContacts.value.toMutableList()
            currentList.removeAll { it.id == contact.id }
            _selectedContacts.value = currentList
            saveContacts()
            filterContacts()
            
            // Validate state consistency
            validateStateConsistency()
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error removing contact: ${contact.id}", e)
        }
    }

    fun moveContact(fromIndex: Int, toIndex: Int) {
        try {
            val currentList = _selectedContacts.value.toMutableList()
            if (fromIndex in currentList.indices && toIndex in currentList.indices) {
                val contact = currentList.removeAt(fromIndex)
                currentList.add(toIndex, contact)
                _selectedContacts.value = currentList
                saveContacts()
                filterContacts()
                
                // Validate state consistency
                validateStateConsistency()
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error moving contact from $fromIndex to $toIndex", e)
        }
    }

    fun updateContactNumber(contact: Contact, selectedNumber: String) {
        try {
            val currentList = _selectedContacts.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == contact.id }
            if (index != -1) {
                val updatedContact = contact.copy(
                    phoneNumber = selectedNumber,
                    phoneNumbers = listOf(selectedNumber)
                )
                currentList[index] = updatedContact
                _selectedContacts.value = currentList
                saveContacts()
                filterContacts()
                validateStateConsistency()
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error updating contact number for: ${contact.id}", e)
        }
    }

    fun updateContactName(contact: Contact, newName: String) {
        try {
            val currentList = _selectedContacts.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == contact.id }
            if (index != -1) {
                val updatedContact = contact.copy(name = newName.trim())
                currentList[index] = updatedContact
                _selectedContacts.value = currentList
                saveContacts()
                filterContacts()
                validateStateConsistency()
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error updating contact name for: ${contact.id}", e)
        }
    }

    // Action execution with validation
    fun makePhoneCall(context: Context, phoneNumber: String) {
        try {
            if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
                android.util.Log.w("ContactsViewModel", "Invalid phone number for call: $phoneNumber")
                return
            }
            // Append country code if needed
            val phoneNumberWithCountryCode = PhoneNumberUtils.appendCountryCodeIfNeeded(phoneNumber, context, _homeCountryCode.value)
            phoneService.makePhoneCall(context, phoneNumberWithCountryCode)
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error making phone call to: $phoneNumber", e)
        }
    }
    
    fun openDialer(context: Context) {
        try {
            phoneService.openDialer(context)
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error opening dialer", e)
        }
    }
    
    fun openWhatsAppChat(context: Context, phoneNumber: String) {
        try {
            if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
                android.util.Log.w("ContactsViewModel", "Invalid phone number for WhatsApp: $phoneNumber")
                return
            }
            // Append country code if needed
            val phoneNumberWithCountryCode = PhoneNumberUtils.appendCountryCodeIfNeeded(phoneNumber, context, _homeCountryCode.value)
            messagingService.openWhatsAppChat(context, phoneNumberWithCountryCode)
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error opening WhatsApp chat for: $phoneNumber", e)
        }
    }

    fun openSmsApp(context: Context, phoneNumber: String) {
        try {
            if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
                android.util.Log.w("ContactsViewModel", "Invalid phone number for SMS: $phoneNumber")
                return
            }
            // Append country code if needed
            val phoneNumberWithCountryCode = PhoneNumberUtils.appendCountryCodeIfNeeded(phoneNumber, context, _homeCountryCode.value)
            messagingService.openSmsApp(context, phoneNumberWithCountryCode)
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error opening SMS app for: $phoneNumber", e)
        }
    }

    fun openTelegramChat(context: Context, phoneNumber: String) {
        try {
            if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
                android.util.Log.w("ContactsViewModel", "Invalid phone number for Telegram: $phoneNumber")
                return
            }
            // Append country code if needed
            val phoneNumberWithCountryCode = PhoneNumberUtils.appendCountryCodeIfNeeded(phoneNumber, context, _homeCountryCode.value)
            messagingService.openTelegramChat(context, phoneNumberWithCountryCode)
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error opening Telegram chat for: $phoneNumber", e)
        }
    }

    fun openMessagingApp(context: Context, phoneNumber: String) {
        // Append country code if needed
        val phoneNumberWithCountryCode = PhoneNumberUtils.appendCountryCodeIfNeeded(phoneNumber, context, _homeCountryCode.value)
        messagingService.openMessagingApp(context, phoneNumberWithCountryCode, _defaultMessagingApp.value)
    }

    fun executeAction(context: Context, action: String, phoneNumber: String) {
        when (action) {
            "Call" -> makePhoneCall(context, phoneNumber)
            "WhatsApp" -> openWhatsAppChat(context, phoneNumber)
            "SMS", "Messages" -> openSmsApp(context, phoneNumber)
            "Telegram" -> openTelegramChat(context, phoneNumber)
        }
    }

    fun openContactInContactsApp(context: Context, contact: Contact) {
        try {
            if (!ContactUtils.isValidContact(contact)) {
                android.util.Log.w("ContactsViewModel", "Invalid contact for opening in contacts app: ${contact.id}")
                return
            }
            phoneService.openContactInContactsApp(context, contact)
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error opening contact in contacts app: ${contact.id}", e)
        }
    }

    fun loadRecentCalls(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoadingRecentCalls.value = true
                android.util.Log.d("ContactsViewModel", "Starting loadRecentCalls - cached calls: ${_cachedRecentCalls.value.size}")
                
                // First, show cached recent calls immediately if available
                if (_cachedRecentCalls.value.isNotEmpty()) {
                    _recentCalls.value = _cachedRecentCalls.value
                    filterContacts()
                    android.util.Log.d("ContactsViewModel", "Showed cached recent calls: ${_recentCalls.value.size}")
                }
                
                // Load fresh recent calls from call log
                val recentCalls = contactService.loadRecentCalls(context, _selectedContacts.value)
                
                // Validate recent calls, but allow short service numbers (3-6 digits)
                val validRecentCalls = recentCalls.filter { contact ->
                    ContactUtils.isValidContact(contact) ||
                    (contact.phoneNumber.replace(Regex("[^\\d]"), "").length in 3..6)
                }
                
                if (validRecentCalls.size != recentCalls.size) {
                    android.util.Log.w("ContactsViewModel", "Removing ${recentCalls.size - validRecentCalls.size} invalid recent calls (except short service numbers)")
                }
                
                // Update both recent calls and cached calls
                _recentCalls.value = validRecentCalls
                _cachedRecentCalls.value = validRecentCalls
                
                // Save the first two recent calls to local storage
                saveFirstTwoRecentCalls(validRecentCalls)
                
                filterContacts()
                
                android.util.Log.d("ContactsViewModel", "Successfully loaded ${validRecentCalls.size} recent calls")
            } catch (e: Exception) {
                android.util.Log.e("ContactsViewModel", "Error loading recent calls", e)
                // Keep cached calls if available, otherwise set empty list
                if (_cachedRecentCalls.value.isEmpty()) {
                    _recentCalls.value = emptyList()
                }
            } finally {
                _isLoadingRecentCalls.value = false
            }
        }
    }
    
    fun loadAllRecentCalls(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoadingRecentCalls.value = true
                android.util.Log.d("ContactsViewModel", "Starting loadAllRecentCalls - cached all calls: ${_cachedAllRecentCalls.value.size}")
                
                // Show cached all recent calls immediately if available
                if (_cachedAllRecentCalls.value.isNotEmpty()) {
                    _allRecentCalls.value = _cachedAllRecentCalls.value
                    filterContacts()
                    android.util.Log.d("ContactsViewModel", "Showed cached all recent calls: ${_allRecentCalls.value.size}")
                }
                
                val allRecentCalls = contactService.loadAllRecentCalls(context)
                
                // Validate recent calls, but allow short service numbers (3-6 digits)
                val validRecentCalls = allRecentCalls.filter { contact ->
                    ContactUtils.isValidContact(contact) ||
                    (contact.phoneNumber.replace(Regex("[^\\d]"), "").length in 3..6)
                }
                
                if (validRecentCalls.size != allRecentCalls.size) {
                    android.util.Log.w("ContactsViewModel", "Removing ${allRecentCalls.size - validRecentCalls.size} invalid all recent calls (except short service numbers)")
                }
                
                _allRecentCalls.value = validRecentCalls
                _cachedAllRecentCalls.value = validRecentCalls
            } catch (e: Exception) {
                android.util.Log.e("ContactsViewModel", "Error loading all recent calls", e)
                _allRecentCalls.value = emptyList()
            } finally {
                _isLoadingRecentCalls.value = false
            }
        }
    }
    
    fun refreshRecentCallsOnAppResume(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Only refresh if recent calls are visible
                if (!_isRecentCallsVisible.value) {
                    return@launch
                }
                
                // Show cached data immediately if available
                if (_cachedRecentCalls.value.isNotEmpty()) {
                    _recentCalls.value = _cachedRecentCalls.value
                }
                
                if (_cachedAllRecentCalls.value.isNotEmpty()) {
                    _allRecentCalls.value = _cachedAllRecentCalls.value
                }
                
                // Load fresh data in background
                loadRecentCalls(context)
                if (_allRecentCalls.value.isNotEmpty()) {
                    loadAllRecentCalls(context)
                }
                
                // Also refresh call activity data for quick list contacts
                loadCallActivityForQuickList(context)
                
            } catch (e: Exception) {
                android.util.Log.e("ContactsViewModel", "Error refreshing recent calls on app resume", e)
            }
        }
    }
    
    fun shouldRefreshRecentCalls(): Boolean {
        return _isRecentCallsVisible.value
    }
    
    private fun loadSavedRecentCalls() {
        try {
            val savedRecentCalls = preferencesRepository.loadRecentCalls()
            android.util.Log.d("ContactsViewModel", "Loaded ${savedRecentCalls.size} saved recent calls")
            
            // Set the saved recent calls as initial value for both recent calls and cached calls
            _recentCalls.value = savedRecentCalls
            _cachedRecentCalls.value = savedRecentCalls
            _allRecentCalls.value = savedRecentCalls
            _cachedAllRecentCalls.value = savedRecentCalls
            filterContacts()
            
            android.util.Log.d("ContactsViewModel", "Cached data set - recentCalls: ${_recentCalls.value.size}, allRecentCalls: ${_allRecentCalls.value.size}")
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error loading saved recent calls", e)
            _recentCalls.value = emptyList()
            _cachedRecentCalls.value = emptyList()
            _allRecentCalls.value = emptyList()
            _cachedAllRecentCalls.value = emptyList()
        }
    }
    
    private fun saveFirstTwoRecentCalls(recentCalls: List<Contact>) {
        try {
            val firstTwoCalls = recentCalls.take(2)
            android.util.Log.d("ContactsViewModel", "Saving first ${firstTwoCalls.size} recent calls to local storage")
            preferencesRepository.saveRecentCalls(firstTwoCalls)
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error saving first two recent calls", e)
        }
    }
    
    fun clearSavedRecentCalls() {
        try {
            preferencesRepository.clearRecentCalls()
            _recentCalls.value = emptyList()
            _cachedRecentCalls.value = emptyList()
            _allRecentCalls.value = emptyList()
            _cachedAllRecentCalls.value = emptyList()
            filterContacts()
            android.util.Log.d("ContactsViewModel", "Cleared saved recent calls")
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error clearing saved recent calls", e)
        }
    }
    
    fun refreshAvailableMessagingApps() {
        checkAvailableMessagingApps()
    }
    
    fun formatPhoneNumber(phoneNumber: String): String {
        return phoneService.formatPhoneNumber(phoneNumber)
    }

    fun refreshAllContactsFromPhone(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contacts = contactService.getAllContacts(context)
                val validContacts = contacts.filter { ContactUtils.isValidContact(it) }
                _allContacts.value = validContacts
                android.util.Log.d("ContactsViewModel", "Refreshed all contacts from phone: ${validContacts.size}")
            } catch (e: Exception) {
                android.util.Log.e("ContactsViewModel", "Error refreshing all contacts from phone", e)
                _allContacts.value = emptyList()
            }
        }
    }

    // State consistency validation
    private fun validateStateConsistency() {
        try {
            val selectedContacts = _selectedContacts.value
            val filteredContacts = _filteredSelectedContacts.value
            
            // Check if filtered contacts are a subset of selected contacts
            val selectedIds = selectedContacts.map { it.id }.toSet()
            val filteredIds = filteredContacts.map { it.id }.toSet()
            
            if (!filteredIds.all { it in selectedIds }) {
                android.util.Log.w("ContactsViewModel", "State inconsistency detected: filtered contacts not in selected contacts")
                // Fix the inconsistency
                filterContacts()
            }
            
            // Validate all contacts in the lists
            val invalidSelected = selectedContacts.filter { !ContactUtils.isValidContact(it) }
            if (invalidSelected.isNotEmpty()) {
                android.util.Log.w("ContactsViewModel", "Found ${invalidSelected.size} invalid contacts in selected list")
                // Remove invalid contacts
                val validContacts = selectedContacts.filter { ContactUtils.isValidContact(it) }
                _selectedContacts.value = validContacts
                saveContacts()
                filterContacts()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error validating state consistency", e)
        }
    }

    // Public method to update home country code
    fun setHomeCountryCode(code: String) {
        _homeCountryCode.value = code
        preferencesRepository.saveHomeCountryCode(code)
        // Refresh call activity data with new home country code
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                loadCallActivityForQuickList(context)
            } catch (e: Exception) {
            }
        }
    }

    // Public method to clear home country code
    fun clearHomeCountryCode() {
        _homeCountryCode.value = null
        preferencesRepository.clearHomeCountryCode()
        // Refresh call activity data with cleared home country code
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                loadCallActivityForQuickList(context)
            } catch (e: Exception) {
                android.util.Log.e("ContactsViewModel", "Error refreshing call activity after clearing home country code", e)
            }
        }
    }
    
    /**
     * Load call activity data for quick list contacts
     * This function efficiently loads the latest call activity for each contact in the quick list
     * Loads call activity for ALL quick list contacts to show in long press dialog
     */
    fun loadCallActivityForQuickList(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selectedContacts = _selectedContacts.value
                if (selectedContacts.isEmpty()) {
                    _callActivityMap.value = emptyMap()
                    return@launch
                }
                
                val callActivityMap = mutableMapOf<String, Contact>()
                
                // Load call activity for ALL quick list contacts (not just those with "Call" as primary action)
                selectedContacts.forEach { contact ->
                    val callActivity = contactService.getLatestCallActivityForContact(context, contact.phoneNumbers)
                    if (callActivity != null) {
                        callActivityMap[contact.id] = callActivity
                    }
                }
                
                _callActivityMap.value = callActivityMap
                
            } catch (e: Exception) {
                android.util.Log.e("ContactsViewModel", "Error loading call activity for quick list", e)
                _callActivityMap.value = emptyMap()
            }
        }
    }

    // Cleanup resources
    override fun onCleared() {
        super.onCleared()
        try {
            contactService.cleanup()
        } catch (e: Exception) {
            android.util.Log.e("ContactsViewModel", "Error during cleanup", e)
        }
    }
} 