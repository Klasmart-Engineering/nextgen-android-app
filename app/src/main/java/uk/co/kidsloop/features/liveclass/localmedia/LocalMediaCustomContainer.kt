package uk.co.kidsloop.features.liveclass.localmedia

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.view.updateLayoutParams
import uk.co.kidsloop.R
import uk.co.kidsloop.app.utils.invisible
import uk.co.kidsloop.app.utils.visible
import uk.co.kidsloop.databinding.LocalMediaContainerBinding

class LocalMediaCustomContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val binding = LocalMediaContainerBinding.inflate(LayoutInflater.from(context), this)

    fun replaceLocalMediaView(remoteMediaView: View?) {
        removeLocalMediaView()
        addLocalMediaView(remoteMediaView)
    }

    private fun addLocalMediaView(localMediaView: View?) {
        addView(localMediaView, 1)
    }

    fun removeLocalMediaView() {
        removeViewAt(1)
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
        binding.localVideoStudentOverlay.elevation = 10F
        binding.localVideoStudentOverlay.visible()
    }

    fun showCameraTurnedOn() {
        binding.localVideoStudentOverlay.elevation = 0F
    }

    fun showHandRaised() {
        binding.raiseHandImageView.elevation = 10F
        binding.raiseHandImageView.visible()
    }

    fun hideRaiseHand() {
        binding.raiseHandImageView.elevation = 0F
        binding.raiseHandImageView.invisible()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
        val desiredHeight = parentHeight / 4 - resources.getDimensionPixelSize(R.dimen.space_8)
        val desiredWidth = parentHeight / 3
        this.setMeasuredDimension(desiredWidth, desiredHeight)
        measureChildren(
            MeasureSpec.makeMeasureSpec(desiredWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.EXACTLY)
        )
    }
}
