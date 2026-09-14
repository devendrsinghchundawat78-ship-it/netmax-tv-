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
            VisualEnhancerMode.Vivid -> Triple(1.28f, 1.14f, 6f)
            VisualEnhancerMode.Cinema -> Triple(1.14f, 1.10f, 3f)
            VisualEnhancerMode.Ultra -> Triple(1.40f, 1.22f, 8f)
            VisualEnhancerMode.Custom -> {
                val sat = (1.0f + (customSaturation / 100f)).coerceIn(0.5f, 2.0f)
                val con = (1.0f + (customContrast / 100f)).coerceIn(0.5f, 2.0f)
                val bri = customBrightness * 1.5f
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
