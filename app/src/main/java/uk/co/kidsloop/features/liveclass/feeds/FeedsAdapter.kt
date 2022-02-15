package uk.co.kidsloop.features.liveclass.feeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.databinding.LocalFeedLayoutBinding
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding.*
import uk.co.kidsloop.features.liveclass.localmedia.LocalMediaCustomContainer
import uk.co.kidsloop.features.liveclass.remoteviews.RemoteMediaCustomContainer

class FeedsAdapter : RecyclerView.Adapter<FeedsAdapter.FeedViewHolder>() {

    companion object {

        private const val SHOW_STUDENT_HAND_RAISED = "show_student_hand_raised"
        private const val HIDE_STUDENT_HAND_RAISED = "hide_student_hand_raised"

        private const val SHOW_LOCAL_MEDIA_HAND_RAISED = "show_local_media_hand_raised"
        private const val HIDE_LOCAL_MEDIA_HAND_RAISED = "hide_local_media_hand_raised"

        private const val SHOW_LOCAL_MIC_OFF = "show_local_mic_off"
        private const val SHOW_LOCAL_MIC_ON = "show_local_mic_on"

        private const val SHOW_LOCAL_MIC_DISABLED = "show_local_mic_disabled"

        private const val SHOW_LOCAL_CAM_ON = "show_local_cam_on"
        private const val SHOW_LOCAL_CAM_OFF = "show_local_cam_off"

        private const val MAX_STUDENT_VIDEO_FEEDS = 4
        private const val LOCAL_MEDIA_ID = "local_media_id"
    }

    private var remoteStudentFeeds = mutableListOf<FeedItem>()

    override fun getItemCount() = remoteStudentFeeds.size

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return FeedType.LOCAL_MEDIA_TYPE.viewType
        } else {
            return FeedType.STUDENT_TYPE.viewType
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        if (viewType == FeedType.LOCAL_MEDIA_TYPE.viewType) {
            return LocalMediaViewHolder(
                LocalFeedLayoutBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            return StudentViewHolder(
                inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val feedItem = remoteStudentFeeds[position]
        holder.setIsRecyclable(false)

        val videoFeed = feedItem.view
        if (holder is StudentViewHolder) {
            val videoFeedContainer = holder.binding.studentVideoFeed
            if (videoFeed.parent != null) {
                (videoFeed.parent as RemoteMediaCustomContainer).removeRemoteMediaView()
            }
            videoFeedContainer.addRemoteMediaView(videoFeed)
        } else if (holder is LocalMediaViewHolder) {
            val localMediaItem = feedItem as LocalMediaFeedItem
            val localMediaContainer = holder.binding.localMediaFeed
            if (videoFeed.parent != null) {
                (videoFeed.parent as LocalMediaCustomContainer).removeLocalMediaView()
            }
            if (localMediaItem.isMicOn) {
                localMediaContainer.showMicTurnedOn()
            } else {
                localMediaContainer.showMicMuted()
            }
            if (!localMediaItem.isCamOn) {
                localMediaContainer.showCameraTurnedOff()
            }
            localMediaContainer.addLocalMediaView(videoFeed)
        }
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                SHOW_STUDENT_HAND_RAISED -> (holder as StudentViewHolder).binding.studentVideoFeed.showHandRaised()
                HIDE_STUDENT_HAND_RAISED -> (holder as StudentViewHolder).binding.studentVideoFeed.hideRaiseHand()
                SHOW_LOCAL_MEDIA_HAND_RAISED -> (holder as LocalMediaViewHolder).binding.localMediaFeed.showHandRaised()
                HIDE_LOCAL_MEDIA_HAND_RAISED -> (holder as LocalMediaViewHolder).binding.localMediaFeed.hideRaiseHand()
                SHOW_LOCAL_MIC_ON -> (holder as LocalMediaViewHolder).binding.localMediaFeed.showMicTurnedOn()
                SHOW_LOCAL_MIC_OFF -> (holder as LocalMediaViewHolder).binding.localMediaFeed.showMicMuted()
                SHOW_LOCAL_CAM_ON -> (holder as LocalMediaViewHolder).binding.localMediaFeed.showCameraTurnedOn()
                SHOW_LOCAL_CAM_OFF -> (holder as LocalMediaViewHolder).binding.localMediaFeed.showCameraTurnedOff()
                SHOW_LOCAL_MIC_DISABLED -> (holder as LocalMediaViewHolder).binding.localMediaFeed.showMicDisabledMuted()
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class StudentViewHolder(val binding: StudentFeedLayoutBinding) :
        FeedViewHolder(binding.root)

    inner class LocalMediaViewHolder(val binding: LocalFeedLayoutBinding) : FeedViewHolder(binding.root)

    open inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    fun addLocalMedia(localMediaView: View?, isMicOn: Boolean, isCamOn: Boolean) {
        localMediaView?.let {
            remoteStudentFeeds.add(LocalMediaFeedItem(localMediaView, LOCAL_MEDIA_ID, isMicOn, isCamOn))
            notifyDataSetChanged()
        }
    }

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
            notifyDataSetChanged()
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

    fun toggleLocalMic(isMicOn: Boolean) {
        notifyItemChanged(0, if (isMicOn) SHOW_LOCAL_MIC_ON else SHOW_LOCAL_MIC_OFF)
    }

    fun toggleLocalCamera(isCamOn: Boolean) {
        notifyItemChanged(0, if (isCamOn) SHOW_LOCAL_CAM_ON else SHOW_LOCAL_CAM_OFF)
    }

    fun toggleHandRaised(isHandRaised: Boolean) {
        notifyItemChanged(0, if (isHandRaised) SHOW_LOCAL_MEDIA_HAND_RAISED else HIDE_LOCAL_MEDIA_HAND_RAISED)
    }

    fun showLocalMicDisabled(){
        notifyItemChanged(0, SHOW_LOCAL_MIC_DISABLED)
    }
}
