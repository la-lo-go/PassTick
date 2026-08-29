package org.ligi.passandroid.ui.state

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.PassRepository
import org.ligi.passandroid.repository.SettingsRepository
import org.ligi.passandroid.repository.ThemeMode

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `exposes immutable pass snapshots and deletes through the repository`() = runTest(dispatcher) {
        val repository = FakePassRepository(listOf(PassImpl("pass-1").apply { description = "Boarding pass" }))
        val viewModel = MainViewModel(repository, FakeSettingsRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.passes.single().description).isEqualTo("Boarding pass")

        viewModel.onAction(AppAction.DeletePass("pass-1"))
        advanceUntilIdle()

        assertThat(repository.deletedIds).containsExactly("pass-1")
        assertThat(viewModel.uiState.value.passes).isEmpty()
    }
}

private class FakePassRepository(initial: List<Pass>) : PassRepository {
    private val passes = MutableStateFlow(initial)
    val deletedIds = mutableListOf<String>()

    override fun observePasses() = passes.asStateFlow()
    override fun find(id: String) = passes.value.firstOrNull { it.id == id }
    override suspend fun import(uri: Uri) = Result.failure<Pass>(UnsupportedOperationException())
    override suspend fun save(pass: Pass) = Unit
    override suspend fun delete(id: String): Boolean {
        deletedIds += id
        passes.value = passes.value.filterNot { it.id == id }
        return true
    }
    override suspend fun export(id: String, destination: Uri) = Result.success(Unit)
    override suspend fun prepareShare(id: String) = Result.failure<Uri>(UnsupportedOperationException())
}

private class FakeSettingsRepository : SettingsRepository {
    override val settings = MutableStateFlow(AppSettings())
    override suspend fun setThemeMode(value: ThemeMode) = Unit
    override suspend fun setCondensedPasses(value: Boolean) = Unit
    override suspend fun setAutomaticBrightness(value: Boolean) = Unit
    override suspend fun setSortOrder(value: PassSortOrder) = Unit
}
