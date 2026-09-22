package za.co.kinplus.app.ui.theme

import androidx.compose.ui.graphics.Color

// Kin+ palette: Apple-style layered surfaces (a soft page background, white/
// dark-grey cards one step "up", badges one step above that) with the
// original Kin+ green as the one tint colour — buttons, links, switches,
// active states — instead of blue. Muted and deliberately not vivid/neon.

// Green — the brand tint, used everywhere: primary buttons, links, an
// enabled Switch, progress fills, active states.
val KinGreenLight = Color(0xFF12805A)   // deep, muted — for light backgrounds
val KinGreenDark = Color(0xFF34A87C)    // lifted a little for contrast on dark backgrounds, still muted (not neon)

// Tinted green containers (e.g. a success banner).
val KinGreenContainerLight = Color(0xFFE1F0E9)
val KinGreenContainerDark = Color(0xFF16342A)

// Red — danger, emergency, destructive actions only.
val AppleRedDark = Color(0xFFFF453A)
val AppleRedLight = Color(0xFFFF3B30)

// Tinted red containers (e.g. Emergency Information badges) — pale pink on
// light backgrounds, a muted dark red on dark ones.
val AppleRedContainerLight = Color(0xFFFDE8E7)
val AppleRedContainerDark = Color(0xFF3A1210)

// System gray — secondary text, muted icons. Kept as one value across both
// themes; only the backgrounds it sits on change.
val AppleGray = Color(0xFF8E8E93)

// Dark theme surfaces (page → card → badge, each one step lighter).
val AppleBackgroundDark = Color(0xFF000000)
val AppleSecondaryBackgroundDark = Color(0xFF1C1C1E)
val AppleTertiaryBackgroundDark = Color(0xFF2C2C2E)
val AppleSeparatorDark = Color(0xFF38383A)
val AppleLabelDark = Color(0xFFFFFFFF)

// Light theme surfaces. The page itself is a soft off-white (not pure
// #FFFFFF, which read as too stark/bright) with white cards floating on top
// — the same "grouped list" depth Apple's own Settings/Health apps use.
val KinPageBackgroundLight = Color(0xFFD9D9DE)
val AppleBackgroundLight = Color(0xFFFFFFFF)
val AppleSeparatorLight = Color(0xFFC6C6C8)
val AppleLabelLight = Color(0xFF000000)

// Plain black/white for literal, non-tinted use (avatar chips, hero text).
val KinBlack = Color(0xFF000000)
val KinWhite = Color(0xFFFFFFFF)

// Legacy aliases kept so existing call sites keep compiling without needing
// every file touched — new code should prefer MaterialTheme.colorScheme.*
// (which is properly dynamic per theme) over these flat constants.
val KinInk = AppleBackgroundDark
val KinSurfaceTint = KinPageBackgroundLight
val KinSurfaceDark = AppleSecondaryBackgroundDark
val KinBadgeLight = KinPageBackgroundLight
val KinBadgeDark = AppleTertiaryBackgroundDark
val KinDivider = AppleSeparatorLight
val KinDividerDark = AppleSeparatorDark
val KinMuted = AppleGray
val KinOnSurface = AppleLabelLight
val KinAccentGreen = KinGreenDark
val KinSosRed = AppleRedDark
val KinErrorContainerLight = AppleRedContainerLight
val KinErrorContainerDark = AppleRedContainerDark
val KinGreenLegacy = KinBlack
val KinSurface = KinSurfaceTint
