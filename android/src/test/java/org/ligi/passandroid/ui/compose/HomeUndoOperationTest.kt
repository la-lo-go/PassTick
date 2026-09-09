package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.ui.state.AppAction

class HomeUndoOperationTest {
    @Test
    fun `undo archive restores a Today pass to its original category`() {
        val action = UndoOperation.Archive("today-pass", "favorites").toAppAction()

        assertThat(action).isEqualTo(AppAction.SetPassArchived("today-pass", false))
    }

    @Test
    fun `undo restore sets archived state`() {
        val action = UndoOperation.Restore("old-pass", "favorites").toAppAction()

        assertThat(action).isEqualTo(AppAction.SetPassArchived("old-pass", true))
    }

}
