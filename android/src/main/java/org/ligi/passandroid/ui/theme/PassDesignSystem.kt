package org.ligi.passandroid.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp

object PassDesignSystem {
    val Today = Color(0xFF006C4C)
    val Upcoming = Color(0xFF6750A4)
    val Past = Color(0xFF6D4C41)
    val Archived = Color(0xFF546E7A)
    val BarcodeBackground = Color.White
    val BarcodeForeground = Color.Black
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val PassActionButtonGap: Dp = ButtonGroupDefaults.ConnectedSpaceBetween

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun PassActionButtonGroup(
    modifier: Modifier = Modifier,
    content: ButtonGroupScope.() -> Unit,
) {
    ButtonGroup(
        overflowIndicator = { _ -> },
        horizontalArrangement = Arrangement.spacedBy(PassActionButtonGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
        content = content,
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun PassActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    index: Int,
    count: Int,
    fillHeight: Boolean = false,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = if (fillHeight) {
            Modifier.fillMaxHeight().width(IconButtonDefaults.mediumContainerSize().width)
        } else {
            Modifier.size(IconButtonDefaults.mediumContainerSize())
        },
        shape = passActionButtonShape(index, count),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(icon, label, Modifier.size(IconButtonDefaults.mediumIconSize))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun passActionButtonGroupWidth(count: Int): Dp =
    IconButtonDefaults.mediumContainerSize().width * count +
        PassActionButtonGap * (count - 1).coerceAtLeast(0)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun passActionButtonShape(index: Int, count: Int): Shape = when {
    count == 1 -> IconButtonDefaults.mediumRoundShape
    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes().shape
    index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes().shape
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes().shape
}
