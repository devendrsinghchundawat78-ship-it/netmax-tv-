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
            SettingsSwitchRow(
                title = "Enable Quick Watch",
                description = "Display the vertical movie reels feed in navigation and browse trailers",
                checked = settings.enabled,
                onCheckedChange = { QuickWatchSettingsRepository.setEnabled(it) },
                isTablet = isTablet,
            )
        }

        if (settings.enabled) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = "Overlay Position",
                    description = when (settings.overlayPosition) {
                        "center" -> "Bottom Center"
                        "minimal" -> "Minimal (Compact)"
                        else -> "Bottom Left (Default)"
                    },
                    isTablet = isTablet,
                    onClick = {
                        val next = when (settings.overlayPosition) {
                            "left" -> "center"
                            "center" -> "minimal"
                            else -> "left"
                        }
                        QuickWatchSettingsRepository.setOverlayPosition(next)
                    },
                )

                SettingsGroupDivider(isTablet = isTablet)

                SettingsSwitchRow(
                    title = "Show Movie Story & Synopsis",
                    description = "Show brief movie overview and genre tags on the reel",
                    checked = settings.showOverview,
                    onCheckedChange = { QuickWatchSettingsRepository.setShowOverview(it) },
                    isTablet = isTablet,
                )

                SettingsGroupDivider(isTablet = isTablet)

                SettingsSwitchRow(
                    title = "Show Action Rail",
                    description = "Show Like, Comment, Watchlist, and Share buttons on the side",
                    checked = settings.showActionRail,
                    onCheckedChange = { QuickWatchSettingsRepository.setShowActionRail(it) },
                    isTablet = isTablet,
                )

                SettingsGroupDivider(isTablet = isTablet)

                SettingsSwitchRow(
                    title = "Start Muted",
                    description = "Mute video audio by default when starting Quick Watch",
                    checked = settings.autoMute,
                    onCheckedChange = { QuickWatchSettingsRepository.setAutoMute(it) },
                    isTablet = isTablet,
                )
            }
        }
    }
}
