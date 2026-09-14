package com.nuvio.app.features.quickwatch

internal expect object QuickWatchSettingsStorage {
    fun loadSettings(): QuickWatchSettings
    fun saveSettings(settings: QuickWatchSettings)
}
