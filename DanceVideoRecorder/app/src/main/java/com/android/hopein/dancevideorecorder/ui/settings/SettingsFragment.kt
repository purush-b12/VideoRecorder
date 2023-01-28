package com.android.hopein.dancevideorecorder.ui.settings

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2CameraInfo.from
import androidx.camera.core.CameraX
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.hopein.dancevideorecorder.R
import com.android.hopein.dancevideorecorder.common.CommonUtils
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.audio_record_setting
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.back_cam_quality
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.front_cam_quality
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.song_record_setting
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.video_start_timer
import com.android.hopein.dancevideorecorder.databinding.FragmentSettingsBinding
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.launch
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

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleBackCamQualities(CommonUtils.backCameraResolutions)
        handleFrontCamQualities(CommonUtils.frontCameraResolutions)
        /*lifecycleScope.launch {
            initiateAd()
        }*/

        binding.checkBoxAudio.isChecked = SharedPreferenceHelper(requireContext()).getBoolean(audio_record_setting,true)
        binding.checkBoxBgMusic.isChecked = SharedPreferenceHelper(requireContext()).getBoolean(song_record_setting,false)

        handleBackCamSelectButton(SharedPreferenceHelper(requireContext()).getString(back_cam_quality,""), R.color.pink_600)
        handleFrontCamSelectButton(SharedPreferenceHelper(requireContext()).getString(front_cam_quality,""), R.color.pink_600)
        handleTimerSelectButton(SharedPreferenceHelper(requireContext()).getInt(video_start_timer,0),R.color.pink_600)

        binding.checkBoxAudio.setOnClickListener {
            if(binding.checkBoxAudio.isChecked){
                //binding.checkBoxAudio.isChecked = false
                SharedPreferenceHelper(requireContext()).putBoolean(audio_record_setting, true)
            }else{
                //binding.checkBoxAudio.isChecked = true
                SharedPreferenceHelper(requireContext()).putBoolean(audio_record_setting, false)
            }
        }

        binding.checkBoxBgMusic.setOnClickListener {
            if(binding.checkBoxBgMusic.isChecked){
                //binding.checkBoxBgMusic.isChecked = false
                SharedPreferenceHelper(requireContext()).putBoolean(song_record_setting, true)
            }else{
                //binding.checkBoxBgMusic.isChecked = true
                SharedPreferenceHelper(requireContext()).putBoolean(song_record_setting, false)
            }
        }

        binding.bcUhd.setOnClickListener {
            if(!CommonUtils.backCameraResolutions.contains(Quality.UHD)){
                Toast.makeText(context, "Not supported", Toast.LENGTH_LONG).show()
            }else{
                handleBackCamSelectButton(SharedPreferenceHelper(requireContext()).getString(back_cam_quality,""), R.color.new_primary)
                SharedPreferenceHelper(requireContext()).putString(back_cam_quality, Quality.UHD.toString())
                handleBackCamSelectButton(SharedPreferenceHelper(requireContext()).getString(back_cam_quality,""), R.color.pink_600)
            }
        }
        binding.bcFhd.setOnClickListener {
            if(!CommonUtils.backCameraResolutions.contains(Quality.FHD)){
                Toast.makeText(context, "Not supported", Toast.LENGTH_LONG).show()
            }else{
                handleBackCamSelectButton(SharedPreferenceHelper(requireContext()).getString(back_cam_quality,""), R.color.new_primary)
                SharedPreferenceHelper(requireContext()).putString(back_cam_quality, Quality.FHD.toString())
                handleBackCamSelectButton(SharedPreferenceHelper(requireContext()).getString(back_cam_quality,""), R.color.pink_600)
            }
        }
        binding.bcHd.setOnClickListener {
            if(!CommonUtils.backCameraResolutions.contains(Quality.HD)){
                Toast.makeText(context, "Not supported", Toast.LENGTH_LONG).show()
            }else{
                handleBackCamSelectButton(SharedPreferenceHelper(requireContext()).getString(back_cam_quality,""), R.color.new_primary)
                SharedPreferenceHelper(requireContext()).putString(back_cam_quality, Quality.HD.toString())
                handleBackCamSelectButton(SharedPreferenceHelper(requireContext()).getString(back_cam_quality,""), R.color.pink_600)
            }
        }
        binding.bcSd.setOnClickListener {
            if(!CommonUtils.backCameraResolutions.contains(Quality.SD)){
                Toast.makeText(context, "Not supported", Toast.LENGTH_LONG).show()
            }else{
                handleBackCamSelectButton(SharedPreferenceHelper(requireContext()).getString(back_cam_quality,""), R.color.new_primary)
                SharedPreferenceHelper(requireContext()).putString(back_cam_quality, Quality.SD.toString())
                handleBackCamSelectButton(SharedPreferenceHelper(requireContext()).getString(back_cam_quality,""), R.color.pink_600)
            }
        }


        binding.fcUhd.setOnClickListener {
            if(!CommonUtils.frontCameraResolutions.contains(Quality.UHD)){
                Toast.makeText(context, "Not supported", Toast.LENGTH_LONG).show()
            }else{
                handleFrontCamSelectButton(SharedPreferenceHelper(requireContext()).getString(front_cam_quality,""), R.color.new_primary)
                SharedPreferenceHelper(requireContext()).putString(front_cam_quality, Quality.UHD.toString())
                handleFrontCamSelectButton(SharedPreferenceHelper(requireContext()).getString(front_cam_quality,""), R.color.pink_600)
            }
        }
        binding.fcFhd.setOnClickListener {
            if(!CommonUtils.frontCameraResolutions.contains(Quality.FHD)){
                Toast.makeText(context, "Not supported", Toast.LENGTH_LONG).show()
            }else{
                handleFrontCamSelectButton(SharedPreferenceHelper(requireContext()).getString(front_cam_quality,""), R.color.new_primary)
                SharedPreferenceHelper(requireContext()).putString(front_cam_quality, Quality.FHD.toString())
                handleFrontCamSelectButton(SharedPreferenceHelper(requireContext()).getString(front_cam_quality,""), R.color.pink_600)
            }
        }
        binding.fcHd.setOnClickListener {
            if(!CommonUtils.frontCameraResolutions.contains(Quality.HD)){
                Toast.makeText(context, "Not supported", Toast.LENGTH_LONG).show()
            }else{
                handleFrontCamSelectButton(SharedPreferenceHelper(requireContext()).getString(front_cam_quality,""), R.color.new_primary)
                SharedPreferenceHelper(requireContext()).putString(front_cam_quality, Quality.HD.toString())
                handleFrontCamSelectButton(SharedPreferenceHelper(requireContext()).getString(front_cam_quality,""), R.color.pink_600)
            }
        }
        binding.fcSd.setOnClickListener {
            if(!CommonUtils.frontCameraResolutions.contains(Quality.SD)){
                Toast.makeText(context, "Not supported", Toast.LENGTH_LONG).show()
            }else{
                handleFrontCamSelectButton(SharedPreferenceHelper(requireContext()).getString(front_cam_quality,""), R.color.new_primary)
                SharedPreferenceHelper(requireContext()).putString(front_cam_quality, Quality.SD.toString())
                handleFrontCamSelectButton(SharedPreferenceHelper(requireContext()).getString(front_cam_quality,""), R.color.pink_600)
            }
        }

        binding.sec0.setOnClickListener {

            handleTimerSelectButton(SharedPreferenceHelper(requireContext()).getInt(video_start_timer,0), R.color.new_primary)
            SharedPreferenceHelper(requireContext()).putInt(video_start_timer, 0)
            handleTimerSelectButton(SharedPreferenceHelper(requireContext()).getInt(video_start_timer,0), R.color.pink_600)

        }
        binding.sec3.setOnClickListener {
            handleTimerSelectButton(SharedPreferenceHelper(requireContext()).getInt(video_start_timer,0), R.color.new_primary)
            SharedPreferenceHelper(requireContext()).putInt(video_start_timer, 3)
            handleTimerSelectButton(SharedPreferenceHelper(requireContext()).getInt(video_start_timer,0), R.color.pink_600)

        }
        binding.sec5.setOnClickListener {
            handleTimerSelectButton(SharedPreferenceHelper(requireContext()).getInt(video_start_timer,0), R.color.new_primary)
            SharedPreferenceHelper(requireContext()).putInt(video_start_timer, 5)
            handleTimerSelectButton(SharedPreferenceHelper(requireContext()).getInt(video_start_timer,0), R.color.pink_600)

        }
        binding.sec10.setOnClickListener {
            handleTimerSelectButton(SharedPreferenceHelper(requireContext()).getInt(video_start_timer,0), R.color.new_primary)
            SharedPreferenceHelper(requireContext()).putInt(video_start_timer, 10)
            handleTimerSelectButton(SharedPreferenceHelper(requireContext()).getInt(video_start_timer,0), R.color.pink_600)

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleBackCamQualities(filteredQualities: List<Quality>) {

        if(!filteredQualities.contains(Quality.UHD)){
            //binding.bcUhd.visibility = View.GONE
            binding.bcUhd.isEnabled = false
            binding.bcUhd.backgroundTintList = requireContext().getColorStateList(R.color.button_disabled);
        }
        if(!filteredQualities.contains(Quality.FHD)){
            //binding.bcFhd.visibility = View.GONE
            binding.bcFhd.isEnabled = false
            binding.bcFhd.backgroundTintList = requireContext().getColorStateList(R.color.button_disabled);

        }
        if(!filteredQualities.contains(Quality.HD)){
            //binding.bcHd.visibility = View.GONE
            binding.bcHd.isEnabled = false
            binding.bcHd.backgroundTintList = requireContext().getColorStateList(R.color.button_disabled);

        }
        if(!filteredQualities.contains(Quality.SD)){
            //binding.bcSd.visibility = View.GONE
            binding.bcSd.isEnabled = false
            binding.bcSd.backgroundTintList = requireContext().getColorStateList(R.color.button_disabled);

        }
    }

    private fun handleFrontCamQualities(filteredQualities: List<Quality>) {
        if(!filteredQualities.contains(Quality.UHD)){
            //binding.fcUhd.visibility = View.GONE
            binding.fcUhd.isEnabled = false
            binding.fcUhd.backgroundTintList = requireContext().getColorStateList(R.color.button_disabled);
        }
        if(!filteredQualities.contains(Quality.FHD)){
            //binding.fcFhd.visibility = View.GONE
            binding.fcFhd.isEnabled = false
            binding.fcFhd.backgroundTintList = requireContext().getColorStateList(R.color.button_disabled);

        }
        if(!filteredQualities.contains(Quality.HD)){
            //binding.fcHd.visibility = View.GONE
            binding.fcHd.isEnabled = false
            binding.fcHd.backgroundTintList = requireContext().getColorStateList(R.color.button_disabled);

        }
        if(!filteredQualities.contains(Quality.SD)){
            //binding.fcSd.visibility = View.GONE
            binding.fcSd.isEnabled = false
            binding.fcSd.backgroundTintList = requireContext().getColorStateList(R.color.button_disabled);

        }

    }

    private fun handleBackCamSelectButton(qualitySelected: String?, colorRes: Int) {

        if(qualitySelected != null){
            if(qualitySelected == Quality.UHD.toString()){
                //binding.bcUhd.isSelected = true
                binding.bcUhd.backgroundTintList = requireContext().getColorStateList(colorRes);
            }
            if(qualitySelected == Quality.FHD.toString()){
                //binding.bcFhd.isSelected = true
                binding.bcFhd.backgroundTintList = requireContext().getColorStateList(colorRes);
            }
            if(qualitySelected == Quality.HD.toString()){
                //binding.bcHd.isSelected = true
                binding.bcHd.backgroundTintList = requireContext().getColorStateList(colorRes);

            }
            if(qualitySelected == Quality.SD.toString()){
                //binding.bcSd.isSelected = true
                binding.bcSd.backgroundTintList = requireContext().getColorStateList(colorRes);

            }
        }

    }

    private fun handleFrontCamSelectButton(qualitySelected: String?,  colorRes: Int) {
        if(qualitySelected != null){
            if(qualitySelected == Quality.UHD.toString()){
                //binding.fcUhd.isSelected = true
                binding.fcUhd.backgroundTintList = requireContext().getColorStateList(colorRes);

            }
            if(qualitySelected == Quality.FHD.toString()){
                //binding.fcFhd.isSelected = true
                binding.fcFhd.backgroundTintList = requireContext().getColorStateList(colorRes);

            }
            if(qualitySelected == Quality.HD.toString()){
                //binding.fcHd.isSelected = true
                binding.fcHd.backgroundTintList = requireContext().getColorStateList(colorRes);

            }
            if(qualitySelected == Quality.SD.toString()){
                //binding.fcSd.isSelected = true
                binding.fcSd.backgroundTintList = requireContext().getColorStateList(colorRes);

            }
        }
    }

    private fun handleTimerSelectButton(timeSelected: Int?, colorRes: Int) {

        if(timeSelected != null){
            if(timeSelected == 0){
                binding.sec0.backgroundTintList = requireContext().getColorStateList(colorRes);
            }
            if(timeSelected == 3){
                binding.sec3.backgroundTintList = requireContext().getColorStateList(colorRes);
            }
            if(timeSelected == 5){
                binding.sec5.backgroundTintList = requireContext().getColorStateList(colorRes);

            }
            if(timeSelected == 10){
                binding.sec10.backgroundTintList = requireContext().getColorStateList(colorRes);
            }
        }

    }

    fun initiateAd(){
        MobileAds.initialize(requireContext()) {}
        //ads:adUnitId="ca-app-pub-5655866277681532/2609247934"
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        binding.adView.adListener = object: AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                // Code to be executed when an ad request fails.
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }
        }
    }
}