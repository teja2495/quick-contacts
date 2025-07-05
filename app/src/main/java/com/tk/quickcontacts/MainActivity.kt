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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.focus.FocusRequester
import com.tk.quickcontacts.ui.components.*
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
    
    // Check if keyboard is open
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = imeBottom > 0
    
    // Animate padding change when keyboard opens/closes
    val searchBarBottomPadding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 0.dp else 40.dp,
        animationSpec = tween(durationMillis = 300),
        label = "searchBarBottomPadding"
    )
    
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

    // Check if permissions are permanently denied
    fun arePermissionsPermanentlyDenied(): Boolean {
        val activity = context as? Activity ?: return false
        
        // Only check for permanent denial if we've already requested permissions
        if (!hasRequestedPermissions) return false
        
        // Don't check while we're actively requesting permissions
        if (isRequestingPermissions) return false
        
        // A permission is permanently denied if it's not granted AND we shouldn't show rationale
        // This happens when user denies with "Don't ask again" or denies multiple times
        val callPermissionDenied = !hasCallPermission && 
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CALL_PHONE)
            
        val contactsPermissionDenied = !hasContactsPermission && 
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)
            
        val callLogPermissionDenied = !hasCallLogPermission && 
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CALL_LOG)
        
        return callPermissionDenied || contactsPermissionDenied || callLogPermissionDenied
    }

    // Function to open app settings
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    // Multiple permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCallPermission = permissions[Manifest.permission.CALL_PHONE] ?: false
        hasContactsPermission = permissions[Manifest.permission.READ_CONTACTS] ?: false
        hasCallLogPermission = permissions[Manifest.permission.READ_CALL_LOG] ?: false
        hasRequestedPermissions = true // Mark that we've requested permissions
        isRequestingPermissions = false // We're no longer requesting permissions
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
    var showEditHint by remember { mutableStateOf(false) }
    
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
        }
    }
    
    // Disable edit mode when home screen loses focus
    LaunchedEffect(isSearching, isSettingsScreenOpen) {
        if (isSearching || isSettingsScreenOpen) {
            editMode = false
        }
    }
    
    // Show edit hint popup when edit button first appears for new users
    LaunchedEffect(selectedContacts) {
        if (selectedContacts.isNotEmpty() && !viewModel.hasShownEditHint()) {
            // Add a small delay to ensure the UI is fully rendered
            delay(500)
            showEditHint = true
        }
    }
    
    // Auto-open settings when permissions are permanently denied after being requested
    LaunchedEffect(arePermissionsPermanentlyDenied(), hasRequestedPermissions, isRequestingPermissions) {
        if (arePermissionsPermanentlyDenied() && hasRequestedPermissions && !isRequestingPermissions) {
            // Add a small delay to ensure permission dialog has closed
            delay(500)
            // Double-check the condition after delay
            if (arePermissionsPermanentlyDenied() && !isRequestingPermissions) {
                openAppSettings()
            }
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
                    // Only show settings icon when not in settings screen, not searching, and all permissions are granted
                    if (!isSettingsScreenOpen && !isSearching && hasCallPermission && hasContactsPermission && hasCallLogPermission) {
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
                !hasCallPermission || !hasContactsPermission || !hasCallLogPermission -> {
                    PermissionRequestScreen(
                        hasCallPermission = hasCallPermission,
                        hasContactsPermission = hasContactsPermission,
                        hasCallLogPermission = hasCallLogPermission,
                        arePermissionsPermanentlyDenied = arePermissionsPermanentlyDenied(),
                        onRequestPermissions = {
                            if (arePermissionsPermanentlyDenied()) {
                                openAppSettings()
                            } else {
                                hasRequestedPermissions = true
                                isRequestingPermissions = true
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.CALL_PHONE,
                                        Manifest.permission.READ_CONTACTS,
                                        Manifest.permission.READ_CALL_LOG
                                    )
                                )
                            }
                        }
                    )
                }
                
                isSettingsScreenOpen -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBackClick = { isSettingsScreenOpen = false }
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
                            modifier = Modifier.weight(1f)
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
                                    bottom = searchBarBottomPadding
                                )
                                .imePadding()
                        )
                    }
                }
                
                selectedContacts.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Scrollable content
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Recent calls section (only show if enabled)
                            if (isRecentCallsVisible) {
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
                                    selectedContacts = selectedContacts
                                )
                            }
                            
                            // Empty contacts screen (hide when recent calls are expanded)
                            if (!isRecentCallsExpanded || !isRecentCallsVisible) {
                                EmptyContactsScreen()
                            }
                        }
                        
                        // Fixed search bar at bottom
                        FakeSearchBar(
                            onClick = { 
                                isSearching = true
                                viewModel.updateSearchQuery("")
                            },
                            onDialerClick = {
                                viewModel.openDialer(context)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .imePadding()
                        )
                    }
                }
                
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Scrollable content
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Recent calls section (only show if enabled)
                            if (isRecentCallsVisible) {
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
                                    selectedContacts = selectedContacts
                                )
                            }
                            
                            // Favourite header with edit functionality (hide when recent calls are expanded)
                            if (selectedContacts.isNotEmpty() && (!isRecentCallsExpanded || !isRecentCallsVisible)) {
                                Column {
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
                                                .padding(end = 10.dp)
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
                                }
                            }
                            
                            // Quick contacts list (hide when recent calls are expanded)
                            if (!isRecentCallsExpanded || !isRecentCallsVisible) {
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
                                    }
                                )
                            }
                        }
                        
                        // Fixed search bar at bottom
                        FakeSearchBar(
                            onClick = { 
                                isSearching = true
                                viewModel.updateSearchQuery("")
                            },
                            onDialerClick = {
                                viewModel.openDialer(context)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .imePadding()
                        )
                    }
                }
            }
            
            // Edit hint dialog - must be inside the main Column
            if (showEditHint) {
                AlertDialog(
                    onDismissRequest = { /* Do nothing - prevent dismissal by tapping outside */ },
                    title = {
                        Text(
                            text = stringResource(R.string.title_editing_quick_list),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.edit_instruction),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showEditHint = false
                                viewModel.markEditHintShown()
                            }
                        ) {
                            Text(stringResource(R.string.action_got_it))
                        }
                    }
                )
            }
        }
    }
} 