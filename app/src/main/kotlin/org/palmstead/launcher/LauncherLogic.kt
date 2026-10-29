package org.palmstead.launcher

/** Pure selection logic for the home list and the bottom button row. */
internal object LauncherLogic {
    const val SLOT_AUTO = "auto"
    const val SLOT_ALL = "all"
    const val SLOT_SETTINGS = "settings"
    const val SLOT_NONE = "none"

    /** Keys ordered by launch count (descending); ties keep the incoming (alphabetical) order. */
    fun rank(keysInOrder: List<String>, usage: Map<String, Int>): List<String> =
        keysInOrder.sortedByDescending { usage[it] ?: 0 }

    fun topUsed(keysInOrder: List<String>, usage: Map<String, Int>, n: Int): List<String> =
        rank(keysInOrder, usage).take(n.coerceAtLeast(0))

    /**
     * Turns configured slots into concrete ones: "auto" becomes the next most used app that is not
     * otherwise pinned, and pinned apps that are no longer installed become empty.
     */
    fun resolveDock(slots: List<String>, ranked: List<String>, installed: Set<String>): List<String> {
        val used = slots.filter { isApp(it) && it in installed }.toMutableSet()
        var next = 0
        return slots.map { s ->
            when {
                s == SLOT_AUTO -> {
                    var pick = SLOT_NONE
                    while (next < ranked.size) {
                        val k = ranked[next++]
                        if (used.add(k)) {
                            pick = k
                            break
                        }
                    }
                    pick
                }
                isApp(s) -> if (s in installed) s else SLOT_NONE
                else -> s
            }
        }
    }

    fun isApp(slot: String?): Boolean =
        slot != null && slot != SLOT_AUTO && slot != SLOT_ALL && slot != SLOT_SETTINGS && slot != SLOT_NONE
}
