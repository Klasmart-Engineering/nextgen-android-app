package uk.co.kidsloop.features.schedule

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import uk.co.kidsloop.R
import uk.co.kidsloop.app.common.BaseFragment
import uk.co.kidsloop.app.utils.getInitials
import uk.co.kidsloop.app.utils.gone
import uk.co.kidsloop.app.utils.visible
import uk.co.kidsloop.databinding.FragmentScheduleBinding

@AndroidEntryPoint
class ScheduleFragment : BaseFragment(R.layout.fragment_schedule) {

    private val binding by viewBinding(FragmentScheduleBinding::bind)

    companion object {

        const val MAX_CLASSES_VISIBLE: Int = 6
        const val FIVE_MIN_IN_MILLIS = (5 * 60 * 1000).toLong()
        const val PROFILE_NAME = "profileName"
    }

    private val viewModel by viewModels<SchedulesViewModel>()

    private var profileName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        profileName = arguments?.getString(PROFILE_NAME)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.schedulesLiveData.observe(
            viewLifecycleOwner,
            Observer {
            }
        )

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

        binding.initials.text = getInitials(profileName)
        binding.welcomeLabel.text = getString(R.string.welcome_comma_first_name_of_user, profileName.split(" ")[0])
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
