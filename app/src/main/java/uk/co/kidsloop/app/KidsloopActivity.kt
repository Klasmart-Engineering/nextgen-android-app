package uk.co.kidsloop.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import uk.co.kidsloop.databinding.ActivityMainBinding

class KidsloopActivity : AppCompatActivity() {

    private val appComponent get() = (application as KidsloopApplication).appComponent

    val activityComponent by lazy {
        appComponent.newActivityComponentBuilder()
            .activity(this)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}