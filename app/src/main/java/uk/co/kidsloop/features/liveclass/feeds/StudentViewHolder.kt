package uk.co.kidsloop.features.liveclass.feeds

import android.os.Bundle
import uk.co.kidsloop.R
import uk.co.kidsloop.databinding.LayoutFeedStudentBinding
import uk.co.kidsloop.features.liveclass.remoteviews.RemoteMediaCustomContainer
import uk.co.kidsloop.liveswitch.Config

/**
 *  Created by paulbisioc on 09.03.2022
 */
class StudentViewHolder(private val binding: LayoutFeedStudentBinding) : GenericFeedViewHolder(binding) {
    override fun bind(item: FeedItem) = with(binding) {
        val videoFeed = item.videoFeedView
        val videoFeedContainer = binding.studentVideoFeed
        if (videoFeed.parent != null) {
            (videoFeed.parent as RemoteMediaCustomContainer).removeRemoteMediaView()
        }

        handleRaiseHand(item.hasHandRaised)
        handleBadge(item.role)

        videoFeedContainer.addRemoteMediaView(videoFeed)
    }

    override fun update(bundle: Bundle) {
        if (bundle.containsKey(FeedsAdapter.HAS_RAISED_HAND)) {
            val newValue = bundle.getBoolean(FeedsAdapter.HAS_RAISED_HAND)
            if (newValue)
                binding.studentVideoFeed.showHandRaised()
            else
                binding.studentVideoFeed.hideRaiseHand()
        }
    }

    private fun handleRaiseHand(hasHandRaised: Boolean) {
        when (hasHandRaised) {
            true -> binding.studentVideoFeed.showHandRaised()
            false -> binding.studentVideoFeed.hideRaiseHand()
        }
    }

    private fun handleBadge(role: String) {
        when (role) {
            Config.ASSISTANT_TEACHER_ROLE -> binding.studentVideoFeed.showBadge(R.drawable.ic_placeholder_assistant_teacher_badge)
            else -> binding.studentVideoFeed.hideBadge()
        }
    }
}