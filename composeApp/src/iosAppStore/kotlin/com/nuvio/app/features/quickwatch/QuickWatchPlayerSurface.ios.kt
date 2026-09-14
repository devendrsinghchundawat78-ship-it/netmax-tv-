package com.nuvio.app.features.quickwatch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun QuickWatchPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    playWhenReady: Boolean,
    muted: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    onProgress: (currentMs: Long, totalMs: Long) -> Unit,
) = Unit
