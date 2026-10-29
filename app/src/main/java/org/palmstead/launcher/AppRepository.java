package org.palmstead.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AppRepository {
    static final String HOME = "Home";
    static final String ALL = "All";
    static final String UNFILED = "Unfiled";

    private AppRepository() {}

    /** Every launchable app except this launcher, sorted by label. */
    static List<AppEntry> load(Context c, boolean withIcons) {
        PackageManager pm = c.getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<AppEntry> out = new ArrayList<>();
        for (ResolveInfo ri : pm.queryIntentActivities(i, 0)) {
            if (c.getPackageName().equals(ri.activityInfo.packageName)) continue;
            ComponentName cn = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            AppEntry e = new AppEntry(AppEntry.APP, cn.flattenToString(), cn,
                    String.valueOf(ri.loadLabel(pm)), categoryOf(ri.activityInfo.applicationInfo));
            if (withIcons) e.icon = ri.loadIcon(pm);
            out.add(e);
        }
        out.sort((a, b) -> a.label.toLowerCase(Locale.ROOT).compareTo(b.label.toLowerCase(Locale.ROOT)));
        return out;
    }

    static String categoryOf(ApplicationInfo ai) {
        if ((ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0) return "Games";
        switch (ai.category) {
            case ApplicationInfo.CATEGORY_GAME: return "Games";
            case ApplicationInfo.CATEGORY_AUDIO:
            case ApplicationInfo.CATEGORY_VIDEO:
            case ApplicationInfo.CATEGORY_IMAGE: return "Media";
            case ApplicationInfo.CATEGORY_SOCIAL: return "Social";
            case ApplicationInfo.CATEGORY_PRODUCTIVITY: return "Productivity";
            case ApplicationInfo.CATEGORY_NEWS:
            case ApplicationInfo.CATEGORY_MAPS:
            case ApplicationInfo.CATEGORY_ACCESSIBILITY: return "Tools";
            default: return UNFILED;
        }
    }
}
