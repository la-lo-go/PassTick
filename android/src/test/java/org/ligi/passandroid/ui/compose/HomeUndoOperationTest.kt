package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.ui.state.AppAction

class HomeUndoOperationTest {
    @Test
    fun `undo archive restores a Today pass to its original category`() {
        val action = UndoOperation.Archive("today-pass", "favorites").toAppAction()

        assertThat(action).isEqualTo(AppAction.MovePass("today-pass", "favorites", announce = false))
    }

    @Test
    fun `undo delete restores a Today pass to its original category`() {
        val action = UndoOperation.Delete("today-pass", "inbox").toAppAction()

        assertThat(action).isEqualTo(AppAction.MovePass("today-pass", "inbox", announce = false))
    }
}
