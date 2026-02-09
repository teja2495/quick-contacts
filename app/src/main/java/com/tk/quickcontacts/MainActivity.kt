package com.tk.quickcontacts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.tk.quickcontacts.ui.AppNavigation
import com.tk.quickcontacts.ui.theme.QuickContactsTheme
import coil.ImageLoader
import coil.Coil
import coil.disk.DiskCache
import coil.memory.MemoryCache

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ContactsViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Switch to main theme after splash screen
        setTheme(R.style.Theme_QuickContacts)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ContactsViewModel::class.java]
        
        // Configure Coil for better performance
        configureImageLoader()
        
        enableEdgeToEdge()
        setContent {
            QuickContactsTheme {
                AppNavigation(viewModel)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh recent calls when app comes back to foreground
        if (viewModel.shouldRefreshRecentCalls()) {
            viewModel.refreshRecentCallsOnAppResume(this)
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Also refresh recent calls when app starts (handles cases where app was in background)
        if (viewModel.shouldRefreshRecentCalls()) {
            viewModel.refreshRecentCallsOnAppResume(this)
        }
    }
    
    private fun configureImageLoader() {
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Use 2% of available disk space
                    .build()
            }
            .respectCacheHeaders(false) // Always use cached images when available
            .build()
        
        // Set as default image loader
        coil.ImageLoader.Builder(this).build().let { defaultLoader ->
            // Replace the default loader with our optimized one
            coil.Coil.setImageLoader(imageLoader)
        }
    }
}
