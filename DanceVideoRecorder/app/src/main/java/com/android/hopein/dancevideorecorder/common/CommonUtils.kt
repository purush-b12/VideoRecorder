package com.android.hopein.dancevideorecorder.common

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.concurrent.futures.await
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.back_cam_quality
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.front_cam_quality
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.video_start_timer
import com.android.hopein.dancevideorecorder.ui.home.HomeFragment
import kotlinx.coroutines.async

object CommonUtils {

    var frontCameraResolutions: ArrayList<Quality> = ArrayList()
    var backCameraResolutions: ArrayList<Quality> = ArrayList()

    //var cameraCapabilities = mutableListOf<HomeFragment.CameraCapability>()

    data class Video(val uri: Uri,
                     val name: String,
                     val duration: String,
                     val size: Int,
                     val date: String
    )

    data class Photo(val uri: Uri,
                     val name: String,
                     val size: Int,
                     val date: String
    )

    @SuppressLint("UnsafeOptInUsageError")
    fun findCameraResolutions(context: Context){

        var cameraInfo = ProcessCameraProvider.getInstance(context).get().availableCameraInfos.filter {
            Camera2CameraInfo.from(it)
                .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK
        }

        var supportedQualities = QualitySelector.getSupportedQualities(cameraInfo[0])
        var filteredQualities = arrayListOf (Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
            .filter { supportedQualities.contains(it) }

        backCameraResolutions = filteredQualities as ArrayList<Quality>

        cameraInfo = ProcessCameraProvider.getInstance(context).get().availableCameraInfos.filter {
            Camera2CameraInfo.from(it)
                .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT
        }

        supportedQualities = QualitySelector.getSupportedQualities(cameraInfo[0])
        filteredQualities = arrayListOf (Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
            .filter { supportedQualities.contains(it) }

        frontCameraResolutions = filteredQualities as ArrayList<Quality>

        if(SharedPreferenceHelper(context).getString(back_cam_quality, "") == ""){
            setCamSelectionInPreference(context,back_cam_quality, backCameraResolutions)
        }

        if(SharedPreferenceHelper(context).getString(front_cam_quality, "") == ""){
            setCamSelectionInPreference(context,front_cam_quality, frontCameraResolutions)
        }

        setInitialStates(context)

    }

    private fun setCamSelectionInPreference(context: Context, camera:String, backCamRes: ArrayList<Quality>){
        if(backCamRes.contains(Quality.UHD)){
            SharedPreferenceHelper(context).putString(camera,Quality.UHD.toString())
        }
        else if(backCamRes.contains(Quality.FHD)){
            SharedPreferenceHelper(context).putString(camera,Quality.FHD.toString())
        }
        else if(backCamRes.contains(Quality.HD)){
            SharedPreferenceHelper(context).putString(camera,Quality.HD.toString())
        }
        else if(backCamRes.contains(Quality.SD)){
            SharedPreferenceHelper(context).putString(camera,Quality.SD.toString())
        }
    }

    private fun setInitialStates(context: Context){
        if(SharedPreferenceHelper(context).getInt(video_start_timer,-1) == -1){
            SharedPreferenceHelper(context).putInt(video_start_timer,0)
        }
    }

    /*fun findCameraCapabilities(context: Context){
        lScopeInstance = lifecycleScope.async {
            whenCreated {
                val provider = ProcessCameraProvider.getInstance(requireContext()).await()
                provider.unbindAll()

                for (camSelector in arrayOf(
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    CameraSelector.DEFAULT_FRONT_CAMERA
                )) {
                    try {
                        if (provider.hasCamera(camSelector)) {
                            val camera = provider.bindToLifecycle(requireActivity(), camSelector)
                            QualitySelector
                                .getSupportedQualities(camera.cameraInfo)
                                .filter { quality ->
                                    listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
                                        .contains(quality)
                                }.also {
                                    cameraCapabilities.add(
                                        HomeFragment.CameraCapability(
                                            camSelector,
                                            it
                                        )
                                    )
                                }
                        }
                    } catch (exc: java.lang.Exception) {
                        Log.e(HomeFragment.TAG, "Camera Face $camSelector is not supported")
                    }
                }
            }
        }
    }*/

}