package uk.co.kidsloop.features.liveclass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.R
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding.*

class StudentsFeedAdapter : RecyclerView.Adapter<StudentsFeedAdapter.ViewHolder>() {

    companion object {

        private const val MAX_STUDENT_VIDEO_FEEDS = 3
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
        val videoFeedContainer = holder.binding.studentVideoFeed
        val videoFeed = remoteStudentFeeds[position].remoteView
        val isHandRaised = remoteStudentFeeds[position].showHandRaised
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

        if (isHandRaised) {
            videoFeedContainer.showHandRaised()
        } else {
            videoFeedContainer.hideRaiseHand()
        }
    }

    inner class ViewHolder(val binding: StudentFeedLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun addVideoFeed(clientId: String, remoteMediaView: View) {
        val studentFeedsCount = remoteStudentFeeds.size
        if (studentFeedsCount < MAX_STUDENT_VIDEO_FEEDS) {
            remoteStudentFeeds.add(StudentFeedItem(remoteMediaView, clientId))
            notifyItemInserted(studentFeedsCount)
        }
    }

    fun removeVideoFeed(clientId: String) {
        val iterator = remoteStudentFeeds.iterator()
        var position = 0
        while (iterator.hasNext()) {
            val studentFeedItem = iterator.next()
            position = position.inc()
            if (studentFeedItem.clientId == clientId) {
                iterator.remove()
                break
            }
        }
        notifyItemRemoved(position)
    }

    fun onHandRaised(clientId: String) {
        var position = 0
        for (studentFeed in remoteStudentFeeds) {
            position = position.inc()
            if (studentFeed.clientId == clientId) {
                studentFeed.showHandRaised = true
                break
            }
        }
        notifyItemChanged(position)
    }

    fun onHandLowered(clientId: String) {
        var position = 0
        for (studentFeed in remoteStudentFeeds) {
            position = position.inc()
            if (studentFeed.clientId == clientId) {
                studentFeed.showHandRaised = false
                break
            }
        }
        notifyItemChanged(position)
    }
}