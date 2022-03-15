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
import timber.log.Timber
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding.*
import uk.co.kidsloop.liveswitch.Config
import java.util.*
import kotlin.math.min

class FeedsAdapter : RecyclerView.Adapter<StudentViewHolder>() {
    private val differ: AsyncListDiffer<StudentFeedItem> = AsyncListDiffer(this, DiffCallback())

    companion object {
        const val MAX_FEEDS_VISIBLE: Int = 4
        const val HAS_RAISED_HAND = "has_raised_hand"
        const val LOCAL_MEDIA_POSITION = 0
        const val ASSISTANT_TEACHER_POSITION = 1
    }

    private var _itemCount = MutableLiveData<Int>()
    val itemCount: LiveData<Int> get() = _itemCount

    private fun currentList(): List<StudentFeedItem> {
        return differ.currentList
    }

    private fun submitList(list: List<StudentFeedItem>) {
        differ.submitList(list)
        _itemCount.postValue(list.size)
        Timber.d(getItemCount().toString())
    }

    override fun getItemCount() = currentList().size

//    override fun onViewAttachedToWindow(holder: StudentViewHolder) {
//        if(holder.adapterPosition == ASSISTANT_TEACHER_POSITION)
//            holder.setIsRecyclable(false)
//        super.onViewAttachedToWindow(holder)
//    }
//
//    override fun onViewDetachedFromWindow(holder: StudentViewHolder) {
//        if(holder.adapterPosition == ASSISTANT_TEACHER_POSITION)
//            holder.setIsRecyclable(true)
//        super.onViewDetachedFromWindow(holder)
//    }

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
        val feedItem = currentList()[position]
        //holder.setIsRecyclable(false)
        holder.bind(feedItem)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val bundle = payloads[0] as Bundle
            holder.update(bundle)
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
        newList.add(0, StudentFeedItem(remoteMediaView, clientId, Config.LOCAL_ROLE))
        submitList(newList)
    }

    private fun addStudentVideoFeed(clientId: String, remoteMediaView: View) {
        val newList = currentList().toMutableList()
        if (isAssistantTeacherPresent()) {
            newList.add(ASSISTANT_TEACHER_POSITION + 1, StudentFeedItem(remoteMediaView, clientId, Config.STUDENT_ROLE))
        } else {
            newList.add(LOCAL_MEDIA_POSITION + 1, StudentFeedItem(remoteMediaView, clientId, Config.STUDENT_ROLE))
        }

        submitList(reorderRaisedHands(newList))
    }

    private fun addAssistantTeacherVideoFeed(clientId: String, remoteMediaView: View) {
        val newList = currentList().toMutableList()
        newList.add(
            ASSISTANT_TEACHER_POSITION,
            StudentFeedItem(remoteMediaView, clientId, Config.ASSISTANT_TEACHER_ROLE)
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

    private fun reorderRaisedHands(list: List<StudentFeedItem>): List<StudentFeedItem> {
        val newList = mutableListOf<StudentFeedItem>()
        val isAssistantPresent = isAssistantTeacherPresent()

        val localFeed = list[0]
        val assistantTeacherFeed = if (isAssistantPresent) list[1] else null

        val raisedHandsList = if (isAssistantPresent) {
            list.subList(2, list.size).filter { it.hasHandRaised }
        } else {
            list.subList(1, list.size).filter { it.hasHandRaised }
        }

        val loweredHandsList = if (isAssistantPresent) {
            list.subList(2, list.size).filter { !it.hasHandRaised }
        } else {
            list.subList(1, list.size).filter { !it.hasHandRaised }
        }

        newList.add(localFeed)
        if (assistantTeacherFeed != null)
            newList.add(assistantTeacherFeed)
        newList.addAll(raisedHandsList)
        newList.addAll(loweredHandsList)

        return newList
    }

    private class DiffCallback : DiffUtil.ItemCallback<StudentFeedItem>() {
        override fun areItemsTheSame(oldItem: StudentFeedItem, newItem: StudentFeedItem) =
            oldItem.clientId == newItem.clientId

        override fun areContentsTheSame(oldItem: StudentFeedItem, newItem: StudentFeedItem) =
            oldItem == newItem

        // This gets called when areItemsTheSame == true && areContentsTheSame == false
        override fun getChangePayload(oldItem: StudentFeedItem, newItem: StudentFeedItem): Any? {
            if (oldItem.clientId == newItem.clientId) {
                return if (oldItem.hasHandRaised == newItem.hasHandRaised)
                    super.getChangePayload(oldItem, newItem)
                else {
                    val diff = Bundle()
                    diff.putBoolean(HAS_RAISED_HAND, newItem.hasHandRaised)
                    diff
                }
            }

            return super.getChangePayload(oldItem, newItem)
        }
    }
}
