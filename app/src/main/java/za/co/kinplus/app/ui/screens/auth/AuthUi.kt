package za.co.kinplus.app.ui.screens.auth

import androidx.core.content.ContextCompat
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.ViewAuthTabsBinding

/** Highlights the active tab of the Sign In / Sign Up segmented control. */
fun ViewAuthTabsBinding.select(signIn: Boolean) {
    val context = root.context
    val ink = ContextCompat.getColor(context, R.color.kin_ink)
    val muted = ContextCompat.getColor(context, R.color.kin_muted)
    tabSignIn.setBackgroundResource(if (signIn) R.drawable.bg_segment_selected else 0)
    tabSignUp.setBackgroundResource(if (signIn) 0 else R.drawable.bg_segment_selected)
    tabSignIn.setTextColor(if (signIn) ink else muted)
    tabSignUp.setTextColor(if (signIn) muted else ink)
}
