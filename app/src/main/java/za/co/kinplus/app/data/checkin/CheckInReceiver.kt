package za.co.kinplus.app.data.checkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import za.co.kinplus.app.R

/** Handles the "I'm OK" button on the check in notification. */
class CheckInReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_IM_OK) return
        Log.i(TAG, "I'm OK tapped from notification")
        CheckInScheduler.confirmOk(context)
        Toast.makeText(context, R.string.checkin_confirmed, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ACTION_IM_OK = "za.co.kinplus.app.action.CHECKIN_IM_OK"
        private const val TAG = "CheckInReceiver"
    }
}
