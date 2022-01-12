package uk.co.kidsloop.features.videostream

import uk.co.kidsloop.app.structure.BaseViewModel
import javax.inject.Inject

class LiveVideoStreamViewModel @Inject constructor() : BaseViewModel() {
    var isCameraGranted: Boolean = false
    var isMicGranted: Boolean = false
}