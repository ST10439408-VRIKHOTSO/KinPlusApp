package za.co.kinplus.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = KinGreenLight,
    onPrimary = KinWhite,
    primaryContainer = KinGreenContainerLight,
    onPrimaryContainer = KinGreenLight,
    secondary = KinGreenLight,
    onSecondary = KinWhite,
    secondaryContainer = KinGreenContainerLight,
    onSecondaryContainer = AppleLabelLight,
    tertiary = KinGreenLight,
    onTertiary = KinWhite,
    error = AppleRedLight,
    errorContainer = AppleRedContainerLight,
    onErrorContainer = AppleRedLight,
    // Page background is a soft off-white, not pure white — cards (surface /
    // surfaceVariant) are white and float a step above it, badges sit on a
    // third tier back at the page tone so they read against a white card.
    background = KinPageBackgroundLight,
    onBackground = AppleLabelLight,
    surface = AppleBackgroundLight,
    onSurface = AppleLabelLight,
    surfaceVariant = AppleBackgroundLight,
    onSurfaceVariant = AppleGray,
    surfaceContainer = KinPageBackgroundLight,
    surfaceContainerHigh = KinPageBackgroundLight,
    surfaceContainerHighest = KinPageBackgroundLight,
    surfaceContainerLow = AppleBackgroundLight,
    surfaceContainerLowest = KinPageBackgroundLight,
    outline = AppleSeparatorLight,
    outlineVariant = AppleSeparatorLight,
)

/**
 * Root theme wrapper applied once in [za.co.kinplus.app.MainActivity]. Kin+
 * is light-mode only by design — there is no dark colour scheme, so this
 * ignores the device/system appearance setting entirely.
 */
@Composable
fun KinPlusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = KinPlusTypography,
        content = content
    )
}

/** A green "on" state for every Switch in the app, via the theme's `tertiary` role. */
@Composable
fun kinSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = KinWhite,
    checkedTrackColor = MaterialTheme.colorScheme.tertiary,
    checkedBorderColor = MaterialTheme.colorScheme.tertiary,
    checkedIconColor = MaterialTheme.colorScheme.tertiary
)
