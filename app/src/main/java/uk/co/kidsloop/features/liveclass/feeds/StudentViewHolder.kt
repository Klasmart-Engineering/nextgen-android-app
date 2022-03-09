package uk.co.kidsloop.features.liveclass.feeds

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding
import uk.co.kidsloop.features.liveclass.remoteviews.RemoteMediaCustomContainer

/**
 *  Created by paulbisioc on 09.03.2022
 */
class StudentViewHolder(private val binding: StudentFeedLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: StudentFeedItem) = with(binding) {
        val videoFeed = item.remoteView
        val videoFeedContainer = binding.studentVideoFeed
        if (videoFeed.parent != null) {
            (videoFeed.parent as RemoteMediaCustomContainer).removeRemoteMediaView()
        }

        when (item.hasHandRaised) {
            true -> { binding.studentVideoFeed.showHandRaised() }
            false -> { binding.studentVideoFeed.hideRaiseHand() }
        }

        videoFeedContainer.addRemoteMediaView(videoFeed)
    }

    fun update(bundle: Bundle) {
        if (bundle.containsKey(FeedsAdapter.HAS_RAISED_HAND)) {
            val newValue = bundle.getBoolean(FeedsAdapter.HAS_RAISED_HAND)
            if (newValue)
                binding.studentVideoFeed.showHandRaised()
            else
                binding.studentVideoFeed.hideRaiseHand()
        }
    }
}