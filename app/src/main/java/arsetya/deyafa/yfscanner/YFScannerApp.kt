package arsetya.deyafa.yfscanner

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import arsetya.deyafa.yfscanner.ui.SettingsActivity

class YFScannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Apply dark mode preference on app startup
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        // Default to system theme or false (light) if not set.
        val isDarkMode = prefs.getBoolean(SettingsActivity.KEY_DARK_MODE, false)
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
