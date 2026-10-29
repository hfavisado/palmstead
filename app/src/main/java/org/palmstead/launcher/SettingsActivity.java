package org.palmstead.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {
    private static final int[] HOME_COUNTS = {6, 9, 12, 18, 24, 36};

    private Prefs prefs;
    private LinearLayout content;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        setTitle(R.string.settings_title);
        ScrollView sv = new ScrollView(this);
        sv.setFitsSystemWindows(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int p = Math.round(16 * getResources().getDisplayMetrics().density);
        content.setPadding(p, p, p, p);
        sv.addView(content);
        setContentView(sv);
        build();
    }

    private void build() {
        content.removeAllViews();

        header("Interface");
        choice("Style", prefs.isGarnet() ? "PalmOS Garnet" : "Modern",
                new String[]{"PalmOS Garnet", "Modern"}, prefs.isGarnet() ? 0 : 1,
                i -> prefs.setGarnet(i == 0));
        if (prefs.isGarnet()) {
            toggle("Emulate 320\u00D7480 screen (off: use full display resolution)",
                    prefs.isLowRes(), prefs::setLowRes);
        }
        String[] counts = new String[HOME_COUNTS.length];
        int cur = 0;
        for (int i = 0; i < counts.length; i++) {
            counts[i] = String.valueOf(HOME_COUNTS[i]);
            if (HOME_COUNTS[i] == prefs.homeCount()) cur = i;
        }
        choice("Apps shown on Home (most used)", String.valueOf(prefs.homeCount()), counts, cur,
                i -> prefs.setHomeCount(HOME_COUNTS[i]));

        header("Bottom row");
        String[] dockSizes = new String[Prefs.MAX_DOCK - Prefs.MIN_DOCK + 1];
        for (int i = 0; i < dockSizes.length; i++) dockSizes[i] = String.valueOf(Prefs.MIN_DOCK + i);
        choice("Number of buttons", String.valueOf(prefs.dockCount()), dockSizes,
                prefs.dockCount() - Prefs.MIN_DOCK, i -> prefs.setDockCount(Prefs.MIN_DOCK + i));
        for (int i = 0; i < prefs.dockCount(); i++) {
            final int slot = i;
            row("Button " + (i + 1), describeSlot(prefs.slot(i)), v -> chooseSlot(slot));
        }

        header("All Apps");
        toggle("Show an \u201CAll Apps\u201D shortcut in the Home list", prefs.allAppsShortcut(),
                prefs::setAllAppsShortcut);
        toggle("Swipe up on the bottom row to show all apps", prefs.allAppsGesture(),
                prefs::setAllAppsGesture);
        note("A bottom-row button can also open All Apps (choose \u201CAll Apps\u201D for a button), "
                + "and the category menu always offers \u201CAll\u201D.");
    }

    private String describeSlot(String slot) {
        switch (slot) {
            case LauncherLogic.SLOT_AUTO: return "Most used app";
            case LauncherLogic.SLOT_ALL: return "All Apps";
            case LauncherLogic.SLOT_SETTINGS: return "Settings";
            case LauncherLogic.SLOT_NONE: return "Empty";
            default:
                for (AppEntry e : AppRepository.load(this, false)) if (e.key.equals(slot)) return e.label;
                return "Empty";
        }
    }

    private void chooseSlot(final int slot) {
        final List<String> values = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        values.add(LauncherLogic.SLOT_AUTO); labels.add("Most used app");
        values.add(LauncherLogic.SLOT_ALL); labels.add("All Apps");
        values.add(LauncherLogic.SLOT_SETTINGS); labels.add("Settings");
        values.add(LauncherLogic.SLOT_NONE); labels.add("Empty");
        for (AppEntry e : AppRepository.load(this, false)) {
            values.add(e.key);
            labels.add(e.label);
        }
        new AlertDialog.Builder(this)
                .setTitle("Button " + (slot + 1))
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    prefs.setSlot(slot, values.get(which));
                    build();
                })
                .show();
    }

    // ---------------------------------------------------------------- widgets

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void header(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(14);
        t.setAllCaps(true);
        t.setPadding(0, dp(20), 0, dp(4));
        content.addView(t);
    }

    private void note(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setPadding(0, dp(8), 0, 0);
        content.addView(t);
    }

    private void toggle(String label, boolean value, java.util.function.Consumer<Boolean> set) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setChecked(value);
        s.setPadding(0, dp(8), 0, dp(8));
        s.setOnCheckedChangeListener((CompoundButton b, boolean on) -> {
            set.accept(on);
            build();
        });
        content.addView(s);
    }

    private void row(String label, String value, View.OnClickListener click) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
        b.setText(label + ": " + value);
        b.setOnClickListener(click);
        content.addView(b);
    }

    private void choice(String label, String value, String[] options, int checked,
                        java.util.function.IntConsumer set) {
        row(label, value, v -> new AlertDialog.Builder(this)
                .setTitle(label)
                .setSingleChoiceItems(options, checked, (d, which) -> {
                    set.accept(which);
                    d.dismiss();
                    build();
                })
                .show());
    }
}
