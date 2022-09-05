package com.android.hopein.dancevideorecorder.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.android.hopein.dancevideorecorder.R
import com.android.hopein.dancevideorecorder.databinding.FragmentHomeBinding
import com.android.hopein.dancevideorecorder.databinding.FragmentSplashScreenBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class SplashScreenFragment: Fragment() {

    private var _binding: FragmentSplashScreenBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSplashScreenBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val navController = findNavController()


        lifecycleScope.launchWhenResumed {
            delay(2 * 1000)
            if(hasPermissions(requireContext()))
                navController.navigate(R.id.action_splash_screen_to_navigation_home)
            else
                navController.navigate(R.id.action_splash_screen_to_navigation_permissions)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private var PERMISSIONS_REQUIRED = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO)

        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

}