package za.co.kinplus.app.ui.common

import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.ItemRowBinding
import za.co.kinplus.app.databinding.ItemRowSwitchBinding

/**
 * Builds list rows inside a card. The short lists in Kin+ (settings, emergency
 * numbers, members of one circle) are inflated straight into a LinearLayout
 * with hairline dividers between them; long scrolling lists use a
 * RecyclerView instead.
 */

/** Removes all rows from the container, ready to be filled again. */
fun LinearLayout.clearRows() = removeAllViews()

/** Adds a hairline divider when the container already has a row. */
fun LinearLayout.addDividerIfNeeded() {
    if (childCount > 0) {
        LayoutInflater.from(context).inflate(R.layout.item_divider, this, true)
    }
}

/**
 * Adds a standard row: icon badge, title, subtitle and chevron.
 * Pass [onClick] as null for a row that is not tappable (no chevron).
 */
fun LinearLayout.addRow(
    title: String,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    danger: Boolean = false,
    trailing: String? = null,
    onClick: (() -> Unit)? = null
): ItemRowBinding {
    addDividerIfNeeded()
    val row = ItemRowBinding.inflate(LayoutInflater.from(context), this, true)
    row.title.text = title
    row.subtitle.setTextOrGone(subtitle)
    if (icon != null) {
        row.icon.setImageResource(icon)
        if (danger) {
            row.icon.setBackgroundResource(R.drawable.bg_icon_badge_danger)
            row.icon.imageTintList = ContextCompat.getColorStateList(context, R.color.kin_red)
        }
    } else {
        row.icon.isVisible = false
    }
    row.trailing.setTextOrGone(trailing)
    row.chevron.isVisible = onClick != null
    if (onClick != null) {
        row.root.setOnClickListener { onClick() }
    } else {
        row.root.isClickable = false
        row.root.background = null
    }
    return row
}

/** Adds an extra icon button at the end of a row (call, delete). */
fun ItemRowBinding.setEndAction(@DrawableRes icon: Int, @ColorRes tint: Int, description: String, onClick: () -> Unit) {
    endIcon.isVisible = true
    endIcon.setImageResource(icon)
    endIcon.imageTintList = ContextCompat.getColorStateList(root.context, tint)
    endIcon.contentDescription = description
    endIcon.setOnClickListener { onClick() }
}

/** Adds a row with a switch. [onChanged] only fires for taps by the user. */
fun LinearLayout.addSwitchRow(
    title: String,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onChanged: (Boolean) -> Unit
): ItemRowSwitchBinding {
    addDividerIfNeeded()
    val row = ItemRowSwitchBinding.inflate(LayoutInflater.from(context), this, true)
    row.title.text = title
    row.subtitle.setTextOrGone(subtitle)
    if (icon != null) {
        row.icon.isVisible = true
        row.icon.setImageResource(icon)
    }
    row.toggle.isChecked = checked
    row.toggle.isEnabled = enabled
    row.toggle.setOnCheckedChangeListener { button, isChecked ->
        if (button.isPressed) onChanged(isChecked)
    }
    return row
}

/**
 * Sets a switch's checked state from the ViewModel without firing its
 * listener as if the user had tapped it.
 */
fun ItemRowSwitchBinding.setCheckedSilently(checked: Boolean) {
    if (toggle.isChecked != checked) {
        toggle.isChecked = checked
    }
}
