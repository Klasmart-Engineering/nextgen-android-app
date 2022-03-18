package uk.co.kidsloop.features.liveclass.feeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.features.liveclass.enums.CameraStatus
import uk.co.kidsloop.features.liveclass.enums.MicStatus
import uk.co.kidsloop.liveswitch.Config
import uk.co.kidsloop.databinding.LayoutFeedLocalBinding.inflate as inflateLocal
import uk.co.kidsloop.databinding.LayoutFeedStudentBinding.inflate as inflateStudent

class FeedsAdapter : RecyclerView.Adapter<GenericFeedViewHolder>() {
    private val differ: AsyncListDiffer<FeedItem> = AsyncListDiffer(this, DiffCallback())

    companion object {
        const val MAX_FEEDS_VISIBLE: Int = 4
        const val HAS_RAISED_HAND = "has_raised_hand"
        const val IS_CAMERA_TURNED_ON = "is_camera_turned_on"
        const val IS_MIC_MUTED = "is_mic_muted"
        const val IS_ORIENTATION_DEFAULT = "is_orientation_default"

        const val LOCAL_MEDIA_POSITION = 0
        const val ASSISTANT_TEACHER_POSITION = 1

        const val VIEW_TYPE_LOCAL = 10
        const val VIEW_TYPE_STUDENT = 11
    }

    // TODO: find a better way of monitoring the elements' number
    private var _itemCount = MutableLiveData<Int>()
    val itemCount: LiveData<Int> get() = _itemCount

    private fun currentList(): List<FeedItem> {
        return differ.currentList
    }

    private fun submitList(list: List<FeedItem>) {
        differ.submitList(list)
        _itemCount.postValue(list.size)
    }

    override fun getItemCount() = currentList().size

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_LOCAL
            else -> VIEW_TYPE_STUDENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericFeedViewHolder {
        when (viewType) {
            VIEW_TYPE_LOCAL -> {
                return LocalViewHolder(
                    inflateLocal(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_STUDENT -> {
                return StudentViewHolder(
                    inflateStudent(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> {
                return StudentViewHolder(
                    inflateStudent(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: GenericFeedViewHolder, position: Int) {
        val feedItem = currentList()[position]
        holder.bind(feedItem)
    }

    override fun onBindViewHolder(holder: GenericFeedViewHolder, position: Int, payloads: MutableList<Any>) {
        val viewType = getItemViewType(position)

        if (payloads.isNotEmpty()) {
            val bundle = payloads[0] as Bundle
            when (viewType) {
                VIEW_TYPE_LOCAL -> (holder as LocalViewHolder).update(bundle)
                VIEW_TYPE_STUDENT -> (holder as StudentViewHolder).update(bundle)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun addVideoFeed(clientId: String, remoteMediaView: View, role: String) {
        when (role) {
            Config.LOCAL_ROLE -> addLocalVideoFeed(clientId, remoteMediaView)
            Config.STUDENT_ROLE -> addStudentVideoFeed(clientId, remoteMediaView)
            Config.ASSISTANT_TEACHER_ROLE -> addAssistantTeacherVideoFeed(clientId, remoteMediaView)
        }
    }

    private fun addLocalVideoFeed(clientId: String, remoteMediaView: View) {
        val newList = currentList().toMutableList()
        newList.add(0, FeedItem(remoteMediaView, clientId, Config.LOCAL_ROLE))
        submitList(newList)
    }

    private fun addStudentVideoFeed(clientId: String, remoteMediaView: View) {
        val newList = currentList().toMutableList()
        if (isAssistantTeacherPresent())
            newList.add(ASSISTANT_TEACHER_POSITION + 1, FeedItem(remoteMediaView, clientId, Config.STUDENT_ROLE))
        else
            newList.add(LOCAL_MEDIA_POSITION + 1, FeedItem(remoteMediaView, clientId, Config.STUDENT_ROLE))

        submitList(reorderRaisedHands(newList))
    }

    private fun addAssistantTeacherVideoFeed(clientId: String, remoteMediaView: View) {
        val newList = currentList().toMutableList()
        newList.add(
            ASSISTANT_TEACHER_POSITION,
            FeedItem(remoteMediaView, clientId, Config.ASSISTANT_TEACHER_ROLE)
        )

        submitList(newList)
    }

    fun removeVideoFeed(clientId: String) {
        val newList = currentList().toMutableList()
        val positionToBeRemoved = currentList().indexOfFirst { it.clientId == clientId }
        if (positionToBeRemoved > -1) {
            newList.removeAt(positionToBeRemoved)
            submitList(newList)
        }
    }

    private fun isAssistantTeacherPresent(): Boolean {
        return if (currentList().size >= 2)
            currentList()[ASSISTANT_TEACHER_POSITION].role == Config.ASSISTANT_TEACHER_ROLE
        else
            false
    }

    fun onHandRaised(clientId: String?) {
        var firstPosition = 0
        val position = currentList().indexOfFirst { it.clientId == clientId }
        if (position > -1) {
            firstPosition =
                if (isAssistantTeacherPresent()) ASSISTANT_TEACHER_POSITION + 1 else ASSISTANT_TEACHER_POSITION

            val element = (currentList()[position]).copy()
            element.hasHandRaised = true

            val newList = currentList().toMutableList()
            newList.removeAt(position)
            newList.add(firstPosition, element)

            submitList(reorderRaisedHands(newList))
        }
    }

    fun onHandLowered(clientId: String?) {
        val position = currentList().indexOfFirst { it.clientId == clientId }
        if (position > -1) {
            val element = (currentList()[position]).copy()
            element.hasHandRaised = false

            val newList = currentList().toMutableList().apply { this[position] = element }
            submitList(reorderRaisedHands(newList))
        }
    }

    private fun reorderRaisedHands(list: List<FeedItem>): List<FeedItem> {
        val newList = mutableListOf<FeedItem>()
        val isAssistantPresent = isAssistantTeacherPresent()

        val localFeed = list[0]
        val assistantTeacherFeed = if (isAssistantPresent) list[1] else null

        val raisedHandsList = if (isAssistantPresent) {
            list.subList(ASSISTANT_TEACHER_POSITION + 1, list.size).filter { it.hasHandRaised }
        } else {
            list.subList(ASSISTANT_TEACHER_POSITION, list.size).filter { it.hasHandRaised }
        }

        val loweredHandsList = if (isAssistantPresent) {
            list.subList(ASSISTANT_TEACHER_POSITION + 1, list.size).filter { !it.hasHandRaised }
        } else {
            list.subList(ASSISTANT_TEACHER_POSITION, list.size).filter { !it.hasHandRaised }
        }

        newList.add(localFeed)
        if (assistantTeacherFeed != null)
            newList.add(assistantTeacherFeed)
        newList.addAll(raisedHandsList)
        newList.addAll(loweredHandsList)

        return newList
    }

    fun showCameraTurnedOn(position: Int) {
        val element = (currentList()[position]).copy()
        element.cameraStatus = CameraStatus.ON

        val newList = currentList().toMutableList().apply { this[position] = element }
        submitList(newList)
    }

    fun showCameraTurnedOff(position: Int) {
        val element = (currentList()[position]).copy()
        element.cameraStatus = CameraStatus.OFF

        val newList = currentList().toMutableList().apply { this[position] = element }
        submitList(newList)
    }

    fun showMicTurnedOn(position: Int) {
        val element = (currentList()[position]).copy()
        element.micStatus = MicStatus.ON

        val newList = currentList().toMutableList().apply { this[position] = element }
        submitList(newList)
    }

    fun showMicMuted(position: Int) {
        val element = (currentList()[position]).copy()
        element.micStatus = MicStatus.MUTED

        val newList = currentList().toMutableList().apply { this[position] = element }
        submitList(newList)
    }

    fun showMicDisabledMuted(position: Int) {
        val element = (currentList()[position]).copy()
        element.micStatus = MicStatus.DISABLED

        val newList = currentList().toMutableList().apply { this[position] = element }
        submitList(newList)
    }

    fun showLocalMediaHandRaised() {
        val element = (currentList()[0]).copy()
        element.hasHandRaised = true

        val newList = currentList().toMutableList().apply { this[0] = element }
        submitList(newList)
    }

    fun hideLocalMediaRaiseHand() {
        val element = (currentList()[0]).copy()
        element.hasHandRaised = false

        val newList = currentList().toMutableList().apply { this[0] = element }
        submitList(newList)
    }

    fun updateMediaViewOrientationDefault(position: Int) {
        val element = (currentList()[position]).copy()
        element.isOrientationDefault = true

        val newList = currentList().toMutableList().apply { this[position] = element }
        submitList(newList)
    }

    fun updateMediaViewOrientationReverse(position: Int) {
        val element = (currentList()[position]).copy()
        element.isOrientationDefault = false

        val newList = currentList().toMutableList().apply { this[position] = element }
        submitList(newList)
    }

    fun updateMicAndCameraStatus(newMicStatus: MicStatus, newCameraStatus: CameraStatus, position: Int) {
        val element = (currentList()[position]).copy()
        element.cameraStatus = newCameraStatus
        element.micStatus = newMicStatus

        val newList = currentList().toMutableList().apply { this[position] = element }
        submitList(newList)
    }

    private class DiffCallback : DiffUtil.ItemCallback<FeedItem>() {
        override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem) =
            oldItem.clientId == newItem.clientId

        override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem) =
            oldItem == newItem

        // This gets called when areItemsTheSame == true && areContentsTheSame == false
        override fun getChangePayload(oldItem: FeedItem, newItem: FeedItem): Any? {
            if (oldItem.clientId == newItem.clientId) {
                val diff = Bundle()

                if (oldItem.hasHandRaised != newItem.hasHandRaised)
                    diff.putBoolean(HAS_RAISED_HAND, newItem.hasHandRaised)

                if (oldItem.micStatus != newItem.micStatus)
                    diff.putSerializable(IS_MIC_MUTED, newItem.micStatus)

                if (oldItem.cameraStatus != newItem.cameraStatus)
                    diff.putSerializable(IS_CAMERA_TURNED_ON, newItem.cameraStatus)

                if (oldItem.isOrientationDefault != newItem.isOrientationDefault)
                    diff.putBoolean(IS_ORIENTATION_DEFAULT, newItem.isOrientationDefault)

                return diff
            }

            return super.getChangePayload(oldItem, newItem)
        }
    }
}
