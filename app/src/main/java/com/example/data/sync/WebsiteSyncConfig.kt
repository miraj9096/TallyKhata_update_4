package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WebsiteSyncConfig(
    val websiteUrl: String = "",
    val apiKey: String = "",
    val storeId: String = "store_main",
    val autoSync: Boolean = true,
    val useWebsiteMySqlAsPrimary: Boolean = false,
    val lastSyncTime: Long = 0L,
    val lastSyncStatus: String = "IDLE", // IDLE, SUCCESS, FAILED, SYNCING
    val lastSyncMessage: String = "",
    val lastPingDbName: String = "",
    val lastPingTablesInfo: String = ""
)

class WebsiteSyncConfigManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("tally_website_sync_prefs", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<WebsiteSyncConfig> = _configFlow.asStateFlow()

    private fun loadConfig(): WebsiteSyncConfig {
        return WebsiteSyncConfig(
            websiteUrl = prefs.getString("websiteUrl", "") ?: "",
            apiKey = prefs.getString("apiKey", "") ?: "",
            storeId = prefs.getString("storeId", "store_main") ?: "store_main",
            autoSync = prefs.getBoolean("autoSync", true),
            useWebsiteMySqlAsPrimary = prefs.getBoolean("useWebsiteMySqlAsPrimary", false),
            lastSyncTime = prefs.getLong("lastSyncTime", 0L),
            lastSyncStatus = prefs.getString("lastSyncStatus", "IDLE") ?: "IDLE",
            lastSyncMessage = prefs.getString("lastSyncMessage", "") ?: "",
            lastPingDbName = prefs.getString("lastPingDbName", "") ?: "",
            lastPingTablesInfo = prefs.getString("lastPingTablesInfo", "") ?: ""
        )
    }

    fun saveConfig(
        websiteUrl: String,
        apiKey: String,
        storeId: String,
        autoSync: Boolean,
        useAsPrimary: Boolean? = null
    ) {
        val editor = prefs.edit()
            .putString("websiteUrl", websiteUrl.trim())
            .putString("apiKey", apiKey.trim())
            .putString("storeId", storeId.trim().ifBlank { "store_main" })
            .putBoolean("autoSync", autoSync)
        if (useAsPrimary != null) {
            editor.putBoolean("useWebsiteMySqlAsPrimary", useAsPrimary)
        }
        editor.apply()
        _configFlow.value = loadConfig()
    }

    fun setUseWebsiteMySqlAsPrimary(enabled: Boolean) {
        prefs.edit().putBoolean("useWebsiteMySqlAsPrimary", enabled).apply()
        _configFlow.value = loadConfig()
    }

    fun updateSyncStatus(status: String, message: String, timestamp: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString("lastSyncStatus", status)
            .putString("lastSyncMessage", message)
            .putLong("lastSyncTime", timestamp)
            .apply()
        _configFlow.value = loadConfig()
    }

    fun updatePingInfo(dbName: String, tablesInfo: String) {
        prefs.edit()
            .putString("lastPingDbName", dbName)
            .putString("lastPingTablesInfo", tablesInfo)
            .apply()
        _configFlow.value = loadConfig()
    }

    fun getConfig(): WebsiteSyncConfig = _configFlow.value
}
