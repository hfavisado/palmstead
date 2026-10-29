package org.palmstead.launcher

import kotlin.math.max
import kotlin.math.min

/** Pure sizing maths, kept free of Android classes so it can be unit tested. */
internal object Geometry {
    const val LOW_W = 320
    const val LOW_H = 480

    fun clamp(v: Float, lo: Float, hi: Float): Float = max(lo, min(hi, v))

    /** Scale factor that fits the emulated 320x480 screen inside a w x h area. */
    fun lowResScale(w: Int, h: Int): Float =
        if (w <= 0 || h <= 0) 1f else min(w / LOW_W.toFloat(), h / LOW_H.toFloat())

    /** Physical pixels per inch, preferring the reported xdpi/ydpi and falling back to the density. */
    fun ppi(xdpi: Float, ydpi: Float, densityDpi: Int): Float {
        val okX = xdpi in 80f..1000f
        val okY = ydpi in 80f..1000f
        return when {
            okX && okY -> (xdpi + ydpi) / 2f
            okX -> xdpi
            okY -> ydpi
            else -> max(densityDpi, 120).toFloat()
        }
    }

    /**
     * Limits a natural pixel size to a physical range expressed in inches, so that
     * UI elements stay usable on any panel regardless of density or display-size settings.
     */
    fun clampedPx(naturalPx: Float, ppi: Float, minInches: Float, maxInches: Float): Float =
        clamp(naturalPx, minInches * ppi, maxInches * ppi)

    /** Pixels per Garnet logical pixel when using the full display resolution. */
    fun garnetFullUnit(ppi: Float, widthPx: Int): Float {
        var u = clamp(ppi / 180f, 1f, 8f)
        if (widthPx > 0) u = min(u, widthPx / LOW_W.toFloat())
        return max(1f, u)
    }
}
