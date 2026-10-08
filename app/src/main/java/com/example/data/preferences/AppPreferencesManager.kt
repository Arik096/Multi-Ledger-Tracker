package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ModernPalette(val title: String, val subtitle: String) {
    EMERALD_MINT("Emerald Mint", "Modern Fintech • Obsidian Void"),
    CYBER_INDIGO("Cyber Indigo", "Electric Indigo • Cyber Cyan"),
    SUNSET_ROSE("Sunset Rose", "Radiant Rose • Sunset Gold"),
    TITANIUM_ICE("Titanium Ice", "Minimalist Ice • Cool Slate")
}

object AppPreferencesManager {
    private const val PREFS_NAME = "cashbook_app_preferences"

    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_MODERN_PALETTE = "key_modern_palette"
    private const val KEY_GOOGLE_ACCOUNT_EMAIL = "key_google_account_email"
    private const val KEY_GOOGLE_DRIVE_CONNECTED = "key_google_drive_connected"
    private const val KEY_AUTO_BACKUP_ENABLED = "key_auto_backup_enabled"
    private const val KEY_WIFI_ONLY_SYNC = "key_wifi_only_sync"
    private const val KEY_LAST_BACKUP_TIME = "key_last_backup_time"
    private const val KEY_LAST_BACKUP_SUMMARY = "key_last_backup_summary"
    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_DEFAULT_CURRENCY = "key_default_currency"
    private const val KEY_PROFILE_PHOTO_PATH = "key_profile_photo_path"
    private const val KEY_PHYSICAL_CASH_FIELDS = "key_physical_cash_fields"
    private const val KEY_ACCOUNT_CASH_FIELDS = "key_account_cash_fields"
    private const val KEY_GOOGLE_DRIVE_FOLDER_NAME = "key_google_drive_folder_name"

    private const val DEFAULT_ACCOUNT = "md.arik.ific@gmail.com"
    private const val DEFAULT_USER_NAME = "Md Arik"
    private const val DEFAULT_CURRENCY = "BDT"
    const val DEFAULT_DRIVE_FOLDER = "CashBook Records"

    val DEFAULT_PHYSICAL_FIELDS = listOf(
        "Bank Asia",
        "IBBPLC",
        "TB PLC",
        "801",
        "901",
        "OD",
        "Bkash",
        "Cash",
        "New"
    )

    val DEFAULT_ACCOUNT_FIELDS = listOf(
        "Savings",
        "Current Month",
        "Extra",
        "Lendings"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(context: Context): ThemeMode {
        val name = getPrefs(context).getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return try {
            ThemeMode.valueOf(name)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getModernPalette(context: Context): ModernPalette {
        val name = getPrefs(context).getString(KEY_MODERN_PALETTE, ModernPalette.EMERALD_MINT.name) ?: ModernPalette.EMERALD_MINT.name
        return try {
            ModernPalette.valueOf(name)
        } catch (e: Exception) {
            ModernPalette.EMERALD_MINT
        }
    }

    fun setModernPalette(context: Context, palette: ModernPalette) {
        getPrefs(context).edit().putString(KEY_MODERN_PALETTE, palette.name).apply()
    }

    fun getGoogleAccountEmail(context: Context): String {
        val email = getPrefs(context).getString(KEY_GOOGLE_ACCOUNT_EMAIL, DEFAULT_ACCOUNT) ?: DEFAULT_ACCOUNT
        return if (email.isBlank()) DEFAULT_ACCOUNT else email
    }

    fun setGoogleAccountEmail(context: Context, email: String) {
        getPrefs(context).edit().putString(KEY_GOOGLE_ACCOUNT_EMAIL, email.trim()).apply()
    }

    fun isGoogleDriveConnected(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_GOOGLE_DRIVE_CONNECTED, true)
    }

    fun setGoogleDriveConnected(context: Context, connected: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_GOOGLE_DRIVE_CONNECTED, connected).apply()
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun isWifiOnlySync(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WIFI_ONLY_SYNC, false)
    }

    fun setWifiOnlySync(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WIFI_ONLY_SYNC, enabled).apply()
    }

    fun getGoogleDriveFolderName(context: Context): String {
        val folder = getPrefs(context).getString(KEY_GOOGLE_DRIVE_FOLDER_NAME, DEFAULT_DRIVE_FOLDER) ?: DEFAULT_DRIVE_FOLDER
        return if (folder.isBlank()) DEFAULT_DRIVE_FOLDER else folder.trim()
    }

    fun setGoogleDriveFolderName(context: Context, folderName: String) {
        val clean = folderName.trim().ifBlank { DEFAULT_DRIVE_FOLDER }
        getPrefs(context).edit().putString(KEY_GOOGLE_DRIVE_FOLDER_NAME, clean).apply()
    }

    fun getLastBackupTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun setLastBackupTime(context: Context, time: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_BACKUP_TIME, time).apply()
    }

    fun getLastBackupSummary(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_BACKUP_SUMMARY, "") ?: ""
    }

    fun setLastBackupSummary(context: Context, summary: String) {
        getPrefs(context).edit().putString(KEY_LAST_BACKUP_SUMMARY, summary).apply()
    }

    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
    }

    fun setUserName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun getDefaultCurrency(context: Context): String {
        return getPrefs(context).getString(KEY_DEFAULT_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY
    }

    fun setDefaultCurrency(context: Context, currency: String) {
        getPrefs(context).edit().putString(KEY_DEFAULT_CURRENCY, currency).apply()
    }

    fun getProfilePhotoPath(context: Context): String {
        return getPrefs(context).getString(KEY_PROFILE_PHOTO_PATH, "") ?: ""
    }

    fun setProfilePhotoPath(context: Context, path: String) {
        getPrefs(context).edit().putString(KEY_PROFILE_PHOTO_PATH, path).apply()
    }

    fun clearUserData(context: Context) {
        getPrefs(context).edit()
            .putString(KEY_GOOGLE_ACCOUNT_EMAIL, "")
            .putString(KEY_USER_NAME, "")
            .putString(KEY_PROFILE_PHOTO_PATH, "")
            .putLong(KEY_LAST_BACKUP_TIME, 0L)
            .putString(KEY_LAST_BACKUP_SUMMARY, "")
            .apply()
    }

    fun getPhysicalCashFields(context: Context): List<String> {
        val raw = getPrefs(context).getString(KEY_PHYSICAL_CASH_FIELDS, null) ?: return DEFAULT_PHYSICAL_FIELDS
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i)
                if (!item.isNullOrBlank()) list.add(item)
            }
            if (list.isEmpty()) DEFAULT_PHYSICAL_FIELDS else list
        } catch (e: Exception) {
            DEFAULT_PHYSICAL_FIELDS
        }
    }

    fun setPhysicalCashFields(context: Context, fields: List<String>) {
        val arr = JSONArray(fields)
        getPrefs(context).edit().putString(KEY_PHYSICAL_CASH_FIELDS, arr.toString()).apply()
    }

    fun getAccountCashFields(context: Context): List<String> {
        val raw = getPrefs(context).getString(KEY_ACCOUNT_CASH_FIELDS, null) ?: return DEFAULT_ACCOUNT_FIELDS
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i)
                if (!item.isNullOrBlank()) list.add(item)
            }
            if (list.isEmpty()) DEFAULT_ACCOUNT_FIELDS else list
        } catch (e: Exception) {
            DEFAULT_ACCOUNT_FIELDS
        }
    }

    fun setAccountCashFields(context: Context, fields: List<String>) {
        val arr = JSONArray(fields)
        getPrefs(context).edit().putString(KEY_ACCOUNT_CASH_FIELDS, arr.toString()).apply()
    }

    fun resetCashPositionFields(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_PHYSICAL_CASH_FIELDS)
            .remove(KEY_ACCOUNT_CASH_FIELDS)
            .apply()
    }
}
