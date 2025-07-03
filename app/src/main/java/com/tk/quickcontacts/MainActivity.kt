package com.tk.quickcontacts

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quickcontacts.ui.theme.QuickContactsTheme
import android.app.Application
import androidx.lifecycle.ViewModelProvider

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

    // Multiple permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCallPermission = permissions[Manifest.permission.CALL_PHONE] ?: false
        hasContactsPermission = permissions[Manifest.permission.READ_CONTACTS] ?: false
        hasCallLogPermission = permissions[Manifest.permission.READ_CALL_LOG] ?: false
    }



    // Contact states
    val selectedContacts by viewModel.selectedContacts.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()
    val filteredSelectedContacts by viewModel.filteredSelectedContacts.collectAsState()
    val filteredRecentCalls by viewModel.filteredRecentCalls.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var editMode by remember { mutableStateOf(false) }
    
    // Load recent calls when permissions are available or selected contacts change
    LaunchedEffect(hasCallLogPermission, hasContactsPermission, selectedContacts) {
        if (hasCallLogPermission && hasContactsPermission) {
            viewModel.loadRecentCalls(context)
        }
    }
    
    // Search all contacts when search query changes
    LaunchedEffect(searchQuery, hasContactsPermission) {
        if (hasContactsPermission && searchQuery.isNotEmpty()) {
            viewModel.searchAllContacts(context, searchQuery)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quick Contacts",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (hasCallPermission && hasContactsPermission && hasCallLogPermission) {
                        if (selectedContacts.isNotEmpty()) {
                            IconButton(onClick = { editMode = !editMode }) {
                                Icon(
                                    imageVector = if (editMode) Icons.Default.Done else Icons.Default.Edit,
                                    contentDescription = if (editMode) "Done" else "Edit Contacts"
                                )
                            }
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
            // Search bar - only show when permissions are granted
            if (hasCallPermission && hasContactsPermission && hasCallLogPermission) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            when {
                !hasCallPermission || !hasContactsPermission || !hasCallLogPermission -> {
                    PermissionRequestScreen(
                        onRequestPermissions = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CALL_PHONE,
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.READ_CALL_LOG
                                )
                            )
                        }
                    )
                }
                
                selectedContacts.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Show search results when searching
                        if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                            SearchResultsSection(
                                searchResults = searchResults,
                                onContactClick = { contact ->
                                    viewModel.makePhoneCall(context, contact.phoneNumber)
                                },
                                onAddContact = { contact ->
                                    viewModel.addContact(contact)
                                }
                            )
                        }
                        
                        // Show recent calls section only when not searching
                        if (searchQuery.isEmpty()) {
                            RecentCallsSection(
                                recentCalls = filteredRecentCalls,
                                onContactClick = { contact ->
                                    viewModel.makePhoneCall(context, contact.phoneNumber)
                                }
                            )
                            
                            // Empty contacts screen
                            EmptyContactsScreen()
                        }
                    }
                }
                
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Show search results when searching
                        if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                            SearchResultsSection(
                                searchResults = searchResults,
                                onContactClick = { contact ->
                                    viewModel.makePhoneCall(context, contact.phoneNumber)
                                },
                                onAddContact = { contact ->
                                    viewModel.addContact(contact)
                                }
                            )
                        }
                        
                        // Show recent calls and quick contacts only when not searching
                        if (searchQuery.isEmpty()) {
                            // Recent calls section
                            RecentCallsSection(
                                recentCalls = filteredRecentCalls,
                                onContactClick = { contact ->
                                    viewModel.makePhoneCall(context, contact.phoneNumber)
                                }
                            )
                            
                            // Quick contacts list
                            ContactList(
                                contacts = filteredSelectedContacts,
                                onContactClick = { contact ->
                                    viewModel.makePhoneCall(context, contact.phoneNumber)
                                },
                                editMode = editMode,
                                onDeleteContact = { contact ->
                                    viewModel.removeContact(contact)
                                },
                                onMove = { from, to ->
                                    viewModel.moveContact(from, to)
                                },
                                onWhatsAppClick = { contact ->
                                    viewModel.openWhatsAppChat(context, contact.phoneNumber)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
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
fun SearchResultsSection(
    searchResults: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onAddContact: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    if (searchResults.isNotEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Display up to 5 search results
                searchResults.take(5).forEach { contact ->
                    SearchResultItem(
                        contact = contact,
                        onContactClick = onContactClick,
                        onAddContact = onAddContact,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (searchResults.size > 5) {
                    Text(
                        text = "+${searchResults.size - 5} more results",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onAddContact: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact Photo
            if (contact.photoUri != null && !imageLoadFailed) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(contact.photoUri)
                            .crossfade(true)
                            .build(),
                        onError = { imageLoadFailed = true }
                    ),
                    contentDescription = "Contact photo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Default avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Contact Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call button
                IconButton(
                    onClick = { onContactClick(contact) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call ${contact.name}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Add to quick contacts button
                IconButton(
                    onClick = { onAddContact(contact) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add ${contact.name} to quick contacts",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
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
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Permission Required",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "This app needs phone, contacts, and call log permissions to make calls, display contact photos, and show recent call history.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun EmptyContactsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No Quick Contacts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Search contacts above and add them to your quick list for easy calling access.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}