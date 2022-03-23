package uk.co.kidsloop.features.profile.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.ProfilesQuery
import uk.co.kidsloop.R
import uk.co.kidsloop.app.utils.getInitials

class ProfileAdapter(private val onProfileClicked: () -> Unit) :
    RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {
    private var dataSet: List<ProfilesQuery.Profile> = emptyList()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val nameTextView: TextView = view.findViewById(R.id.name_textView)
        val initialsTextView: TextView = view.findViewById(R.id.initials_textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fullName = dataSet[position].givenName + " " + dataSet[position].familyName
        holder.nameTextView.text = fullName
        holder.initialsTextView.text = fullName.getInitials()
        holder.itemView.setOnClickListener {
            onProfileClicked.invoke()
        }
    }

    override fun getItemCount() = dataSet.size

    fun refresh(dataSet: List<ProfilesQuery.Profile>) {
        val sortedData = dataSet.sortedBy { it.givenName }
        this.dataSet = sortedData
        notifyDataSetChanged()
    }
}
