package com.tk.quickcontacts.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.tk.quickcontacts.Contact
import com.tk.quickcontacts.ContactsViewModel
import com.tk.quickcontacts.R
import com.tk.quickcontacts.models.MessagingApp
import com.tk.quickcontacts.ui.components.*
import kotlinx.coroutines.delay

private sealed class NavDestination(val depth: Int) {
    data object Permission : NavDestination(0)
    data object Home : NavDestination(1)
    data object Search : NavDestination(2)
    data object Settings : NavDestination(2)
    data object ActionEditor : NavDestination(2)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: ContactsViewModel) {
    val context = LocalContext.current

    var isSearching by remember { mutableStateOf(false) }
    var isSettingsScreenOpen by remember { mutableStateOf(false) }
    var actionEditorContact by remember { mutableStateOf<Contact?>(null) }

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

    var hasRequestedPermissions by remember { mutableStateOf(false) }
    var isRequestingPermissions by remember { mutableStateOf(false) }
    var hasDeniedPermissionsAfterFirstRequest by remember { mutableStateOf(false) }
    var hasRequestedPhonePermissionFromSettings by remember { mutableStateOf(false) }
    var hasRequestedCallLogPermissionFromSettings by remember { mutableStateOf(false) }
    var callLogPermissionJustDenied by remember { mutableStateOf(false) }
    var phonePermissionJustDenied by remember { mutableStateOf(false) }

    fun arePermissionsPermanentlyDenied(): Boolean {
        val activity = context as? Activity ?: return false
        if (!hasRequestedPermissions) return false
        if (isRequestingPermissions) return false
        val contactsPermissionDenied = !hasContactsPermission &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)
        return contactsPermissionDenied
    }

    fun isCallLogPermissionPermanentlyDenied(): Boolean {
        val activity = context as? Activity ?: return false
        if (callLogPermissionJustDenied && !hasCallLogPermission) return true
        if (!hasRequestedCallLogPermissionFromSettings) return false
        return !hasCallLogPermission &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CALL_LOG)
    }

    fun isCallPermissionPermanentlyDenied(): Boolean {
        val activity = context as? Activity ?: return false
        if (phonePermissionJustDenied && !hasCallPermission) return true
        if (!hasRequestedPhonePermissionFromSettings) return false
        return !hasCallPermission &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CALL_PHONE)
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallLogPermission = isGranted
        hasRequestedPermissions = true
        isRequestingPermissions = false
        if (!hasContactsPermission) {
            hasDeniedPermissionsAfterFirstRequest = true
        }
        if (isGranted) {
            viewModel.enableRecentCallsVisibility()
        }
    }

    val callLogPermissionFromSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallLogPermission = isGranted
        hasRequestedCallLogPermissionFromSettings = true
        val isRecentCallsVisible = viewModel.isRecentCallsVisible.value
        if (isGranted && !isRecentCallsVisible) {
            viewModel.toggleRecentCallsVisibility()
            callLogPermissionJustDenied = false
        } else if (!isGranted) {
            callLogPermissionJustDenied = true
        }
    }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallPermission = isGranted
        callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
    }

    val phonePermissionFromSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallPermission = isGranted
        hasRequestedPhonePermissionFromSettings = true
        val isDirectDialEnabled = viewModel.isDirectDialEnabled.value
        if (isGranted && !isDirectDialEnabled) {
            viewModel.toggleDirectDial()
            phonePermissionJustDenied = false
        } else if (!isGranted) {
            phonePermissionJustDenied = true
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
        phonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
    }

    val selectedContacts by viewModel.selectedContacts.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()
    val filteredSelectedContacts by viewModel.filteredSelectedContacts.collectAsState()
    val allRecentCalls by viewModel.allRecentCalls.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val customActionPreferences by viewModel.customActionPreferences.collectAsState()
    val isRecentCallsVisible by viewModel.isRecentCallsVisible.collectAsState()
    val defaultMessagingApp by viewModel.defaultMessagingApp.collectAsState()
    val availableMessagingApps by viewModel.availableMessagingApps.collectAsState()
    val hasSeenCallWarning by viewModel.hasSeenCallWarning.collectAsState()
    val callActivityMap by viewModel.callActivityMap.collectAsState()

    fun resolvedActionsFor(contact: Contact): ResolvedQuickContactActions {
        return resolveQuickContactActions(
            customActionPreferences[contact.id],
            defaultMessagingApp
        )
    }

    var editMode by remember { mutableStateOf(false) }
    var isRecentCallsExpanded by remember { mutableStateOf(false) }
    var showEditBanner by remember { mutableStateOf(false) }
    var showRecentCallsHint by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    val currentDestination = when {
        !hasContactsPermission || isRequestingPermissions -> NavDestination.Permission
        actionEditorContact != null -> NavDestination.ActionEditor
        isSettingsScreenOpen -> NavDestination.Settings
        isSearching -> NavDestination.Search
        else -> NavDestination.Home
    }

    BackHandler(enabled = isSearching || isSettingsScreenOpen || actionEditorContact != null) {
        when {
            actionEditorContact != null -> actionEditorContact = null
            isSearching -> {
                viewModel.updateSearchQuery("")
                isSearching = false
            }
            isSettingsScreenOpen -> isSettingsScreenOpen = false
        }
    }

    BackHandler(enabled = isRecentCallsExpanded) {
        isRecentCallsExpanded = false
    }

    BackHandler(enabled = editMode && !isSearching && !isSettingsScreenOpen && actionEditorContact == null) {
        editMode = false
    }

    LaunchedEffect(hasCallLogPermission, hasContactsPermission, selectedContacts, isRecentCallsVisible) {
        if (isRecentCallsVisible && hasCallLogPermission) {
            viewModel.loadRecentCalls(context)
            viewModel.loadAllRecentCalls(context)
        }
    }

    LaunchedEffect(selectedContacts, hasCallLogPermission) {
        if (selectedContacts.isNotEmpty() && hasCallLogPermission) {
            viewModel.loadCallActivityForQuickList(context)
        }
    }

    LaunchedEffect(hasContactsPermission) {
        if (hasContactsPermission) {
            viewModel.checkAndLoadFavoriteContactsOnFirstLaunch(context)
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
            viewModel.refreshAllContactsFromPhone(context)
        }
    }

    LaunchedEffect(isSearching, isSettingsScreenOpen) {
        if (isSearching || isSettingsScreenOpen) {
            editMode = false
            actionEditorContact = null
        }
    }

    LaunchedEffect(isSearching, isSettingsScreenOpen, isRecentCallsExpanded, selectedContacts) {
        val onQuickListScreen = !isSearching && !isSettingsScreenOpen && !isRecentCallsExpanded && selectedContacts.isNotEmpty()
        if (onQuickListScreen && !viewModel.hasShownEditHint()) {
            showEditBanner = true
        }
    }

    LaunchedEffect(isRecentCallsExpanded) {
        if (isRecentCallsExpanded && !viewModel.hasShownRecentCallsHint()) {
            showRecentCallsHint = true
        }
    }

    fun refreshPermissionStates() {
        val newCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val newContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val newCallLogPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        if (newCallLogPermission && !hasCallLogPermission) {
            callLogPermissionJustDenied = false
        }
        if (newCallPermission && !hasCallPermission) {
            phonePermissionJustDenied = false
        }

        hasCallPermission = newCallPermission
        hasContactsPermission = newContactsPermission
        hasCallLogPermission = newCallLogPermission
    }

    LaunchedEffect(Unit) {
        refreshPermissionStates()
    }

    LaunchedEffect(Unit) {
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
                            actionEditorContact != null -> "Change Actions for ${actionEditorContact?.name.orEmpty()}"
                            isSearching -> stringResource(R.string.title_search)
                            isSettingsScreenOpen -> stringResource(R.string.title_settings)
                            else -> stringResource(R.string.title_quick_contacts)
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                navigationIcon = {
                    if (isSearching || isSettingsScreenOpen || actionEditorContact != null) {
                        IconButton(
                            onClick = {
                                when {
                                    actionEditorContact != null -> actionEditorContact = null
                                    isSearching -> {
                                        viewModel.updateSearchQuery("")
                                        isSearching = false
                                    }
                                    isSettingsScreenOpen -> isSettingsScreenOpen = false
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
                    if (!isSettingsScreenOpen && !isSearching && actionEditorContact == null && hasContactsPermission) {
                        IconButton(
                            onClick = { isSettingsScreenOpen = true },
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
            AnimatedContent(
                targetState = currentDestination,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val goingBack = targetState.depth < initialState.depth
                    val slideIn = slideInHorizontally(
                        initialOffsetX = { if (goingBack) -it else it },
                        animationSpec = tween(300)
                    )
                    val slideOut = slideOutHorizontally(
                        targetOffsetX = { if (goingBack) it else -it },
                        animationSpec = tween(300)
                    )
                    slideIn togetherWith slideOut
                },
                label = "nav"
            ) { destination ->
                when (destination) {
                    NavDestination.Permission -> PermissionRequestScreen(
                        hasCallPermission = hasCallPermission,
                        hasContactsPermission = hasContactsPermission,
                        hasCallLogPermission = hasCallLogPermission,
                        arePermissionsPermanentlyDenied = arePermissionsPermanentlyDenied(),
                        onRequestPermissions = {
                            if (arePermissionsPermanentlyDenied() && hasDeniedPermissionsAfterFirstRequest) {
                                openAppSettings()
                            } else {
                                hasRequestedPermissions = true
                                isRequestingPermissions = true
                                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    )

                    NavDestination.ActionEditor -> {
                        actionEditorContact?.let { selectedContact ->
                            QuickContactActionEditorScreen(
                                contact = selectedContact,
                                customActions = customActionPreferences[selectedContact.id],
                                defaultMessagingApp = defaultMessagingApp,
                                availableMessagingApps = availableMessagingApps,
                                selectedContacts = selectedContacts,
                                hasSeenCallWarning = hasSeenCallWarning,
                                onExecuteAction = { executionContext, action, phoneNumber ->
                                    viewModel.executeAction(executionContext, action, phoneNumber)
                                },
                                onUpdateContactNumber = { contactToUpdate, selectedNumber ->
                                    viewModel.updateContactNumber(contactToUpdate, selectedNumber)
                                },
                                onActionUpdated = { updatedActions ->
                                    viewModel.setCustomActions(selectedContact.id, updatedActions)
                                }
                            )
                        }
                    }

                    NavDestination.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        onBackClick = { isSettingsScreenOpen = false },
                        hasCallLogPermission = hasCallLogPermission,
                        onRequestCallLogPermission = {
                            callLogPermissionFromSettingsLauncher.launch(Manifest.permission.READ_CALL_LOG)
                        },
                        isCallLogPermissionPermanentlyDenied = isCallLogPermissionPermanentlyDenied(),
                        hasCallPermission = hasCallPermission,
                        onRequestCallPermission = {
                            phonePermissionFromSettingsLauncher.launch(Manifest.permission.CALL_PHONE)
                        },
                        isCallPermissionPermanentlyDenied = isCallPermissionPermanentlyDenied(),
                        onOpenAppSettings = { openAppSettings() }
                    )

                    NavDestination.Search -> Column(modifier = Modifier.fillMaxSize()) {
                        SearchResultsContent(
                            viewModel = viewModel,
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            selectedContacts = selectedContacts,
                            defaultMessagingApp = defaultMessagingApp,
                            modifier = Modifier.weight(1f),
                            availableMessagingApps = availableMessagingApps,
                            onExecuteAction = { ctx, action, phoneNumber ->
                                viewModel.executeAction(ctx, action, phoneNumber)
                            }
                        )
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = viewModel::updateSearchQuery,
                            focusRequester = focusRequester,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 6.dp)
                                .imePadding()
                        )
                    }

                    NavDestination.Home -> Column(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (isRecentCallsVisible && hasCallLogPermission) {
                                RecentCallsSection(
                                    recentCalls = if (isRecentCallsExpanded) allRecentCalls else recentCalls,
                                    onContactClick = { contact ->
                                        viewModel.makePhoneCall(context, contact.phoneNumber)
                                    },
                                    onWhatsAppClick = { contact ->
                                        viewModel.openMessagingApp(context, contact.phoneNumber)
                                    },
                                    onContactImageClick = { contact ->
                                        viewModel.openContactInContactsApp(context, contact)
                                    },
                                    isExpanded = isRecentCallsExpanded,
                                    onExpandedChange = { expanded ->
                                        isRecentCallsExpanded = expanded
                                        if (expanded) {
                                            editMode = false
                                            viewModel.loadAllRecentCalls(context)
                                        } else {
                                            viewModel.loadRecentCalls(context)
                                        }
                                    },
                                    defaultMessagingApp = defaultMessagingApp,
                                    selectedContacts = selectedContacts,
                                    availableMessagingApps = availableMessagingApps,
                                    onExecuteAction = { ctx, action, phoneNumber ->
                                        viewModel.executeAction(ctx, action, phoneNumber)
                                    },
                                    onAddToQuickList = { contact ->
                                        viewModel.addContact(contact)
                                    },
                                    showRecentCallsHint = showRecentCallsHint,
                                    onDismissRecentCallsHint = {
                                        showRecentCallsHint = false
                                        viewModel.markRecentCallsHintShown()
                                    },
                                    getLastShownPhoneNumber = viewModel::getLastShownPhoneNumber,
                                    setLastShownPhoneNumber = viewModel::setLastShownPhoneNumber
                                )
                            }

                            if (selectedContacts.isEmpty()) {
                                if (!isRecentCallsExpanded || !isRecentCallsVisible) {
                                    EmptyContactsScreen()
                                }
                            } else {
                                if (!isRecentCallsExpanded || !isRecentCallsVisible) {
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
                                                    .padding(end = 12.dp)
                                            )
                                        }

                                        if (editMode) {
                                            Text(
                                                text = stringResource(R.string.edit_hint),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                                            )
                                        }

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

                                    ContactList(
                                        contacts = filteredSelectedContacts,
                                        onContactClick = { contact ->
                                            val resolvedActions = resolvedActionsFor(contact)
                                            if (resolvedActions.cardTapAction != QuickContactAction.NONE) {
                                                viewModel.executeAction(context, resolvedActions.cardTapAction, contact.phoneNumber)
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
                                            val resolvedActions = resolvedActionsFor(contact)
                                            if (resolvedActions.firstButtonTapAction != QuickContactAction.NONE) {
                                                viewModel.executeAction(context, resolvedActions.firstButtonTapAction, contact.phoneNumber)
                                            }
                                        },
                                        onContactImageClick = { contact ->
                                            viewModel.openContactInContactsApp(context, contact)
                                        },
                                        onLongClick = { },
                                        onOpenActionEditor = { contact ->
                                            actionEditorContact = contact
                                        },
                                        customActionPreferences = customActionPreferences,
                                        defaultMessagingApp = defaultMessagingApp,
                                        availableMessagingApps = availableMessagingApps,
                                        selectedContacts = selectedContacts,
                                        onExecuteAction = { ctx, action, phoneNumber ->
                                            viewModel.executeAction(ctx, action, phoneNumber)
                                        },
                                        onUpdateContactNumber = { contact, selectedNumber ->
                                            viewModel.updateContactNumber(contact, selectedNumber)
                                        },
                                        hasSeenCallWarning = hasSeenCallWarning,
                                        onMarkCallWarningSeen = { viewModel.markCallWarningSeen() },
                                        onEditContactName = { contact, newName ->
                                            viewModel.updateContactName(contact, newName)
                                        },
                                        callActivityMap = callActivityMap
                                    )
                                }
                            }
                        }

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
