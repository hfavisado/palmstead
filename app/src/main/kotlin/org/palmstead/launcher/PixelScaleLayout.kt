package org.palmstead.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.view.MotionEvent
import android.view.ViewGroup
import kotlin.math.roundToInt

/**
 * Hosts a single child laid out at exactly 320x480 pixels, renders it into a bitmap of that size
 * and blows the bitmap up with nearest-neighbour sampling, letterboxed in the available space.
 * Touch events are mapped back into the child's coordinate space.
 */
internal class PixelScaleLayout(c: Context) : ViewGroup(c) {
    private val paint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
    }
    private var buffer: Bitmap? = null
    private var bufferCanvas: Canvas? = null
    private var scale = 1f
    private var offX = 0
    private var offY = 0

    init {
        setWillNotDraw(false)
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        val h = MeasureSpec.getSize(hSpec)
        setMeasuredDimension(w, h)
        scale = Geometry.lowResScale(w, h)
        for (i in 0 until childCount) {
            getChildAt(i).measure(
                MeasureSpec.makeMeasureSpec(Geometry.LOW_W, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Geometry.LOW_H, MeasureSpec.EXACTLY),
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        offX = ((width - Geometry.LOW_W * scale) / 2f).roundToInt()
        offY = ((height - Geometry.LOW_H * scale) / 2f).roundToInt()
        for (i in 0 until childCount) getChildAt(i).layout(0, 0, Geometry.LOW_W, Geometry.LOW_H)
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        val buf = buffer ?: Bitmap.createBitmap(Geometry.LOW_W, Geometry.LOW_H, Bitmap.Config.ARGB_8888)
            .also { buffer = it; bufferCanvas = Canvas(it) }
        buf.eraseColor(Color.TRANSPARENT)
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            if (v.visibility == VISIBLE) v.draw(bufferCanvas!!)
        }
        canvas.save()
        canvas.translate(offX.toFloat(), offY.toFloat())
        canvas.scale(scale, scale)
        canvas.drawBitmap(buf, 0f, 0f, paint)
        canvas.restore()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (childCount == 0) return false
        val copy = MotionEvent.obtain(ev)
        val m = Matrix().apply {
            setTranslate(-offX.toFloat(), -offY.toFloat())
            postScale(1f / scale, 1f / scale)
        }
        copy.transform(m)
        val handled = getChildAt(0).dispatchTouchEvent(copy)
        copy.recycle()
        return handled
    }
}
