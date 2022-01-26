package uk.co.kidsloop.app.utils.permissions

/**
 *  Created by paulbisioc on 11.01.2022
 */
interface PermissionsDialogButtonsListener {
    fun onAllowClicked(permissions: Array<String>)
    fun onDenyClicked(permissions: Array<String>)
}