package za.co.kinplus.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = KinGreenLight,
    onPrimary = KinWhite,
    primaryContainer = KinGreenContainerLight,
    onPrimaryContainer = KinGreenOnContainer,
    inversePrimary = KinGreenDark,
    secondary = KinGreenLight,
    onSecondary = KinWhite,
    // secondaryContainer is the bottom navigation pill behind the selected
    // tab, so it gets the soft evergreen tint with an evergreen icon on top.
    secondaryContainer = KinGreenContainerLight,
    onSecondaryContainer = KinGreenLight,
    tertiary = KinGreenLight,
    onTertiary = KinWhite,
    tertiaryContainer = KinGreenContainerLight,
    onTertiaryContainer = KinGreenOnContainer,
    error = AppleRedLight,
    onError = KinWhite,
    errorContainer = AppleRedContainerLight,
    onErrorContainer = AppleRedLight,
    // Warm stone page with white cards floating a step above it. Inset
    // tiles, tracks and badges use a slightly deeper stone so they still read
    // against a white card.
    background = KinPageBackgroundLight,
    onBackground = AppleLabelLight,
    surface = AppleBackgroundLight,
    onSurface = AppleLabelLight,
    surfaceVariant = AppleBackgroundLight,
    onSurfaceVariant = AppleGray,
    surfaceTint = KinGreenLight,
    inverseSurface = KinForest,
    inverseOnSurface = KinWhite,
    surfaceBright = AppleBackgroundLight,
    surfaceDim = KinStoneInset,
    surfaceContainer = KinStoneInset,
    surfaceContainerHigh = KinStoneInset,
    surfaceContainerHighest = KinStoneInset,
    surfaceContainerLow = AppleBackgroundLight,
    surfaceContainerLowest = AppleBackgroundLight,
    outline = AppleSeparatorLight,
    outlineVariant = AppleSeparatorLight,
    scrim = KinForest,
)

/** Soft, generous corners: 20 for cards, 28 for sheets and dialogs. Buttons stay fully rounded pills. */
private val KinShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Root theme wrapper applied once in [za.co.kinplus.app.MainActivity]. Kin+
 * is light mode only by design, there is no dark colour scheme, so this
 * ignores the device/system appearance setting entirely.
 */
@Composable
fun KinPlusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = KinPlusTypography,
        shapes = KinShapes,
        content = content
    )
}

/** An evergreen "on" state for every Switch in the app, via the theme's `tertiary` role. */
@Composable
fun kinSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = KinWhite,
    checkedTrackColor = MaterialTheme.colorScheme.tertiary,
    checkedBorderColor = MaterialTheme.colorScheme.tertiary,
    checkedIconColor = MaterialTheme.colorScheme.tertiary,
    uncheckedThumbColor = KinWhite,
    uncheckedTrackColor = AppleSeparatorLight,
    uncheckedBorderColor = AppleSeparatorLight
)
