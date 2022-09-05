package com.android.hopein.dancevideorecorder.ui.settings

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2CameraInfo.from
import androidx.camera.core.CameraX
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.hopein.dancevideorecorder.databinding.FragmentSettingsBinding
import java.util.Date.from

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(SettingsViewModel::class.java)

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        var cameraInfo = ProcessCameraProvider.getInstance(requireContext()).get().availableCameraInfos.filter {
            from(it).getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK
        }

        var supportedQualities = QualitySelector.getSupportedQualities(cameraInfo[0])
        var filteredQualities = arrayListOf (Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
            .filter { supportedQualities.contains(it) }

        handleBackCamQualities(filteredQualities)

        cameraInfo = ProcessCameraProvider.getInstance(requireContext()).get().availableCameraInfos.filter {
            from(it).getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT
        }

        supportedQualities = QualitySelector.getSupportedQualities(cameraInfo[0])
        filteredQualities = arrayListOf (Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
            .filter { supportedQualities.contains(it) }

        handleFrontCamQualities(filteredQualities)


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleBackCamQualities(filteredQualities: List<Quality>) {

        if(!filteredQualities.contains(Quality.UHD)){
            binding.bcUhd.visibility = View.GONE
        }
        if(!filteredQualities.contains(Quality.FHD)){
            binding.bcFhd.visibility = View.GONE
        }
        if(!filteredQualities.contains(Quality.HD)){
            binding.bcHd.visibility = View.GONE
        }
        if(!filteredQualities.contains(Quality.SD)){
            binding.bcSd.visibility = View.GONE
        }
    }

    private fun handleFrontCamQualities(filteredQualities: List<Quality>) {
        if(!filteredQualities.contains(Quality.UHD)){
            binding.fcUhd.visibility = View.GONE
        }
        if(!filteredQualities.contains(Quality.FHD)){
            binding.fcFhd.visibility = View.GONE
        }
        if(!filteredQualities.contains(Quality.HD)){
            binding.fcHd.visibility = View.GONE
        }
        if(!filteredQualities.contains(Quality.SD)){
            binding.fcSd.visibility = View.GONE
        }

    }
}