package org.palmstead.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LogicTest {
    @Test public void lowResScaleFitsInside() {
        assertEquals(3.375f, Geometry.lowResScale(1080, 2400), 0.001f);
        assertEquals(1.5f, Geometry.lowResScale(1000, 720), 0.001f);
    }

    @Test public void ppiFallsBackToDensity() {
        assertEquals(400f, Geometry.ppi(0, 0, 400), 0.01f);
        assertEquals(420f, Geometry.ppi(400, 440, 480), 0.01f);
    }

    @Test public void modernSizesAreLimitedByPhysicalSize() {
        assertEquals(0.40f * 300, Geometry.clampedPx(500, 300, 0.25f, 0.40f), 0.01f);
        assertEquals(0.25f * 300, Geometry.clampedPx(10, 300, 0.25f, 0.40f), 0.01f);
        assertEquals(100f, Geometry.clampedPx(100, 300, 0.25f, 0.40f), 0.01f);
    }

    @Test public void garnetFullUnitNeverBelowOne() {
        assertTrue(Geometry.garnetFullUnit(100, 1080) >= 1f);
        assertEquals(3.375f, Geometry.garnetFullUnit(1000, 1080), 0.001f);
    }

    @Test public void topUsedPrefersCountThenOrder() {
        Map<String, Integer> u = new HashMap<>();
        u.put("c", 5);
        u.put("b", 1);
        List<String> top = LauncherLogic.topUsed(Arrays.asList("a", "b", "c", "d"), u, 3);
        assertEquals(Arrays.asList("c", "b", "a"), top);
    }

    @Test public void dockAutoSkipsPinnedAndMissing() {
        List<String> ranked = Arrays.asList("x/y", "p/q", "m/n");
        List<String> out = LauncherLogic.resolveDock(
                Arrays.asList("all", "auto", "p/q", "gone/app", "auto"), ranked,
                new HashSet<>(ranked));
        assertEquals(Arrays.asList("all", "x/y", "p/q", "none", "m/n"), out);
    }
}
