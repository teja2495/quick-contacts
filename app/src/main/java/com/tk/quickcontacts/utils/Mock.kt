package com.tk.quickcontacts.utils

import com.tk.quickcontacts.Contact

object Mocks {
    // Mock mode flag - set to true to enable mock data for demonstration purposes
    const val ENABLE_MOCK_MODE = true
    
    // Mock contact data for demonstration
    object MockData {
        val MOCK_CONTACTS = listOf(
            Contact(
                id = "mock_1",
                name = "John Smith",
                phoneNumber = "+1-555-0101",
                phoneNumbers = listOf("+1-555-0101"),
                photo = null,
                photoUri = null,
                callType = null
            ),
            Contact(
                id = "mock_2",
                name = "Sarah Johnson",
                phoneNumber = "+1-555-0102",
                phoneNumbers = listOf("+1-555-0102"),
                photo = null,
                photoUri = null,
                callType = null
            ),
            Contact(
                id = "mock_3",
                name = "Michael Brown",
                phoneNumber = "+1-555-0103",
                phoneNumbers = listOf("+1-555-0103"),
                photo = null,
                photoUri = null,
                callType = null
            ),
            Contact(
                id = "mock_4",
                name = "Emily Davis",
                phoneNumber = "+1-555-0104",
                phoneNumbers = listOf("+1-555-0104"),
                photo = null,
                photoUri = null,
                callType = null
            ),
            Contact(
                id = "mock_5",
                name = "David Wilson",
                phoneNumber = "+1-555-0105",
                phoneNumbers = listOf("+1-555-0105"),
                photo = null,
                photoUri = null,
                callType = null
            ),
            Contact(
                id = "mock_6",
                name = "Lisa Anderson",
                phoneNumber = "+1-555-0106",
                phoneNumbers = listOf("+1-555-0106"),
                photo = null,
                photoUri = null,
                callType = null
            ),
            Contact(
                id = "mock_7",
                name = "Robert Taylor",
                phoneNumber = "+1-555-0107",
                phoneNumbers = listOf("+1-555-0107"),
                photo = null,
                photoUri = null,
                callType = null
            ),
            Contact(
                id = "mock_8",
                name = "Jennifer Martinez",
                phoneNumber = "+1-555-0108",
                phoneNumbers = listOf("+1-555-0108"),
                photo = null,
                photoUri = null,
                callType = null
            ),
            Contact(
                id = "mock_9",
                name = "Christopher Garcia",
                phoneNumber = "+1-555-0109",
                phoneNumbers = listOf("+1-555-0109"),
                photo = null,
                photoUri = null,
                callType = null
            ),
            Contact(
                id = "mock_10",
                name = "Amanda Rodriguez",
                phoneNumber = "+1-555-0110",
                phoneNumbers = listOf("+1-555-0110"),
                photo = null,
                photoUri = null,
                callType = null
            )
        )
        
        // Mock recent calls with call types and timestamps
        val MOCK_RECENT_CALLS = listOf(
            Contact(
                id = "mock_recent_1",
                name = "John Smith",
                phoneNumber = "+1-555-0101",
                phoneNumbers = listOf("+1-555-0101"),
                photo = null,
                photoUri = null,
                callType = "outgoing",
                callTimestamp = System.currentTimeMillis() - 300000 // 5 minutes ago
            ),
            Contact(
                id = "mock_recent_2",
                name = "Sarah Johnson",
                phoneNumber = "+1-555-0102",
                phoneNumbers = listOf("+1-555-0102"),
                photo = null,
                photoUri = null,
                callType = "incoming",
                callTimestamp = System.currentTimeMillis() - 900000 // 15 minutes ago
            ),
            Contact(
                id = "mock_recent_3",
                name = "Michael Brown",
                phoneNumber = "+1-555-0103",
                phoneNumbers = listOf("+1-555-0103"),
                photo = null,
                photoUri = null,
                callType = "missed",
                callTimestamp = System.currentTimeMillis() - 1800000 // 30 minutes ago
            ),
            Contact(
                id = "mock_recent_4",
                name = "Emily Davis",
                phoneNumber = "+1-555-0104",
                phoneNumbers = listOf("+1-555-0104"),
                photo = null,
                photoUri = null,
                callType = "outgoing",
                callTimestamp = System.currentTimeMillis() - 3600000 // 1 hour ago
            ),
            Contact(
                id = "mock_recent_5",
                name = "David Wilson",
                phoneNumber = "+1-555-0105",
                phoneNumbers = listOf("+1-555-0105"),
                photo = null,
                photoUri = null,
                callType = "incoming",
                callTimestamp = System.currentTimeMillis() - 7200000 // 2 hours ago
            )
        )
        
        // Quick list contacts (first 3 from mock contacts)
        val MOCK_QUICK_LIST_CONTACTS = MOCK_CONTACTS.take(3)
    }
} 