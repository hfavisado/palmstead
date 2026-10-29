package org.palmstead.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Simple vector glyphs for the built-in "All Apps" and "Settings" buttons. */
final class GlyphDrawable extends Drawable {
    private final int type;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    GlyphDrawable(int type, int color, boolean antiAlias) {
        this.type = type;
        paint.setColor(color);
        paint.setAntiAlias(antiAlias);
    }

    @Override public void draw(Canvas c) {
        float w = getBounds().width(), h = getBounds().height();
        float x0 = getBounds().left, y0 = getBounds().top;
        if (type == AppEntry.ALL_APPS) {
            float cell = w / 4f;
            for (int r = 0; r < 3; r++) {
                for (int col = 0; col < 3; col++) {
                    float x = x0 + w * 0.1f + col * (cell * 1.1f) + cell * 0.1f;
                    float y = y0 + h * 0.1f + r * (cell * 1.1f) + cell * 0.1f;
                    c.drawRoundRect(new RectF(x, y, x + cell * 0.9f, y + cell * 0.9f), cell * 0.2f, cell * 0.2f, paint);
                }
            }
        } else {
            for (int i = 0; i < 3; i++) {
                float y = y0 + h * (0.2f + i * 0.3f);
                c.drawRect(x0 + w * 0.1f, y, x0 + w * 0.9f, y + h * 0.08f, paint);
                float kx = x0 + w * (0.25f + ((i * 2) % 3) * 0.25f);
                c.drawCircle(kx, y + h * 0.04f, h * 0.1f, paint);
            }
        }
    }

    @Override public void setAlpha(int a) { paint.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter f) { paint.setColorFilter(f); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
