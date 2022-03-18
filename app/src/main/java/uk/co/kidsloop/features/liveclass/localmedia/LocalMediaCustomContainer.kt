package uk.co.kidsloop.features.liveclass.localmedia

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.view.updateLayoutParams
import uk.co.kidsloop.R
import uk.co.kidsloop.app.utils.gone
import uk.co.kidsloop.app.utils.visible
import uk.co.kidsloop.databinding.LocalMediaContainerBinding

class LocalMediaCustomContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val binding = LocalMediaContainerBinding.inflate(LayoutInflater.from(context), this)

    fun addLocalMediaView(localMediaView: View?) {
        if (getChildAt(0) != null)
            removeLocalMediaView()
        addView(localMediaView, 0)
    }

    fun removeLocalMediaView() {
        removeViewAt(0)
    }

    fun updateLocalMediaViewOrientationReverse() {
        updateLayoutParams { getChildAt(1)?.rotation = 180F }
    }

    fun updateLocalMediaViewOrientationDefault() {
        updateLayoutParams { getChildAt(1)?.rotation = 0F }
    }

    fun showMicMuted() {
        binding.micStatusImageView.setImageResource(R.drawable.ic_mic_muted)
    }

    fun showMicDisabledMuted() {
        binding.micStatusImageView.setImageResource(R.drawable.ic_mic_disabled_small)
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
        binding.raiseHandImageView.gone()
    }
}
