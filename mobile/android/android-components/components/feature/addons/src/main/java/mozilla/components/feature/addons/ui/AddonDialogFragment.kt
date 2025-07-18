/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.addons.ui

import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toDrawable
import mozilla.components.feature.addons.Addon
import mozilla.components.support.utils.ext.getParcelableCompat

@VisibleForTesting
internal const val KEY_ICON = "KEY_ICON"

/**
 * A generic [Addon] dialog which has an [Addon]'s icon.
 */
open class AddonDialogFragment : AppCompatDialogFragment() {
    init {
        arguments = arguments ?: Bundle()
    }

    @VisibleForTesting
    internal val safeArguments get() = requireNotNull(arguments)

    internal fun loadIcon(addon: Addon, iconView: AppCompatImageView) {
        val icon =
            safeArguments.getParcelableCompat(KEY_ICON, Bitmap::class.java)
        if (icon != null) {
            iconView.setImageDrawable(addon.icon?.toDrawable(iconView.resources))
        } else {
            safeArguments.putParcelable(KEY_ICON, addon.provideIcon())
            iconView.setIcon(addon)
        }
    }

    /**
     * Styling for the addon installation dialog.
     */
    data class PromptsStyling(
        val gravity: Int,
        val shouldWidthMatchParent: Boolean = false,
        @param:ColorRes val confirmButtonBackgroundColor: Int? = null,
        @param:ColorRes val confirmButtonTextColor: Int? = null,
        @param:ColorRes val confirmButtonDisabledBackgroundColor: Int? = null,
        @param:ColorRes val confirmButtonDisabledTextColor: Int? = null,
        @param:ColorRes val learnMoreLinkTextColor: Int? = null,
        val confirmButtonRadius: Float? = null,
    )
}
