package com.tk.quickcontacts.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract

object ContactActionAvailability {
    private const val EMAIL_ACTION_PREFIX = "Email:"
    private const val APP_ACTION_PREFIX = "AppAction:"
    private const val ACTION_SEPARATOR = "|"

    private val CONTACT_REQUIRED_ACTIONS = listOf(
        "WhatsApp Voice Call" to ContactMethodMimeTypes.WHATSAPP_VOICE_CALL,
        "WhatsApp Video Call" to ContactMethodMimeTypes.WHATSAPP_VIDEO_CALL,
        "Telegram Voice Call" to ContactMethodMimeTypes.TELEGRAM_CALL,
        "Telegram Video Call" to ContactMethodMimeTypes.TELEGRAM_VIDEO_CALL,
        "Signal Voice Call" to ContactMethodMimeTypes.SIGNAL_CALL,
        "Signal Video Call" to ContactMethodMimeTypes.SIGNAL_VIDEO_CALL
    )

    private val CONTACT_ACTIONS_BY_MIMETYPE = listOf(
        "WhatsApp Chat" to ContactMethodMimeTypes.WHATSAPP_MESSAGE,
        "WhatsApp Voice Call" to ContactMethodMimeTypes.WHATSAPP_VOICE_CALL,
        "WhatsApp Video Call" to ContactMethodMimeTypes.WHATSAPP_VIDEO_CALL,
        "Telegram Chat" to ContactMethodMimeTypes.TELEGRAM_MESSAGE,
        "Telegram Voice Call" to ContactMethodMimeTypes.TELEGRAM_CALL,
        "Telegram Video Call" to ContactMethodMimeTypes.TELEGRAM_VIDEO_CALL,
        "Signal Chat" to ContactMethodMimeTypes.SIGNAL_MESSAGE,
        "Signal Voice Call" to ContactMethodMimeTypes.SIGNAL_CALL,
        "Signal Video Call" to ContactMethodMimeTypes.SIGNAL_VIDEO_CALL
    )

    fun getContactAvailableActions(context: Context, phoneNumber: String?): Set<String> {
        return getContactAvailableActions(
            context = context,
            contactId = null,
            phoneNumber = phoneNumber
        )
    }

    fun getContactAvailableActions(
        context: Context,
        contactId: Long?,
        phoneNumber: String?
    ): Set<String> {
        val result = mutableSetOf<String>()
        result.add("Call")
        result.add("Message")
        val resolvedContactId = resolveContactIdForLookup(context, contactId, phoneNumber)
        for ((actionName, mimeType) in CONTACT_ACTIONS_BY_MIMETYPE) {
            val isAvailable = when {
                resolvedContactId != null -> hasContactDataRow(context, resolvedContactId, mimeType)
                !phoneNumber.isNullOrBlank() -> hasContactDataRow(context, phoneNumber, mimeType)
                else -> false
            }
            if (isAvailable) {
                result.add(actionName)
            }
        }
        if (isGoogleMeetInstalled(context)) {
            result.add("Google Meet")
        }
        result.addAll(getEmailActions(context, resolvedContactId))
        result.addAll(
            getThirdPartyAppActions(
                context = context,
                contactId = resolvedContactId,
                selectedPhoneNumber = phoneNumber
            )
        )
        return result
    }

    private fun isGoogleMeetInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.apps.tachyon", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun resolveContactAvailableActions(
        context: Context,
        phoneNumber: String?,
        appAvailableActions: Set<String>
    ): Set<String> {
        if (phoneNumber.isNullOrBlank()) return appAvailableActions
        val result = appAvailableActions.toMutableSet()
        for ((actionName, mimeType) in CONTACT_REQUIRED_ACTIONS) {
            if (actionName in result && !hasContactDataRow(context, phoneNumber, mimeType)) {
                result.remove(actionName)
            }
        }
        return result
    }

    private fun hasContactDataRow(context: Context, phoneNumber: String, mimeType: String): Boolean {
        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber) ?: return false
        val contactId = resolveContactId(context, cleanNumber) ?: return false
        return hasContactDataRow(context, contactId, mimeType)
    }

    private fun hasContactDataRow(context: Context, contactId: Long, mimeType: String): Boolean {
        return try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId.toString(), mimeType),
                null
            )?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveContactId(context: Context, cleanPhoneNumber: String): Long? {
        return try {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(cleanPhoneNumber)
            )
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    if (idx >= 0) cursor.getLong(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveContactIdForLookup(
        context: Context,
        contactId: Long?,
        phoneNumber: String?
    ): Long? {
        contactId?.let { return it }
        val cleanNumber = phoneNumber?.let { PhoneNumberUtils.cleanPhoneNumber(it) } ?: return null
        return resolveContactId(context, cleanNumber)
    }

    private fun getEmailActions(context: Context, contactId: Long?): Set<String> {
        if (contactId == null) return emptySet()
        return try {
            val emailActions = linkedSetOf<String>()
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                if (addressIndex >= 0) {
                    while (cursor.moveToNext()) {
                        val address = cursor.getString(addressIndex)?.trim().orEmpty()
                        if (address.isNotEmpty()) {
                            emailActions.add("$EMAIL_ACTION_PREFIX$address")
                        }
                    }
                }
            }
            emailActions
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun isEmailAction(action: String): Boolean = action.startsWith(EMAIL_ACTION_PREFIX)

    fun getEmailFromAction(action: String): String? {
        if (!isEmailAction(action)) return null
        return action.removePrefix(EMAIL_ACTION_PREFIX).trim().ifBlank { null }
    }

    fun isThirdPartyAppAction(action: String): Boolean = action.startsWith(APP_ACTION_PREFIX)

    fun getThirdPartyAppActionLabel(action: String): String? {
        val parsed = parseThirdPartyAppAction(action) ?: return null
        return parsed.label
    }

    fun resolveThirdPartyAppPackageName(context: Context, action: String): String? {
        val parsed = parseThirdPartyAppAction(action) ?: return null
        val packageManager = context.packageManager

        // Prefer explicit package when it is visible and installed.
        val explicitPackage = parsed.packageName
        if (!explicitPackage.isNullOrBlank()) {
            try {
                packageManager.getPackageInfo(explicitPackage, 0)
                return explicitPackage
            } catch (_: Exception) {
            }
        }

        // Fallback: resolve the handler from the exact intent we use to launch this action.
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://com.android.contacts/data/${parsed.dataId}"), parsed.mimeType)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val resolved = packageManager.resolveActivity(intent, 0)
            val resolvedPackage = resolved?.activityInfo?.packageName?.trim()
            if (!resolvedPackage.isNullOrEmpty()) {
                return resolvedPackage
            }
        } catch (_: Exception) {
        }

        // Last fallback: infer package name from mime type pattern.
        return resolvePackageNameForMimeType(packageManager, parsed.mimeType)
    }

    fun openThirdPartyAppAction(context: Context, action: String): Boolean {
        val parsed = parseThirdPartyAppAction(action) ?: return false
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("content://com.android.contacts/data/${parsed.dataId}"), parsed.mimeType)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                parsed.packageName?.let { setPackage(it) }
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private data class ThirdPartyAppAction(
        val dataId: Long,
        val mimeType: String,
        val label: String,
        val packageName: String?
    )

    private fun buildThirdPartyAppAction(
        dataId: Long,
        mimeType: String,
        label: String,
        packageName: String?
    ): String {
        return listOf(
            APP_ACTION_PREFIX + dataId.toString(),
            Uri.encode(mimeType),
            Uri.encode(label),
            Uri.encode(packageName.orEmpty())
        ).joinToString(ACTION_SEPARATOR)
    }

    private fun parseThirdPartyAppAction(action: String): ThirdPartyAppAction? {
        if (!isThirdPartyAppAction(action)) return null
        val parts = action.split(ACTION_SEPARATOR, limit = 4)
        if (parts.size < 4) return null
        val idPart = parts[0].removePrefix(APP_ACTION_PREFIX)
        val dataId = idPart.toLongOrNull() ?: return null
        val mimeType = Uri.decode(parts[1]).trim()
        val label = Uri.decode(parts[2]).trim()
        val packageName = Uri.decode(parts[3]).trim().ifBlank { null }
        if (mimeType.isEmpty() || label.isEmpty()) return null
        return ThirdPartyAppAction(
            dataId = dataId,
            mimeType = mimeType,
            label = label,
            packageName = packageName
        )
    }

    private fun getThirdPartyAppActions(
        context: Context,
        contactId: Long?,
        selectedPhoneNumber: String?
    ): Set<String> {
        if (contactId == null) return emptySet()

        val knownMimeTypes = CONTACT_ACTIONS_BY_MIMETYPE.mapTo(mutableSetOf()) { it.second }.apply {
            add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
        }

        val selectedNumber = selectedPhoneNumber
        return try {
            val actions = linkedSetOf<String>()
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data._ID,
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.Data.DATA1,
                    ContactsContract.Data.DATA2,
                    ContactsContract.Data.DATA3
                ),
                "${ContactsContract.Data.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.Data._ID)
                val mimeTypeIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
                val data1Index = cursor.getColumnIndex(ContactsContract.Data.DATA1)
                val data2Index = cursor.getColumnIndex(ContactsContract.Data.DATA2)
                val data3Index = cursor.getColumnIndex(ContactsContract.Data.DATA3)
                if (idIndex < 0 || mimeTypeIndex < 0) {
                    return@use
                }
                while (cursor.moveToNext()) {
                    val dataId = cursor.getLong(idIndex)
                    val mimeType = cursor.getString(mimeTypeIndex)?.trim().orEmpty()
                    if (mimeType.isEmpty() || mimeType in knownMimeTypes) continue
                    if (!mimeType.startsWith("vnd.android.cursor.item/vnd.")) continue

                    val data1 = if (data1Index >= 0) cursor.getString(data1Index)?.trim().orEmpty() else ""
                    if (selectedNumber != null &&
                        PhoneNumberUtils.isValidPhoneNumber(data1) &&
                        !PhoneNumberUtils.isSameNumber(data1, selectedNumber)
                    ) {
                        continue
                    }

                    val data2 = if (data2Index >= 0) cursor.getString(data2Index)?.trim().orEmpty() else ""
                    val data3 = if (data3Index >= 0) cursor.getString(data3Index)?.trim().orEmpty() else ""
                    val packageName = resolvePackageNameForMimeType(context.packageManager, mimeType)
                    val label = resolveThirdPartyLabel(context.packageManager, mimeType, data2, data3, packageName)
                    val isNoOpViberEntry =
                        packageName == "com.viber.voip" && label.equals("Viber", ignoreCase = true)
                    if (isNoOpViberEntry) continue
                    actions.add(buildThirdPartyAppAction(dataId, mimeType, label, packageName))
                }
            }
            actions
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun resolveThirdPartyLabel(
        packageManager: PackageManager,
        mimeType: String,
        data2: String,
        data3: String,
        packageName: String?
    ): String {
        val data3Label = data3.takeIf { it.isNotBlank() && !PhoneNumberUtils.isValidPhoneNumber(it) }
        if (data3Label != null) return data3Label
        val data2Label = data2.takeIf { it.isNotBlank() && !PhoneNumberUtils.isValidPhoneNumber(it) }
        if (data2Label != null) return data2Label
        if (!packageName.isNullOrBlank()) {
            try {
                val appLabel = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageName, 0)
                ).toString().trim()
                if (appLabel.isNotEmpty()) return appLabel
            } catch (_: Exception) {
            }
        }
        return friendlyLabelFromMimeType(mimeType)
    }

    private fun resolvePackageNameForMimeType(packageManager: PackageManager, mimeType: String): String? {
        val prefix = "vnd.android.cursor.item/vnd."
        if (!mimeType.startsWith(prefix)) return null
        val remainder = mimeType.removePrefix(prefix)
        if (remainder.isBlank()) return null
        val segments = remainder.split(".")
        val candidates = linkedSetOf<String>()
        candidates.add(remainder)
        if (segments.size >= 3) candidates.add(segments.take(3).joinToString("."))
        if (segments.size >= 2) candidates.add(segments.take(2).joinToString("."))
        for (candidate in candidates) {
            try {
                packageManager.getPackageInfo(candidate, 0)
                return candidate
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun friendlyLabelFromMimeType(mimeType: String): String {
        val prefix = "vnd.android.cursor.item/vnd."
        val raw = mimeType.removePrefix(prefix)
        if (raw.isBlank()) return "Third-party app"
        val head = raw.split(".").firstOrNull().orEmpty()
        if (head.isNotBlank()) {
            return head.replaceFirstChar { c -> c.uppercase() }
        }
        return "Third-party app"
    }
}
