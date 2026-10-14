package za.co.kinplus.app.ui.common

import androidx.core.view.isVisible
import za.co.kinplus.app.databinding.ViewTopBarBinding

/**
 * Configures the shared top bar (layout/view_top_bar.xml). Main tabs show the
 * brand mark; pushed screens pass [onBack] to show a round back button
 * instead. Filter, profile and a text action only appear when a screen asks
 * for them.
 */
fun ViewTopBarBinding.setup(
    title: String,
    onBack: (() -> Unit)? = null,
    onFilter: (() -> Unit)? = null,
    onProfile: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    this.title.text = title

    btnBack.isVisible = onBack != null
    brandMark.isVisible = onBack == null
    btnBack.setOnClickListener { onBack?.invoke() }

    btnFilter.isVisible = onFilter != null
    btnFilter.setOnClickListener { onFilter?.invoke() }

    btnProfile.isVisible = onProfile != null
    btnProfile.setOnClickListener { onProfile?.invoke() }

    btnAction.isVisible = actionLabel != null && onAction != null
    btnAction.text = actionLabel
    btnAction.setOnClickListener { onAction?.invoke() }
}
