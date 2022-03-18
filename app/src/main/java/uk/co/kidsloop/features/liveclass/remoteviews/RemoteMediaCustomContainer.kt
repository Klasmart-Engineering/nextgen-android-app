package uk.co.kidsloop.features.liveclass.remoteviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import uk.co.kidsloop.R
import uk.co.kidsloop.app.utils.gone
import uk.co.kidsloop.app.utils.invisible
import uk.co.kidsloop.app.utils.visible
import uk.co.kidsloop.databinding.RemoteMediaContainerBinding

/**
 *  Created by paulbisioc on 01.02.2022
 */
class RemoteMediaCustomContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val binding = RemoteMediaContainerBinding.inflate(LayoutInflater.from(context), this)

    fun addRemoteMediaView(remoteMediaView: View?) {
        if (getChildAt(0) != null)
            removeRemoteMediaView()
        addView(remoteMediaView, 0)
    }

    fun removeRemoteMediaView() {
        removeViewAt(0)
    }

    fun showMicMuted() {
        binding.micStatusImageView.setImageResource(R.drawable.ic_mic_muted)
    }

    fun showMicTurnedOn() {
        binding.micStatusImageView.setImageResource(R.drawable.ic_mic_on)
    }

    fun showCameraTurnedOff() {
        binding.localVideoStudentOverlay.visible()
    }

    fun showCameraTurnedOn() {
        binding.localVideoStudentOverlay.gone()
    }

    fun showHandRaised() {
        binding.raiseHandImageView.visible()
    }

    fun hideRaiseHand() {
        binding.raiseHandImageView.invisible()
    }

    fun showBadge(badge: Int) {
        binding.badgeImageView.visible()
        binding.badgeImageView.setImageDrawable(ResourcesCompat.getDrawable(resources, badge, null))
    }

    fun hideBadge() {
        binding.badgeImageView.gone()
    }
}
