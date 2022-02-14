package uk.co.kidsloop.features.liveclass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding.*
import uk.co.kidsloop.features.liveclass.remoteviews.RemoteMediaCustomContainer

class StudentFeedsAdapter : RecyclerView.Adapter<StudentFeedsAdapter.ViewHolder>() {

    companion object {

        private const val SHOW_HAND_RAISED = "show_hand_raised"
        private const val HIDE_HAND_RAISED = "hide_hand_raised"
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
        holder.setIsRecyclable(false)
        val videoFeed = studentFeedItem.remoteView
        val videoFeedContainer = holder.binding.studentVideoFeed
        if (videoFeed.parent != null) {
            (videoFeed.parent as RemoteMediaCustomContainer).removeRemoteMediaView()
        }
        videoFeedContainer.addRemoteMediaView(videoFeed)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                SHOW_HAND_RAISED -> holder.binding.studentVideoFeed.showHandRaised()
                HIDE_HAND_RAISED -> holder.binding.studentVideoFeed.hideRaiseHand()
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
            notifyItemInserted(studentFeedsCount)
        }
    }

    fun removeVideoFeed(clientId: String) {
        val position = remoteStudentFeeds.indexOfFirst { it.clientId == clientId }
        if (position > -1) {
            remoteStudentFeeds.removeAt(position)
            notifyDataSetChanged()
        }
    }

    fun onHandRaised(clientId: String?) {
        val position = remoteStudentFeeds.indexOfFirst { it.clientId == clientId }
        if (position > -1) {
            notifyItemChanged(position, SHOW_HAND_RAISED)
        }
    }

    fun onHandLowered(clientId: String?) {
        val position = remoteStudentFeeds.indexOfFirst { it.clientId == clientId }
        if (position > -1) {
            notifyItemChanged(position, HIDE_HAND_RAISED)
        }
    }
}
