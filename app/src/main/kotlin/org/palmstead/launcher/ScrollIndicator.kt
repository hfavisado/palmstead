package org.palmstead.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import kotlin.math.max

/** Thin, non-interactive bar showing where the visible window sits within the list. */
internal class ScrollIndicator(c: Context, color: Int, antiAlias: Boolean) : View(c) {
    private val paint = Paint().apply {
        this.color = color
        isAntiAlias = antiAlias
    }
    private var start = 0f
    private var size = 1f

    init {
        isClickable = false
        isFocusable = false
    }

    /** [start] is the fraction of the track where the thumb begins; [size] is its length fraction. */
    fun setThumb(start: Float, size: Float) {
        this.start = start
        this.size = size
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        if (size >= 1f) return
        val h = height.toFloat()
        val len = max(width * 4f, h * size)
        val top = (h - len) * start.coerceIn(0f, 1f)
        c.drawRect(0f, top, width.toFloat(), top + len, paint)
    }
}
