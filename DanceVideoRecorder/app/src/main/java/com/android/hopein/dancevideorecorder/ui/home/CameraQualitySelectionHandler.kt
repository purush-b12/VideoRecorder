package com.android.hopein.dancevideorecorder.ui.home

import android.content.Context
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.video.Quality
import com.android.hopein.dancevideorecorder.R
import com.android.hopein.dancevideorecorder.common.CommonUtils
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper
import com.android.hopein.dancevideorecorder.databinding.FragmentHomeBinding

object CameraQualitySelectionHandler {

    private var currentQuality: String? = null
    lateinit var mCurrentCamera: CameraSelector

    fun setQualitySelectionUI(binding: FragmentHomeBinding, context: Context, currentCamera: CameraSelector){
        mCurrentCamera = currentCamera
        if(currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA){
            currentQuality = SharedPreferenceHelper(context).getString(SharedPreferenceHelper.front_cam_quality,"")
            handleCamQualities(binding, CommonUtils.frontCameraResolutions)
            if(currentQuality == ""){
                //todo handle this case
            }else{
                handleCamSelectButton(binding,currentQuality!!)
            }

        }else if(currentCamera == CameraSelector.DEFAULT_BACK_CAMERA){
            currentQuality = SharedPreferenceHelper(context).getString(SharedPreferenceHelper.back_cam_quality,"")
            handleCamQualities(binding, CommonUtils.backCameraResolutions)
            if(currentQuality == ""){
                //todo handle this case
            }else{
                handleCamSelectButton(binding,currentQuality!!)
            }
        }

        binding.qualityFeature.qualitySd.setOnClickListener {
            qualityOnclickHandler(binding, currentQuality, Quality.SD, context)

        }
        binding.qualityFeature.qualityHd.setOnClickListener {
            qualityOnclickHandler(binding, currentQuality, Quality.HD, context)

        }
        binding.qualityFeature.qualityFhd.setOnClickListener {
            qualityOnclickHandler(binding, currentQuality, Quality.FHD, context)

        }
        binding.qualityFeature.qualityUhd.setOnClickListener {
            qualityOnclickHandler(binding, currentQuality, Quality.UHD, context)

        }

    }

    private fun qualityOnclickHandler(binding: FragmentHomeBinding, prevQuality: String?,selectedQuality: Quality, context: Context){
        if(!CommonUtils.backCameraResolutions.contains(selectedQuality)){
            Toast.makeText(context, "Not supported", Toast.LENGTH_LONG).show()
        }else{

            handleCamUnSelectButton(binding, prevQuality!!)
            if(mCurrentCamera == CameraSelector.DEFAULT_FRONT_CAMERA){
                SharedPreferenceHelper(context).putString(SharedPreferenceHelper.front_cam_quality, selectedQuality.toString())
            }else if(mCurrentCamera == CameraSelector.DEFAULT_BACK_CAMERA){
                SharedPreferenceHelper(context).putString(SharedPreferenceHelper.back_cam_quality, selectedQuality.toString())
            }
            handleCamSelectButton(binding, selectedQuality.toString())
        }
    }

    private fun handleCamQualities(binding: FragmentHomeBinding, filteredQualities: List<Quality>) {

        if(!filteredQualities.contains(Quality.UHD)){
            binding.qualityFeature.qualityUhd.isEnabled = false
            binding.qualityFeature.qualityUhd.setTextAppearance(R.style.TextviewAppearance_Home_Disabled )
        }
        if(!filteredQualities.contains(Quality.FHD)){
            binding.qualityFeature.qualityFhd.isEnabled = false
            binding.qualityFeature.qualityFhd.setTextAppearance(R.style.TextviewAppearance_Home_Disabled )

        }
        if(!filteredQualities.contains(Quality.HD)){
            binding.qualityFeature.qualityHd.isEnabled = false
            binding.qualityFeature.qualityHd.setTextAppearance(R.style.TextviewAppearance_Home_Disabled )

        }
        if(!filteredQualities.contains(Quality.SD)){
            binding.qualityFeature.qualitySd.isEnabled = false
            binding.qualityFeature.qualitySd.setTextAppearance(R.style.TextviewAppearance_Home_Disabled )

        }
    }

    private fun handleCamSelectButton(binding: FragmentHomeBinding, qualitySelected: String) {
            if(qualitySelected == Quality.UHD.toString()){
                binding.qualityFeature.qualityUhd.setTextAppearance(R.style.TextviewAppearance_Home_Selected )

            }
            if(qualitySelected == Quality.FHD.toString()){
                binding.qualityFeature.qualityFhd.setTextAppearance(R.style.TextviewAppearance_Home_Selected )

            }
            if(qualitySelected == Quality.HD.toString()){
                binding.qualityFeature.qualityHd.setTextAppearance(R.style.TextviewAppearance_Home_Selected )

            }
            if(qualitySelected == Quality.SD.toString()){
                binding.qualityFeature.qualitySd.setTextAppearance(R.style.TextviewAppearance_Home_Selected )

            }
    }

    private fun handleCamUnSelectButton(binding: FragmentHomeBinding, qualitySelected: String) {
        if(qualitySelected == Quality.UHD.toString()){
            binding.qualityFeature.qualityUhd.setTextAppearance(R.style.TextviewAppearance_Home_NotSelected )

        }
        if(qualitySelected == Quality.FHD.toString()){
            binding.qualityFeature.qualityFhd.setTextAppearance(R.style.TextviewAppearance_Home_NotSelected )

        }
        if(qualitySelected == Quality.HD.toString()){
            binding.qualityFeature.qualityHd.setTextAppearance(R.style.TextviewAppearance_Home_NotSelected )

        }
        if(qualitySelected == Quality.SD.toString()){
            binding.qualityFeature.qualitySd.setTextAppearance(R.style.TextviewAppearance_Home_NotSelected )

        }
    }
}