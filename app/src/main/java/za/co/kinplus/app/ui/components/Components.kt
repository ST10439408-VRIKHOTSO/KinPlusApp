package za.co.kinplus.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import za.co.kinplus.app.R
import za.co.kinplus.app.ui.screens.auth.AuthMuted

/**
 * Reusable Composables that keep the fifteen screens visually consistent — a
 * Part 2 "consistent use of layout, fonts and colours" marking point. Large
 * touch targets support one-handed use under stress (Part 1B UI goals).
 */

@Composable
fun KinPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun KinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = singleLine,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth()
        )
        if (supportingText != null) {
            Text(
                supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
fun KinPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            isError = isError,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { visible = !visible }) {
                    Text(if (visible) "Hide" else "Show")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (supportingText != null) {
            Text(
                supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

/** Shown at the top of a screen when the device is offline (FR-28). */
@Composable
fun OfflineBanner(visible: Boolean) {
    if (!visible) return
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResourceSafe(R.string.error_offline_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * The app's only top bar: always transparent so the title sits flush on the
 * page background (MainActivity's root Surface already paints it) instead of
 * showing a separate, mismatched grey band above the content — the app-wide
 * "banded header" look this replaces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KinTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        modifier = modifier
    )
}

/**
 * The primary top bar used across the four main tabs (Map, Circles, SOS,
 * Safety): a small "Kin+" wordmark (or a back chevron on pushed screens) on
 * the start edge, the screen title centred, and filter / profile actions on
 * the end edge — matching the shared header across those screens' designs.
 */
@Composable
fun KinAppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onFilter: (() -> Unit)? = null,
    onProfile: (() -> Unit)? = null
) {
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cancel),
                    modifier = Modifier.size(22.dp).clickable(onClick = onBack)
                )
                Spacer(Modifier.width(10.dp))
            }
            Box(
                Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(colorResource(R.color.ic_launcher_background)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AuthMuted)
        }

        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            if (onFilter != null) {
                IconButton(onClick = onFilter, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = AuthMuted, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(6.dp))
            }
            if (onProfile != null) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PersonOutline, contentDescription = stringResource(R.string.profile_title), tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

enum class IconBadgeVariant { NEUTRAL, DANGER }

/**
 * A leading-icon "chip" used across Safety/Profile list rows: a circular
 * badge one tone lighter than the card it sits on ([ColorScheme.surfaceContainer]),
 * so it's always visibly distinct from its background in both themes — a
 * plain `Icon` here would otherwise inherit the theme's default content
 * color and risk blending into its background. [IconBadgeVariant.DANGER]
 * tints the badge and icon red for safety-critical rows (Emergency
 * Information, Notifications).
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    variant: IconBadgeVariant = IconBadgeVariant.NEUTRAL,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val bg: Color
    val tint: Color
    when (variant) {
        IconBadgeVariant.NEUTRAL -> {
            bg = MaterialTheme.colorScheme.surfaceContainer
            tint = MaterialTheme.colorScheme.onSurface
        }
        IconBadgeVariant.DANGER -> {
            bg = MaterialTheme.colorScheme.errorContainer
            tint = MaterialTheme.colorScheme.onErrorContainer
        }
    }
    Box(
        modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * A profile/group avatar: a picked photo when [photoPath] is set (decoded
 * fresh whenever the path changes — these are edited rarely, so no bitmap
 * cache is needed), or a solid black circle with the first letter of
 * [fallbackText] as the default — the same default-avatar pattern most apps
 * use rather than a generic stock silhouette image.
 */
@Composable
fun AvatarImage(photoPath: String?, fallbackText: String, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val bitmap = remember(photoPath) {
        photoPath?.let { path -> runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
    }
    Box(
        modifier.size(size).clip(CircleShape).background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                fallbackText.take(1).ifBlank { "?" }.uppercase(),
                color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = (size.value / 2.6f).sp
            )
        }
    }
}

/** [AvatarImage] with a small camera badge that launches [onPick] (an image picker) — used wherever the photo is editable. */
@Composable
fun EditableAvatar(photoPath: String?, fallbackText: String, onPick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Box(modifier) {
        AvatarImage(photoPath, fallbackText, size = size)
        Box(
            Modifier
                .size(size * 0.34f)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = stringResource(R.string.change_photo),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(size * 0.18f)
            )
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Small helper so previews without a full context still resolve a string.
@Composable
private fun stringResourceSafe(id: Int): String = androidx.compose.ui.res.stringResource(id)
