package uk.co.kidsloop.features.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.R
import uk.co.kidsloop.app.utils.clickable
import uk.co.kidsloop.app.utils.convertTimestampConsideringTimeZone
import uk.co.kidsloop.app.utils.visible
import uk.co.kidsloop.features.schedule.usecases.DataEntity

class ScheduleAdapter(
    private val onClassClicked: () -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    private var dataSet: List<DataEntity> = emptyList()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemClass: ConstraintLayout = itemView.findViewById(R.id.item_class)
        val classStatus: ImageView = itemView.findViewById(R.id.class_status)
        val className: TextView = itemView.findViewById(R.id.class_name)
        val teacherName: TextView = itemView.findViewById(R.id.teacher_name)
        val teacherInitials: TextView = itemView.findViewById(R.id.teacher_initials)
        val classTime: TextView = itemView.findViewById(R.id.class_time)
        val enterClass: RelativeLayout = itemView.findViewById(R.id.enter_class)
        val joinClassBtn: ImageView = itemView.findViewById(R.id.join_class_btn)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_class, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (dataSet[position].status) {
            "Started" -> {
                viewHolder.classStatus.setBackgroundResource(R.drawable.ic_class_live)
                viewHolder.joinClassBtn.visible()
                viewHolder.enterClass.clickable()
            }
            "NotStart" -> {
                viewHolder.classStatus.setBackgroundResource(R.drawable.ic_class_scheduled)
                viewHolder.itemClass.setBackgroundResource(R.drawable.rounded_blue_corners)
            }
            else -> {
                viewHolder.classStatus.setBackgroundResource(R.drawable.ic_class_ended)
                viewHolder.itemClass.setBackgroundResource(R.drawable.rounded_gray_corners)
            }
        }
        viewHolder.className.text = dataSet[position].title
        // viewHolder.teacherName.text = dataSet[position].teacherName
        // viewHolder.teacherInitials.text = getInitials(dataSet[position].teacherName)
        val startAt = convertTimestampConsideringTimeZone(dataSet[position].startAt, "hh:mm")
        val endAt = convertTimestampConsideringTimeZone(dataSet[position].endAt, "hh:mma")

        viewHolder.classTime.text = startAt.plus(" - ").plus(endAt)

        viewHolder.enterClass.setOnClickListener {
            if (viewHolder.joinClassBtn.isVisible)
                onClassClicked.invoke()
        }
    }

    override fun getItemCount() = dataSet.size

    fun refresh(dataSet: List<DataEntity>) {
        val sortedData = dataSet
            .filter { it.status != "Closed" }
            .sortedBy { it.startAt }
            .take(ScheduleFragment.MAX_CLASSES_VISIBLE)
        this.dataSet = sortedData
        notifyDataSetChanged()
    }
}
