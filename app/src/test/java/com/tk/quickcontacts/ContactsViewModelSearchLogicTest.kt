package com.tk.quickcontacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsViewModelSearchLogicTest {

    @Test
    fun shouldPublishSearchResults_returnsTrueForLatestMatchingRequest() {
        val shouldPublish = ContactsViewModel.shouldPublishSearchResults(
            requestId = 5L,
            latestRequestId = 5L,
            requestQuery = "alex",
            currentQuery = "alex"
        )

        assertTrue(shouldPublish)
    }

    @Test
    fun shouldPublishSearchResults_returnsFalseForStaleRequestId() {
        val shouldPublish = ContactsViewModel.shouldPublishSearchResults(
            requestId = 4L,
            latestRequestId = 5L,
            requestQuery = "al",
            currentQuery = "alex"
        )

        assertFalse(shouldPublish)
    }

    @Test
    fun applyPhoneQueryFallback_addsDummyContact_whenPhoneQueryHasNoResults() {
        val results = ContactsViewModel.applyPhoneQueryFallback(
            query = "1234567",
            isPhoneNumberQuery = true,
            baseResults = emptyList()
        )

        assertEquals(1, results.size)
        assertEquals("search_number_1234567", results.first().id)
        assertEquals("1234567", results.first().phoneNumber)
    }

    @Test
    fun applyPhoneQueryFallback_returnsOriginalResults_whenAlreadyMatched() {
        val existing = Contact(
            id = "1",
            name = "Alex",
            phoneNumber = "1234567",
            phoneNumbers = listOf("1234567")
        )

        val results = ContactsViewModel.applyPhoneQueryFallback(
            query = "1234567",
            isPhoneNumberQuery = true,
            baseResults = listOf(existing)
        )

        assertEquals(1, results.size)
        assertEquals("1", results.first().id)
    }
}
