package za.co.kinplus.app.ui.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import za.co.kinplus.app.R

/**
 * Shared building blocks for the dark-hero / white-sheet auth layout used by
 * both LoginScreen and RegisterScreen. The palette here is intentionally
 * monochrome, distinct from the app's green brand scheme used elsewhere.
 */

val AuthHeroBg = Color(0xFF0D0D0D)
val AuthInputFill = Color(0xFFF5F5F5)
val AuthDivider = Color(0xFFE0E0E0)
val AuthMuted = Color(0xFF9E9E9E)
val AuthBody = Color(0xFF212121)
val AuthPlaceholder = Color(0xFFBDBDBD)

enum class AuthTabKind { SIGN_IN, SIGN_UP }

/** Faint family-silhouette graphic (two adults, a child) used behind the dark hero/splash. */
@Composable
fun AuthFamilyGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun fx(v: Float) = v * w / 390f
        fun fy(v: Float) = v * h / 380f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x552E2E2E), Color.Transparent),
                center = Offset(fx(200f), fy(150f)),
                radius = fx(230f)
            ),
            radius = fx(230f),
            center = Offset(fx(200f), fy(150f))
        )

        val bodyBrush = Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF161616)))

        fun person(headCx: Float, headCy: Float, headR: Float, shoulderY: Float, shoulderW: Float, hemY: Float, hemW: Float) {
            drawCircle(bodyBrush, radius = fx(headR), center = Offset(fx(headCx), fy(headCy)))
            val sx1 = headCx - shoulderW / 2f; val sx2 = headCx + shoulderW / 2f
            val hx1 = headCx - hemW / 2f; val hx2 = headCx + hemW / 2f
            val path = Path().apply {
                moveTo(fx(sx1), fy(shoulderY))
                cubicTo(fx(sx1 - 12f), fy(shoulderY + 37f), fx(hx1 - 5f), fy(hemY - 35f), fx(hx1), fy(hemY))
                lineTo(fx(hx2), fy(hemY))
                cubicTo(fx(hx2 + 5f), fy(hemY - 35f), fx(sx2 + 12f), fy(shoulderY + 37f), fx(sx2), fy(shoulderY))
                close()
            }
            drawPath(path, brush = bodyBrush)
        }

        person(155f, 120f, 16f, 138f, 76f, 250f, 130f)
        person(205f, 160f, 11f, 173f, 44f, 250f, 70f)
        person(250f, 115f, 16f, 133f, 76f, 250f, 130f)
    }
}

/** Dark hero: logo, headline, subtitle, over a faint family-silhouette graphic. */
@Composable
fun AuthHero(headline: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(modifier.background(AuthHeroBg)) {
        AuthFamilyGraphic(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().padding(start = 24.dp, top = 24.dp, end = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(32.dp).border(1.5.dp, Color.White.copy(alpha = 0.38f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.app_name), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.weight(1f))

            Text(
                headline,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 32.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }
    }
}

/** The Sign In / Sign Up segmented control shown at the top of the sheet. */
@Composable
fun AuthTabRow(active: AuthTabKind, onSignIn: () -> Unit, onSignUp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF0F0F0))
            .padding(4.dp)
    ) {
        AuthTab(
            stringResource(R.string.tab_sign_in),
            selected = active == AuthTabKind.SIGN_IN,
            modifier = Modifier.weight(1f),
            onClick = onSignIn
        )
        AuthTab(
            stringResource(R.string.tab_sign_up),
            selected = active == AuthTabKind.SIGN_UP,
            modifier = Modifier.weight(1f),
            onClick = onSignUp
        )
    }
}

@Composable
private fun AuthTab(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color.Black else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else AuthMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Filled, rounded text field matching the sign-in design (grey fill, icon, no visible border). */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    supportingText: String? = null
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = AuthPlaceholder) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = AuthMuted) },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = AuthMuted
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AuthInputFill,
                unfocusedContainerColor = AuthInputFill,
                disabledContainerColor = AuthInputFill,
                focusedIndicatorColor = Color.Black,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = Color.Black,
                focusedTextColor = AuthBody,
                unfocusedTextColor = AuthBody
            )
        )
        if (supportingText != null) {
            Text(
                supportingText,
                color = AuthMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}
