package uk.co.kidsloop.features.liveclass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding
import uk.co.kidsloop.databinding.StudentFeedLayoutBinding.*
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia

class StudentsFeedAdapter : RecyclerView.Adapter<StudentsFeedAdapter.ViewHolder>() {

    private var remoteStudentsFeedList: ArrayList<SFURemoteMedia> = ArrayList(3)

    override fun getItemCount() = remoteStudentsFeedList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val videoFeedContainer =  holder.binding.studentVideoFeed
        val videoFeed = remoteStudentsFeedList[position].view
//        val layoutParams = ConstraintLayout.LayoutParams(0, 0)
//        videoFeed.layoutParams = layoutParams
        videoFeed.id = View.generateViewId()
        videoFeedContainer.addRemoteMediaView(videoFeed)

//        val constraintSet = ConstraintSet()
//        constraintSet.clone(videoFeedContainer)
//        constraintSet.constrainDefaultHeight(videoFeed.id, ConstraintSet.MATCH_CONSTRAINT)
//        constraintSet.constrainDefaultWidth(videoFeed.id, ConstraintSet.MATCH_CONSTRAINT)
//        constraintSet.setDimensionRatio(videoFeed.id, "4:3")
//        constraintSet.applyTo(videoFeedContainer)
    }

    inner class ViewHolder(val binding: StudentFeedLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun addVideoFeed(remoteMedia: SFURemoteMedia) {
        remoteStudentsFeedList.add(remoteMedia)
        notifyDataSetChanged()
    }

    fun removeVideoFeed() {
    }
}