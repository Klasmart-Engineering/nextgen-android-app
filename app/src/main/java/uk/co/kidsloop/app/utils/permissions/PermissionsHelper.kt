package uk.co.kidsloop.app.utils.permissions

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

/**
 *  Created by paulbisioc on 07.01.2022
 */

fun AppCompatActivity.isPermissionGranted(permission: String): Boolean {
    return isPermissionGrantedImpl(this, permission)
}

fun Fragment.isPermissionGranted(context: Context, permission: String): Boolean {
    return isPermissionGrantedImpl(context, permission)
}

private fun isPermissionGrantedImpl(context: Context, permission: String): Boolean {
    val isAndroidMOrLater: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    return if (isAndroidMOrLater.not()) {
        true
    } else {
        PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            context,
            permission
        )
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