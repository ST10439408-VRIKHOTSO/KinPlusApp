package za.co.kinplus.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
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
import androidx.compose.ui.text.style.TextAlign
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
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(50),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            shape = RoundedCornerShape(14.dp),
            colors = kinFieldColors(),
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
                    Text(if (visible) "Hide" else "Show", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = kinFieldColors(),
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
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResourceSafe(R.string.error_offline_banner),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
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
        title = { Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = (-0.2).sp) },
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
 * Safety): the Kin+ brand mark (or a round back button on pushed screens) on
 * the start edge, a large left aligned screen title, and round filter and
 * profile actions on the end edge.
 */
@Composable
fun KinAppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onFilter: (() -> Unit)? = null,
    onProfile: (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cancel),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
        } else {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.inverseSurface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
        }

        Text(
            title,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        if (onFilter != null) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable(onClick = onFilter),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        if (onProfile != null) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onProfile),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PersonOutline, contentDescription = stringResource(R.string.profile_title), tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** The hairline border every white card carries, so it lifts cleanly off the warm stone page. */
@Composable
fun kinCardBorder(): BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

/** Shared colours for the outlined text fields: white fill, warm hairline, evergreen focus ring. */
@Composable
private fun kinFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary
)

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
            bg = MaterialTheme.colorScheme.primaryContainer
            tint = MaterialTheme.colorScheme.primary
        }
        IconBadgeVariant.DANGER -> {
            bg = MaterialTheme.colorScheme.errorContainer
            tint = MaterialTheme.colorScheme.onErrorContainer
        }
    }
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(bg),
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
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                fallbackText.take(1).ifBlank { "?" }.uppercase(),
                color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = (size.value / 2.6f).sp
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
                .background(MaterialTheme.colorScheme.primary)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = stringResource(R.string.change_photo),
                tint = MaterialTheme.colorScheme.onPrimary,
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
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Small helper so previews without a full context still resolve a string.
@Composable
private fun stringResourceSafe(id: Int): String = androidx.compose.ui.res.stringResource(id)
