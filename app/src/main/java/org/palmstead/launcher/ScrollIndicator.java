package org.palmstead.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Thin, non-interactive bar showing where the visible window sits within the list. */
final class ScrollIndicator extends View {
    private final Paint paint = new Paint();
    private float start;
    private float size = 1f;

    ScrollIndicator(Context c, int color, boolean antiAlias) {
        super(c);
        paint.setColor(color);
        paint.setAntiAlias(antiAlias);
        setClickable(false);
        setFocusable(false);
    }

    /** @param start fraction [0,1] of the track where the thumb begins; @param size thumb length fraction. */
    void setThumb(float start, float size) {
        this.start = start;
        this.size = size;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        if (size >= 1f) return;
        float h = getHeight();
        float len = Math.max(getWidth() * 4f, h * size);
        float top = (h - len) * Math.max(0f, Math.min(1f, start));
        c.drawRect(0, top, getWidth(), top + len, paint);
    }
}
