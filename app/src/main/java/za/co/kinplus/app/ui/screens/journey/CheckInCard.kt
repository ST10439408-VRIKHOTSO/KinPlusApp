package za.co.kinplus.app.ui.screens.journey

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import za.co.kinplus.app.R
import za.co.kinplus.app.data.checkin.CheckInScheduler
import za.co.kinplus.app.ui.components.IconBadge
import za.co.kinplus.app.ui.components.kinCardBorder
import za.co.kinplus.app.ui.screens.auth.AuthMuted
import java.text.DateFormat
import java.util.Date

/**
 * "I'm OK" check in card shown on the Safe Journey screen (Batch 1). Lets the
 * user start or stop the timer run by [CheckInScheduler].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var dueAt by remember { mutableStateOf(CheckInScheduler.dueAt(context)) }
    var minutes by remember { mutableIntStateOf(30) }

    fun startNow() {
        CheckInScheduler.start(context, minutes)
        dueAt = CheckInScheduler.dueAt(context)
        Log.d("CheckInCard", "Started check in every $minutes min")
    }

    // Android 13+ needs notification permission or the "Are you OK?" prompt cannot appear.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.d("CheckInCard", "Notification permission granted=$granted")
        startNow()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = kinCardBorder(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.VerifiedUser, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.checkin_title), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    val active = dueAt
                    Text(
                        if (active != null) {
                            stringResource(R.string.checkin_active, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(active)))
                        } else {
                            stringResource(R.string.checkin_sub)
                        },
                        color = if (active != null) MaterialTheme.colorScheme.primary else AuthMuted,
                        fontSize = 13.sp,
                        fontWeight = if (active != null) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            if (dueAt == null) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.checkin_every), color = AuthMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckInScheduler.intervalOptions.forEach { option ->
                        FilterChip(
                            selected = minutes == option,
                            onClick = { minutes = option },
                            label = { Text(stringResource(R.string.checkin_minutes, option)) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (dueAt == null) {
                Button(
                    onClick = {
                        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else startNow()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50)
                ) { Text(stringResource(R.string.checkin_start), fontWeight = FontWeight.Bold) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            // Confirming early restarts the timer for another full interval.
                            CheckInScheduler.confirmOk(context)
                            startNow()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(50)
                    ) { Text(stringResource(R.string.checkin_im_ok), fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = {
                            CheckInScheduler.stop(context)
                            dueAt = null
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(50)
                    ) { Text(stringResource(R.string.checkin_stop), fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
