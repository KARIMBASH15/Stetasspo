package com.example.manager

import android.content.Context
import com.example.AppTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TabVisibilityManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("filehub_nav_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_VISIBLE_TABS = "key_visible_tabs_v1"
        val DEFAULT_VISIBLE_TABS = listOf(
            AppTab.FILES,
            AppTab.MEDIA,
            AppTab.BROWSER,
            AppTab.CLOUD,
            AppTab.VAULT
        )
    }

    private val _visibleTabs = MutableStateFlow(loadVisibleTabs())
    val visibleTabs: StateFlow<List<AppTab>> = _visibleTabs.asStateFlow()

    private fun loadVisibleTabs(): List<AppTab> {
        val savedSet = prefs.getStringSet(KEY_VISIBLE_TABS, null) ?: return DEFAULT_VISIBLE_TABS
        val list = AppTab.values().filter { savedSet.contains(it.name) }
        return if (list.size >= 2) list else DEFAULT_VISIBLE_TABS
    }

    fun isTabVisible(tab: AppTab): Boolean {
        return _visibleTabs.value.contains(tab)
    }

    fun toggleTab(tab: AppTab): Boolean {
        val current = _visibleTabs.value.toMutableList()
        if (current.contains(tab)) {
            if (current.size <= 2) {
                // Must keep at least 2 tabs visible
                return false
            }
            current.remove(tab)
        } else {
            // Add back in natural order
            val all = AppTab.values().toList()
            current.add(tab)
            current.sortBy { all.indexOf(it) }
        }
        _visibleTabs.value = current
        saveVisibleTabs(current)
        return true
    }

    fun resetToDefaults() {
        _visibleTabs.value = DEFAULT_VISIBLE_TABS
        saveVisibleTabs(DEFAULT_VISIBLE_TABS)
    }

    private fun saveVisibleTabs(tabs: List<AppTab>) {
        val set = tabs.map { it.name }.toSet()
        prefs.edit().putStringSet(KEY_VISIBLE_TABS, set).apply()
    }
}
