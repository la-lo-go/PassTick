package org.ligi.passandroid.ui.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CodeZoomStateTest {
    @Test
    fun `zooming in stops at the largest zoom that fits the parent`() {
        val state = CodeZoomState().apply { max = 1.5f }

        state.pinch(1.2f)
        state.pinch(1.5f)

        assertThat(state.zoom).isEqualTo(1.5f)
        assertThat(state.layoutScale).isEqualTo(1.5f)
    }

    @Test
    fun `a code that already fills its parent cannot grow`() {
        val state = CodeZoomState().apply { max = 1f }

        state.pinch(2f)

        assertThat(state.zoom).isEqualTo(1f)
    }

    @Test
    fun `zooming out keeps the surface at its configured size`() {
        val state = CodeZoomState().apply { max = 2f }

        state.pinch(0.5f)

        assertThat(state.zoom).isEqualTo(0.5f)
        assertThat(state.layoutScale).isEqualTo(1f)
    }

    @Test
    fun `a lower limit pulls the zoom down with it`() {
        val state = CodeZoomState().apply { max = 2f }
        state.pinch(1.8f)

        state.max = 1.3f

        assertThat(state.zoom).isEqualTo(1.3f)
    }

    @Test
    fun `zooming out has a floor`() {
        val state = CodeZoomState().apply { max = 2f }

        state.pinch(0.01f)

        assertThat(state.zoom).isEqualTo(0.4f)
    }
}
