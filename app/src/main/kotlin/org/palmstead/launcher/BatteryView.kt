package org.palmstead.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import kotlin.math.max

/** Small battery gauge: an outlined cell with a fill proportional to the charge level. */
internal class BatteryView(c: Context, private val color: Int, antiAlias: Boolean) : View(c) {
    private val paint = Paint().apply { isAntiAlias = antiAlias }
    private var percent = 100
    private var charging = false

    fun setLevel(percent: Int, charging: Boolean) {
        this.percent = percent.coerceIn(0, 100)
        this.charging = charging
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val stroke = max(1f, h / 8f)
        val tip = max(2f, w / 8f)
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        c.drawRect(stroke / 2, stroke / 2, w - tip - stroke / 2, h - stroke / 2, paint)
        paint.style = Paint.Style.FILL
        c.drawRect(w - tip, h * 0.3f, w, h * 0.7f, paint)
        val inner = stroke * 2f
        val fillW = (w - tip - inner * 2f) * percent / 100f
        c.drawRect(inner, inner, inner + fillW, h - inner, paint)
        if (charging) {
            paint.color = (color.inv() and 0x00FFFFFF) or (0xFF shl 24)
            c.drawRect(w * 0.4f, h * 0.2f, w * 0.4f + stroke, h * 0.8f, paint)
        }
    }
}
