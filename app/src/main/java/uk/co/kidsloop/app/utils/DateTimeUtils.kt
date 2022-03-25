package uk.co.kidsloop.app.utils

import android.text.format.DateFormat
import java.util.*

/**
 *  Created by paulbisioc on 24.03.2022
 */

fun convertTimestampIntoDate(time: Int, pattern: String): String {
    val cal: Calendar = Calendar.getInstance(Locale.ENGLISH) // TODO : change to Calendar.getInstance()
    cal.timeInMillis = time * 1000L
    return DateFormat.format(pattern, cal).toString()
}