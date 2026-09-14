package org.ligi.passandroid.ui.state

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
import org.ligi.passandroid.repository.PassImageExportOptions
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
    fun `pending deletion hides a pass and undo restores it before physical deletion`() = runTest(dispatcher) {
        val repository = FakePassRepository(listOf(snapshot("pass-1", "Boarding pass")))
        val viewModel = MainViewModel(repository, FakeSettingsRepository(), FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onAction(AppAction.SetPassPendingDeletion("pass-1", true))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.passes).isEmpty()
        assertThat(repository.deletedIds).isEmpty()

        viewModel.onAction(AppAction.SetPassPendingDeletion("pass-1", false))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.passes.map(PassUiModel::id)).containsExactly("pass-1")
        assertThat(repository.deletedIds).isEmpty()
    }

    @Test
    fun `protects a pass through the repository and exposes the updated state`() = runTest(dispatcher) {
        val repository = FakePassRepository(listOf(snapshot("pass-1", "Boarding pass")))
        val viewModel = MainViewModel(repository, FakeSettingsRepository(), FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onAction(AppAction.SetPassProtected("pass-1", true))
        advanceUntilIdle()

        assertThat(repository.protectionChanges).containsExactly("pass-1" to true)
        assertThat(viewModel.uiState.value.passes.single().isProtected).isTrue()
    }

    @Test
    fun `updates pin tags and archive state in the visible model`() = runTest(dispatcher) {
        val repository = FakePassRepository(listOf(snapshot("pass-1", "Boarding pass")))
        val viewModel = MainViewModel(repository, FakeSettingsRepository(), FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onAction(AppAction.SetPassPinned("pass-1", true))
        viewModel.onAction(AppAction.SetPassTags("pass-1", setOf("travel", "important")))
        viewModel.onAction(AppAction.SetPassArchived("pass-1", true))
        advanceUntilIdle()

        val pass = viewModel.uiState.value.passes.single()
        assertThat(pass.isPinned).isTrue()
        assertThat(pass.tagIds).containsExactlyInAnyOrder("travel", "important")
        assertThat(pass.isArchived).isTrue()
    }

    @Test
    fun `silent archive does not publish a second message`() = runTest(dispatcher) {
        val repository = FakePassRepository(listOf(snapshot("pass-1", "Boarding pass")))
        val viewModel = MainViewModel(repository, FakeSettingsRepository(), FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onAction(AppAction.SetPassArchived("pass-1", true, announce = false))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.message).isNull()
        assertThat(viewModel.uiState.value.passes.single().isArchived).isTrue()
    }

    @Test
    fun `keeps content loading until passes and settings are ready`() = runTest(dispatcher) {
        val repository = FakePassRepository(listOf(snapshot("pass-1", "Boarding pass")))
        val viewModel = MainViewModel(repository, FakeSettingsRepository(), FakePlatformActions())

        assertThat(viewModel.uiState.value.isContentLoading).isTrue()
        assertThat(viewModel.uiState.value.passes).isEmpty()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isContentLoading).isFalse()
        assertThat(viewModel.uiState.value.passes.single().description).isEqualTo("Boarding pass")
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
    fun `serializes a silent move and its undo without an extra message`() = runTest(dispatcher) {
        val repository = FakePassRepository(listOf(snapshot("pass-1", "Train", "new"))).apply {
            moveDelayMillis["archive"] = 100
        }
        val viewModel = MainViewModel(repository, FakeSettingsRepository(), FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        viewModel.onAction(AppAction.MovePass("pass-1", "archive", announce = false))
        viewModel.onAction(AppAction.MovePass("pass-1", "new", announce = false))
        advanceUntilIdle()

        assertThat(repository.moved).containsExactly("pass-1" to "archive", "pass-1" to "new")
        assertThat(viewModel.uiState.value.passes.single().categoryId).isEqualTo("new")
        assertThat(viewModel.uiState.value.message).isNull()
    }

    @Test
    fun `reorders passes and switches to manual order`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val repository = FakePassRepository(
            listOf(snapshot("one", "One"), snapshot("two", "Two"), snapshot("three", "Three")),
        )
        val viewModel = MainViewModel(repository, settings, FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onAction(AppAction.ReorderPass(listOf("one", "three", "two")))
        advanceUntilIdle()

        assertThat(settings.settings.value.sortOrder).isEqualTo(PassSortOrder.MANUAL)
        assertThat(viewModel.uiState.value.passes.map(PassUiModel::id)).containsExactly("one", "three", "two")
    }

    @Test
    fun `reorders only the visible subset without disturbing passes between it`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val repository = FakePassRepository(
            listOf(snapshot("one", "One"), snapshot("hidden", "Hidden"), snapshot("three", "Three")),
        )
        val viewModel = MainViewModel(repository, settings, FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onAction(AppAction.ReorderPass(listOf("three", "one")))
        advanceUntilIdle()

        assertThat(settings.settings.value.passOrder).containsExactly("three", "hidden", "one")
        assertThat(settings.settings.value.sortOrder).isEqualTo(PassSortOrder.MANUAL)
    }

    @Test
    fun `updates home card layout through explicit actions`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val viewModel = MainViewModel(FakePassRepository(emptyList()), settings, FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onAction(AppAction.MoveHomeCardSection(org.ligi.passandroid.repository.HomeCardSection.DATE, -1))
        viewModel.onAction(
            AppAction.SetHomeCardSectionVisible(org.ligi.passandroid.repository.HomeCardSection.CREATOR, true),
        )
        advanceUntilIdle()

        assertThat(settings.settings.value.homeCardSectionOrder.take(4)).containsExactly(
            org.ligi.passandroid.repository.HomeCardSection.ARTWORK,
            org.ligi.passandroid.repository.HomeCardSection.TITLE,
            org.ligi.passandroid.repository.HomeCardSection.DATE,
            org.ligi.passandroid.repository.HomeCardSection.PRIMARY_FIELD,
        )
        assertThat(settings.settings.value.hiddenHomeCardSections)
            .doesNotContain(org.ligi.passandroid.repository.HomeCardSection.CREATOR)
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
    fun `deleting a custom category removes the tag from its passes`() = runTest(dispatcher) {
        val category = PassCategory("travel", "Travel", 0xFF006C4C, PassCategoryRole.CUSTOM)
        val settings = FakeSettingsRepository().apply {
            setCategories(settings.value.categories + category)
        }
        val repository = FakePassRepository(listOf(snapshot("pass-1", "Train", "travel").copy(tagIds = setOf("travel"))))
        val viewModel = MainViewModel(repository, settings, FakePlatformActions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onAction(AppAction.DeleteCategory("travel"))
        advanceUntilIdle()

        assertThat(repository.moved).isEmpty()
        assertThat(repository.observePasses().first().single().tagIds).isEmpty()
        assertThat(settings.settings.value.categories.map { it.id }).doesNotContain("travel")
    }

    @Test
    fun `configures and disables a reminder for one pass`() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val viewModel = MainViewModel(FakePassRepository(emptyList()), settings, FakePlatformActions())

        viewModel.onAction(AppAction.ConfigurePassReminder("pass-1", enabled = true, leadMinutes = 30))
        advanceUntilIdle()

        assertThat(settings.settings.value.reminderLeadMinutesByPass).containsEntry("pass-1", 30)
        assertThat(settings.settings.value.reminderExcludedPassIds).doesNotContain("pass-1")

        viewModel.setPassReminderActions(
            AppAction.SetPassReminderActions(
                "pass-1",
                setOf(org.ligi.passandroid.reminder.NotificationAction.OPEN_CODE),
            ),
        )
        advanceUntilIdle()
        assertThat(settings.savedReminderActions).containsKey("pass-1")

        viewModel.setPassReminderActions(AppAction.SetPassReminderActions("pass-1", null))
        assertThat(settings.savedReminderActions).doesNotContainKey("pass-1")

        viewModel.onAction(AppAction.ConfigurePassReminder("pass-1", enabled = false, leadMinutes = null))
        advanceUntilIdle()

        assertThat(settings.settings.value.reminderLeadMinutesByPass).doesNotContainKey("pass-1")
        assertThat(settings.settings.value.reminderExcludedPassIds).contains("pass-1")
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
    private val trashed = MutableStateFlow<List<PassSnapshot>>(emptyList())
    val deletedIds = mutableListOf<String>()
    val updates = mutableListOf<Pair<String, PassUpdate>>()
    val exports = mutableListOf<Pair<String, Uri>>()
    val created = mutableListOf<PassUpdate>()
    val moved = mutableListOf<Pair<String, String>>()
    val protectionChanges = mutableListOf<Pair<String, Boolean>>()
    val moveDelayMillis = mutableMapOf<String, Long>()
    val purgedRetentions = mutableListOf<Long>()

    override fun observePasses() = passes.asStateFlow()
    override suspend fun import(uri: Uri) = Result.failure<PassSnapshot>(UnsupportedOperationException())
    override suspend fun create(update: PassUpdate): PassSnapshot {
        created += update
        return snapshot("created", update.description).also { passes.value += it }
    }
    override suspend fun update(id: String, update: PassUpdate) { updates += id to update }
    override suspend fun moveToCategory(id: String, categoryId: String) {
        delay(moveDelayMillis[categoryId] ?: 0)
        moved += id to categoryId
        passes.value = passes.value.map { if (it.id == id) it.copy(categoryId = categoryId) else it }
    }
    override suspend fun setProtected(id: String, isProtected: Boolean) {
        protectionChanges += id to isProtected
        passes.value = passes.value.map { if (it.id == id) it.copy(isProtected = isProtected) else it }
    }
    override suspend fun setFavorite(id: String, isFavorite: Boolean) {
        passes.value = passes.value.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it }
    }
    override suspend fun setTags(id: String, tagIds: Set<String>) {
        passes.value = passes.value.map { if (it.id == id) it.copy(tagIds = tagIds) else it }
    }
    override suspend fun setArchived(id: String, isArchived: Boolean) {
        passes.value = passes.value.map { if (it.id == id) it.copy(isArchived = isArchived) else it }
    }
    override suspend fun setPreferredArtwork(id: String, kind: org.ligi.passandroid.repository.PassArtworkKind?) {
        passes.value = passes.value.map { if (it.id == id) it.copy(preferredArtworkKind = kind) else it }
    }
    override suspend fun trashPass(id: String) {
        val pass = passes.value.first { it.id == id }
        passes.value = passes.value.filterNot { it.id == id }
        trashed.value = trashed.value + pass.copy(
            trashedAtEpochMillis = org.threeten.bp.Instant.now().toEpochMilli(),
        )
    }
    override suspend fun restoreFromTrash(id: String) {
        val pass = trashed.value.first { it.id == id }
        trashed.value = trashed.value.filterNot { it.id == id }
        passes.value = passes.value + pass.copy(trashedAtEpochMillis = null)
    }
    override fun observeTrashedPasses() = trashed.asStateFlow()
    override suspend fun purgeExpiredTrash(retention: org.threeten.bp.Duration) {
        purgedRetentions += retention.toMillis()
        val now = org.threeten.bp.Instant.now().toEpochMilli()
        trashed.value.filter { pass ->
            pass.trashedAtEpochMillis?.let { now - it > retention.toMillis() } == true
        }.forEach { delete(it.id) }
    }
    override suspend fun emptyTrash() {
        trashed.value.toList().forEach { delete(it.id) }
    }
    override suspend fun delete(id: String): Boolean {
        deletedIds += id
        passes.value = passes.value.filterNot { it.id == id }
        trashed.value = trashed.value.filterNot { it.id == id }
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
    override fun printImage(jobName: String, bitmap: android.graphics.Bitmap) = Unit
    override fun openLocation(location: PlatformLocation) = Unit
    override fun openUrl(url: String) = Unit
    override fun shareImage(pass: PassUiModel, options: PassImageExportOptions) = Unit
}

private class FakeSettingsRepository : SettingsRepository {
    override val settings = MutableStateFlow(AppSettings())
    var savedReminderActions: Map<String, Set<org.ligi.passandroid.reminder.NotificationAction>> = emptyMap()
    override suspend fun setThemeMode(value: ThemeMode) = Unit
    override suspend fun setAmoledBlackBackground(value: Boolean) = Unit
    override suspend fun setAutomaticBrightness(value: Boolean) = Unit
    override suspend fun setSortOrder(value: PassSortOrder) {
        settings.value = settings.value.copy(sortOrder = value)
    }
    override suspend fun setPassOrder(value: List<String>) {
        settings.value = settings.value.copy(passOrder = value)
    }
    override suspend fun setCategories(value: List<PassCategory>) {
        settings.value = settings.value.copy(categories = value)
    }
    override suspend fun setHighlightTodayPasses(value: Boolean) = Unit
    override suspend fun setAutomaticallyMarkPast(value: Boolean) = Unit
    override suspend fun setOfferCalendarAfterImport(value: Boolean) = Unit
    override suspend fun setRemindersEnabled(value: Boolean) = Unit
    override suspend fun setReminderMinutes(value: Set<Int>) = Unit
    override suspend fun setReminderExcludedPassIds(value: Set<String>) {
        settings.value = settings.value.copy(reminderExcludedPassIds = value)
    }
    override suspend fun setReminderLeadMinutesByPass(value: Map<String, Int>) {
        settings.value = settings.value.copy(reminderLeadMinutesByPass = value)
    }
    override suspend fun setReminderExactPassIds(value: Set<String>) = Unit
    override suspend fun setReminderActionsByPass(
        value: Map<String, Set<org.ligi.passandroid.reminder.NotificationAction>>,
    ) {
        savedReminderActions = value
        settings.value = settings.value.copy(reminderActionsByPass = value)
    }
    override suspend fun setNotificationAccessWindowMinutes(value: Int) = Unit
    override suspend fun setNotificationExactTiming(value: Boolean) = Unit
    override suspend fun setNotificationActionsEnabled(value: Boolean) = Unit
    override suspend fun setNotificationLockScreenDetail(
        value: org.ligi.passandroid.reminder.NotificationLockScreenDetail,
    ) = Unit
    override suspend fun setPassDetailLayout(
        order: List<org.ligi.passandroid.repository.PassDetailSection>,
        hidden: Set<org.ligi.passandroid.repository.PassDetailSection>,
    ) {
        settings.value = settings.value.copy(passDetailSectionOrder = order, hiddenPassDetailSections = hidden)
    }
    override suspend fun setHomeCardLayout(
        order: List<org.ligi.passandroid.repository.HomeCardSection>,
        hidden: Set<org.ligi.passandroid.repository.HomeCardSection>,
    ) {
        settings.value = settings.value.copy(homeCardSectionOrder = order, hiddenHomeCardSections = hidden)
    }
    override suspend fun movePassDetailSection(
        section: org.ligi.passandroid.repository.PassDetailSection,
        offset: Int,
    ) {
        settings.value = settings.value.copy(
            passDetailSectionOrder = settings.value.passDetailSectionOrder.moveForTest(section, offset),
        )
    }
    override suspend fun setPassDetailSectionVisible(
        section: org.ligi.passandroid.repository.PassDetailSection,
        visible: Boolean,
    ) {
        settings.value = settings.value.copy(
            hiddenPassDetailSections = settings.value.hiddenPassDetailSections.withVisibilityForTest(section, visible),
        )
    }
    override suspend fun moveHomeCardSection(
        section: org.ligi.passandroid.repository.HomeCardSection,
        offset: Int,
    ) {
        settings.value = settings.value.copy(
            homeCardSectionOrder = settings.value.homeCardSectionOrder.moveForTest(section, offset),
        )
    }
    override suspend fun setHomeCardSectionVisible(
        section: org.ligi.passandroid.repository.HomeCardSection,
        visible: Boolean,
    ) {
        settings.value = settings.value.copy(
            hiddenHomeCardSections = settings.value.hiddenHomeCardSections.withVisibilityForTest(section, visible),
        )
    }
    override suspend fun setLockAllPasses(value: Boolean) {
        settings.value = settings.value.copy(lockAllPasses = value)
    }
    override suspend fun setShowProtectedPassLockIcon(value: Boolean) {
        settings.value = settings.value.copy(showProtectedPassLockIcon = value)
    }
    override suspend fun setBlurProtectedPassCards(value: Boolean) {
        settings.value = settings.value.copy(blurProtectedPassCards = value)
    }
    override suspend fun setSeparateProtectedPasses(value: Boolean) {
        settings.value = settings.value.copy(separateProtectedPasses = value)
    }
    override suspend fun setBlockScreenshots(value: Boolean) {
        settings.value = settings.value.copy(blockScreenshots = value)
    }
    override suspend fun setImageExportOptions(value: PassImageExportOptions) {
        settings.value = settings.value.copy(imageExportOptions = value)
    }
}

private fun <T> List<T>.moveForTest(item: T, offset: Int): List<T> {
    val from = indexOf(item)
    val to = (from + offset).coerceIn(indices)
    return toMutableList().apply { add(to, removeAt(from)) }
}

private fun <T> Set<T>.withVisibilityForTest(item: T, visible: Boolean): Set<T> =
    toMutableSet().apply { if (visible) remove(item) else add(item) }
