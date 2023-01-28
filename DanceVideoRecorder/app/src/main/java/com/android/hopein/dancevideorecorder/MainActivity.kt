package com.android.hopein.dancevideorecorder

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.hopein.dancevideorecorder.databinding.ActivityMainBinding
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"
private const val IMMERSIVE_FLAG_TIMEOUT = 500L

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var navView: BottomNavigationView

    private lateinit var bottomMenuIncludedFragmentList: Array<String>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val graph = navController.navInflater.inflate(R.navigation.mobile_navigation)

        bottomMenuIncludedFragmentList = arrayOf(
            getString(R.string.title_recorder),
            getString(R.string.title_videos),
            getString(R.string.title_settings))

        /*val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()*/

        /*navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navController.navigate(R.id.navigation_home, null, options)
                }
                R.id.navigation_videos -> {
                    navController.navigate(R.id.navigation_videos, null, options)
                }
                R.id.navigation_settings -> {
                    navController.navigate(R.id.navigation_settings, null, options)
                }
            }
            true

        }*/

        /*navView.setOnTabSelectListener(object :AnimatedBottomBar.OnTabSelectListener{
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {



            }

        })*/

            navController.addOnDestinationChangedListener { _, destination, _ ->
                lifecycleScope.launch {
                    if (bottomMenuIncludedFragmentList.contains(destination.label)) {
                        showNavigationBottom()
                        graph.setStartDestination(R.id.navigation_home)
                    } else {
                        hideNavigationBottom()
                    }
                }
            }
            navController.graph = graph
            navView.setupWithNavController(navController)
    }

    private fun showNavigationBottom()
    {
        navView.visibility = View.VISIBLE
    }

    private fun hideNavigationBottom()
    {
        navView.visibility = View.GONE
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val intent = Intent(KEY_EVENT_ACTION).apply { putExtra(KEY_EVENT_EXTRA, keyCode) }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

   /* override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Workaround for Android Q memory leak issue in IRequestFinishCallback$Stub.
            // (https://issuetracker.google.com/issues/139738913)
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }*/

    /*override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        binding.container.postDelayed({
            hideSystemUI()
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.container).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }*/

    companion object {

        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }

    /*private fun toggleBottomNavBehavior(scrollToHide: Boolean) {
        // Get the layout params of your BottomNavigation
        val layoutParams = binding.navView.layoutParams as CoordinatorLayout.LayoutParams

        // Apply LayoutBehavior to the layout params
        layoutParams.behavior =
            if (scrollToHide) HideBottomViewOnScrollBehavior<CoordinatorLayout>() else null

        // Add margin to Nav Host Fragment if the layout behavior is NOT added
        // bottom_nav_height is 56dp when using Material BottomNavigation
        binding.navHostMain.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(
                0, 0, 0, if (scrollToHide) 0 else resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
            )
        }
    }*/

}