package uk.co.kidsloop.features.regionAndLanguage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.R
import uk.co.kidsloop.features.regionAndLanguage.data.Language

class LanguageAdapter(private val dataSet: List<Language>) :
    RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.item_name_textView)
        val checkmark: ImageView = itemView.findViewById(R.id.checkmark)

        init {
            itemView.setOnClickListener {
                checkmark.visibility = View.VISIBLE
                textView.setTextColor(ContextCompat.getColor(this.itemView.context, R.color.kidsloop_blue))
                Navigation.findNavController(itemView)
                    .navigate(LanguageFragmentDirections.languageToRegion())
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_language, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.textView.text = dataSet[position].name
    }

    override fun getItemCount() = dataSet.size
}
