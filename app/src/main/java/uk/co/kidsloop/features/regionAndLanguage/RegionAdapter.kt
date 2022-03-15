package uk.co.kidsloop.features.regionAndLanguage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.R
import uk.co.kidsloop.app.utils.gone
import uk.co.kidsloop.app.utils.visible
import uk.co.kidsloop.features.regionAndLanguage.data.Region

class RegionAdapter(
    private val onRegionClicked: () -> Unit,
    private val dataSet: Array<Region>
) :
    RecyclerView.Adapter<RegionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val textView: TextView = view.findViewById(R.id.item_name_textView)
        val checkmark: ImageView = view.findViewById(R.id.checkmark)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_language, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.setOnClickListener {
            viewHolder.checkmark.visibility = View.VISIBLE
            viewHolder.textView.setTextColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.kidsloop_blue))
            onRegionClicked.invoke()
        }
        if (dataSet[position].isSelected) {
            viewHolder.checkmark.visible()
            viewHolder.textView.setTextColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.kidsloop_blue))
        } else {
            viewHolder.checkmark.gone()
            viewHolder.textView.setTextColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.dark_gray))
        }
        viewHolder.textView.text = dataSet[position].name
    }

    override fun getItemCount() = dataSet.size

    fun invalidateSelection() {
        dataSet.map { it.isSelected = false }
        notifyDataSetChanged()
    }
}
