package org.palmstead.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : Activity() {
    /** Sizes and colours for the current interface style. All sizes are in real pixels. */
    private class Look(
        val garnet: Boolean,
        val lowRes: Boolean,
        val u: Float,
        val columns: Int,
        val iconPx: Int,
        val dockIconPx: Int,
        val titleH: Int,
        val dockH: Int,
        val rowH: Int,
        val textPx: Int,
        val titleTextPx: Int,
        val swipePx: Int,
        val scrollW: Int,
        val bg: Int,
        val fg: Int,
        val barBg: Int,
        val barFg: Int,
        val line: Int,
        val pressed: Int,
        val face: Typeface,
        val antiAlias: Boolean,
    )

    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs
    private lateinit var look: Look
    private var signature = ""
    private var loadToken = 0

    private var apps: List<AppEntry> = emptyList()
    private var category = AppRepository.HOME

    private var timeView: TextView? = null
    private lateinit var batteryText: TextView
    private lateinit var categoryButton: TextView
    private lateinit var batteryView: BatteryView
    private var grid: GridView? = null
    private lateinit var indicator: ScrollIndicator
    private var dockRow: LinearLayout? = null
    private lateinit var overlayHost: FrameLayout
    private val adapter = ListAdapter()
    private var batteryPercent = 100
    private var charging = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (Intent.ACTION_BATTERY_CHANGED == i.action) readBattery(i)
            updateClock()
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = Prefs(this)
        rebuildUi()
    }

    override fun onStart() {
        super.onStart()
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(receiver, f)?.let { readBattery(it) }
        updateClock()
    }

    override fun onStop() {
        unregisterReceiver(receiver)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (prefs.signature() != signature) rebuildUi()
        reloadApps()
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action) goHome()
    }

    override fun onConfigurationChanged(c: Configuration) {
        super.onConfigurationChanged(c)
        rebuildUi()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = goHome()

    private fun goHome() {
        dismissOverlay()
        if (category != AppRepository.HOME) {
            category = AppRepository.HOME
            refresh()
        }
        grid?.setSelection(0)
    }

    // ---------------------------------------------------------------- data

    private fun reloadApps() {
        val token = ++loadToken
        io.execute {
            val loaded = AppRepository.load(this, true)
            ui.post {
                if (token != loadToken || isDestroyed) return@post
                apps = loaded
                refresh()
            }
        }
    }

    private fun orderedKeys() = apps.map { it.key }

    private fun find(key: String) = apps.firstOrNull { it.key == key }

    private fun categories(): List<String> {
        val named = apps.map { it.category }.filter { it != AppRepository.UNFILED }.distinct().sorted()
        val unfiled = apps.any { it.category == AppRepository.UNFILED }
        return listOf(AppRepository.HOME, AppRepository.ALL) + named +
            if (unfiled) listOf(AppRepository.UNFILED) else emptyList()
    }

    private fun visibleEntries(): List<AppEntry> = when (category) {
        AppRepository.HOME -> {
            val out = mutableListOf<AppEntry>()
            if (prefs.allAppsShortcut) out += specialEntry(AppEntry.ALL_APPS)
            LauncherLogic.topUsed(orderedKeys(), prefs.usageCounts(), prefs.homeCount)
                .mapNotNullTo(out) { find(it) }
            out
        }
        AppRepository.ALL -> apps
        else -> apps.filter { it.category == category }
    }

    private fun specialEntry(type: Int): AppEntry {
        val all = type == AppEntry.ALL_APPS
        return AppEntry(
            type, if (all) LauncherLogic.SLOT_ALL else LauncherLogic.SLOT_SETTINGS,
            null, if (all) "All Apps" else "Settings", AppRepository.HOME,
            GlyphDrawable(type, look.fg, look.antiAlias),
        )
    }

    private fun refresh() {
        val g = grid ?: return
        categoryButton.text = "$category \u25BE"
        adapter.set(visibleEntries())
        g.setSelection(0)
        updateIndicator()
        fillDock()
    }

    // ---------------------------------------------------------------- UI

    private fun computeLook(): Look {
        val dm = resources.displayMetrics
        val ppi = Geometry.ppi(dm.xdpi, dm.ydpi, dm.densityDpi)
        val garnet = prefs.isGarnet
        val lowRes = garnet && prefs.isLowRes
        return if (garnet) {
            val u = if (lowRes) 1f else Geometry.garnetFullUnit(ppi, dm.widthPixels)
            Look(
                garnet = true, lowRes = lowRes, u = u,
                columns = if (lowRes) 3 else max(3, (dm.widthPixels / u / 107f).roundToInt()),
                iconPx = (32 * u).roundToInt(),
                dockIconPx = (26 * u).roundToInt(),
                titleH = (16 * u).roundToInt(),
                dockH = (40 * u).roundToInt(),
                rowH = (58 * u).roundToInt(),
                textPx = (10 * u).roundToInt(),
                titleTextPx = (11 * u).roundToInt(),
                swipePx = (20 * u).roundToInt(),
                scrollW = max(1, (2 * u).roundToInt()),
                bg = Color.WHITE, fg = Color.BLACK,
                barBg = Color.BLACK, barFg = Color.WHITE,
                line = Color.BLACK, pressed = 0xFFC0C0C0.toInt(),
                face = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD),
                antiAlias = false,
            )
        } else {
            val d = dm.density
            val icon = Geometry.clampedPx(48 * d, ppi, 0.25f, 0.40f).roundToInt()
            Look(
                garnet = false, lowRes = false, u = 1f, columns = 1,
                iconPx = icon,
                dockIconPx = (icon * 0.9f).roundToInt(),
                titleH = Geometry.clampedPx(48 * d, ppi, 0.25f, 0.40f).roundToInt(),
                dockH = (icon * 1.7f).roundToInt(),
                rowH = (icon * 1.5f).roundToInt(),
                textPx = Geometry.clampedPx(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, dm), ppi, 0.09f, 0.14f,
                ).roundToInt(),
                titleTextPx = Geometry.clampedPx(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, dm), ppi, 0.08f, 0.13f,
                ).roundToInt(),
                swipePx = (0.25f * ppi).roundToInt(),
                scrollW = max(2, (3 * d).roundToInt()),
                bg = 0xFFFAFAFA.toInt(), fg = 0xFF202124.toInt(),
                barBg = 0xFF37474F.toInt(), barFg = Color.WHITE,
                line = 0xFFDADCE0.toInt(), pressed = 0xFFE0E0E0.toInt(),
                face = Typeface.create("sans-serif", Typeface.NORMAL),
                antiAlias = true,
            )
        }
    }

    private fun rebuildUi() {
        signature = prefs.signature()
        val l = computeLook()
        look = l

        val root = FrameLayout(this)
        root.setBackgroundColor(l.bg)

        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(column, FrameLayout.LayoutParams(-1, -1))

        // Title bar: time and battery on the left, category filter on the right.
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(l.barBg)
        }
        val pad = (4 * l.u * (if (l.garnet) 1 else 3)).roundToInt()
        bar.setPadding(pad, 0, pad, 0)
        timeView = text(l.barFg, l.titleTextPx).also { bar.addView(it) }
        batteryView = BatteryView(this, l.barFg, l.antiAlias)
        val bh = (l.titleTextPx * 0.8f).roundToInt()
        bar.addView(
            batteryView,
            LinearLayout.LayoutParams((bh * 1.9f).roundToInt(), bh).apply { setMargins(pad * 2, 0, pad, 0) },
        )
        batteryText = text(l.barFg, l.titleTextPx).also { bar.addView(it) }
        bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        categoryButton = text(l.barFg, l.titleTextPx).apply {
            gravity = Gravity.CENTER
            setPadding(pad, 0, pad, 0)
            setOnClickListener { showCategoryMenu() }
        }
        bar.addView(categoryButton, LinearLayout.LayoutParams(-2, -1))
        bar.setOnLongClickListener { openSettings(); true }
        column.addView(bar, LinearLayout.LayoutParams(-1, l.titleH))

        // List with a thin position indicator on the right.
        val body = FrameLayout(this)
        val g = GridView(this).apply {
            numColumns = l.columns
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            selector = ColorDrawable(Color.TRANSPARENT)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            adapter = this@MainActivity.adapter
            setOnScrollListener(object : AbsListView.OnScrollListener {
                override fun onScrollStateChanged(v: AbsListView, s: Int) {}
                override fun onScroll(v: AbsListView, f: Int, c: Int, t: Int) = updateIndicator()
            })
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateIndicator() }
        }
        grid = g
        body.addView(g, FrameLayout.LayoutParams(-1, -1))
        indicator = ScrollIndicator(this, if (l.garnet) l.fg else 0xFF9E9E9E.toInt(), l.antiAlias)
        body.addView(indicator, FrameLayout.LayoutParams(l.scrollW, -1, Gravity.END))
        column.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))

        // Bottom button row.
        val rule = View(this).apply { setBackgroundColor(l.line) }
        column.addView(rule, LinearLayout.LayoutParams(-1, max(1, l.u.roundToInt())))
        val dock = SwipeUpLayout(this, l.swipePx.toFloat()).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(l.bg)
            onSwipeUp = { if (prefs.allAppsGesture) showCategory(AppRepository.ALL) }
        }
        dockRow = dock
        column.addView(dock, LinearLayout.LayoutParams(-1, l.dockH))

        overlayHost = root

        val content: View = if (l.lowRes) {
            PixelScaleLayout(this).apply {
                addView(root, ViewGroup.LayoutParams(Geometry.LOW_W, Geometry.LOW_H))
            }
        } else {
            root.also { it.fitsSystemWindows = true }
        }
        setContentView(content)
        window.statusBarColor = if (l.lowRes) Color.BLACK else l.barBg
        window.navigationBarColor = if (l.lowRes) Color.BLACK else l.bg
        updateClock()
        refresh()
    }

    private fun text(color: Int, px: Int) = TextView(this).apply {
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, px.toFloat())
        typeface = look.face
        isSingleLine = true
        includeFontPadding = false
        if (!look.antiAlias) paintFlags = paintFlags and Paint.ANTI_ALIAS_FLAG.inv()
    }

    private fun pressedBackground(): Drawable = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(look.pressed))
        addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
    }

    private fun updateClock() {
        val t = timeView ?: return
        t.text = DateFormat.getTimeFormat(this).format(Date())
        batteryView.setLevel(batteryPercent, charging)
        batteryText.text = "$batteryPercent%"
    }

    private fun readBattery(i: Intent) {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level >= 0 && scale > 0) batteryPercent = (level * 100f / scale).roundToInt()
        val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun updateIndicator() {
        val g = grid ?: return
        val total = adapter.count
        val cols = max(1, look.columns)
        val first = g.getChildAt(0)
        if (total == 0 || first == null || first.height == 0) {
            indicator.setThumb(0f, 1f)
            return
        }
        val rows = ceil(total / cols.toDouble()).toFloat()
        val visible = g.height / first.height.toFloat()
        if (visible >= rows) {
            indicator.setThumb(0f, 1f)
            return
        }
        val topRow = g.firstVisiblePosition / cols - first.top / first.height.toFloat()
        indicator.setThumb(topRow / (rows - visible), visible / rows)
    }

    // ---------------------------------------------------------------- dock

    private fun fillDock() {
        val row = dockRow ?: return
        row.removeAllViews()
        val keys = orderedKeys()
        val resolved = LauncherLogic.resolveDock(
            prefs.slots(), LauncherLogic.rank(keys, prefs.usageCounts()), keys.toSet(),
        )
        for (slot in resolved) {
            val v = ImageView(this)
            v.scaleType = ImageView.ScaleType.CENTER_INSIDE
            val p = ((look.dockH - look.dockIconPx) / 2f).roundToInt()
            v.setPadding(p, p, p, p)
            v.background = pressedBackground()
            val entry = when {
                slot == LauncherLogic.SLOT_ALL -> specialEntry(AppEntry.ALL_APPS)
                slot == LauncherLogic.SLOT_SETTINGS -> specialEntry(AppEntry.SETTINGS)
                LauncherLogic.isApp(slot) -> find(slot)
                else -> null
            }
            if (entry != null) {
                v.setImageDrawable(entry.icon)
                v.contentDescription = entry.label
                v.setOnClickListener { activate(entry) }
            }
            row.addView(v, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    // ---------------------------------------------------------------- actions

    private fun activate(e: AppEntry) {
        dismissOverlay()
        when (e.type) {
            AppEntry.ALL_APPS -> showCategory(AppRepository.ALL)
            AppEntry.SETTINGS -> openSettings()
            else -> launch(e)
        }
    }

    private fun showCategory(c: String) {
        category = c
        refresh()
    }

    private fun launch(e: AppEntry) {
        try {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(e.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(i)
            prefs.recordLaunch(e.key)
        } catch (ex: RuntimeException) {
            reloadApps()
        }
    }

    private fun showAppInfo(e: AppEntry) {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", e.component?.packageName, null),
                ),
            )
        } catch (_: RuntimeException) {
        }
    }

    private fun openSettings() {
        dismissOverlay()
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    // ---------------------------------------------------------------- category menu

    private fun dismissOverlay() {
        if (!::overlayHost.isInitialized) return
        overlayHost.findViewWithTag<View>("overlay")?.let { overlayHost.removeView(it) }
    }

    private fun showCategoryMenu() {
        if (overlayHost.findViewWithTag<View>("overlay") != null) {
            dismissOverlay()
            return
        }
        val scrim = FrameLayout(this).apply {
            tag = "overlay"
            setOnClickListener { dismissOverlay() }
        }
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(look.bg)
                setStroke(max(1, look.u.roundToInt()), look.line)
            }
        }
        for (name in categories() + "Settings\u2026") {
            val t = text(look.fg, look.textPx).apply {
                gravity = Gravity.CENTER_VERTICAL
                val p = (look.titleH * 0.4f).roundToInt()
                setPadding(p * 2, 0, p * 2, 0)
                text = if (name == category) "\u2022 $name" else name
                background = pressedBackground()
                setOnClickListener {
                    dismissOverlay()
                    if (name.startsWith("Settings")) openSettings() else showCategory(name)
                }
            }
            menu.addView(t, LinearLayout.LayoutParams(-1, (look.titleH * 1.1f).roundToInt()))
        }
        val sv = ScrollView(this).apply { addView(menu) }
        scrim.addView(
            sv,
            FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { topMargin = look.titleH },
        )
        overlayHost.addView(scrim, FrameLayout.LayoutParams(-1, -1))
    }

    // ---------------------------------------------------------------- adapter

    private inner class ListAdapter : BaseAdapter() {
        private var items: List<AppEntry> = emptyList()

        fun set(list: List<AppEntry>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getCount() = items.size
        override fun getItem(i: Int): Any = items[i]
        override fun getItemId(i: Int) = i.toLong()

        override fun getView(pos: Int, convert: View?, parent: ViewGroup): View {
            val e = items[pos]
            val cell = LinearLayout(this@MainActivity)
            val icon = ImageView(this@MainActivity).apply { setImageDrawable(e.icon) }
            val label = text(look.fg, look.textPx).apply {
                text = e.label
                ellipsize = TextUtils.TruncateAt.END
            }
            if (look.garnet) {
                cell.orientation = LinearLayout.VERTICAL
                cell.gravity = Gravity.CENTER_HORIZONTAL
                val p = (4 * look.u).roundToInt()
                cell.setPadding(p / 2, p, p / 2, 0)
                label.gravity = Gravity.CENTER
                cell.addView(icon, LinearLayout.LayoutParams(look.iconPx, look.iconPx))
                cell.addView(label, LinearLayout.LayoutParams(-1, -2).apply { topMargin = p })
            } else {
                cell.orientation = LinearLayout.HORIZONTAL
                cell.gravity = Gravity.CENTER_VERTICAL
                val p = (look.iconPx * 0.4f).roundToInt()
                cell.setPadding(p, 0, p + look.scrollW, 0)
                cell.addView(icon, LinearLayout.LayoutParams(look.iconPx, look.iconPx))
                cell.addView(label, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = p })
            }
            cell.layoutParams = AbsListView.LayoutParams(-1, look.rowH)
            cell.background = pressedBackground()
            cell.setOnClickListener { activate(e) }
            if (e.type == AppEntry.APP) {
                cell.setOnLongClickListener { showAppInfo(e); true }
            }
            return cell
        }
    }
}
