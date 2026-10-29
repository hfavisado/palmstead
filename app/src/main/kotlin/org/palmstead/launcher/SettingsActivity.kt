package org.palmstead.launcher

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import kotlin.math.roundToInt

class SettingsActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var content: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = Prefs(this)
        setTitle(R.string.settings_title)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(16)
            setPadding(p, p, p, p)
        }
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(content)
        })
        build()
    }

    private fun build() {
        content.removeAllViews()

        header("Interface")
        choice(
            "Style", if (prefs.isGarnet) "PalmOS Garnet" else "Modern",
            listOf("PalmOS Garnet", "Modern"), if (prefs.isGarnet) 0 else 1,
        ) { prefs.isGarnet = it == 0 }
        if (prefs.isGarnet) {
            toggle("Emulate 320\u00D7480 screen (off: use full display resolution)", prefs.isLowRes) {
                prefs.isLowRes = it
            }
        }
        choice(
            "Apps shown on Home (most used)", prefs.homeCount.toString(),
            HOME_COUNTS.map { it.toString() }, HOME_COUNTS.indexOf(prefs.homeCount).coerceAtLeast(0),
        ) { prefs.homeCount = HOME_COUNTS[it] }

        header("Bottom row")
        choice(
            "Number of buttons", prefs.dockCount.toString(),
            (Prefs.MIN_DOCK..Prefs.MAX_DOCK).map { it.toString() }, prefs.dockCount - Prefs.MIN_DOCK,
        ) { prefs.dockCount = Prefs.MIN_DOCK + it }
        for (i in 0 until prefs.dockCount) {
            row("Button ${i + 1}", describeSlot(prefs.slot(i))) { chooseSlot(i) }
        }

        header("All Apps")
        toggle("Show an \u201CAll Apps\u201D shortcut in the Home list", prefs.allAppsShortcut) {
            prefs.allAppsShortcut = it
        }
        toggle("Swipe up on the bottom row to show all apps", prefs.allAppsGesture) {
            prefs.allAppsGesture = it
        }
        note(
            "A bottom-row button can also open All Apps (choose \u201CAll Apps\u201D for a button), " +
                "and the category menu always offers \u201CAll\u201D.",
        )
    }

    private fun describeSlot(slot: String): String = when (slot) {
        LauncherLogic.SLOT_AUTO -> "Most used app"
        LauncherLogic.SLOT_ALL -> "All Apps"
        LauncherLogic.SLOT_SETTINGS -> "Settings"
        LauncherLogic.SLOT_NONE -> "Empty"
        else -> AppRepository.load(this, false).firstOrNull { it.key == slot }?.label ?: "Empty"
    }

    private fun chooseSlot(slot: Int) {
        val options = mutableListOf(
            LauncherLogic.SLOT_AUTO to "Most used app",
            LauncherLogic.SLOT_ALL to "All Apps",
            LauncherLogic.SLOT_SETTINGS to "Settings",
            LauncherLogic.SLOT_NONE to "Empty",
        )
        AppRepository.load(this, false).mapTo(options) { it.key to it.label }
        AlertDialog.Builder(this)
            .setTitle("Button ${slot + 1}")
            .setItems(options.map { it.second }.toTypedArray()) { _, which ->
                prefs.setSlot(slot, options[which].first)
                build()
            }
            .show()
    }

    // ---------------------------------------------------------------- widgets

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private fun header(s: String) {
        content.addView(TextView(this).apply {
            text = s
            textSize = 14f
            isAllCaps = true
            setPadding(0, dp(20), 0, dp(4))
        })
    }

    private fun note(s: String) {
        content.addView(TextView(this).apply {
            text = s
            textSize = 12f
            setPadding(0, dp(8), 0, 0)
        })
    }

    private fun toggle(label: String, value: Boolean, set: (Boolean) -> Unit) {
        content.addView(Switch(this).apply {
            text = label
            isChecked = value
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, on ->
                set(on)
                build()
            }
        })
    }

    private fun row(label: String, value: String, click: () -> Unit) {
        content.addView(Button(this).apply {
            isAllCaps = false
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            text = "$label: $value"
            setOnClickListener { click() }
        })
    }

    private fun choice(label: String, value: String, options: List<String>, checked: Int, set: (Int) -> Unit) {
        row(label, value) {
            AlertDialog.Builder(this)
                .setTitle(label)
                .setSingleChoiceItems(options.toTypedArray(), checked) { d, which ->
                    set(which)
                    d.dismiss()
                    build()
                }
                .show()
        }
    }

    private companion object {
        val HOME_COUNTS = listOf(6, 9, 12, 18, 24, 36)
    }
}
