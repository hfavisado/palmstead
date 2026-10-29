package org.palmstead.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Hosts a single child laid out at exactly 320x480 pixels, renders it into a bitmap of that size
 * and blows the bitmap up with nearest-neighbour sampling, letterboxed in the available space.
 * Touch events are mapped back into the child's coordinate space.
 */
final class PixelScaleLayout extends ViewGroup {
    private final Paint paint = new Paint();
    private Bitmap buffer;
    private Canvas bufferCanvas;
    private float scale = 1f;
    private int offX, offY;

    PixelScaleLayout(Context c) {
        super(c);
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        setWillNotDraw(false);
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec), h = MeasureSpec.getSize(hSpec);
        setMeasuredDimension(w, h);
        scale = Geometry.lowResScale(w, h);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(Geometry.LOW_W, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(Geometry.LOW_H, MeasureSpec.EXACTLY));
        }
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        offX = Math.round((getWidth() - Geometry.LOW_W * scale) / 2f);
        offY = Math.round((getHeight() - Geometry.LOW_H * scale) / 2f);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).layout(0, 0, Geometry.LOW_W, Geometry.LOW_H);
        }
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        if (buffer == null) {
            buffer = Bitmap.createBitmap(Geometry.LOW_W, Geometry.LOW_H, Bitmap.Config.ARGB_8888);
            bufferCanvas = new Canvas(buffer);
        }
        buffer.eraseColor(Color.TRANSPARENT);
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == VISIBLE) v.draw(bufferCanvas);
        }
        canvas.save();
        canvas.translate(offX, offY);
        canvas.scale(scale, scale);
        canvas.drawBitmap(buffer, 0, 0, paint);
        canvas.restore();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        if (getChildCount() == 0) return false;
        MotionEvent copy = MotionEvent.obtain(ev);
        Matrix m = new Matrix();
        m.setTranslate(-offX, -offY);
        m.postScale(1f / scale, 1f / scale);
        copy.transform(m);
        boolean handled = getChildAt(0).dispatchTouchEvent(copy);
        copy.recycle();
        return handled;
    }
}
