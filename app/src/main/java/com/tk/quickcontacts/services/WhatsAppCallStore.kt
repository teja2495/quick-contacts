package com.tk.quickcontacts.services

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Notification-listener events. This store is deliberately local-only and is
 * read only by enabled non-Google-Play builds when recent calls are loaded.
 */
object WhatsAppCallStore {
    private const val SETTINGS_PREFERENCES_NAME = "QuickContactsPrefs"
    private const val ENABLED_KEY = "whatsapp_recent_calls_enabled"
    private const val PREFERENCES_NAME = "whatsapp_call_events"
    private const val EVENTS_KEY = "events"
    private const val MAX_EVENTS = 50
    private val gson = Gson()

    data class Event(
        val notificationKey: String,
        val name: String,
        val phoneNumber: String?,
        val callType: String,
        val timestamp: Long,
    )

    fun isEnabled(context: Context): Boolean = context
        .getSharedPreferences(SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getBoolean(ENABLED_KEY, false)

    fun record(context: Context, event: Event) {
        val updatedEvents = load(context)
            .filterNot { it.notificationKey == event.notificationKey }
            .plus(event)
            .sortedByDescending { it.timestamp }
            .take(MAX_EVENTS)
        save(context, updatedEvents)
    }

    fun load(context: Context): List<Event> {
        val json = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(EVENTS_KEY, null)
            ?: return emptyList()
        return runCatching {
            gson.fromJson<List<Event>>(json, object : TypeToken<List<Event>>() {}.type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, events: List<Event>) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(EVENTS_KEY, gson.toJson(events))
            .apply()
    }
}
