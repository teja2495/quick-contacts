package com.tk.quickcontacts

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dialpad

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quickcontacts.ui.theme.QuickContactsTheme
import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import android.content.Context


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickContactsTheme {
                QuickContactsApp()
            }
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
    
    // Search all contacts when search query changes
    LaunchedEffect(searchQuery, hasContactsPermission) {
        if (hasContactsPermission && searchQuery.isNotEmpty()) {
            viewModel.searchAllContacts(context, searchQuery)
        }
    }
    
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
                            isSearching -> "Search"
                            isSettingsScreenOpen -> "Settings"
                            else -> "Quick Contacts"
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
                                contentDescription = "Back"
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
                                contentDescription = "Settings",
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
                                            text = "Quick List",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        
                                        Text(
                                            text = if (editMode) "Done" else "Edit",
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
                                            text = "Tap contact to change actions",
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
                            text = "Editing Quick List...",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Tap ‘Edit’ button to delete, reorder contacts in quick list. You can also set customize actions for each contact.",
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
                            Text("Got it!")
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
        placeholder = {
            Text(
                text = "Search contacts...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun FakeSearchBar(
    onClick: () -> Unit,
    onDialerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search area - clickable to open search
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Search contacts...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Dialer icon - clickable to open dialer
            Icon(
                imageVector = Icons.Default.Dialpad,
                contentDescription = "Dialer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp)
                    .clickable { onDialerClick() }
            )
        }
    }
}

@Composable
fun SearchResultsContent(
    viewModel: ContactsViewModel,
    searchQuery: String,
    searchResults: List<Contact>,
    selectedContacts: List<Contact>,
    isInternationalDetectionEnabled: Boolean = true,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = 10.dp),
        reverseLayout = true // This makes the LazyColumn scroll from bottom to top
    ) {
        if (searchQuery.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Reverse the order to account for reverseLayout = true
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Search Contacts",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Type a name to search through all your contacts",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (searchResults.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Reverse the order to account for reverseLayout = true
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "No Results Found",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Try a different search term",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Instruction text first (will appear between search results and search bar due to reverseLayout)
            item {
                Text(
                    text = "Tap + to add to your quick list",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 10.dp, end = 24.dp, bottom = 0.dp)
                )
            }
            
            // Search results (they're already reversed, and with reverseLayout=true, 
            // most relevant will be closest to search bar)
            items(searchResults) { contact ->
                SearchResultItem(
                    contact = contact,
                    onContactClick = { contact ->
                        viewModel.makePhoneCall(context, contact.phoneNumber)
                    },
                    onToggleContact = { contact ->
                        val isSelected = selectedContacts.any { it.id == contact.id }
                        if (isSelected) {
                            viewModel.removeContact(contact)
                        } else {
                            viewModel.addContact(contact)
                        }
                    },
                    onAddContact = { contact ->
                        viewModel.addContact(contact)
                    },
                    onRemoveContact = { contact ->
                        viewModel.removeContact(contact)
                    },
                    onWhatsAppClick = { contact ->
                        viewModel.openMessagingApp(context, contact.phoneNumber)
                    },
                    onContactImageClick = { contact ->
                        viewModel.openContactInContactsApp(context, contact)
                    },
                    selectedContacts = selectedContacts,
                    isInternationalDetectionEnabled = isInternationalDetectionEnabled,
                    defaultMessagingApp = defaultMessagingApp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onToggleContact: (Contact) -> Unit,
    onAddContact: (Contact) -> Unit,
    onRemoveContact: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit,
    onContactImageClick: (Contact) -> Unit,
    selectedContacts: List<Contact>,
    isInternationalDetectionEnabled: Boolean = true,
    defaultMessagingApp: MessagingApp = MessagingApp.WHATSAPP,
    modifier: Modifier = Modifier
) {
    val isSelected = selectedContacts.any { it.id == contact.id }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val isInternational = isInternationalNumber(context, contact.getPrimaryPhoneNumber(), isInternationalDetectionEnabled)
    
    // Phone number selection dialog
    if (showPhoneNumberDialog) {
        PhoneNumberSelectionDialog(
            contact = contact,
            onPhoneNumberSelected = { selectedNumber: String ->
                val contactWithSelectedNumber = contact.copy(
                    phoneNumber = selectedNumber,
                    phoneNumbers = listOf(selectedNumber)
                )
                when (dialogAction) {
                    "call" -> onContactClick(contactWithSelectedNumber)
                    "whatsapp" -> onWhatsAppClick(contactWithSelectedNumber)
                    "add" -> onAddContact(contactWithSelectedNumber)
                }
                showPhoneNumberDialog = false
                dialogAction = null
            },
            onDismiss = {
                showPhoneNumberDialog = false
                dialogAction = null
            },
            selectedContacts = selectedContacts,
            onAddContact = if (dialogAction == "add") { contactWithSelectedNumber ->
                // Create a new contact with the selected number
                val newContact = contact.copy(
                    phoneNumber = contactWithSelectedNumber.phoneNumber,
                    phoneNumbers = listOf(contactWithSelectedNumber.phoneNumber)
                )
                onAddContact(newContact)
            } else null,
            onRemoveContact = if (dialogAction == "add") { contactToRemove ->
                onRemoveContact(contactToRemove)
            } else null
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .combinedClickable(
                onClick = { 
                    if (contact.phoneNumbers.size > 1) {
                        dialogAction = if (isInternational) "whatsapp" else "call"
                        showPhoneNumberDialog = true
                    } else {
                        if (isInternational) {
                            onWhatsAppClick(contact)
                        } else {
                            onContactClick(contact)
                        }
                    }
                },
                onLongClick = {
                    onContactImageClick(contact)
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Toggle quick contacts button
            if (contact.phoneNumbers.size > 1) {
                // For multiple numbers, always open dialog, do not show tick/+
                IconButton(
                    onClick = {
                        dialogAction = "add"
                        showPhoneNumberDialog = true
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add ${contact.name} to quick contacts",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        if (isSelected) {
                            onRemoveContact(contact)
                        } else {
                            onAddContact(contact)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Done else Icons.Default.Add,
                        contentDescription = if (isSelected)
                            "Remove ${contact.name} from quick contacts"
                        else
                            "Add ${contact.name} to quick contacts",
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Contact Info - for international: open WhatsApp, for domestic: call
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                if (contact.phoneNumbers.size > 1) {
                    Text(
                        text = "${contact.phoneNumbers.size} numbers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // For international: Phone button (to call), for domestic: messaging app button
            IconButton(
                onClick = { 
                    if (contact.phoneNumbers.size > 1) {
                        dialogAction = if (isInternational) "call" else "whatsapp"
                        showPhoneNumberDialog = true
                    } else {
                        if (isInternational) {
                            onContactClick(contact)
                        } else {
                            onWhatsAppClick(contact)
                        }
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                if (isInternational) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call ${contact.name}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    when (defaultMessagingApp) {
                        MessagingApp.WHATSAPP -> {
                            Icon(
                                painter = painterResource(id = R.drawable.whatsapp_icon),
                                contentDescription = "Send WhatsApp message to ${contact.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        MessagingApp.SMS -> {
                            Icon(
                                painter = painterResource(id = R.drawable.sms_icon),
                                contentDescription = "Send SMS to ${contact.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        MessagingApp.TELEGRAM -> {
                            Icon(
                                painter = painterResource(id = R.drawable.telegram_icon),
                                contentDescription = "Send Telegram message to ${contact.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseContactFromUri(context: android.content.Context, contactUri: Uri): Contact? {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )

    var cursor: Cursor? = null
    try {
        cursor = context.contentResolver.query(
            contactUri,
            projection,
            null,
            null,
            null
        )

        cursor?.let {
            if (it.moveToFirst()) {
                val idColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                if (idColumn >= 0 && nameColumn >= 0 && numberColumn >= 0) {
                    val id = it.getString(idColumn)
                    val name = it.getString(nameColumn)
                    val number = it.getString(numberColumn)

                    if (name != null && number != null) {
                        // Get the contact photo URI using the contact ID
                        val photoUri = getContactPhotoUri(context, id)
                        
                        return Contact(
                            id = id,
                            name = name,
                            phoneNumber = number,
                            photoUri = photoUri
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        cursor?.close()
    }

    return null
}

private fun getContactPhotoUri(context: android.content.Context, contactId: String): String? {
    try {
        val photoUri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_URI,
            contactId
        ).let { contactUri ->
            Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
        }
        
        // Check if photo exists by trying to open an input stream
        context.contentResolver.openInputStream(photoUri)?.use {
            return photoUri.toString()
        }
    } catch (e: Exception) {
        // Photo doesn't exist or can't be accessed
    }
    
    return null
}

@Composable
fun PermissionRequestScreen(
    hasCallPermission: Boolean,
    hasContactsPermission: Boolean,
    hasCallLogPermission: Boolean,
    arePermissionsPermanentlyDenied: Boolean,
    onRequestPermissions: () -> Unit
) {
    val spacing = 16.dp
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
        contentPadding = PaddingValues(top = 8.dp, bottom = spacing)
    ) {
        item {
            // Header icon
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Title
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(spacing))
            // Privacy assurance card with distinct design
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Your Privacy is Protected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This app has no internet access. All your data stays on your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing))
            // Permission cards list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionCard(
                    icon = Icons.Default.Phone,
                    title = "Phone Access",
                    description = "Make calls directly from the app when you tap on a contact",
                    isGranted = hasCallPermission
                )
                PermissionCard(
                    icon = Icons.Default.Person,
                    title = "Contacts Access",
                    description = "Search and display your contacts with their names and photos",
                    isGranted = hasContactsPermission
                )
                PermissionCard(
                    icon = Icons.Default.History,
                    title = "Call History Access",
                    description = "Show your recent calls for quick redial and easy access",
                    isGranted = hasCallLogPermission
                )
            }
            Spacer(modifier = Modifier.height(spacing))
            Spacer(modifier = Modifier.height(12.dp))
            // Grant permissions button
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Grant Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = if (isGranted) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Show checkmark when permission is granted
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Permission granted",
                    tint = androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green tint
                    modifier = Modifier
                        .size(20.dp)
                        .padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyContactsScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Empty Quick List",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Search contacts below and add them to your quick list for quick access.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsScreen(
    viewModel: ContactsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInternationalDetectionEnabled by viewModel.isInternationalDetectionEnabled.collectAsState()
    val isRecentCallsVisible by viewModel.isRecentCallsVisible.collectAsState()
    val useWhatsAppAsDefault by viewModel.useWhatsAppAsDefault.collectAsState()
    val defaultMessagingApp by viewModel.defaultMessagingApp.collectAsState()
    val availableMessagingApps by viewModel.availableMessagingApps.collectAsState()
    
    // Refresh available messaging apps when settings screen is opened
    LaunchedEffect(Unit) {
        viewModel.refreshAvailableMessagingApps()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Settings content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                // Messaging App Setting (no Card)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Default Messaging App",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Explanation text for default messaging app setting
                    Text(
                        text = "You can change this for individual contacts in quick list by tapping on the edit button",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // SMS Option (always available)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                viewModel.setMessagingApp(MessagingApp.SMS)
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultMessagingApp == MessagingApp.SMS,
                            onClick = { 
                                viewModel.setMessagingApp(MessagingApp.SMS)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.sms_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SMS",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // WhatsApp Option
                    val isWhatsAppAvailable = availableMessagingApps.contains(MessagingApp.WHATSAPP)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isWhatsAppAvailable) { 
                                if (isWhatsAppAvailable) {
                                    viewModel.setMessagingApp(MessagingApp.WHATSAPP)
                                }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultMessagingApp == MessagingApp.WHATSAPP,
                            onClick = { 
                                if (isWhatsAppAvailable) {
                                    viewModel.setMessagingApp(MessagingApp.WHATSAPP)
                                }
                            },
                            enabled = isWhatsAppAvailable,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                disabledSelectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                disabledUnselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.whatsapp_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WhatsApp",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isWhatsAppAvailable) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            if (!isWhatsAppAvailable) {
                                Text(
                                    text = "Not installed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    // Telegram Option
                    val isTelegramAvailable = availableMessagingApps.contains(MessagingApp.TELEGRAM)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isTelegramAvailable) { 
                                if (isTelegramAvailable) {
                                    viewModel.setMessagingApp(MessagingApp.TELEGRAM)
                                }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultMessagingApp == MessagingApp.TELEGRAM,
                            onClick = { 
                                if (isTelegramAvailable) {
                                    viewModel.setMessagingApp(MessagingApp.TELEGRAM)
                                }
                            },
                            enabled = isTelegramAvailable,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                disabledSelectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                disabledUnselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.telegram_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Telegram",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isTelegramAvailable) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            if (!isTelegramAvailable) {
                                Text(
                                    text = "Not installed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                // Recent Calls Visibility Setting (no Card)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Show Recent Calls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Display recent calls at the top of the main screen for quick access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isRecentCallsVisible,
                        onCheckedChange = { viewModel.toggleRecentCallsVisibility() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            item {
                // International Number Detection Setting (no Card)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "International Number Detection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                defaultMessagingApp == MessagingApp.SMS ->
                                    "Disabled when SMS is the default messaging app"
                                else -> 
                                    "Detect international numbers with country code and open WhatsApp or Telegram when the contact card is tapped." 
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (defaultMessagingApp == MessagingApp.SMS) 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isInternationalDetectionEnabled,
                        onCheckedChange = { viewModel.toggleInternationalDetection() },
                        enabled = defaultMessagingApp != MessagingApp.SMS,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            item {
                // Feedback button just below international number detection
                val context = LocalContext.current
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:quickcontacts.feedback@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Quick Contacts Feedback")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Send Feedback",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        // Version number at the bottom
        val context = LocalContext.current
        val versionName = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Version $versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}