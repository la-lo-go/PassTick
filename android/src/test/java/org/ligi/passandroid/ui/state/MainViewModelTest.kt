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
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.platform.PlatformActions
import org.ligi.passandroid.platform.PlatformLocation
import org.ligi.passandroid.platform.PrintablePass
import org.ligi.passandroid.functions.CalendarEvent
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.PassRepository
import org.ligi.passandroid.repository.PassFieldSnapshot
import org.ligi.passandroid.repository.PassLocationSnapshot
import org.ligi.passandroid.repository.PassTimeSpanSnapshot
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.repository.PassUpdate
import org.ligi.passandroid.repository.SettingsRepository
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.threeten.bp.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `exposes immutable pass snapshots and deletes through the repository`() = runTest(dispatcher) {
        val repository = FakePassRepository(listOf(snapshot("pass-1", "Boarding pass")))
        val viewModel = MainViewModel(repository, FakeSettingsRepository(), FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.passes.single().description).isEqualTo("Boarding pass")

        viewModel.onAction(AppAction.DeletePass("pass-1"))
        advanceUntilIdle()

        assertThat(repository.deletedIds).containsExactly("pass-1")
        assertThat(viewModel.uiState.value.passes).isEmpty()
    }

    @Test
    fun `saves all edited pass data through the repository`() = runTest(dispatcher) {
        val repository = FakePassRepository(listOf(snapshot("pass-1", "Old")))
        val viewModel = MainViewModel(repository, FakeSettingsRepository(), FakePlatformActions())

        viewModel.onAction(
            AppAction.SavePass(
                "pass-1",
                PassDraft(
                    "Updated",
                    "Issuer",
                    PassType.COUPON,
                    0xFF123456.toInt(),
                    PassBarCodeFormat.QR_CODE,
                    "payload",
                    "Show this",
                    listOf(PassFieldUiModel("key", "Label", "Value", false, null)),
                    calendarStart = "2026-09-01T10:00:00+02:00",
                    calendarEnd = "2026-09-01T12:00:00+02:00",
                    locations = listOf(PassLocationDraft("Station", "40.4", "-3.7")),
                ),
            ),
        )
        advanceUntilIdle()

        assertThat(repository.updates.single()).isEqualTo(
            "pass-1" to PassUpdate(
                "Updated",
                "Issuer",
                PassType.COUPON,
                0xFF123456.toInt(),
                PassBarCodeFormat.QR_CODE,
                "payload",
                "Show this",
                listOf(PassFieldSnapshot("key", "Label", "Value", false, null)),
                calendarTimeSpan = PassTimeSpanSnapshot(
                    ZonedDateTime.parse("2026-09-01T10:00:00+02:00"),
                    ZonedDateTime.parse("2026-09-01T12:00:00+02:00"),
                ),
                locations = listOf(PassLocationSnapshot("Station", 40.4, -3.7)),
            ),
        )
    }

    @Test
    fun `exports through a content uri`() = runTest(dispatcher) {
        val repository = FakePassRepository(emptyList())
        val viewModel = MainViewModel(repository, FakeSettingsRepository(), FakePlatformActions())
        val destination = org.mockito.Mockito.mock(Uri::class.java)

        viewModel.onAction(AppAction.Export("pass-1", destination))
        advanceUntilIdle()

        assertThat(repository.exports).containsExactly("pass-1" to destination)
    }

    @Test
    fun `selects categories and moves a pass between them`() = runTest(dispatcher) {
        val repository = FakePassRepository(
            listOf(
                snapshot("new-pass", "Train", "new"),
                snapshot("archived-pass", "Museum", "archive"),
            ),
        )
        val viewModel = MainViewModel(repository, FakeSettingsRepository(), FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.categories.map { it.id }).contains("new", "archive")
        assertThat(viewModel.uiState.value.selectedCategoryId).isNull()

        viewModel.onAction(AppAction.SelectCategory("archive"))
        viewModel.onAction(AppAction.MovePass("new-pass", "archive"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedCategoryId).isEqualTo("archive")
        assertThat(repository.moved).containsExactly("new-pass" to "archive")
        assertThat(viewModel.uiState.value.passes.single { it.id == "new-pass" }.categoryId).isEqualTo("archive")
    }

    @Test
    fun `adds a configurable category`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val viewModel = MainViewModel(FakePassRepository(emptyList()), settings, FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        val category = PassCategory("travel", "Travel", 0xFF006C4C, PassCategoryRole.CUSTOM)

        viewModel.onAction(AppAction.SaveCategory(category))
        advanceUntilIdle()

        assertThat(settings.settings.value.categories).contains(category)
    }

    @Test
    fun `deleting a custom category moves its passes to inbox`() = runTest(dispatcher) {
        val category = PassCategory("travel", "Travel", 0xFF006C4C, PassCategoryRole.CUSTOM)
        val settings = FakeSettingsRepository().apply {
            setCategories(settings.value.categories + category)
        }
        val repository = FakePassRepository(listOf(snapshot("pass-1", "Train", "travel")))
        val viewModel = MainViewModel(repository, settings, FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onAction(AppAction.DeleteCategory("travel"))
        advanceUntilIdle()

        assertThat(repository.moved).containsExactly("pass-1" to "new")
        assertThat(settings.settings.value.categories.map { it.id }).doesNotContain("travel")
    }
}

private fun snapshot(id: String, description: String, categoryId: String = "new") = PassSnapshot(
    id = id,
    description = description,
    creator = null,
    type = PassType.EVENT,
    accentColor = 0,
    barcodeFormat = null,
    barcodeMessage = null,
    barcodeAlternativeText = null,
    fields = emptyList(),
    locations = emptyList(),
    calendarTimeSpan = null,
    categoryId = categoryId,
)

private class FakePassRepository(initial: List<PassSnapshot>) : PassRepository {
    private val passes = MutableStateFlow(initial)
    val deletedIds = mutableListOf<String>()
    val updates = mutableListOf<Pair<String, PassUpdate>>()
    val exports = mutableListOf<Pair<String, Uri>>()
    val created = mutableListOf<PassUpdate>()
    val moved = mutableListOf<Pair<String, String>>()

    override fun observePasses() = passes.asStateFlow()
    override suspend fun import(uri: Uri) = Result.failure<PassSnapshot>(UnsupportedOperationException())
    override suspend fun create(update: PassUpdate): PassSnapshot {
        created += update
        return snapshot("created", update.description).also { passes.value += it }
    }
    override suspend fun update(id: String, update: PassUpdate) { updates += id to update }
    override suspend fun moveToCategory(id: String, categoryId: String) {
        moved += id to categoryId
        passes.value = passes.value.map { if (it.id == id) it.copy(categoryId = categoryId) else it }
    }
    override suspend fun delete(id: String): Boolean {
        deletedIds += id
        passes.value = passes.value.filterNot { it.id == id }
        return true
    }
    override suspend fun export(id: String, destination: Uri): Result<Unit> {
        exports += id to destination
        return Result.success(Unit)
    }
    override suspend fun prepareShare(id: String) = Result.failure<Uri>(UnsupportedOperationException())
}

private class FakePlatformActions : PlatformActions {
    override fun addToCalendar(event: CalendarEvent) = Unit
    override fun addToCalendarAutomatically(event: CalendarEvent) = true
    override fun share(uri: Uri, mimeType: String) = Unit
    override fun print(pass: PrintablePass) = Unit
    override fun openLocation(location: PlatformLocation) = Unit
}

private class FakeSettingsRepository : SettingsRepository {
    override val settings = MutableStateFlow(AppSettings())
    override suspend fun setThemeMode(value: ThemeMode) = Unit
    override suspend fun setAutomaticBrightness(value: Boolean) = Unit
    override suspend fun setSortOrder(value: PassSortOrder) = Unit
    override suspend fun setCategories(value: List<PassCategory>) {
        settings.value = settings.value.copy(categories = value)
    }
    override suspend fun setHighlightTodayPasses(value: Boolean) = Unit
    override suspend fun setAutomaticallyMarkPast(value: Boolean) = Unit
    override suspend fun setOfferCalendarAfterImport(value: Boolean) = Unit
    override suspend fun setRemindersEnabled(value: Boolean) = Unit
    override suspend fun setReminderMinutes(value: Set<Int>) = Unit
    override suspend fun setReminderExcludedPassIds(value: Set<String>) = Unit
    override suspend fun setReminderLeadMinutesByPass(value: Map<String, Int>) = Unit
}
