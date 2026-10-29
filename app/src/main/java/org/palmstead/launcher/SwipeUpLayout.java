package org.palmstead.launcher;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/** Horizontal row that reports an upward swipe without breaking taps on its children. */
final class SwipeUpLayout extends LinearLayout {
    interface Listener { void onSwipeUp(); }

    private final float threshold;
    private Listener listener;
    private float downY;
    private boolean swiping;

    SwipeUpLayout(Context c, float thresholdPx) {
        super(c);
        threshold = thresholdPx;
    }

    void setListener(Listener l) { listener = l; }

    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
        if (listener == null) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = e.getY();
                swiping = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (downY - e.getY() > threshold) {
                    swiping = true;
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (swiping) {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_UP) {
                swiping = false;
                if (listener != null) listener.onSwipeUp();
            } else if (a == MotionEvent.ACTION_CANCEL) {
                swiping = false;
            }
            return true;
        }
        return super.onTouchEvent(e);
    }
}
