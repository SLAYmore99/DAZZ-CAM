package com.funcam.app

import com.funcam.app.gl.FrameSpec
import com.funcam.app.model.Border
import com.funcam.app.model.Cameras
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameSpecTest {
    private val eps = 1e-4f

    @Test fun noBorderFillsFrame() {
        val s = FrameSpec.compute(Border.NONE, 0.75f, true)
        assertEquals(0.75f, s.outAspect, eps)
        assertEquals(listOf(0f, 0f, 1f, 1f), s.rect.toList())
    }

    @Test fun bordersDisabledFallBackToFullFrame() {
        val s = FrameSpec.compute(Border.INSTANT, 1f, false)
        assertEquals(1f, s.outAspect, eps)
    }

    @Test fun instantRectKeepsPictureAspect() {
        for (b in Border.entries) {
            val ia = 0.8f
            val s = FrameSpec.compute(b, ia, true)
            // picture rect in output pixels must have the requested aspect
            val w = (s.rect[2] - s.rect[0]) * s.outAspect
            val h = s.rect[3] - s.rect[1]
            assertEquals("border $b", ia, w / h, 1e-3f)
            assertTrue(s.rect.all { it in 0f..1f })
        }
    }

    @Test fun instantHasThickBottom() {
        val s = FrameSpec.compute(Border.INSTANT, 1f, true)
        val bottom = s.rect[1]
        val top = 1f - s.rect[3]
        assertTrue(bottom > top * 2)
    }

    @Test fun centreCropIsCentred() {
        val c = FrameSpec.crop(0.75f, 1f)
        assertEquals(0f, c[0], eps); assertEquals(1f, c[2], eps)
        assertEquals(0.125f, c[1], eps); assertEquals(0.75f, c[3], eps)
        val d = FrameSpec.crop(0.75f, 0.5f)
        assertEquals(2f / 3f, d[2], eps)
        assertEquals((1f - 2f / 3f) / 2f, d[0], eps)
    }

    @Test fun rosterIsCompleteAndUnique() {
        // classic edition: the original 30 photo + 12 video cameras
        com.funcam.app.model.Engine.v4 = false
        assertEquals(42, Cameras.all.size)
        assertEquals(30, Cameras.all.count { !it.isVideo })
        assertEquals(12, Cameras.all.count { it.isVideo })
        // engine v4 editions: 37 photo + 14 video
        com.funcam.app.model.Engine.v4 = true
        assertEquals(51, Cameras.all.size)
        assertEquals(37, Cameras.all.count { !it.isVideo })
        assertEquals(Cameras.all.size, Cameras.all.map { it.id }.toSet().size)
        assertTrue(Cameras.all.all { it.ratios.isNotEmpty() })
        assertTrue(Cameras.all.mapNotNull { it.look.lut }.all { java.io.File("src/main/assets/luts/$it.png").exists() })
    }
}
