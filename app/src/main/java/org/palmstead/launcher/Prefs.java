package org.palmstead.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Prefs {
    static final int MAX_DOCK = 6;
    static final int MIN_DOCK = 3;

    private final SharedPreferences p;
    private final SharedPreferences usage;

    Prefs(Context c) {
        Context app = c.getApplicationContext();
        p = app.getSharedPreferences("settings", Context.MODE_PRIVATE);
        usage = app.getSharedPreferences("usage", Context.MODE_PRIVATE);
    }

    boolean isGarnet() { return "garnet".equals(p.getString("style", "garnet")); }
    void setGarnet(boolean v) { p.edit().putString("style", v ? "garnet" : "modern").apply(); }

    /** True: emulate a 320x480 screen. False: use the full display resolution. */
    boolean isLowRes() { return p.getBoolean("lowres", true); }
    void setLowRes(boolean v) { p.edit().putBoolean("lowres", v).apply(); }

    int homeCount() { return p.getInt("home_count", 12); }
    void setHomeCount(int n) { p.edit().putInt("home_count", n).apply(); }

    int dockCount() { return Math.max(MIN_DOCK, Math.min(MAX_DOCK, p.getInt("dock_count", 5))); }
    void setDockCount(int n) { p.edit().putInt("dock_count", n).apply(); }

    String slot(int i) {
        return p.getString("slot" + i, i == 0 ? LauncherLogic.SLOT_ALL : LauncherLogic.SLOT_AUTO);
    }
    void setSlot(int i, String v) { p.edit().putString("slot" + i, v).apply(); }

    List<String> slots() {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < dockCount(); i++) l.add(slot(i));
        return l;
    }

    boolean allAppsShortcut() { return p.getBoolean("all_shortcut", true); }
    void setAllAppsShortcut(boolean v) { p.edit().putBoolean("all_shortcut", v).apply(); }

    boolean allAppsGesture() { return p.getBoolean("all_gesture", true); }
    void setAllAppsGesture(boolean v) { p.edit().putBoolean("all_gesture", v).apply(); }

    /** Changes whenever a setting that affects the layout changes. */
    String signature() { return String.valueOf(p.getAll()); }

    Map<String, Integer> usageCounts() {
        Map<String, Integer> m = new HashMap<>();
        for (Map.Entry<String, ?> e : usage.getAll().entrySet()) {
            if (e.getValue() instanceof Integer) m.put(e.getKey(), (Integer) e.getValue());
        }
        return m;
    }

    void recordLaunch(String key) {
        usage.edit().putInt(key, usage.getInt(key, 0) + 1).apply();
    }
}
