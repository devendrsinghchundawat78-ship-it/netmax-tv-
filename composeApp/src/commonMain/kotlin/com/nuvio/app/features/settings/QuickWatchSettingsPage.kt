package com.nuvio.app.features.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.quickwatch.QuickWatchSettingsRepository

internal fun LazyListScope.quickWatchSettingsContent(
    isTablet: Boolean,
) {
    item {
        QuickWatchSettingsSection(isTablet = isTablet)
    }
}

@Composable
private fun QuickWatchSettingsSection(isTablet: Boolean) {
    QuickWatchSettingsRepository.ensureLoaded()
    val settings by QuickWatchSettingsRepository.settings.collectAsStateWithLifecycle()

    SettingsSection(
        title = "Quick Watch",
        isTablet = isTablet,
    ) {
        SettingsGroup(isTablet = isTablet) {
            SettingsToggleRow(
                title = "Enable Quick Watch",
                subtitle = "Display the vertical movie reels feed in navigation and browse trailers",
                checked = settings.enabled,
                onCheckedChange = { QuickWatchSettingsRepository.setEnabled(it) },
                isTablet = isTablet,
            )
        }

        if (settings.enabled) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSingleSelectRow(
                    title = "Overlay Position",
                    subtitle = when (settings.overlayPosition) {
                        "center" -> "Bottom Center"
                        "minimal" -> "Minimal (Hidden Overview)"
                        else -> "Bottom Left (Default)"
                    },
                    options = listOf(
                        "left" to "Bottom Left (Default)",
                        "center" to "Bottom Center",
                        "minimal" to "Minimal (Compact)",
                    ),
                    selectedKey = settings.overlayPosition,
                    onOptionSelected = { QuickWatchSettingsRepository.setOverlayPosition(it) },
                    isTablet = isTablet,
                )

                SettingsToggleRow(
                    title = "Show Movie Story & Synopsis",
                    subtitle = "Show brief movie overview and genre tags on the reel",
                    checked = settings.showOverview,
                    onCheckedChange = { QuickWatchSettingsRepository.setShowOverview(it) },
                    isTablet = isTablet,
                )

                SettingsToggleRow(
                    title = "Show Action Rail",
                    subtitle = "Show Like, Comment, Watchlist, and Share buttons on the side",
                    checked = settings.showActionRail,
                    onCheckedChange = { QuickWatchSettingsRepository.setShowActionRail(it) },
                    isTablet = isTablet,
                )

                SettingsToggleRow(
                    title = "Start Muted",
                    subtitle = "Mute video audio by default when starting Quick Watch",
                    checked = settings.autoMute,
                    onCheckedChange = { QuickWatchSettingsRepository.setAutoMute(it) },
                    isTablet = isTablet,
                )
            }
        }
    }
}
