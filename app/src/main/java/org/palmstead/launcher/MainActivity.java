package org.palmstead.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    /** Sizes and colours for the current interface style. All sizes are in real pixels. */
    private static final class Look {
        boolean garnet, lowRes;
        float u = 1f;
        int columns, iconPx, dockIconPx, titleH, dockH, rowH, textPx, titleTextPx, swipePx, scrollW;
        int bg, fg, barBg, barFg, line, pressed;
        Typeface face;
        boolean antiAlias;
    }

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private Look look;
    private String signature;
    private int loadToken;

    private List<AppEntry> apps = new ArrayList<>();
    private String category = AppRepository.HOME;

    private TextView timeView, batteryText, categoryButton;
    private BatteryView batteryView;
    private GridView grid;
    private ScrollIndicator indicator;
    private LinearLayout dockRow;
    private FrameLayout overlayHost;
    private ListAdapter adapter;
    private int batteryPercent = 100;
    private boolean charging;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(i.getAction())) readBattery(i);
            updateClock();
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        rebuildUi();
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        Intent sticky = registerReceiver(receiver, f);
        if (sticky != null) readBattery(sticky);
        updateClock();
    }

    @Override protected void onStop() {
        unregisterReceiver(receiver);
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        if (!prefs.signature().equals(signature)) rebuildUi();
        reloadApps();
    }

    @Override protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_MAIN.equals(intent.getAction())) goHome();
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        rebuildUi();
        refresh();
    }

    @Override @SuppressWarnings("deprecation")
    public void onBackPressed() {
        goHome();
    }

    private void goHome() {
        dismissOverlay();
        if (!AppRepository.HOME.equals(category)) {
            category = AppRepository.HOME;
            refresh();
        }
        if (grid != null) grid.setSelection(0);
    }

    // ---------------------------------------------------------------- data

    private void reloadApps() {
        final int token = ++loadToken;
        io.execute(() -> {
            final List<AppEntry> loaded = AppRepository.load(this, true);
            ui.post(() -> {
                if (token != loadToken || isDestroyed()) return;
                apps = loaded;
                refresh();
            });
        });
    }

    private List<String> orderedKeys() {
        List<String> keys = new ArrayList<>();
        for (AppEntry e : apps) keys.add(e.key);
        return keys;
    }

    private AppEntry find(String key) {
        for (AppEntry e : apps) if (e.key.equals(key)) return e;
        return null;
    }

    private List<String> categories() {
        Set<String> found = new LinkedHashSet<>();
        List<String> sorted = new ArrayList<>();
        boolean unfiled = false;
        for (AppEntry e : apps) {
            if (AppRepository.UNFILED.equals(e.category)) unfiled = true;
            else if (!sorted.contains(e.category)) sorted.add(e.category);
        }
        java.util.Collections.sort(sorted);
        found.add(AppRepository.HOME);
        found.add(AppRepository.ALL);
        found.addAll(sorted);
        if (unfiled) found.add(AppRepository.UNFILED);
        return new ArrayList<>(found);
    }

    private List<AppEntry> visibleEntries() {
        List<AppEntry> out = new ArrayList<>();
        if (AppRepository.HOME.equals(category)) {
            if (prefs.allAppsShortcut()) out.add(specialEntry(AppEntry.ALL_APPS));
            for (String k : LauncherLogic.topUsed(orderedKeys(), prefs.usageCounts(), prefs.homeCount())) {
                AppEntry e = find(k);
                if (e != null) out.add(e);
            }
        } else if (AppRepository.ALL.equals(category)) {
            out.addAll(apps);
        } else {
            for (AppEntry e : apps) if (category.equals(e.category)) out.add(e);
        }
        return out;
    }

    private AppEntry specialEntry(int type) {
        boolean all = type == AppEntry.ALL_APPS;
        AppEntry e = new AppEntry(type, all ? LauncherLogic.SLOT_ALL : LauncherLogic.SLOT_SETTINGS,
                null, all ? "All Apps" : "Settings", AppRepository.HOME);
        e.icon = new GlyphDrawable(type, look.fg, look.antiAlias);
        return e;
    }

    private void refresh() {
        if (grid == null) return;
        categoryButton.setText(category + " \u25BE");
        adapter.set(visibleEntries());
        grid.setSelection(0);
        updateIndicator();
        fillDock();
    }

    // ---------------------------------------------------------------- UI

    private Look computeLook() {
        Look l = new Look();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float ppi = Geometry.ppi(dm.xdpi, dm.ydpi, dm.densityDpi);
        l.garnet = prefs.isGarnet();
        l.lowRes = l.garnet && prefs.isLowRes();
        if (l.garnet) {
            l.u = l.lowRes ? 1f : Geometry.garnetFullUnit(ppi, dm.widthPixels);
            float u = l.u;
            l.columns = l.lowRes ? 3 : Math.max(3, Math.round(dm.widthPixels / u / 107f));
            l.iconPx = Math.round(32 * u);
            l.dockIconPx = Math.round(26 * u);
            l.titleH = Math.round(16 * u);
            l.dockH = Math.round(40 * u);
            l.rowH = Math.round(58 * u);
            l.textPx = Math.round(10 * u);
            l.titleTextPx = Math.round(11 * u);
            l.swipePx = Math.round(20 * u);
            l.scrollW = Math.max(1, Math.round(2 * u));
            l.bg = Color.WHITE; l.fg = Color.BLACK;
            l.barBg = Color.BLACK; l.barFg = Color.WHITE;
            l.line = Color.BLACK; l.pressed = 0xFFC0C0C0;
            l.face = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
            l.antiAlias = false;
        } else {
            float d = dm.density;
            l.columns = 1;
            l.iconPx = Math.round(Geometry.clampedPx(48 * d, ppi, 0.25f, 0.40f));
            l.dockIconPx = Math.round(l.iconPx * 0.9f);
            l.titleH = Math.round(Geometry.clampedPx(48 * d, ppi, 0.25f, 0.40f));
            l.rowH = Math.round(l.iconPx * 1.5f);
            l.dockH = Math.round(l.iconPx * 1.7f);
            l.textPx = Math.round(Geometry.clampedPx(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, dm), ppi, 0.09f, 0.14f));
            l.titleTextPx = Math.round(Geometry.clampedPx(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, dm), ppi, 0.08f, 0.13f));
            l.swipePx = Math.round(0.25f * ppi);
            l.scrollW = Math.max(2, Math.round(3 * d));
            l.bg = 0xFFFAFAFA; l.fg = 0xFF202124;
            l.barBg = 0xFF37474F; l.barFg = Color.WHITE;
            l.line = 0xFFDADCE0; l.pressed = 0xFFE0E0E0;
            l.face = Typeface.create("sans-serif", Typeface.NORMAL);
            l.antiAlias = true;
        }
        return l;
    }

    private void rebuildUi() {
        signature = prefs.signature();
        look = computeLook();
        final Look l = look;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(l.bg);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));

        // Title bar: time and battery on the left, category filter on the right.
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(l.barBg);
        int pad = Math.round(4 * l.u * (l.garnet ? 1 : 3));
        bar.setPadding(pad, 0, pad, 0);
        timeView = text(l.barFg, l.titleTextPx);
        bar.addView(timeView);
        batteryView = new BatteryView(this, l.barFg, l.antiAlias);
        int bh = Math.round(l.titleTextPx * 0.8f);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Math.round(bh * 1.9f), bh);
        blp.setMargins(pad * 2, 0, pad, 0);
        bar.addView(batteryView, blp);
        batteryText = text(l.barFg, l.titleTextPx);
        bar.addView(batteryText);
        View spacer = new View(this);
        bar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        categoryButton = text(l.barFg, l.titleTextPx);
        categoryButton.setGravity(Gravity.CENTER);
        categoryButton.setPadding(pad, 0, pad, 0);
        categoryButton.setOnClickListener(v -> showCategoryMenu());
        bar.addView(categoryButton, new LinearLayout.LayoutParams(-2, -1));
        bar.setOnLongClickListener(v -> { openSettings(); return true; });
        column.addView(bar, new LinearLayout.LayoutParams(-1, l.titleH));

        // List with a thin position indicator on the right.
        FrameLayout body = new FrameLayout(this);
        grid = new GridView(this);
        grid.setNumColumns(l.columns);
        grid.setVerticalScrollBarEnabled(false);
        grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        adapter = new ListAdapter();
        grid.setAdapter(adapter);
        grid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView v, int s) {}
            @Override public void onScroll(AbsListView v, int f, int c, int t) { updateIndicator(); }
        });
        grid.addOnLayoutChangeListener((v, a, b, c, d, e, f, g, h) -> updateIndicator());
        body.addView(grid, new FrameLayout.LayoutParams(-1, -1));
        indicator = new ScrollIndicator(this, l.garnet ? l.fg : 0xFF9E9E9E, l.antiAlias);
        body.addView(indicator, new FrameLayout.LayoutParams(l.scrollW, -1, Gravity.END));
        column.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Bottom button row.
        View rule = new View(this);
        rule.setBackgroundColor(l.line);
        column.addView(rule, new LinearLayout.LayoutParams(-1, Math.max(1, Math.round(l.u))));
        SwipeUpLayout dock = new SwipeUpLayout(this, l.swipePx);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setBackgroundColor(l.bg);
        dock.setListener(() -> {
            if (prefs.allAppsGesture()) showCategory(AppRepository.ALL);
        });
        dockRow = dock;
        column.addView(dock, new LinearLayout.LayoutParams(-1, l.dockH));

        overlayHost = root;

        View content = root;
        if (l.lowRes) {
            PixelScaleLayout scaled = new PixelScaleLayout(this);
            scaled.addView(root, new ViewGroup.LayoutParams(Geometry.LOW_W, Geometry.LOW_H));
            content = scaled;
        } else {
            content.setFitsSystemWindows(true);
        }
        setContentView(content);
        getWindow().setStatusBarColor(l.lowRes ? Color.BLACK : l.barBg);
        getWindow().setNavigationBarColor(l.lowRes ? Color.BLACK : l.bg);
        updateClock();
        refresh();
    }

    private TextView text(int color, int px) {
        TextView t = new TextView(this);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTypeface(look.face);
        t.setSingleLine(true);
        t.setIncludeFontPadding(false);
        if (!look.antiAlias) t.setPaintFlags(t.getPaintFlags() & ~Paint.ANTI_ALIAS_FLAG);
        return t;
    }

    private Drawable pressedBackground() {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(look.pressed));
        s.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        return s;
    }

    private void updateClock() {
        if (timeView == null) return;
        timeView.setText(DateFormat.getTimeFormat(this).format(new Date()));
        batteryView.setLevel(batteryPercent, charging);
        batteryText.setText(batteryPercent + "%");
    }

    private void readBattery(Intent i) {
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (level >= 0 && scale > 0) batteryPercent = Math.round(level * 100f / scale);
        int st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL;
    }

    private void updateIndicator() {
        if (grid == null || indicator == null) return;
        int total = adapter.getCount();
        int cols = Math.max(1, look.columns);
        View first = grid.getChildAt(0);
        if (total == 0 || first == null || first.getHeight() == 0) {
            indicator.setThumb(0f, 1f);
            return;
        }
        float rows = (float) Math.ceil(total / (double) cols);
        float visible = grid.getHeight() / (float) first.getHeight();
        if (visible >= rows) {
            indicator.setThumb(0f, 1f);
            return;
        }
        float topRow = grid.getFirstVisiblePosition() / cols - first.getTop() / (float) first.getHeight();
        indicator.setThumb(topRow / (rows - visible), visible / rows);
    }

    // ---------------------------------------------------------------- dock

    private void fillDock() {
        if (dockRow == null) return;
        dockRow.removeAllViews();
        Map<String, Integer> usage = prefs.usageCounts();
        Set<String> installed = new LinkedHashSet<>(orderedKeys());
        List<String> resolved = LauncherLogic.resolveDock(
                prefs.slots(), LauncherLogic.rank(orderedKeys(), usage), installed);
        for (String slot : resolved) {
            ImageView v = new ImageView(this);
            v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int p = Math.round((look.dockH - look.dockIconPx) / 2f);
            v.setPadding(p, p, p, p);
            v.setBackground(pressedBackground());
            AppEntry entry = null;
            if (LauncherLogic.SLOT_ALL.equals(slot)) entry = specialEntry(AppEntry.ALL_APPS);
            else if (LauncherLogic.SLOT_SETTINGS.equals(slot)) entry = specialEntry(AppEntry.SETTINGS);
            else if (LauncherLogic.isApp(slot)) entry = find(slot);
            if (entry != null) {
                v.setImageDrawable(entry.icon);
                v.setContentDescription(entry.label);
                final AppEntry target = entry;
                v.setOnClickListener(x -> activate(target));
            }
            dockRow.addView(v, new LinearLayout.LayoutParams(0, -1, 1f));
        }
    }

    // ---------------------------------------------------------------- actions

    private void activate(AppEntry e) {
        dismissOverlay();
        if (e.type == AppEntry.ALL_APPS) {
            showCategory(AppRepository.ALL);
        } else if (e.type == AppEntry.SETTINGS) {
            openSettings();
        } else {
            launch(e);
        }
    }

    private void showCategory(String c) {
        category = c;
        refresh();
    }

    private void launch(AppEntry e) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(e.component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
            prefs.recordLaunch(e.key);
        } catch (RuntimeException ex) {
            reloadApps();
        }
    }

    private void showAppInfo(AppEntry e) {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", e.component.getPackageName(), null)));
        } catch (RuntimeException ignored) {
        }
    }

    private void openSettings() {
        dismissOverlay();
        startActivity(new Intent(this, SettingsActivity.class));
    }

    // ---------------------------------------------------------------- category menu

    private void dismissOverlay() {
        if (overlayHost == null) return;
        View o = overlayHost.findViewWithTag("overlay");
        if (o != null) overlayHost.removeView(o);
    }

    private void showCategoryMenu() {
        if (overlayHost.findViewWithTag("overlay") != null) {
            dismissOverlay();
            return;
        }
        FrameLayout scrim = new FrameLayout(this);
        scrim.setTag("overlay");
        scrim.setOnClickListener(v -> dismissOverlay());

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable border = new GradientDrawable();
        border.setColor(look.bg);
        border.setStroke(Math.max(1, Math.round(look.u)), look.line);
        menu.setBackground(border);
        List<String> items = new ArrayList<>(categories());
        items.add("Settings\u2026");
        for (final String name : items) {
            TextView t = text(look.fg, look.textPx);
            t.setGravity(Gravity.CENTER_VERTICAL);
            int p = Math.round(look.titleH * 0.4f);
            t.setPadding(p * 2, 0, p * 2, 0);
            t.setText(name.equals(category) ? "\u2022 " + name : name);
            t.setBackground(pressedBackground());
            t.setOnClickListener(v -> {
                dismissOverlay();
                if (name.startsWith("Settings")) openSettings();
                else showCategory(name);
            });
            menu.addView(t, new LinearLayout.LayoutParams(-1, Math.round(look.titleH * 1.1f)));
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(menu);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        lp.topMargin = look.titleH;
        scrim.addView(sv, lp);
        overlayHost.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
    }

    // ---------------------------------------------------------------- adapter

    private final class ListAdapter extends BaseAdapter {
        private List<AppEntry> items = new ArrayList<>();

        void set(List<AppEntry> list) {
            items = list;
            notifyDataSetChanged();
        }

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int i) { return items.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override public View getView(int pos, View convert, ViewGroup parent) {
            final AppEntry e = items.get(pos);
            LinearLayout cell = new LinearLayout(MainActivity.this);
            ImageView icon = new ImageView(MainActivity.this);
            icon.setImageDrawable(e.icon);
            TextView label = text(look.fg, look.textPx);
            label.setText(e.label);
            label.setEllipsize(TextUtils.TruncateAt.END);
            if (look.garnet) {
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                int p = Math.round(4 * look.u);
                cell.setPadding(p / 2, p, p / 2, 0);
                label.setGravity(Gravity.CENTER);
                cell.addView(icon, new LinearLayout.LayoutParams(look.iconPx, look.iconPx));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = p;
                cell.addView(label, lp);
            } else {
                cell.setOrientation(LinearLayout.HORIZONTAL);
                cell.setGravity(Gravity.CENTER_VERTICAL);
                int p = Math.round(look.iconPx * 0.4f);
                cell.setPadding(p, 0, p + look.scrollW, 0);
                cell.addView(icon, new LinearLayout.LayoutParams(look.iconPx, look.iconPx));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
                lp.leftMargin = p;
                cell.addView(label, lp);
            }
            cell.setLayoutParams(new AbsListView.LayoutParams(-1, look.rowH));
            cell.setBackground(pressedBackground());
            cell.setOnClickListener(v -> activate(e));
            if (e.type == AppEntry.APP) {
                cell.setOnLongClickListener(v -> { showAppInfo(e); return true; });
            }
            return cell;
        }
    }
}
