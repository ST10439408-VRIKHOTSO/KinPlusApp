package za.co.kinplus.app.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import za.co.kinplus.app.R
import za.co.kinplus.app.auth.AuthManager
import javax.inject.Inject

/**
 * Splash / Authentication Gate (screen 1). Checks the current Firebase session
 * (FR-06) and routes to Home when signed in, or Login otherwise.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    fun isSignedIn(): Boolean = authManager.isSignedIn
}

@Composable
fun SplashScreen(
    onSignedIn: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Brief pause so the brand splash is visible, then route by session state.
    LaunchedEffect(Unit) {
        delay(900)
        if (viewModel.isSignedIn()) onSignedIn() else onSignedOut()
    }

    Box(Modifier.fillMaxSize().background(AuthHeroBg)) {
        AuthFamilyGraphic(Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.38f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.app_name),
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.tagline), color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.checking_session), color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
        }
    }
}
