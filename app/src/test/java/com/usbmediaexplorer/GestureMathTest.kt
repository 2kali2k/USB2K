package com.usbmediaexplorer

import com.usbmediaexplorer.ui.player.scaledGestureValue
import com.usbmediaexplorer.ui.player.seekPositionFromDrag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureMathTest {
    @Test
    fun `small vertical movement is ignored`() {
        assertNull(scaledGestureValue(0.5f, 4f, 1000f, 0.3f, 8f))
    }

    @Test
    fun `upward drag increases vertical value`() {
        assertEquals(0.53f, scaledGestureValue(0.5f, -100f, 1000f, 0.3f, 8f)!!, 0.001f)
    }

    @Test
    fun `right drag moves seek forward`() {
        assertEquals(13_500L, seekPositionFromDrag(10_000L, 350f, 1000f, 20_000L, 0.5f))
    }
}