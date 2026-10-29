package org.palmstead.launcher

import android.content.Context
import android.view.MotionEvent
import android.widget.LinearLayout

/** Horizontal row that reports an upward swipe without breaking taps on its children. */
internal class SwipeUpLayout(c: Context, private val threshold: Float) : LinearLayout(c) {
    var onSwipeUp: (() -> Unit)? = null
    private var downY = 0f
    private var swiping = false

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        if (onSwipeUp == null) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = e.y
                swiping = false
            }
            MotionEvent.ACTION_MOVE -> if (downY - e.y > threshold) {
                swiping = true
                return true
            }
        }
        return false
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (swiping) {
            when (e.actionMasked) {
                MotionEvent.ACTION_UP -> {
                    swiping = false
                    onSwipeUp?.invoke()
                }
                MotionEvent.ACTION_CANCEL -> swiping = false
            }
            return true
        }
        return super.onTouchEvent(e)
    }
}
