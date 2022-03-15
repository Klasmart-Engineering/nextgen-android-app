package uk.co.kidsloop.features.liveclass.feeds

import android.os.Bundle
import uk.co.kidsloop.databinding.LayoutFeedLocalBinding
import uk.co.kidsloop.features.liveclass.localmedia.LocalMediaCustomContainer

/**
 *  Created by paulbisioc on 15.03.2022
 */
class LocalViewHolder(private val binding: LayoutFeedLocalBinding) : GenericFeedViewHolder(binding) {
    override fun bind(item: FeedItem) = with(binding) {
        val videoFeed = item.videoFeedView
        val videoFeedContainer = binding.localVideoFeed
        if (videoFeed.parent != null) {
            (videoFeed.parent as LocalMediaCustomContainer).removeLocalMediaView()
        }

        when (item.hasHandRaised) {
            true -> { binding.localVideoFeed.showHandRaised() }
            false -> { binding.localVideoFeed.hideRaiseHand() }
        }

        videoFeedContainer.replaceLocalMediaView(videoFeed)
    }

    override fun update(bundle: Bundle) {
        if (bundle.containsKey(FeedsAdapter.HAS_RAISED_HAND)) {
            val newValue = bundle.getBoolean(FeedsAdapter.HAS_RAISED_HAND)
            if (newValue)
                binding.localVideoFeed.showHandRaised()
            else
                binding.localVideoFeed.hideRaiseHand()
        }
    }
}