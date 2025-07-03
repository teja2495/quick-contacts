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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    // Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { contactUri ->
                // Parse the selected contact
                val contact = parseContactFromUri(context, contactUri)
                contact?.let { viewModel.addContact(it) }
            }
        }
    }

    // Contact states
    val selectedContacts by viewModel.selectedContacts.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()
    var editMode by remember { mutableStateOf(false) }
    
    // Load recent calls when permissions are available or selected contacts change
    LaunchedEffect(hasCallLogPermission, hasContactsPermission, selectedContacts) {
        if (hasCallLogPermission && hasContactsPermission) {
            viewModel.loadRecentCalls(context)
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

                        IconButton(
                            onClick = {
                                // Launch the built-in contact picker
                                val intent = Intent(Intent.ACTION_PICK).apply {
                                    type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                                }
                                contactPickerLauncher.launch(intent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Contact"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                        // Show recent calls section even when no quick contacts are selected
                        RecentCallsSection(
                            recentCalls = recentCalls,
                            onContactClick = { contact ->
                                viewModel.makePhoneCall(context, contact.phoneNumber)
                            }
                        )
                        
                        // Empty contacts screen
                        EmptyContactsScreen(
                            onAddContact = {
                                if (hasCallPermission && hasContactsPermission && hasCallLogPermission) {
                                    val intent = Intent(Intent.ACTION_PICK).apply {
                                        type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                                    }
                                    contactPickerLauncher.launch(intent)
                                }
                            }
                        )
                    }
                }
                
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Recent calls section at the top
                        RecentCallsSection(
                            recentCalls = recentCalls,
                            onContactClick = { contact ->
                                viewModel.makePhoneCall(context, contact.phoneNumber)
                            }
                        )
                        
                        // Quick contacts list
                        ContactList(
                            contacts = selectedContacts,
                            onContactClick = { contact ->
                                viewModel.makePhoneCall(context, contact.phoneNumber)
                            },
                            editMode = editMode,
                            onDeleteContact = { contact ->
                                viewModel.removeContact(contact)
                            },
                            onMove = { from, to ->
                                viewModel.moveContact(from, to)
                            }
                        )
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
fun EmptyContactsScreen(
    onAddContact: () -> Unit
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
            text = "No Quick Contacts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Add contacts to your quick list for easy calling access.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onAddContact,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Contact")
        }
    }
}