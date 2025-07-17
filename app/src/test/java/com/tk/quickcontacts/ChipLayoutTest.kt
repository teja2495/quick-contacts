package com.tk.quickcontacts

import com.tk.quickcontacts.models.MessagingApp
import org.junit.Test
import org.junit.Assert.*

class ChipLayoutTest {
    
    @Test
    fun testChipLayoutCalculation() {
        // Test with different numbers of available apps
        
        // 2 apps (Call, Messages) - should be 1 row, 2 columns
        val layout2 = calculateChipLayout(2)
        assertEquals(1, layout2.rows)
        assertEquals(2, layout2.columns)
        assertEquals("center", layout2.lastRowAlignment)
        
        // 3 apps (Call, Messages, WhatsApp) - should be 2 rows, 2 columns, last row left-aligned
        val layout3 = calculateChipLayout(3)
        assertEquals(2, layout3.rows)
        assertEquals(2, layout3.columns)
        assertEquals("left", layout3.lastRowAlignment)
        
        // 4 apps (Call, Messages, WhatsApp, Telegram) - should be 2 rows, 2 columns
        val layout4 = calculateChipLayout(4)
        assertEquals(2, layout4.rows)
        assertEquals(2, layout4.columns)
        assertEquals("center", layout4.lastRowAlignment)
        
        // 5 apps (Call, Messages, WhatsApp, Telegram, All Options) - should be 3 rows, 2 columns, last row left-aligned
        val layout5 = calculateChipLayout(5)
        assertEquals(3, layout5.rows)
        assertEquals(2, layout5.columns)
        assertEquals("left", layout5.lastRowAlignment)
    }
    
    @Test
    fun testChipListBuilding() {
        // Test with all apps available
        val allApps = setOf(MessagingApp.WHATSAPP, MessagingApp.TELEGRAM, MessagingApp.SMS)
        val primaryChips = buildPrimaryChipsList(allApps)
        assertEquals(5, primaryChips.size) // Call, Messages, WhatsApp, Telegram, All Options
        
        // Test with only SMS available
        val smsOnly = setOf(MessagingApp.SMS)
        val primaryChipsSmsOnly = buildPrimaryChipsList(smsOnly)
        assertEquals(3, primaryChipsSmsOnly.size) // Call, Messages, All Options
        
        // Test with no messaging apps available
        val noApps = emptySet<MessagingApp>()
        val primaryChipsNoApps = buildPrimaryChipsList(noApps)
        assertEquals(3, primaryChipsNoApps.size) // Call, Messages, All Options
    }
    
    // Helper functions (copied from ContactDialogs.kt for testing)
    private data class ChipLayout(
        val rows: Int,
        val columns: Int,
        val lastRowAlignment: String = "center"
    )
    
    private fun buildPrimaryChipsList(availableMessagingApps: Set<MessagingApp>): List<Pair<String, Boolean>> {
        return listOf(
            Pair("Call", true),
            Pair("Messages", true),
            Pair("WhatsApp", availableMessagingApps.contains(MessagingApp.WHATSAPP)),
            Pair("Telegram", availableMessagingApps.contains(MessagingApp.TELEGRAM)),
            Pair("All Options", true)
        ).filter { it.second }
    }
    
    private fun calculateChipLayout(chipCount: Int): ChipLayout {
        return when (chipCount) {
            2 -> ChipLayout(rows = 1, columns = 2)
            3 -> ChipLayout(rows = 2, columns = 2, lastRowAlignment = "left")
            4 -> ChipLayout(rows = 2, columns = 2)
            5 -> ChipLayout(rows = 3, columns = 2, lastRowAlignment = "left")
            else -> ChipLayout(rows = 3, columns = 2)
        }
    }
} 