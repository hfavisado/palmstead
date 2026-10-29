package org.palmstead.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Small battery gauge: an outlined cell with a fill proportional to the charge level. */
final class BatteryView extends View {
    private final Paint paint = new Paint();
    private int percent = 100;
    private boolean charging;
    private int color;

    BatteryView(Context c, int color, boolean antiAlias) {
        super(c);
        this.color = color;
        paint.setAntiAlias(antiAlias);
    }

    void setLevel(int percent, boolean charging) {
        this.percent = Math.max(0, Math.min(100, percent));
        this.charging = charging;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float stroke = Math.max(1f, h / 8f);
        float tip = Math.max(2f, w / 8f);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        c.drawRect(stroke / 2, stroke / 2, w - tip - stroke / 2, h - stroke / 2, paint);
        paint.setStyle(Paint.Style.FILL);
        c.drawRect(w - tip, h * 0.3f, w, h * 0.7f, paint);
        float inner = stroke * 2f;
        float fillW = (w - tip - inner * 2f) * percent / 100f;
        c.drawRect(inner, inner, inner + fillW, h - inner, paint);
        if (charging) {
            paint.setColor((~color & 0x00FFFFFF) | 0xFF000000);
            c.drawRect(w * 0.4f, h * 0.2f, w * 0.4f + stroke, h * 0.8f, paint);
        }
    }
}
