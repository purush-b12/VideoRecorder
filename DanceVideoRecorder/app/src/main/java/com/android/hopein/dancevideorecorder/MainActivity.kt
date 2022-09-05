package com.android.hopein.dancevideorecorder

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.hopein.dancevideorecorder.databinding.ActivityMainBinding
import kotlinx.coroutines.delay

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

        /*lifecycleScope.launchWhenResumed {
            delay(2 * 1000)
            navController.navigate(R.id.action_splash_screen_to_navigation_home)
            navView.visibility = View.VISIBLE
        }

        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()

        navView.setOnItemReselectedListener { item ->
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

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (bottomMenuIncludedFragmentList.contains(destination.label))
            {
                showNavigationBottom()
                graph.setStartDestination(R.id.navigation_home)
            }
            else
            {
                hideNavigationBottom()
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

}