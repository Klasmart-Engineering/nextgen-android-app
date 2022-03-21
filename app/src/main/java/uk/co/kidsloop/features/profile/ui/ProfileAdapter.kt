package uk.co.kidsloop.features.profile.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.ProfilesQuery
import uk.co.kidsloop.R

class ProfileAdapter(private val onProfileClicked: () -> Unit) :
    RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {
    private var dataSet: List<ProfilesQuery.Profile> = emptyList()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val textView: TextView = view.findViewById(R.id.name_textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fullName = dataSet[position].familyName + " " + dataSet[position].givenName
        holder.textView.text = fullName
        holder.itemView.setOnClickListener {
            onProfileClicked.invoke()
        }
    }

    override fun getItemCount() = dataSet.size

    fun refresh(dataSet: List<ProfilesQuery.Profile>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }
}
