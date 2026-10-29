package org.palmstead.launcher

import android.content.Context

internal class Prefs(c: Context) {
    private val app = c.applicationContext
    private val p = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val usage = app.getSharedPreferences("usage", Context.MODE_PRIVATE)

    var isGarnet: Boolean
        get() = p.getString("style", "garnet") == "garnet"
        set(v) = p.edit().putString("style", if (v) "garnet" else "modern").apply()

    /** True: emulate a 320x480 screen. False: use the full display resolution. */
    var isLowRes: Boolean
        get() = p.getBoolean("lowres", true)
        set(v) = p.edit().putBoolean("lowres", v).apply()

    var homeCount: Int
        get() = p.getInt("home_count", 12)
        set(v) = p.edit().putInt("home_count", v).apply()

    var dockCount: Int
        get() = p.getInt("dock_count", 5).coerceIn(MIN_DOCK, MAX_DOCK)
        set(v) = p.edit().putInt("dock_count", v).apply()

    fun slot(i: Int): String =
        p.getString("slot$i", if (i == 0) LauncherLogic.SLOT_ALL else LauncherLogic.SLOT_AUTO)!!

    fun setSlot(i: Int, v: String) = p.edit().putString("slot$i", v).apply()

    fun slots(): List<String> = (0 until dockCount).map { slot(it) }

    var allAppsShortcut: Boolean
        get() = p.getBoolean("all_shortcut", true)
        set(v) = p.edit().putBoolean("all_shortcut", v).apply()

    var allAppsGesture: Boolean
        get() = p.getBoolean("all_gesture", true)
        set(v) = p.edit().putBoolean("all_gesture", v).apply()

    /** Changes whenever a setting that affects the layout changes. */
    fun signature(): String = p.all.toString()

    fun usageCounts(): Map<String, Int> =
        usage.all.mapNotNull { (k, v) -> (v as? Int)?.let { k to it } }.toMap()

    fun recordLaunch(key: String) = usage.edit().putInt(key, usage.getInt(key, 0) + 1).apply()

    companion object {
        const val MAX_DOCK = 6
        const val MIN_DOCK = 3
    }
}
