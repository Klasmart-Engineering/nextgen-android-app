package uk.co.kidsloop.features.liveclass.feeds

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 *  Created by paulbisioc on 15.03.2022
 */
abstract class GenericFeedViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
    abstract fun bind(item: FeedItem)
    abstract fun update(bundle: Bundle)
}