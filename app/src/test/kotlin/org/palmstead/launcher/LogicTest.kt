package org.palmstead.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    @Test fun lowResScaleFitsInside() {
        assertEquals(3.375f, Geometry.lowResScale(1080, 2400), 0.001f)
        assertEquals(1.5f, Geometry.lowResScale(1000, 720), 0.001f)
    }

    @Test fun ppiFallsBackToDensity() {
        assertEquals(400f, Geometry.ppi(0f, 0f, 400), 0.01f)
        assertEquals(420f, Geometry.ppi(400f, 440f, 480), 0.01f)
    }

    @Test fun modernSizesAreLimitedByPhysicalSize() {
        assertEquals(0.40f * 300, Geometry.clampedPx(500f, 300f, 0.25f, 0.40f), 0.01f)
        assertEquals(0.25f * 300, Geometry.clampedPx(10f, 300f, 0.25f, 0.40f), 0.01f)
        assertEquals(100f, Geometry.clampedPx(100f, 300f, 0.25f, 0.40f), 0.01f)
    }

    @Test fun garnetFullUnitNeverBelowOne() {
        assertTrue(Geometry.garnetFullUnit(100f, 1080) >= 1f)
        assertEquals(3.375f, Geometry.garnetFullUnit(1000f, 1080), 0.001f)
    }

    @Test fun topUsedPrefersCountThenOrder() {
        val top = LauncherLogic.topUsed(listOf("a", "b", "c", "d"), mapOf("c" to 5, "b" to 1), 3)
        assertEquals(listOf("c", "b", "a"), top)
    }

    @Test fun dockAutoSkipsPinnedAndMissing() {
        val ranked = listOf("x/y", "p/q", "m/n")
        val out = LauncherLogic.resolveDock(
            listOf("all", "auto", "p/q", "gone/app", "auto"), ranked, ranked.toSet(),
        )
        assertEquals(listOf("all", "x/y", "p/q", "none", "m/n"), out)
    }
}
