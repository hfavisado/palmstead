package org.palmstead.launcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure selection logic for the home list and the bottom button row. */
final class LauncherLogic {
    static final String SLOT_AUTO = "auto";
    static final String SLOT_ALL = "all";
    static final String SLOT_SETTINGS = "settings";
    static final String SLOT_NONE = "none";

    private LauncherLogic() {}

    /** Keys ordered by launch count (descending); ties keep the incoming (alphabetical) order. */
    static List<String> rank(List<String> keysInOrder, Map<String, Integer> usage) {
        List<String> out = new ArrayList<>(keysInOrder);
        out.sort((a, b) -> Integer.compare(count(usage, b), count(usage, a)));
        return out;
    }

    static List<String> topUsed(List<String> keysInOrder, Map<String, Integer> usage, int n) {
        List<String> ranked = rank(keysInOrder, usage);
        return new ArrayList<>(ranked.subList(0, Math.max(0, Math.min(n, ranked.size()))));
    }

    /**
     * Turns configured slots into concrete ones: "auto" becomes the next most used app that is not
     * otherwise pinned, and pinned apps that are no longer installed become empty.
     */
    static List<String> resolveDock(List<String> slots, List<String> ranked, Set<String> installed) {
        Set<String> used = new HashSet<>();
        for (String s : slots) if (isApp(s) && installed.contains(s)) used.add(s);
        List<String> out = new ArrayList<>();
        int next = 0;
        for (String s : slots) {
            if (SLOT_AUTO.equals(s)) {
                String pick = SLOT_NONE;
                while (next < ranked.size()) {
                    String k = ranked.get(next++);
                    if (!used.contains(k)) {
                        used.add(k);
                        pick = k;
                        break;
                    }
                }
                out.add(pick);
            } else if (isApp(s)) {
                out.add(installed.contains(s) ? s : SLOT_NONE);
            } else {
                out.add(s);
            }
        }
        return out;
    }

    static boolean isApp(String slot) {
        return slot != null && !SLOT_AUTO.equals(slot) && !SLOT_ALL.equals(slot)
                && !SLOT_SETTINGS.equals(slot) && !SLOT_NONE.equals(slot);
    }

    private static int count(Map<String, Integer> usage, String k) {
        Integer c = usage.get(k);
        return c == null ? 0 : c;
    }
}
