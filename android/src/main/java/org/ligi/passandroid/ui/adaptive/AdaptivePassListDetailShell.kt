package org.ligi.passandroid.ui.adaptive

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.ligi.passandroid.navigation.AppDestination

/**
 * Adapts the pass list-detail route pair to the current window and folding posture.
 * Navigation 3 remains the source of truth so deep links and system back use one back stack.
 */
@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun AdaptivePassListDetailShell(
    selectedDestination: AppDestination.PassDetail?,
    listPane: @Composable () -> Unit,
    detailPane: @Composable (AppDestination.PassDetail) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scaffoldDirective = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
        currentWindowAdaptiveInfoV2(),
    )
    val adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies()
    val destinationHistory = remember(selectedDestination) {
        buildList {
            add(ThreePaneScaffoldDestinationItem<AppDestination>(ThreePaneScaffoldRole.Secondary))
            selectedDestination?.let {
                add(ThreePaneScaffoldDestinationItem(ThreePaneScaffoldRole.Primary, it))
            }
        }
    }
    val scaffoldValue = calculateThreePaneScaffoldValue(
        maxHorizontalPartitions = scaffoldDirective.maxHorizontalPartitions,
        adaptStrategies = adaptStrategies,
        destinationHistory = destinationHistory,
    )

    ListDetailPaneScaffold(
        directive = scaffoldDirective,
        value = scaffoldValue,
        listPane = { listPane() },
        detailPane = {
            if (selectedDestination != null) detailPane(selectedDestination)
        },
        modifier = modifier,
    )
}
