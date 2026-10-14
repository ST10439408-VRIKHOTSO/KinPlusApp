package za.co.kinplus.app.ui.theme

import androidx.compose.ui.graphics.Color

// Kin+ palette, "calm guardian" design refresh: a warm stone page, white
// cards with hairline borders floating on top, one deep evergreen tint for
// everything interactive, and red kept strictly for emergency and danger.
// Deep forest is the hero tone (auth hero, splash, map overlays).

// Evergreen: the brand tint, used for primary buttons, links, an enabled
// Switch, progress fills and active states.
val KinGreenLight = Color(0xFF0E6B4E)
val KinGreenDark = Color(0xFF3FB68B)    // lifted mint, for small accents on dark surfaces (live dots)

// Tinted evergreen containers (icon badges, nav indicator, success banners).
val KinGreenContainerLight = Color(0xFFE3F1EA)
val KinGreenOnContainer = Color(0xFF0A5039)
val KinGreenContainerDark = Color(0xFF16342A)

// Deep forest hero tone and a lighter forest used for its glow.
val KinForest = Color(0xFF0F2A21)
val KinForestGlow = Color(0xFF1F5A45)

// Red: danger, emergency and destructive actions only.
val AppleRedDark = Color(0xFFB42318)
val AppleRedLight = Color(0xFFD92D20)
val KinSosRedBright = Color(0xFFEF4B3E)

// Tinted red containers (Emergency Information badges, offline banner).
val AppleRedContainerLight = Color(0xFFFDECEA)
val AppleRedContainerDark = Color(0xFF3A1210)

// Secondary text and muted icons. Warmer and darker than the old system
// grey so it passes contrast on both the stone page and white cards.
val AppleGray = Color(0xFF66706B)

// Dark surfaces (kept for legacy call sites; the app is light only).
val AppleBackgroundDark = Color(0xFF0F2A21)
val AppleSecondaryBackgroundDark = Color(0xFF1C1C1E)
val AppleTertiaryBackgroundDark = Color(0xFF2C2C2E)
val AppleSeparatorDark = Color(0xFF38383A)
val AppleLabelDark = Color(0xFFFFFFFF)

// Light surfaces: warm stone page, white cards, a slightly deeper stone for
// inset tiles, tracks and badges, and a warm hairline for borders.
val KinPageBackgroundLight = Color(0xFFF4F2EE)
val KinStoneInset = Color(0xFFEFECE6)
val AppleBackgroundLight = Color(0xFFFFFFFF)
val AppleSeparatorLight = Color(0xFFE7E3DC)
val AppleLabelLight = Color(0xFF14201B)  // green tinted ink, softer than pure black

// Plain black/white for literal, non-tinted use (avatar chips, hero text).
val KinBlack = Color(0xFF14201B)
val KinWhite = Color(0xFFFFFFFF)

// Legacy aliases kept so existing call sites keep compiling without needing
// every file touched. New code should prefer MaterialTheme.colorScheme.*
val KinInk = KinForest
val KinSurfaceTint = KinPageBackgroundLight
val KinSurfaceDark = AppleSecondaryBackgroundDark
val KinBadgeLight = KinStoneInset
val KinBadgeDark = AppleTertiaryBackgroundDark
val KinDivider = AppleSeparatorLight
val KinDividerDark = AppleSeparatorDark
val KinMuted = AppleGray
val KinOnSurface = AppleLabelLight
val KinAccentGreen = KinGreenDark
val KinSosRed = AppleRedLight
val KinErrorContainerLight = AppleRedContainerLight
val KinErrorContainerDark = AppleRedContainerDark
val KinGreenLegacy = KinBlack
val KinSurface = KinSurfaceTint
