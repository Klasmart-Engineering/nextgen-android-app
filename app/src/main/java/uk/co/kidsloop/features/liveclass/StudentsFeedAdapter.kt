package uk.co.kidsloop.features.liveclass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.R
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding.*

class StudentsFeedAdapter : ListAdapter<StudentFeedItem, StudentsFeedAdapter.ViewHolder>(VideoFeedDiffCallBack()) {

    companion object {

        private const val RAISE_HAND = "raise_hand"
        private const val LOWER_HAND = "lower_hand"
        private const val MAX_STUDENT_VIDEO_FEEDS = 4
    }

    private var remoteStudentFeeds = mutableListOf<StudentFeedItem>()

    override fun getItemCount() = remoteStudentFeeds.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val studentFeedItem = remoteStudentFeeds[position]
        if (!studentFeedItem.isDisplayed) {
            studentFeedItem.isDisplayed = true
            holder.setIsRecyclable(false)
            val videoFeed = studentFeedItem.remoteView
            val videoFeedContainer = holder.binding.studentVideoFeed
            val layoutParams = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
            videoFeed.layoutParams = layoutParams
            videoFeed.id = View.generateViewId()
            videoFeed.setBackgroundResource(R.drawable.rounded_bg)
            videoFeed.clipToOutline = true
            videoFeedContainer.addRemoteMediaView(videoFeed)

            val constraintSet = ConstraintSet()
            constraintSet.clone(videoFeedContainer)
            constraintSet.constrainDefaultHeight(videoFeed.id, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.setDimensionRatio(videoFeed.id, "4:3")
            constraintSet.applyTo(videoFeedContainer)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                RAISE_HAND -> holder.binding.studentVideoFeed.showHandRaised()
                LOWER_HAND -> holder.binding.studentVideoFeed.hideRaiseHand()
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class ViewHolder(val binding: StudentFeedLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun addVideoFeed(clientId: String, remoteMediaView: View) {
        val studentFeedsCount = remoteStudentFeeds.size
        if (studentFeedsCount < MAX_STUDENT_VIDEO_FEEDS) {
            remoteStudentFeeds.add(StudentFeedItem(remoteMediaView, clientId))
            submitList(remoteStudentFeeds)
        }
    }

    fun removeVideoFeed(clientId: String) {
        val iterator = remoteStudentFeeds.iterator()
        var position = -1
        while (iterator.hasNext()) {
            val studentFeedItem = iterator.next()
            position = position.inc()
            if (studentFeedItem.clientId == clientId) {
                iterator.remove()
                break
            }
        }
        submitList(remoteStudentFeeds)
    }

    fun onHandRaised(clientId: String) {
        var position = -1
        for (studentFeed in remoteStudentFeeds) {
            position = position.inc()
            if (studentFeed.clientId == clientId) {
                break
            }
        }
        notifyItemChanged(position, RAISE_HAND)
    }

    fun onHandLowered(clientId: String) {
        var position = -1
        for (studentFeed in remoteStudentFeeds) {
            position = position.inc()
            if (studentFeed.clientId == clientId) {
                break
            }
        }
        notifyItemChanged(position, LOWER_HAND)
    }

    class VideoFeedDiffCallBack : DiffUtil.ItemCallback<StudentFeedItem>() {
        override fun areItemsTheSame(oldItem: StudentFeedItem, newItem: StudentFeedItem): Boolean {
            return oldItem.clientId == newItem.clientId
        }

        override fun areContentsTheSame(oldItem: StudentFeedItem, newItem: StudentFeedItem): Boolean {
            return oldItem.clientId == newItem.clientId
        }
    }
}