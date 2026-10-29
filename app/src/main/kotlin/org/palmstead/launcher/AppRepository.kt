package org.palmstead.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import java.util.Locale

internal object AppRepository {
    const val HOME = "Home"
    const val ALL = "All"
    const val UNFILED = "Unfiled"

    @Suppress("DEPRECATION")
    /** Every launchable app except this launcher, sorted by label. */
    fun load(c: Context, withIcons: Boolean): List<AppEntry> {
        val pm = c.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != c.packageName }
            .map { ri ->
                val cn = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
                AppEntry(
                    AppEntry.APP, cn.flattenToString(), cn,
                    ri.loadLabel(pm).toString(), categoryOf(ri.activityInfo.applicationInfo),
                    if (withIcons) ri.loadIcon(pm) else null,
                )
            }
            .sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    fun categoryOf(ai: ApplicationInfo): String = when (ai.category) {
        ApplicationInfo.CATEGORY_GAME -> "Games"
        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO,
        ApplicationInfo.CATEGORY_IMAGE -> "Media"
        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
        ApplicationInfo.CATEGORY_NEWS, ApplicationInfo.CATEGORY_MAPS,
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Tools"
        else -> UNFILED
    }
}
