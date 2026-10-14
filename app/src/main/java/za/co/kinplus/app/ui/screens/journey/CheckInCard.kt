package za.co.kinplus.app.ui.screens.journey

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import za.co.kinplus.app.R
import za.co.kinplus.app.data.checkin.CheckInScheduler
import za.co.kinplus.app.databinding.ViewCheckinCardBinding
import java.text.DateFormat
import java.util.Date

/**
 * "I'm OK" check in card shown on the Safe Journey screen (Batch 1). Lets the
 * user start or stop the timer run by [CheckInScheduler]. Create it in the
 * Fragment's constructor or onCreate (it registers a permission request), then
 * call [bind] once the view exists.
 */
class CheckInCard(private val fragment: Fragment) {

    private var binding: ViewCheckinCardBinding? = null
    private var minutes = 30

    // Android 13+ needs notification permission or the "Are you OK?" prompt cannot appear.
    private val permissionLauncher = fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.d(TAG, "Notification permission granted=$granted")
        startNow()
    }

    fun bind(view: ViewCheckinCardBinding) {
        binding = view
        val context = fragment.requireContext()

        view.intervalChips.removeAllViews()
        CheckInScheduler.intervalOptions.forEach { option ->
            val chip = LayoutInflater.from(context).inflate(R.layout.item_choice_chip, view.intervalChips, false) as Chip
            chip.id = android.view.View.generateViewId()
            chip.text = context.getString(R.string.checkin_minutes, option)
            chip.isChecked = option == minutes
            chip.setOnClickListener { minutes = option }
            view.intervalChips.addView(chip)
        }

        view.startButton.setOnClickListener {
            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needsPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else startNow()
        }
        view.okButton.setOnClickListener {
            // Confirming early restarts the timer for another full interval.
            CheckInScheduler.confirmOk(context)
            startNow()
        }
        view.stopButton.setOnClickListener {
            CheckInScheduler.stop(context)
            render()
        }
        render()
    }

    fun unbind() {
        binding = null
    }

    private fun startNow() {
        val context = fragment.context ?: return
        CheckInScheduler.start(context, minutes)
        Log.d(TAG, "Started check in every $minutes min")
        render()
    }

    /** Shows the interval picker and Start, or the due time with "I'm OK" and Stop while active. */
    private fun render() {
        val view = binding ?: return
        val context = view.root.context
        val dueAt = CheckInScheduler.dueAt(context)
        val active = dueAt != null
        view.checkinStatus.text = if (dueAt != null) {
            context.getString(R.string.checkin_active, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(dueAt)))
        } else {
            context.getString(R.string.checkin_sub)
        }
        view.checkinStatus.setTextColor(ContextCompat.getColor(context, if (active) R.color.kin_green else R.color.kin_muted))
        view.checkinStatus.typeface = androidx.core.content.res.ResourcesCompat.getFont(
            context, if (active) R.font.plus_jakarta_sans_semibold else R.font.plus_jakarta_sans_regular
        )
        view.intervalGroup.isVisible = !active
        view.startButton.isVisible = !active
        view.activeButtons.isVisible = active
    }

    private companion object {
        const val TAG = "CheckInCard"
    }
}
