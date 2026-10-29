package org.palmstead.launcher;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

final class AppEntry {
    static final int APP = 0;
    static final int ALL_APPS = 1;
    static final int SETTINGS = 2;

    final int type;
    final String key;
    final ComponentName component;
    final String label;
    final String category;
    Drawable icon;

    AppEntry(int type, String key, ComponentName component, String label, String category) {
        this.type = type;
        this.key = key;
        this.component = component;
        this.label = label;
        this.category = category;
    }
}
