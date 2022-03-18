package uk.co.kidsloop.app.common

import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import uk.co.kidsloop.R

class ToastHelper constructor(
    private val layoutInflater: LayoutInflater,
    private val context: Activity,
    private val toastDetailsProvider: ToastDetailsProvider
) {

    private var toastView: View = layoutInflater.inflate(R.layout.custom_toast_layout, null)
    private var notificationToast: Toast? = null

    fun showCustomToast(
        isOverlayDisplayed: Boolean
    ) {
        val toastDetails = toastDetailsProvider.getToastDetails()
        toastDetails?.let {
            notificationToast?.cancel()
            notificationToast = Toast(context)
            if (isOverlayDisplayed) {
                notificationToast?.setGravity(Gravity.TOP or Gravity.FILL_HORIZONTAL, 0, 0)
            } else {
                notificationToast?.setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, 0)
            }
            toastView.findViewById<TextView>(R.id.status_textview).text = toastDetails.message
            toastView.findViewById<ImageView>(R.id.mic_muted_imageView).isVisible = toastDetails.isMicDisabled
            toastView.findViewById<ImageView>(R.id.cam_muted_imageView).isVisible = toastDetails.isCamDisabled
            notificationToast?.view = toastView
            notificationToast?.duration = Toast.LENGTH_LONG
            notificationToast?.show()
        }
    }

    fun onMicControlClicked() {
        val message = toastDetailsProvider.getMessageOnMicClicked()
        message?.let {
            notificationToast?.cancel()
            notificationToast = Toast(context)
            notificationToast?.setGravity(Gravity.TOP or Gravity.FILL_HORIZONTAL, 0, 0)
            toastView.findViewById<TextView>(R.id.status_textview).text = message
            toastView.findViewById<ImageView>(R.id.mic_muted_imageView).isVisible = true
            toastView.findViewById<ImageView>(R.id.cam_muted_imageView).isVisible = false
            notificationToast?.view = toastView
            notificationToast?.duration = Toast.LENGTH_LONG
            notificationToast?.show()
        }
    }

    fun onCamControlClicked() {
        val message = toastDetailsProvider.getMessageOnCamClicked()
        message?.let {
            notificationToast?.cancel()
            notificationToast = Toast(context)
            notificationToast?.setGravity(Gravity.TOP or Gravity.FILL_HORIZONTAL, 0, 0)
            toastView.findViewById<TextView>(R.id.status_textview).text = message
            toastView.findViewById<ImageView>(R.id.mic_muted_imageView).isVisible = false
            toastView.findViewById<ImageView>(R.id.cam_muted_imageView).isVisible = true
            notificationToast?.view = toastView
            notificationToast?.duration = Toast.LENGTH_LONG
            notificationToast?.show()
        }
    }
}
