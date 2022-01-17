package uk.co.kidsloop.features.preview

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class PreviewViewModel @Inject constructor() : ViewModel() {
   var isCameraGranted: Boolean = false
   var isMicGranted: Boolean = false
}