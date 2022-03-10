package uk.co.kidsloop.features.regionAndLanguage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.R
import uk.co.kidsloop.features.regionAndLanguage.data.Language

class LanguageAdapter(
    private val onLanguageClicked: (Language) -> Unit,
    private val dataSet: Array<Language>
) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val textView: TextView = itemView.findViewById(R.id.item_name_textView)
        val checkmark: ImageView = itemView.findViewById(R.id.checkmark)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_language, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.textView.text = dataSet[position].name
        viewHolder.itemView.setOnClickListener {
            viewHolder.checkmark.visibility = View.VISIBLE
            viewHolder.textView.setTextColor(ContextCompat.getColor(it.context, R.color.kidsloop_blue))
            onLanguageClicked.invoke(dataSet[position])
        }
    }

    override fun getItemCount() = dataSet.size
}
