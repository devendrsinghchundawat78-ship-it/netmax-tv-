package com.nuvio.app.features.player

import android.graphics.ColorMatrix

internal object VisualEnhancerMatrix {
    fun buildColorMatrix(
        mode: VisualEnhancerMode,
        customBrightness: Int = 0,
        customContrast: Int = 0,
        customSaturation: Int = 0,
    ): ColorMatrix {
        val (satFactor, conFactor, briOffset) = when (mode) {
            VisualEnhancerMode.Vivid -> Triple(1.45f, 1.22f, 10f)
            VisualEnhancerMode.Cinema -> Triple(1.22f, 1.15f, 4f)
            VisualEnhancerMode.Ultra -> Triple(1.75f, 1.35f, 16f)
            VisualEnhancerMode.Custom -> {
                val sat = (1.0f + (customSaturation / 35f)).coerceIn(0.0f, 3.5f)
                val con = (1.0f + (customContrast / 35f)).coerceIn(0.2f, 3.5f)
                val bri = customBrightness * 2.5f
                Triple(sat, con, bri)
            }
        }

        val satMatrix = ColorMatrix().apply { setSaturation(satFactor) }
        val c = conFactor
        val t = 128f * (1f - c) + briOffset
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                c,  0f, 0f, 0f, t,
                0f, c,  0f, 0f, t,
                0f, 0f, c,  0f, t,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        contrastMatrix.preConcat(satMatrix)
        return contrastMatrix
    }
}
