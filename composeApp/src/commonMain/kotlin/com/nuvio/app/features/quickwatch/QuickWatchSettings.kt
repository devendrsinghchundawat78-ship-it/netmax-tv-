package com.nuvio.app.features.quickwatch

import kotlinx.serialization.Serializable

@Serializable
data class QuickWatchSettings(
    val enabled: Boolean = true,
    val overlayPosition: String = "left", // "left", "center", "minimal"
    val showOverview: Boolean = true,
    val autoMute: Boolean = false,
    val showActionRail: Boolean = true,
)
