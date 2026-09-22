package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.repository.PassDetailSection
import org.ligi.passandroid.repository.HomeCardSection
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.remember
import org.ligi.passandroid.R
import org.ligi.passandroid.functions.CalendarEvent
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationUiModel
import org.ligi.passandroid.ui.state.PassTimeSpanUiModel
import org.ligi.passandroid.ui.state.PassUiModel
import org.threeten.bp.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassDetailLayoutSettingsScreen(
    order: List<PassDetailSection>,
    hidden: Set<PassDetailSection>,
    onAction: (org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.layout_pass_view)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pass_detail_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "code") {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.QrCode, null) },
                        modifier = Modifier.clickable {
                            onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.OpenCodeSettings)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { Text(stringResource(R.string.settings_code)) }
                }
            }
            itemsIndexed(order, key = { _, section -> section.name }) { index, section ->
                Surface(
                    modifier = Modifier.animateItem(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    ListItem(
                        trailingContent = {
                            Row {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.Move(section, -1)) },
                                ) { Icon(Icons.Default.ArrowUpward, stringResource(R.string.layout_move_section_up, section.displayName())) }
                                IconButton(
                                    enabled = index < order.lastIndex,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.Move(section, 1)) },
                                ) { Icon(Icons.Default.ArrowDownward, stringResource(R.string.layout_move_section_down, section.displayName())) }
                                Switch(
                                    checked = section !in hidden,
                                    onCheckedChange = { visible ->
                                        onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.SetVisible(section, visible))
                                    },
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { Text(section.displayName()) }
                }
            }
            item {
                Text(
                    stringResource(R.string.layout_preview),
                    Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                PassDetailSectionList(
                    pass = remember { layoutPreviewPass() },
                    sectionOrder = order,
                    hiddenSections = hidden,
                    calendarEventPresent = false,
                    onAction = {},
                    onBarcodeHoldChanged = {},
                    onBarcodePin = {},
                    onEditDateRequested = {},
                    modifier = Modifier.padding(horizontal = 8.dp),
                    artworkFallback = { PreviewBannerArtwork(PreviewAccentColor) },
                )
            }
        }
    }
}

@Composable
private fun PassDetailSection.displayName() = when (this) {
    PassDetailSection.ARTWORK -> stringResource(R.string.layout_pass_image)
    PassDetailSection.BARCODE -> stringResource(R.string.layout_barcode)
    PassDetailSection.FIELDS -> stringResource(R.string.layout_pass_details)
    PassDetailSection.LOCATIONS -> stringResource(R.string.edit_pass_locations)
    PassDetailSection.CALENDAR -> stringResource(R.string.pass_detail_date_and_time)
    PassDetailSection.NOTES -> stringResource(R.string.layout_notes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCardLayoutSettingsScreen(
    order: List<HomeCardSection>,
    hidden: Set<HomeCardSection>,
    onAction: (org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.layout_home_cards)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pass_detail_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "artwork") {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    ListItem(
                        trailingContent = {
                            Switch(
                                checked = HomeCardSection.ARTWORK !in hidden,
                                onCheckedChange = { visible ->
                                    onAction(
                                        org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.SetVisible(
                                            HomeCardSection.ARTWORK,
                                            visible,
                                        ),
                                    )
                                },
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { Text(stringResource(R.string.layout_pass_image)) }
                }
            }
            val textSections = order.filterNot { it == HomeCardSection.ARTWORK }
            itemsIndexed(textSections, key = { _, section -> section.name }) { index, section ->
                Surface(
                    modifier = Modifier.animateItem(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    ListItem(
                        trailingContent = {
                            Row {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.Move(section, -1)) },
                                ) { Icon(Icons.Default.ArrowUpward, stringResource(R.string.layout_move_section_up, section.displayName())) }
                                IconButton(
                                    enabled = index < textSections.lastIndex,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.Move(section, 1)) },
                                ) { Icon(Icons.Default.ArrowDownward, stringResource(R.string.layout_move_section_down, section.displayName())) }
                                Switch(
                                    checked = section !in hidden,
                                    onCheckedChange = { visible ->
                                        onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.SetVisible(section, visible))
                                    },
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { Text(section.displayName()) }
                }
            }
            item {
                Text(
                    stringResource(R.string.layout_preview),
                    Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    HomeCardBody(
                        pass = remember { layoutPreviewPass() },
                        hero = false,
                        sectionOrder = order,
                        hiddenSections = hidden,
                        tagCategories = listOf(samplePreviewTag),
                        modifier = Modifier.padding(16.dp),
                        thumbnailFallback = { thumbModifier -> PreviewThumbnailArtwork(thumbModifier, PreviewAccentColor) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCardSection.displayName() = when (this) {
    HomeCardSection.ARTWORK -> stringResource(R.string.layout_pass_image)
    HomeCardSection.TITLE -> stringResource(R.string.layout_title)
    HomeCardSection.PRIMARY_FIELD -> stringResource(R.string.layout_primary_field)
    HomeCardSection.DATE -> stringResource(R.string.pass_detail_date_and_time)
    HomeCardSection.CREATOR -> stringResource(R.string.edit_pass_creator)
    HomeCardSection.CATEGORY -> stringResource(R.string.layout_tag)
    HomeCardSection.PASS_TYPE -> stringResource(R.string.layout_pass_type)
}

private const val SAMPLE_PREVIEW_TAG_ID = "layout-preview-tag"

private val PreviewAccentColor = 0xFF006C4C.toInt()

private val samplePreviewTag = PassCategory(
    id = SAMPLE_PREVIEW_TAG_ID,
    name = "Travel",
    colorArgb = 0xFF1565C0,
    role = PassCategoryRole.CUSTOM,
)

@Composable
private fun PreviewBannerArtwork(accentColor: Int) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        color = Color(accentColor),
        contentColor = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_notification_pass), null, Modifier.size(56.dp))
        }
    }
}

@Composable
private fun PreviewThumbnailArtwork(modifier: Modifier, accentColor: Int) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(accentColor),
        contentColor = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_notification_pass), null, Modifier.fillMaxSize(0.62f))
        }
    }
}

private fun layoutPreviewPass(): PassUiModel {
    val start = ZonedDateTime.now().plusDays(1).withHour(9).withMinute(30).withSecond(0).withNano(0)
    val end = start.plusHours(2)
    return PassUiModel(
        id = "layout-preview",
        description = "Boarding pass",
        creator = "Sample Air",
        type = PassType.BOARDING,
        accentColor = PreviewAccentColor,
        barcodeFormat = PassBarCodeFormat.QR_CODE,
        barcodeMessage = "PASSTICK-PREVIEW",
        barcodeAlternativeText = null,
        fields = listOf(
            PassFieldUiModel(key = null, label = "Gate", value = "A12", hidden = false, hint = "primaryFields"),
            PassFieldUiModel(key = null, label = "Seat", value = "12A", hidden = false, hint = null),
        ),
        locations = listOf(PassLocationUiModel(name = "Sample Station", latitude = 52.52, longitude = 13.405)),
        calendarEvent = CalendarEvent(
            title = "Boarding pass",
            beginTimeMillis = start.toEpochSecond() * 1000,
            endTimeMillis = end.toEpochSecond() * 1000,
            location = "Sample Station",
        ),
        calendarTimeSpan = PassTimeSpanUiModel(from = start, to = end),
        tagIds = setOf(SAMPLE_PREVIEW_TAG_ID),
        notes = "Boarding closes 30 minutes before departure",
    )
}
