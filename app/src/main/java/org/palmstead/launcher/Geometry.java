package org.palmstead.launcher;

/** Pure sizing maths, kept free of Android classes so it can be unit tested. */
final class Geometry {
    static final int LOW_W = 320;
    static final int LOW_H = 480;

    private Geometry() {}

    static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Scale factor that fits the emulated 320x480 screen inside a w x h area. */
    static float lowResScale(int w, int h) {
        if (w <= 0 || h <= 0) return 1f;
        return Math.min(w / (float) LOW_W, h / (float) LOW_H);
    }

    /** Physical pixels per inch, preferring the reported xdpi/ydpi and falling back to the density. */
    static float ppi(float xdpi, float ydpi, int densityDpi) {
        boolean okX = xdpi >= 80 && xdpi <= 1000;
        boolean okY = ydpi >= 80 && ydpi <= 1000;
        if (okX && okY) return (xdpi + ydpi) / 2f;
        if (okX) return xdpi;
        if (okY) return ydpi;
        return Math.max(densityDpi, 120);
    }

    /**
     * Limits a natural pixel size to a physical range expressed in inches, so that
     * UI elements stay usable on any panel regardless of density or display-size settings.
     */
    static float clampedPx(float naturalPx, float ppi, float minInches, float maxInches) {
        return clamp(naturalPx, minInches * ppi, maxInches * ppi);
    }

    /** Pixels per Garnet logical pixel when using the full display resolution. */
    static float garnetFullUnit(float ppi, int widthPx) {
        float u = clamp(ppi / 180f, 1f, 8f);
        if (widthPx > 0) u = Math.min(u, widthPx / (float) LOW_W);
        return Math.max(1f, u);
    }
}
