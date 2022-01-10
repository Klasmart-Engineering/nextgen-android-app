package uk.co.kidsloop.app.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import uk.co.kidsloop.R
import uk.co.kidsloop.data.enums.KidsloopPermissions

/**
 *  Created by paulbisioc on 07.01.2022
 */

const val PERMISSION_REQUEST_CODE = 1000

fun AppCompatActivity.requestPermission(permission: String, rationaleTitle: String, rationaleMessage: String) {
    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission).not()) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            PERMISSION_REQUEST_CODE
        )
    } else {
        showRationaleDialog(this, permission, rationaleTitle, rationaleMessage)
    }
}

fun AppCompatActivity.isPermissionGranted(permission: String): Boolean {
    return isPermissionGrantedImpl(this, permission)
}

fun Fragment.isPermissionGranted(context: Context, permission: String): Boolean {
    return isPermissionGrantedImpl(context, permission)
}

fun hasAllPermissions(context: Context) =
    arrayOf(KidsloopPermissions.RECORD_AUDIO.type, KidsloopPermissions.CAMERA.type).all {
        isPermissionGrantedImpl(context, it)
    }

private fun isPermissionGrantedImpl(context: Context, permission: String): Boolean {
    val isAndroidQOrLater: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    return if (isAndroidQOrLater.not()) {
        true
    } else {
        PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            context,
            permission
        )
    }
}

private fun showRationaleDialog(activity: AppCompatActivity, permission: String, title: String,  message: String) {
    AlertDialog.Builder(activity).apply {
        setTitle(title)
        setMessage(message)
        setPositiveButton(R.string.permission_rationale_dialog_positive_button_text) { _, _ ->
            ActivityCompat.requestPermissions(
                activity, arrayOf(permission),
                PERMISSION_REQUEST_CODE
            )
        }
        setNegativeButton(R.string.permission_rationale_dialog_negative_button_text) { dialog, _ ->
            dialog.dismiss()
        }
    }.run {
        create()
        show()
    }
}

fun showSettingsDialog(activity: AppCompatActivity) {
    AlertDialog.Builder(activity).apply {
        setTitle(R.string.settings_dialog_title)
        setMessage(R.string.settings_dialog_message)
        setPositiveButton(R.string.settings_dialog_positive_button_text) { _, _ ->
            startAppSettings(activity)
        }
        setNegativeButton(R.string.settings_dialog_negative_button_text) { dialog, _ ->
            dialog.dismiss()
        }
    }.run {
        create()
        show()
    }
}

private fun startAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri: Uri = Uri.fromParts("package", context.packageName, null)
    intent.data = uri
    context.startActivity(intent)
}