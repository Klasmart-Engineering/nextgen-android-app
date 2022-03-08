package uk.co.kidsloop.features.charts

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.anychart.AnyChart
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian3d
import com.anychart.data.Mapping
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.FragmentChartsBinding
import com.anychart.data.Set;
import com.anychart.enums.MarkerType
import com.anychart.graphics.vector.hatchfill.HatchFillType

/**
 *  Created by paulbisioc on 04.03.2022
 */
@AndroidEntryPoint
class ChartsFragment : BaseFragment(R.layout.fragment_charts) {
    private val binding by viewBinding(FragmentChartsBinding::bind)
    private val viewModel by viewModels<ChartsViewModel>()

    private lateinit var area3d: Cartesian3d
    private lateinit var set: Set
    private val seriesData = arrayListOf<DataEntry>()

    init {
        // student's class' date / student's attendance / student's sentiment
        seriesData.add(CustomDataEntry("08:00 28/02/2022", 1, 1))
        seriesData.add(CustomDataEntry("09:00 28/02/2022", 1, -1))
        seriesData.add(CustomDataEntry("10:00 28/02/2022", 1, 0))
        seriesData.add(CustomDataEntry("11:00 28/02/2022", 1, 0))
        seriesData.add(CustomDataEntry("08:00 01/03/2022", 1, 1))
        seriesData.add(CustomDataEntry("09:00 01/03/2022", 0, -1))
        seriesData.add(CustomDataEntry("10:00 01/03/2022", 1, 1))
        seriesData.add(CustomDataEntry("11:00 01/03/2022", 0, 1))
        seriesData.add(CustomDataEntry("08:00 02/03/2022", 1, 1))
        seriesData.add(CustomDataEntry("09:00 02/03/2022", 1, 0))
        seriesData.add(CustomDataEntry("10:00 02/03/2022", 1, 0))
        seriesData.add(CustomDataEntry("11:00 02/03/2022", 0, 1))
        seriesData.add(CustomDataEntry("08:00 03/03/2022", 1, 0))
        seriesData.add(CustomDataEntry("09:00 03/03/2022", 1, -1))
        seriesData.add(CustomDataEntry("10:00 03/03/2022", 0, 0))
        seriesData.add(CustomDataEntry("11:00 03/03/2022", 1, 1))
        seriesData.add(CustomDataEntry("08:00 04/03/2022", 0, 0))
        seriesData.add(CustomDataEntry("09:00 04/03/2022", 1, -1))
        seriesData.add(CustomDataEntry("10:00 04/03/2022", 1, 0))
        seriesData.add(CustomDataEntry("11:00 04/03/2022", 1, 1))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        area3d = AnyChart.area3d()

        set = Set.instantiate()
        val series1Data: Mapping = set.mapAs("{ x: 'x', value: 'value2'}")
        val series2Data: Mapping = set.mapAs("{ x: 'x', value: 'value'}")

        set.data(seriesData)

        val series1 = area3d.area(series1Data)
        series1.name("Attendance")
        series1.hovered().markers(false)
        series1.hatchFill(HatchFillType.DIAGONAL_CROSS, "#000", 0.6, 10.0)
        // Markers
        series1.markers(true)

        val series2 = area3d.area(series2Data)
        series2.name("The Student\\'s Sentiment")
        series2.hovered().markers(false)
        series2.hatchFill(HatchFillType.CONFETTI, "#000", 0.6, 20.0)
        // Markers
        series2.markers(true)
        series2.markers().type(MarkerType.STAR5)
        series2.markers().fill("gold")
        series2.markers().size(10)

        area3d.animation(true)
        // Zoom and move scrollbar
        area3d.xScroller(true)

        binding.chartsView.setChart(area3d)
    }

    internal class CustomDataEntry constructor(x: String?, value: Number?, value2: Number?) : ValueDataEntry(x, value) {
        init {
            setValue("value2", value2)
        }
    }
}