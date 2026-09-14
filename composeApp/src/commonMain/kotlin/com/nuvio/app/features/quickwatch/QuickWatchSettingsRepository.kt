package com.nuvio.app.features.quickwatch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object QuickWatchSettingsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _settings = MutableStateFlow(QuickWatchSettings())
    val settings: StateFlow<QuickWatchSettings> = _settings.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        _settings.value = QuickWatchSettingsStorage.loadSettings()
    }

    fun updateSettings(transform: (QuickWatchSettings) -> QuickWatchSettings) {
        ensureLoaded()
        val updated = transform(_settings.value)
        _settings.value = updated
        QuickWatchSettingsStorage.saveSettings(updated)
    }

    fun setEnabled(enabled: Boolean) {
        updateSettings { it.copy(enabled = enabled) }
    }

    fun setOverlayPosition(position: String) {
        updateSettings { it.copy(overlayPosition = position) }
    }

    fun setShowOverview(show: Boolean) {
        updateSettings { it.copy(showOverview = show) }
    }

    fun setAutoMute(mute: Boolean) {
        updateSettings { it.copy(autoMute = mute) }
    }

    fun setShowActionRail(show: Boolean) {
        updateSettings { it.copy(showActionRail = show) }
    }
}
