package uk.co.kidsloop.features.liveclass.feeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding.*
import java.util.*

class FeedsAdapter : RecyclerView.Adapter<StudentViewHolder>() {
    private val differ: AsyncListDiffer<StudentFeedItem> = AsyncListDiffer(this, DiffCallback())

    companion object {
        const val HAS_RAISED_HAND = "has_raised_hand"
    }

    private fun currentList(): List<StudentFeedItem> {
        return differ.currentList
    }

    fun submitList(list: List<StudentFeedItem>) {
        differ.submitList(list)
    }

    override fun getItemCount() = currentList().size

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
        holder.setIsRecyclable(false)
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

    fun addVideoFeed(clientId: String, remoteMediaView: View) {
        val newList = mutableListOf<StudentFeedItem>()
        currentList().forEach {
            newList.add(it)
        }
        newList.add(StudentFeedItem(remoteMediaView, clientId))
        submitList(newList)
    }

    fun addFirstVideoFeed(clientId: String, remoteMediaView: View) {
        val newList = mutableListOf<StudentFeedItem>()
        newList.add(0, StudentFeedItem(remoteMediaView, clientId))
        currentList().forEach {
            newList.add(it)
        }
        submitList(newList)
    }

    fun removeVideoFeed(clientId: String) {
        val newList = mutableListOf<StudentFeedItem>()
        val position = currentList().indexOfFirst { it.clientId == clientId }
        if (position > -1) {
            currentList().forEachIndexed { index, element ->
                if (index != position)
                    newList.add(element)
            }
        }
        submitList(newList)
    }

    fun onHandRaised(clientId: String?) {
        val newList = mutableListOf<StudentFeedItem>()
        val position = currentList().indexOfFirst { it.clientId == clientId }
        if (position > -1) {
            currentList().forEachIndexed { index, element ->
                if (index == position) {
                    (element.copy()).let {
                        it.hasHandRaised = true
                        newList.add(it)
                    }
                } else {
                    newList.add(element)
                }
            }
            Collections.swap(newList, 0, position)
        }
        submitList(newList)
    }

    fun onHandLowered(clientId: String?) {
        val newList = mutableListOf<StudentFeedItem>()
        val position = currentList().indexOfFirst { it.clientId == clientId }
        if (position > -1) {
            currentList().forEachIndexed { index, element ->
                if (index == position) {
                    (element.copy()).let {
                        it.hasHandRaised = false
                        newList.add(it)
                    }
                } else {
                    newList.add(element)
                }
            }
        }
        submitList(newList)
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
