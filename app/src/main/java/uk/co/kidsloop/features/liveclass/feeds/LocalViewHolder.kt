package uk.co.kidsloop.features.liveclass.feeds

import android.os.Bundle
import uk.co.kidsloop.databinding.LayoutFeedLocalBinding
import uk.co.kidsloop.features.liveclass.enums.CameraStatus
import uk.co.kidsloop.features.liveclass.enums.MicStatus
import uk.co.kidsloop.features.liveclass.localmedia.LocalMediaCustomContainer

/**
 *  Created by paulbisioc on 15.03.2022
 */
class LocalViewHolder(private val binding: LayoutFeedLocalBinding) : GenericFeedViewHolder(binding) {
    override fun bind(item: FeedItem) = with(binding) {
        val videoFeed = item.videoFeedView
        val videoFeedContainer = binding.localVideoFeed
        if (videoFeed.parent != null)
            (videoFeed.parent as LocalMediaCustomContainer).removeLocalMediaView()

        when (item.hasHandRaised) {
            true -> binding.localVideoFeed.showHandRaised()
            false -> binding.localVideoFeed.hideRaiseHand()
        }

        when (item.cameraStatus) {
            CameraStatus.ON -> binding.localVideoFeed.showCameraTurnedOn()
            CameraStatus.OFF -> binding.localVideoFeed.showCameraTurnedOff()
            CameraStatus.INITIAL -> binding.localVideoFeed.showCameraTurnedOff()
        }

        when (item.micStatus) {
            MicStatus.ON -> binding.localVideoFeed.showMicTurnedOn()
            MicStatus.MUTED -> binding.localVideoFeed.showMicMuted()
            MicStatus.DISABLED -> binding.localVideoFeed.showMicDisabledMuted()
            MicStatus.INITIAL -> binding.localVideoFeed.showMicMuted()
        }

        when (item.isOrientationDefault) {
            true -> { binding.localVideoFeed.updateLocalMediaViewOrientationDefault() }
            false -> { binding.localVideoFeed.updateLocalMediaViewOrientationReverse() }
        }

        videoFeedContainer.addLocalMediaView(videoFeed)
    }

    override fun update(bundle: Bundle) {
        if (bundle.containsKey(FeedsAdapter.HAS_RAISED_HAND)) {
            when (bundle.getBoolean(FeedsAdapter.HAS_RAISED_HAND)) {
                true -> binding.localVideoFeed.showHandRaised()
                false -> binding.localVideoFeed.hideRaiseHand()
            }
        }

        if (bundle.containsKey(FeedsAdapter.IS_MIC_MUTED)) {
            when (bundle.getSerializable(FeedsAdapter.IS_MIC_MUTED)) {
                MicStatus.MUTED -> binding.localVideoFeed.showMicMuted()
                MicStatus.ON -> binding.localVideoFeed.showMicTurnedOn()
                MicStatus.DISABLED -> binding.localVideoFeed.showMicDisabledMuted()
            }
        }

        if (bundle.containsKey(FeedsAdapter.IS_CAMERA_TURNED_ON)) {
            when (bundle.getSerializable(FeedsAdapter.IS_CAMERA_TURNED_ON)) {
                CameraStatus.ON -> binding.localVideoFeed.showCameraTurnedOn()
                CameraStatus.OFF -> binding.localVideoFeed.showCameraTurnedOff()
            }
        }

        if (bundle.containsKey(FeedsAdapter.IS_ORIENTATION_DEFAULT)) {
            when (bundle.getBoolean(FeedsAdapter.IS_ORIENTATION_DEFAULT)) {
                true -> binding.localVideoFeed.updateLocalMediaViewOrientationDefault()
                false -> binding.localVideoFeed.updateLocalMediaViewOrientationReverse()
            }
        }
    }
}