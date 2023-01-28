package com.android.hopein.dancevideorecorder.ui

import android.Manifest
import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.hopein.dancevideorecorder.R
import com.android.hopein.dancevideorecorder.databinding.FragmentPermissionsBinding
import com.android.hopein.dancevideorecorder.databinding.FragmentSplashScreenBinding

class PermissionsFragment: Fragment() {

    private var _binding: FragmentPermissionsBinding? = null

    private val binding get() = _binding!!
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val tf = Typeface.createFromAsset(requireContext().assets, "Exo-Bold-Italic.otf")
        binding.permTitle.typeface = tf

        val tf1 = Typeface.createFromAsset(requireContext().assets, "ProximaNova-Light.otf")
        binding.permDesc.typeface = tf1

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permissionList = PERMISSIONS_REQUIRED.toMutableList()
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            //permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            PERMISSIONS_REQUIRED = permissionList.toTypedArray()
        }

        binding.permissionButton.setOnClickListener {
            if (!hasPermissions(requireContext())) {
                activityResultLauncher.launch(PERMISSIONS_REQUIRED)
            }
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        val navController = findNavController()
        if(hasPermissions(requireContext()))
            navController.navigate(R.id.action_permissionsFragment_to_navigation_home)
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

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in PERMISSIONS_REQUIRED && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

}