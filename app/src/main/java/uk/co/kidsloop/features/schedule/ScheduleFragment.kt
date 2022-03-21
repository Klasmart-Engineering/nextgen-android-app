package uk.co.kidsloop.features.schedule

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.app.utils.gone
import uk.co.kidsloop.app.utils.visible
import uk.co.kidsloop.databinding.FragmentScheduleBinding
import java.io.IOException

@AndroidEntryPoint
class ScheduleFragment : BaseFragment(R.layout.fragment_schedule) {
    private val binding by viewBinding(FragmentScheduleBinding::bind)

    companion object {
        const val MAX_CLASSES_VISIBLE: Int = 6
        const val FIVE_MIN_IN_MILLIS = (5 * 60 * 1000).toLong()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val classes: MutableList<Class> = mutableListOf()

        try {
            val obj = JSONObject(loadJSONFromAsset())
            val jsonArray: JSONArray = obj.getJSONArray("data")
            for (i in (0 until jsonArray.length())) {
                val jsonObject: JSONObject = jsonArray.getJSONObject(i)
                val id: String = jsonObject.getString("id")
                val classTitle: String = jsonObject.getString("title")
                val startAt: Long = jsonObject.getLong("start_at")
                val endAt: Long = jsonObject.getLong("end_at")
                val classType: String = jsonObject.getString("class_type")
                val status: String = jsonObject.getString("status")

                classes.add(i, Class(id, classTitle, startAt, endAt, classType, status, "Bill Smith"))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        if (classes.size == 0) {
            binding.noClassTextview.visible()
        } else {
            binding.noClassTextview.gone()
            binding.classesRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                addItemDecoration(DividerItemDecorationLastExcluded(resources.getDimensionPixelSize(R.dimen.space_16)))
                adapter = ScheduleAdapter(
                    { onClassClicked() },
                    classes.filter { clazz -> clazz.classStatus != "Closed" }
                        .sortedBy { it.startAt }
                        .take(MAX_CLASSES_VISIBLE)
                        .toTypedArray()
                )
            }
        }
    }

    private fun onClassClicked() {
        findNavController().navigate(ScheduleFragmentDirections.scheduleToLogin())
    }

    private fun loadJSONFromAsset(): String {
        val json: String = try {
            val inputStream = requireActivity().assets.open("schedule.json")
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, charset("UTF-8"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            return ""
        }
        return json
    }
}
