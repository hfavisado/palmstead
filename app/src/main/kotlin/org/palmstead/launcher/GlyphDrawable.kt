package org.palmstead.launcher

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/** Simple vector glyphs for the built-in "All Apps" and "Settings" buttons. */
internal class GlyphDrawable(private val type: Int, color: Int, antiAlias: Boolean) : Drawable() {
    private val paint = Paint().apply {
        this.color = color
        isAntiAlias = antiAlias
    }

    override fun draw(c: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val x0 = bounds.left.toFloat()
        val y0 = bounds.top.toFloat()
        if (type == AppEntry.ALL_APPS) {
            val cell = w / 4f
            for (r in 0 until 3) {
                for (col in 0 until 3) {
                    val x = x0 + w * 0.1f + col * (cell * 1.1f) + cell * 0.1f
                    val y = y0 + h * 0.1f + r * (cell * 1.1f) + cell * 0.1f
                    c.drawRoundRect(RectF(x, y, x + cell * 0.9f, y + cell * 0.9f), cell * 0.2f, cell * 0.2f, paint)
                }
            }
        } else {
            for (i in 0 until 3) {
                val y = y0 + h * (0.2f + i * 0.3f)
                c.drawRect(x0 + w * 0.1f, y, x0 + w * 0.9f, y + h * 0.08f, paint)
                val kx = x0 + w * (0.25f + ((i * 2) % 3) * 0.25f)
                c.drawCircle(kx, y + h * 0.04f, h * 0.1f, paint)
            }
        }
    }

    override fun setAlpha(a: Int) { paint.alpha = a }
    override fun setColorFilter(f: ColorFilter?) { paint.colorFilter = f }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
