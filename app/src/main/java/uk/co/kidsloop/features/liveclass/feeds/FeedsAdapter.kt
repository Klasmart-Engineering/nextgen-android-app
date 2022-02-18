package uk.co.kidsloop.features.liveclass.feeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding.*
import uk.co.kidsloop.features.liveclass.remoteviews.RemoteMediaCustomContainer

class FeedsAdapter : RecyclerView.Adapter<FeedsAdapter.StudentViewHolder>() {

    companion object {

        private const val SHOW_STUDENT_HAND_RAISED = "show_student_hand_raised"
        private const val HIDE_STUDENT_HAND_RAISED = "hide_student_hand_raised"

        private const val MAX_STUDENT_VIDEO_FEEDS = 4
    }

    private var remoteStudentFeeds = mutableListOf<StudentFeedItem>()

    override fun getItemCount() = remoteStudentFeeds.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        return StudentViewHolder(
            inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val feedItem = remoteStudentFeeds[position]
        holder.setIsRecyclable(false)
        val videoFeed = feedItem.view
        val videoFeedContainer = holder.binding.studentVideoFeed
        if (videoFeed.parent != null) {
            (videoFeed.parent as RemoteMediaCustomContainer).removeRemoteMediaView()
        }
        videoFeedContainer.addRemoteMediaView(videoFeed)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                SHOW_STUDENT_HAND_RAISED -> holder.binding.studentVideoFeed.showHandRaised()
                HIDE_STUDENT_HAND_RAISED -> holder.binding.studentVideoFeed.hideRaiseHand()
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class StudentViewHolder(val binding: StudentFeedLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun addVideoFeed(clientId: String, remoteMediaView: View) {
        val studentFeedsCount = remoteStudentFeeds.size
        if (studentFeedsCount < MAX_STUDENT_VIDEO_FEEDS) {
            remoteStudentFeeds.add(StudentFeedItem(remoteMediaView, clientId))
            notifyItemInserted(studentFeedsCount)
        }
    }

    fun removeVideoFeed(clientId: String) {
        val position = remoteStudentFeeds.indexOfFirst { it.id == clientId }
        if (position > -1) {
            remoteStudentFeeds.removeAt(position)
            notifyItemChanged(position)
        }
    }

    fun onHandRaised(clientId: String?) {
        val position = remoteStudentFeeds.indexOfFirst { it.id == clientId }
        if (position > -1) {
            notifyItemChanged(position, SHOW_STUDENT_HAND_RAISED)
        }
    }

    fun onHandLowered(clientId: String?) {
        val position = remoteStudentFeeds.indexOfFirst { it.id == clientId }
        if (position > -1) {
            notifyItemChanged(position, HIDE_STUDENT_HAND_RAISED)
        }
    }
}
