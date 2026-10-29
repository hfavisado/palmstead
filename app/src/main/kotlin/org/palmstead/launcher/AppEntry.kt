package org.palmstead.launcher

import android.content.ComponentName
import android.graphics.drawable.Drawable

internal class AppEntry(
    val type: Int,
    val key: String,
    val component: ComponentName?,
    val label: String,
    val category: String,
    var icon: Drawable? = null,
) {
    companion object {
        const val APP = 0
        const val ALL_APPS = 1
        const val SETTINGS = 2
    }
}
