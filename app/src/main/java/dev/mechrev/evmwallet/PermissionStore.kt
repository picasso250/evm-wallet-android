package dev.mechrev.evmwallet

import android.content.Context

class PermissionStore(context: Context) {
    private val prefs = context.getSharedPreferences("dapp_permissions", Context.MODE_PRIVATE)

    fun isAllowed(origin: String): Boolean = prefs.getBoolean(origin, false)

    fun allow(origin: String) {
        prefs.edit().putBoolean(origin, true).apply()
    }

    fun revoke(origin: String) {
        prefs.edit().remove(origin).apply()
    }

    fun origins(): List<String> = prefs.all.filterValues { it == true }.keys.sorted()
}
