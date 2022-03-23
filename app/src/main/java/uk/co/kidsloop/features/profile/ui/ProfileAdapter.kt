package uk.co.kidsloop.features.profile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.ProfilesQuery
import uk.co.kidsloop.app.utils.getInitials
import uk.co.kidsloop.databinding.ItemProfileBinding

class ProfileAdapter(private val onProfileClicked: () -> Unit) :
    RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

    private var dataSet: List<ProfilesQuery.Profile> = emptyList()

    inner class ViewHolder(val binding: ItemProfileBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemProfileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fullName = dataSet[position].givenName + " " + dataSet[position].familyName
        holder.binding.nameTextView.text = fullName
        holder.binding.initialsTextView.text = fullName.getInitials()
        holder.itemView.setOnClickListener {
            onProfileClicked.invoke()
        }
    }

    override fun getItemCount() = dataSet.size
    fun refresh(dataSet: List<ProfilesQuery.Profile>) {
        this.dataSet = dataSet.sortedBy { it.givenName }
        notifyDataSetChanged()
    }
}
