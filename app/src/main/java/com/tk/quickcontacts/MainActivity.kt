package com.tk.quickcontacts

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quickcontacts.ui.theme.QuickContactsTheme
import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.models.CustomActions

import androidx.compose.ui.focus.FocusRequester
import com.tk.quickcontacts.ui.components.*
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Switch to main theme after splash screen
        setTheme(R.style.Theme_QuickContacts)
        
        // Configure Coil for better performance
        configureImageLoader()
        
        enableEdgeToEdge()
        setContent {
            QuickContactsTheme {
                QuickContactsApp()
            }
        }
    }
    
    private fun configureImageLoader() {
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Use 2% of available disk space
                    .build()
            }
            .respectCacheHeaders(false) // Always use cached images when available
            .build()
        
        // Set as default image loader
        coil.ImageLoader.Builder(this).build().let { defaultLoader ->
            // Replace the default loader with our optimized one
            coil.Coil.setImageLoader(imageLoader)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickContactsApp() {
    val context = LocalContext.current
    val viewModel: ContactsViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )
    
    // Navigation state - replace isSearchScreenOpen with isSearching
    var currentScreen by remember { mutableStateOf("home") }
    var isSearching by remember { mutableStateOf(false) }
    var isSettingsScreenOpen by remember { mutableStateOf(false) }
    
    // Removed animated padding logic for consistent spacing across all devices
    // Using only imePadding modifier for proper keyboard handling
    
    // Permission states
    var hasCallPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var hasCallLogPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Track if we've requested permissions before
    var hasRequestedPermissions by remember { mutableStateOf(false) }
    
    // Track if we're currently requesting permissions
    var isRequestingPermissions by remember { mutableStateOf(false) }
    
    // Track if user has denied permissions after first request
    var hasDeniedPermissionsAfterFirstRequest by remember { mutableStateOf(false) }

    // Check if permissions are permanently denied
    fun arePermissionsPermanentlyDenied(): Boolean {
        val activity = context as? Activity ?: return false
        
        // Only check for permanent denial if we've already requested permissions
        if (!hasRequestedPermissions) return false
        
        // Don't check while we're actively requesting permissions
        if (isRequestingPermissions) return false
        
        // A permission is permanently denied if it's not granted AND we shouldn't show rationale
        // This happens when user denies with "Don't ask again" or denies multiple times
        // Note: Call History permission is optional, so we don't check it for permanent denial
        val callPermissionDenied = !hasCallPermission && 
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CALL_PHONE)
            
        val contactsPermissionDenied = !hasContactsPermission && 
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)
        
        return callPermissionDenied || contactsPermissionDenied
    }

    // Check if call log permission is permanently denied
    fun isCallLogPermissionPermanentlyDenied(): Boolean {
        val activity = context as? Activity ?: return false
        
        // A permission is permanently denied if it's not granted AND we shouldn't show rationale
        return !hasCallLogPermission && 
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CALL_LOG)
    }

    // Function to open app settings
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
        // Refresh permissions after a short delay to catch changes when user returns
        // This will be handled by the LaunchedEffect that listens to onResume
    }

    // Call log permission launcher (for settings screen and sequential requests)
    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallLogPermission = isGranted
        // Mark that we've completed the permission request flow
        hasRequestedPermissions = true
        isRequestingPermissions = false
        
        // If permission is granted, automatically enable recent calls
        if (isGranted) {
            viewModel.enableRecentCallsVisibility()
        }
    }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallPermission = isGranted
        if (isGranted) {
            // If phone permission granted, request call log permission next
            callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        } else {
            // If phone permission denied, mark as requested and check for permanent denial
            hasRequestedPermissions = true
            isRequestingPermissions = false
            if (hasRequestedPermissions && !hasCallPermission) {
                hasDeniedPermissionsAfterFirstRequest = true
            }
        }
    }

    // Individual permission launchers for sequential permission requests
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
        if (isGranted) {
            // If contacts permission granted, request phone permission next
            phonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        } else {
            // If contacts permission denied, mark as requested and check for permanent denial
            hasRequestedPermissions = true
            isRequestingPermissions = false
            if (hasRequestedPermissions && !hasContactsPermission) {
                hasDeniedPermissionsAfterFirstRequest = true
            }
        }
    }

    // Contact states
    val selectedContacts by viewModel.selectedContacts.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()
    val filteredSelectedContacts by viewModel.filteredSelectedContacts.collectAsState()
    val filteredRecentCalls by viewModel.filteredRecentCalls.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val customActionPreferences by viewModel.customActionPreferences.collectAsState()
    val isInternationalDetectionEnabled by viewModel.isInternationalDetectionEnabled.collectAsState()
    val isRecentCallsVisible by viewModel.isRecentCallsVisible.collectAsState()
    val defaultMessagingApp by viewModel.defaultMessagingApp.collectAsState()
    val availableMessagingApps by viewModel.availableMessagingApps.collectAsState()
    
    // Disable international detection when SMS is selected as default messaging app
    val effectiveInternationalDetectionEnabled = isInternationalDetectionEnabled && defaultMessagingApp != MessagingApp.SMS
    var editMode by remember { mutableStateOf(false) }
    var isRecentCallsExpanded by remember { mutableStateOf(false) }
    var showEditBanner by remember { mutableStateOf(false) }
    
    // Focus requester for search
    val focusRequester = remember { FocusRequester() }
    
    // Handle Android system back button when searching or in settings
    BackHandler(enabled = isSearching || isSettingsScreenOpen) {
        when {
            isSearching -> {
                viewModel.updateSearchQuery("")
                isSearching = false
            }
            isSettingsScreenOpen -> {
                isSettingsScreenOpen = false
            }
        }
    }
    
    // Load recent calls when permissions are available or selected contacts change
    LaunchedEffect(hasCallLogPermission, hasContactsPermission, selectedContacts) {
        if (hasCallLogPermission && hasContactsPermission) {
            viewModel.loadRecentCalls(context)
        }
    }
    
    // Check and load favorite contacts on first launch when permissions are granted
    LaunchedEffect(hasContactsPermission) {
        if (hasContactsPermission) {
            viewModel.checkAndLoadFavoriteContactsOnFirstLaunch(context)
        }
    }
    
    // Search all contacts when search query changes - now handled by debounced search in ViewModel
    // Removed this LaunchedEffect as it's now handled more efficiently in the ViewModel
    
    // Request focus when entering search mode
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
            viewModel.refreshAllContactsFromPhone(context)
        }
    }
    
    // Disable edit mode when home screen loses focus
    LaunchedEffect(isSearching, isSettingsScreenOpen) {
        if (isSearching || isSettingsScreenOpen) {
            editMode = false
        }
    }
    
    // Show edit banner when user lands on quick list screen for the first time
    LaunchedEffect(isSearching, isSettingsScreenOpen, isRecentCallsExpanded, selectedContacts) {
        val onQuickListScreen = !isSearching && !isSettingsScreenOpen && !isRecentCallsExpanded && selectedContacts.isNotEmpty()
        if (onQuickListScreen && !viewModel.hasShownEditHint()) {
            showEditBanner = true
        }
    }
    

    
    // Note: Removed auto-opening settings when permissions are permanently denied
    // Now settings are only opened when user explicitly taps "Grant Permissions" after denying
    
    // Function to refresh permission states
    fun refreshPermissionStates() {
        hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        
        hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        
        hasCallLogPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // Refresh permissions when app becomes active
    LaunchedEffect(Unit) {
        // Initial check
        refreshPermissionStates()
    }
    
    // Refresh permissions when returning from settings
    LaunchedEffect(Unit) {
        // Check permissions every 1 second to catch changes from settings
        while (true) {
            delay(1000)
            refreshPermissionStates()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isSearching -> stringResource(R.string.title_search)
                            isSettingsScreenOpen -> stringResource(R.string.title_settings)
                            else -> stringResource(R.string.title_quick_contacts)
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp) // Align with Recent Calls text
                    )
                },
                navigationIcon = {
                    if (isSearching || isSettingsScreenOpen) {
                        IconButton(
                            onClick = {
                                when {
                                    isSearching -> {
                                        viewModel.updateSearchQuery("")
                                        isSearching = false
                                    }
                                    isSettingsScreenOpen -> {
                                        isSettingsScreenOpen = false
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    }
                },
                actions = {
                    // Only show settings icon when not in settings screen, not searching, and required permissions are granted
                    if (!isSettingsScreenOpen && !isSearching && hasCallPermission && hasContactsPermission) {
                        IconButton(
                            onClick = {
                                isSettingsScreenOpen = true
                            },
                            modifier = Modifier.padding(end = 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.cd_settings),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                !hasCallPermission || !hasContactsPermission -> {
                    PermissionRequestScreen(
                        hasCallPermission = hasCallPermission,
                        hasContactsPermission = hasContactsPermission,
                        hasCallLogPermission = hasCallLogPermission,
                        arePermissionsPermanentlyDenied = arePermissionsPermanentlyDenied(),
                        onRequestPermissions = {
                            if (arePermissionsPermanentlyDenied() && hasDeniedPermissionsAfterFirstRequest) {
                                // Only open settings if user has denied permissions after first request
                                openAppSettings()
                            } else {
                                hasRequestedPermissions = true
                                isRequestingPermissions = true
                                // Start with contacts permission first
                                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    )
                }
                
                isSettingsScreenOpen -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBackClick = { isSettingsScreenOpen = false },
                        hasCallLogPermission = hasCallLogPermission,
                        onRequestCallLogPermission = {
                            callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                        },
                        isCallLogPermissionPermanentlyDenied = isCallLogPermissionPermanentlyDenied(),
                        onOpenAppSettings = {
                            openAppSettings()
                        }
                    )
                }
                
                isSearching -> {
                    // Show search results when searching
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Search results with top result at bottom (most relevant closer to search bar)
                        SearchResultsContent(
                            viewModel = viewModel,
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            selectedContacts = selectedContacts,
                            isInternationalDetectionEnabled = effectiveInternationalDetectionEnabled,
                            defaultMessagingApp = defaultMessagingApp,
                            modifier = Modifier.weight(1f),
                            availableMessagingApps = availableMessagingApps,
                            onExecuteAction = { context, action, phoneNumber ->
                                viewModel.executeAction(context, action, phoneNumber)
                            }
                        )
                        
                        // Keep the search bar at the bottom
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = viewModel::updateSearchQuery,
                            focusRequester = focusRequester,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp, 
                                    top = 8.dp, 
                                    end = 16.dp, 
                                    bottom = 6.dp
                                )
                                .imePadding()
                        )
                    }
                }
                
                else -> {
                    // Home screen - show either empty state or contacts with edit functionality
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Scrollable content
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Recent calls section (only show if enabled and permission granted)
                            if (isRecentCallsVisible && hasCallLogPermission) {
                                RecentCallsSection(
                                    recentCalls = filteredRecentCalls,
                                    onContactClick = { contact ->
                                        viewModel.makePhoneCall(context, contact.phoneNumber)
                                    },
                                    onWhatsAppClick = { contact ->
                                        viewModel.openMessagingApp(context, contact.phoneNumber)
                                    },
                                    onContactImageClick = { contact ->
                                        viewModel.openContactInContactsApp(context, contact)
                                    },
                                    onExpandedChange = { expanded ->
                                        isRecentCallsExpanded = expanded
                                        // Reset edit mode when expanding recent calls
                                        if (expanded) {
                                            editMode = false
                                        }
                                    },
                                    isInternationalDetectionEnabled = effectiveInternationalDetectionEnabled,
                                    defaultMessagingApp = defaultMessagingApp,
                                    selectedContacts = selectedContacts,
                                    availableMessagingApps = availableMessagingApps,
                                    onExecuteAction = { context, action, phoneNumber ->
                                        viewModel.executeAction(context, action, phoneNumber)
                                    }
                                )
                            }
                            
                            // Show empty state when no contacts selected
                            if (selectedContacts.isEmpty()) {
                                if (!isRecentCallsExpanded || !isRecentCallsVisible) {
                                    EmptyContactsScreen()
                                }
                            } else {
                                // Show contacts with edit functionality when contacts are selected
                                if (!isRecentCallsExpanded || !isRecentCallsVisible) {
                                    Column {
                                        // Favourite header with edit functionality
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.title_quick_list),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            
                                            Text(
                                                text = if (editMode) stringResource(R.string.action_done) else stringResource(R.string.action_edit),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .clickable { editMode = !editMode }
                                                    .padding(end = 12.dp)
                                            )
                                        }
                                        
                                        // Show hint text in edit mode
                                        if (editMode) {
                                            Text(
                                                text = stringResource(R.string.edit_hint),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                                            )
                                        }
                                        
                                        // Show edit banner for first-time users
                                        if (showEditBanner) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.edit_instruction),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    
                                                    IconButton(
                                                        onClick = {
                                                            showEditBanner = false
                                                            viewModel.markEditHintShown()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = stringResource(R.string.cd_close),
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Quick contacts list
                                    ContactList(
                                        contacts = filteredSelectedContacts,
                                        onContactClick = { contact ->
                                            // Check if contact has custom actions
                                            val customActions = customActionPreferences[contact.id]
                                            if (customActions != null) {
                                                viewModel.executeAction(context, customActions.primaryAction, contact.phoneNumber)
                                            } else {
                                                viewModel.makePhoneCall(context, contact.phoneNumber)
                                            }
                                        },
                                        editMode = editMode,
                                        onDeleteContact = { contact ->
                                            viewModel.removeContact(contact)
                                        },
                                        onMove = { from, to ->
                                            viewModel.moveContact(from, to)
                                        },
                                        onWhatsAppClick = { contact ->
                                            // Check if contact has custom actions
                                            val customActions = customActionPreferences[contact.id]
                                            if (customActions != null) {
                                                viewModel.executeAction(context, customActions.secondaryAction, contact.phoneNumber)
                                            } else {
                                                viewModel.openMessagingApp(context, contact.phoneNumber)
                                            }
                                        },
                                        onContactImageClick = { contact ->
                                            viewModel.openContactInContactsApp(context, contact)
                                        },
                                        onLongClick = { contact ->
                                            // Long click handled within ContactItem in edit mode
                                        },
                                        onSetCustomActions = { contact, primaryAction, secondaryAction ->
                                            viewModel.setCustomActions(contact.id, primaryAction, secondaryAction)
                                        },
                                        customActionPreferences = customActionPreferences,
                                        isInternationalDetectionEnabled = effectiveInternationalDetectionEnabled,
                                        defaultMessagingApp = defaultMessagingApp,
                                        availableMessagingApps = availableMessagingApps,
                                        selectedContacts = selectedContacts,
                                        onExecuteAction = { context, action, phoneNumber ->
                                            viewModel.executeAction(context, action, phoneNumber)
                                        },
                                        onUpdateContactNumber = { contact, selectedNumber ->
                                            viewModel.updateContactNumber(contact, selectedNumber)
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Fixed search bar at bottom
                        FakeSearchBar(
                            onClick = { 
                                isSearching = true
                                viewModel.updateSearchQuery("")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .imePadding()
                        )
                    }
                }
            }
        }
    }
} 