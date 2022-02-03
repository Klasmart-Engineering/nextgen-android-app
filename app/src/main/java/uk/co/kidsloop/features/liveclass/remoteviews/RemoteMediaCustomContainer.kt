package uk.co.kidsloop.features.liveclass.remoteviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.cardview.widget.CardView
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
        addView(remoteMediaView, 1)
    }

    fun removeRemoteMediaView() {
        removeViewAt(1)
    }

    fun showMicMuted() {
        binding.micStatusImageView.setImageResource(R.drawable.ic_mic_muted)
    }

    fun showMicTurnedOn() {
        binding.micStatusImageView.setImageResource(R.drawable.ic_mic_on)
    }

    fun showCameraTurnedOff() {
        binding.localVideoStudentOverlay.elevation = 10F
        binding.localVideoStudentOverlay.visible()
    }

    fun showCameraTurnedOn() {
        binding.localVideoStudentOverlay.elevation = 0F
        binding.localVideoStudentOverlay.invisible()
    }

    fun showHandRaised() {
        binding.raiseHandImageView.visible()
    }

    fun hideRaiseHand() {
        binding.raiseHandImageView.gone()
    }
}
