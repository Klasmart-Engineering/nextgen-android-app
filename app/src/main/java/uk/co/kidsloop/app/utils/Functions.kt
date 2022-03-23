package uk.co.kidsloop.app.utils

import android.content.Context
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import java.util.* // ktlint-disable no-wildcard-imports

/**
 *  Created by paulbisioc on 07.01.2022
 */

// Long Toast functions
fun Context.longToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun AppCompatActivity.longToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Fragment.longToast(message: String) {
    Toast.makeText(this.requireActivity(), message, Toast.LENGTH_LONG).show()
}

// Short Toast functions
fun Context.shortToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun AppCompatActivity.shortToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Fragment.shortToast(message: String) {
    Toast.makeText(this.requireActivity(), message, Toast.LENGTH_SHORT).show()
}

// View visibility functions
fun View?.visible() {
    this?.visibility = View.VISIBLE
}

fun View?.invisible() {
    this?.visibility = View.INVISIBLE
}

fun View?.gone() {
    this?.visibility = View.GONE
}

fun View.setVisible(visible: Boolean) {
    visibility = if (visible) {
        View.VISIBLE
    } else {
        View.INVISIBLE
    }
}

fun View?.enable() {
    this?.isEnabled = true
}

fun View?.disable() {
    this?.isEnabled = false
}

fun View?.clickable() {
    this?.isClickable = true
}

fun View?.unclickable() {
    this?.isClickable = false
}

// String functions
fun emptyString(): String = ""

fun convertTimestampIntoDate(time: Int, pattern: String): String {
    val cal: Calendar = Calendar.getInstance(Locale.ENGLISH) // TODO : change to Calendar.getInstance()
    cal.timeInMillis = time * 1000L
    return DateFormat.format(pattern, cal).toString()
}
