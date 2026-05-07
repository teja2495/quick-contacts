package com.tk.quickcontacts.actions

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object GoogleSearchActions {
    private const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
    private const val GOOGLE_SEARCH_ACTION = "com.google.android.googlequicksearchbox.GOOGLE_SEARCH"
    private const val GMS_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION"
    private const val GMS_SEARCH_EXTRA_QUERY = "query"
    private const val GOOGLE_SEARCH_URL_BASE = "https://www.google.com/search?q="

    fun openGoogleSearch(context: Context, query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            if (openGoogleApp(context)) return
            openWebFallback(context, buildSearchUrl(trimmedQuery))
            return
        }

        val searchUrl = buildSearchUrl(trimmedQuery)
        val queryIntentFlags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

        val intentCandidates =
            listOf(
                Intent(Intent.ACTION_SEARCH).apply {
                    setPackage(GOOGLE_APP_PACKAGE)
                    putExtra(SearchManager.QUERY, trimmedQuery)
                    putExtra(GMS_SEARCH_EXTRA_QUERY, trimmedQuery)
                    addFlags(queryIntentFlags)
                },
                Intent(Intent.ACTION_WEB_SEARCH).apply {
                    setPackage(GOOGLE_APP_PACKAGE)
                    putExtra(SearchManager.QUERY, trimmedQuery)
                    putExtra(GMS_SEARCH_EXTRA_QUERY, trimmedQuery)
                    addFlags(queryIntentFlags)
                },
                Intent(GOOGLE_SEARCH_ACTION).apply {
                    setPackage(GOOGLE_APP_PACKAGE)
                    putExtra(SearchManager.QUERY, trimmedQuery)
                    putExtra(GMS_SEARCH_EXTRA_QUERY, trimmedQuery)
                    addFlags(queryIntentFlags)
                },
                Intent(GMS_SEARCH_ACTION).apply {
                    setPackage(GOOGLE_APP_PACKAGE)
                    putExtra(SearchManager.QUERY, trimmedQuery)
                    putExtra(GMS_SEARCH_EXTRA_QUERY, trimmedQuery)
                    addFlags(queryIntentFlags)
                },
                Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                    setPackage(GOOGLE_APP_PACKAGE)
                    putExtra(SearchManager.QUERY, trimmedQuery)
                    putExtra(GMS_SEARCH_EXTRA_QUERY, trimmedQuery)
                    addFlags(queryIntentFlags)
                },
            )

        for (candidate in intentCandidates) {
            if (!canResolveIntent(context, candidate)) continue
            if (startIntentSafely(context, candidate)) return
        }

        openWebFallback(context, searchUrl)
    }

    private fun openGoogleApp(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(GOOGLE_APP_PACKAGE) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startIntentSafely(context, launchIntent)
    }

    private fun canResolveIntent(context: Context, intent: Intent): Boolean =
        intent.resolveActivity(context.packageManager) != null

    private fun startIntentSafely(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun buildSearchUrl(query: String): String = GOOGLE_SEARCH_URL_BASE + Uri.encode(query)

    private fun openWebFallback(context: Context, searchUrl: String) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startIntentSafely(context, webIntent)
    }
}
