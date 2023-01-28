package com.android.hopein.dancevideorecorder.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.camera.core.impl.utils.ContextUtil.getApplicationContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.hopein.dancevideorecorder.R
import com.android.hopein.dancevideorecorder.common.CommonUtils
import com.android.hopein.dancevideorecorder.databinding.FragmentSplashScreenBinding
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

        val tf = Typeface.createFromAsset(requireContext().assets, "Exo-Bold-Italic.otf")
        binding.appName.typeface = tf

        var animCrossFadeIn = AnimationUtils.loadAnimation(requireContext(),
            R.anim.slide_in_from_left);

        binding.imageView.startAnimation(animCrossFadeIn)
        binding.appName.startAnimation(animCrossFadeIn)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()


        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permissionList = PERMISSIONS_REQUIRED.toMutableList()
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            PERMISSIONS_REQUIRED = permissionList.toTypedArray()
        }


        lifecycleScope.launchWhenResumed {
            delay(1 * 1000)
            CommonUtils.findCameraResolutions(requireContext())
            if(hasPermissions(requireContext()))
                navController.navigate(R.id.action_splash_screen_to_navigation_home)
            else
                navController.navigate(R.id.action_splash_screen_to_navigation_permissions)
        }

    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private var PERMISSIONS_REQUIRED = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE)

        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

}