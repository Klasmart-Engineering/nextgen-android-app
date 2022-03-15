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
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import uk.co.kidsloop.liveswitch.Config
import uk.co.kidsloop.databinding.LayoutFeedLocalBinding.inflate as inflateLocal
import uk.co.kidsloop.databinding.LayoutFeedStudentBinding.inflate as inflateStudent

class FeedsAdapter : RecyclerView.Adapter<GenericFeedViewHolder>() {
    private val differ: AsyncListDiffer<FeedItem> = AsyncListDiffer(this, DiffCallback())

    companion object {
        const val MAX_FEEDS_VISIBLE: Int = 4
        const val HAS_RAISED_HAND = "has_raised_hand"
        const val LOCAL_MEDIA_POSITION = 0
        const val ASSISTANT_TEACHER_POSITION = 1

        const val VIEW_TYPE_LOCAL = 10
        const val VIEW_TYPE_STUDENT = 11
    }

    private var _itemCount = MutableLiveData<Int>()
    val itemCount: LiveData<Int> get() = _itemCount

    private fun currentList(): List<FeedItem> {
        return differ.currentList
    }

    private fun submitList(list: List<FeedItem>) {
        differ.submitList(list)
        _itemCount.postValue(list.size)
        Timber.d(getItemCount().toString())
    }

    override fun getItemCount() = currentList().size

//    override fun onViewAttachedToWindow(holder: GenericFeedViewHolder) {
//        if(holder.adapterPosition == ASSISTANT_TEACHER_POSITION)
//            holder.setIsRecyclable(false)
//        super.onViewAttachedToWindow(holder)
//    }
//
//    override fun onViewDetachedFromWindow(holder: GenericFeedViewHolder) {
//        if(holder.adapterPosition == ASSISTANT_TEACHER_POSITION)
//            holder.setIsRecyclable(true)
//        super.onViewDetachedFromWindow(holder)
//    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            //0 -> VIEW_TYPE_LOCAL
            0 -> VIEW_TYPE_STUDENT
            else -> VIEW_TYPE_STUDENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericFeedViewHolder {
        Timber.d("FEEDS ADAPTER onCreateViewHolder")

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
        Timber.d("FEEDS ADAPTER onBindViewHolder")

        val feedItem = currentList()[position]
        Timber.d(feedItem.videoFeedView.toString())

        //holder.setIsRecyclable(false)
        holder.bind(feedItem)
    }

    override fun onBindViewHolder(holder: GenericFeedViewHolder, position: Int, payloads: MutableList<Any>) {
        Timber.d("FEEDS ADAPTER onBindViewHolder WITH payloads")

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
            Config.LOCAL_ROLE -> {
                addLocalVideoFeed(clientId, remoteMediaView)
                Timber.d("LOCAL ROLE ADDED")
                Timber.d(remoteMediaView.toString())
            }
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
        if (isAssistantTeacherPresent()) {
            newList.add(ASSISTANT_TEACHER_POSITION + 1, FeedItem(remoteMediaView, clientId, Config.STUDENT_ROLE))
        } else {
            newList.add(LOCAL_MEDIA_POSITION + 1, FeedItem(remoteMediaView, clientId, Config.STUDENT_ROLE))
        }

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

        runBlocking {
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
        }

        return newList
    }

    private class DiffCallback : DiffUtil.ItemCallback<FeedItem>() {
        override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem) =
            oldItem.clientId == newItem.clientId

        override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem) =
            oldItem == newItem

        // This gets called when areItemsTheSame == true && areContentsTheSame == false
        override fun getChangePayload(oldItem: FeedItem, newItem: FeedItem): Any? {
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
