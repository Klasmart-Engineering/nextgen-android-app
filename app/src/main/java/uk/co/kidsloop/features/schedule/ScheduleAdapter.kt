package uk.co.kidsloop.features.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.R
import uk.co.kidsloop.app.utils.clickable
import uk.co.kidsloop.app.utils.convertTimestampIntoDate
import uk.co.kidsloop.app.utils.visible
import uk.co.kidsloop.databinding.ItemClassBinding
import uk.co.kidsloop.features.schedule.usecases.DataEntity

class ScheduleAdapter(
    private val onClassClicked: () -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    companion object {
        const val MAX_CLASSES_VISIBLE: Int = 6
    }

    private var dataSet: List<DataEntity> = emptyList()

    inner class ViewHolder(val binding: ItemClassBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemClassBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (dataSet[position].status) {
            "Started" -> {
                holder.binding.classStatus.setBackgroundResource(R.drawable.ic_class_live)
                holder.binding.joinClassBtn.visible()
                holder.binding.itemClass.clickable()
            }
            "NotStart" -> {
                holder.binding.classStatus.setBackgroundResource(R.drawable.ic_class_scheduled)
                holder.binding.itemClass.setBackgroundResource(R.drawable.rounded_blue_corners)
            }
            else -> {
                holder.binding.classStatus.setBackgroundResource(R.drawable.ic_class_ended)
                holder.binding.itemClass.setBackgroundResource(R.drawable.rounded_gray_corners)
            }
        }
        holder.binding.className.text = dataSet[position].title
        val startAt = convertTimestampIntoDate(dataSet[position].startAt, "hh:mm")
        val endAt = convertTimestampIntoDate(dataSet[position].endAt, "hh:mma")

        holder.binding.classTime.text = startAt.plus(" - ").plus(endAt)
        holder.binding.itemClass.setOnClickListener {
            onClassClicked.invoke()
        }
    }

    override fun getItemCount() = dataSet.size

    fun refresh(dataSet: List<DataEntity>) {
        val sortedData = dataSet
            .filter { it.status != "Closed" }
            .sortedBy { it.startAt }
            .take(MAX_CLASSES_VISIBLE)
        this.dataSet = sortedData
        notifyDataSetChanged()
    }
}
