package za.co.kinplus.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import za.co.kinplus.app.R

/**
 * Plus Jakarta Sans (SIL Open Font License), bundled in res/font. A warm,
 * rounded humanist sans that stays very legible at small sizes, which suits
 * the "calm guardian" look of the design refresh.
 */
val KinFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

private fun kinStyle(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = KinFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

// Larger, high-contrast type supports the "one-handed use under stress" goal
// from the Part 1B UI design section. Every style carries the brand font, so
// any Text that only sets fontSize/fontWeight still inherits it through
// LocalTextStyle.
val KinPlusTypography = Typography(
    displayLarge = kinStyle(FontWeight.ExtraBold, 48, 54, -0.8),
    displayMedium = kinStyle(FontWeight.ExtraBold, 40, 46, -0.6),
    displaySmall = kinStyle(FontWeight.ExtraBold, 34, 40, -0.5),
    headlineLarge = kinStyle(FontWeight.ExtraBold, 30, 36, -0.5),
    headlineMedium = kinStyle(FontWeight.Bold, 24, 30, -0.3),
    headlineSmall = kinStyle(FontWeight.Bold, 22, 28, -0.2),
    titleLarge = kinStyle(FontWeight.Bold, 20, 26, -0.2),
    titleMedium = kinStyle(FontWeight.SemiBold, 16, 22),
    titleSmall = kinStyle(FontWeight.SemiBold, 14, 20),
    bodyLarge = kinStyle(FontWeight.Normal, 16, 22),
    bodyMedium = kinStyle(FontWeight.Normal, 14, 20),
    bodySmall = kinStyle(FontWeight.Normal, 12, 16),
    labelLarge = kinStyle(FontWeight.SemiBold, 14, 18),
    labelMedium = kinStyle(FontWeight.SemiBold, 12, 16),
    labelSmall = kinStyle(FontWeight.SemiBold, 11, 14, 0.2),
)
