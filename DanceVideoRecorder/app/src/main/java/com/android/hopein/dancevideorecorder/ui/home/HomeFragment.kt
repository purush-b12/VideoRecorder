package com.android.hopein.dancevideorecorder.ui.home

import android.annotation.SuppressLint
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.camera.core.*
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.android.hopein.dancevideorecorder.KEY_EVENT_ACTION
import com.android.hopein.dancevideorecorder.KEY_EVENT_EXTRA
import com.android.hopein.dancevideorecorder.MainActivity
import com.android.hopein.dancevideorecorder.R
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.audio_record_setting
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.grid_setting
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.torch_setting
import com.android.hopein.dancevideorecorder.common.SharedPreferenceHelper.Companion.video_start_timer
import com.android.hopein.dancevideorecorder.databinding.FragmentHomeBinding
import com.android.hopein.dancevideorecorder.extension.getAspectRatio
import com.android.hopein.dancevideorecorder.extension.getAspectRatioString
import com.android.hopein.dancevideorecorder.extension.getNameString
import com.android.hopein.dancevideorecorder.sdk.audio.AudioManagerImpl
import com.android.hopein.dancevideorecorder.ui.PermissionsFragment
import com.android.hopein.dancevideorecorder.ui.camerautils.Utils
import com.android.hopein.dancevideorecorder.ui.camerautils.VideoHandler
import com.android.hopein.dancevideorecorder.ui.common.CameraMode
import com.android.hopein.dancevideorecorder.ui.common.UiState
import com.android.hopein.dancevideorecorder.ui.common.ViewerMode
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var lScopeInstance: Deferred<Unit>? = null

    private lateinit var navController: NavController

    var homeViewModel: HomeViewModel? = null

    private var cameraIndex = 0
    private val cameraCapabilities = mutableListOf<CameraCapability>()

    data class CameraCapability(val camSelector: CameraSelector, val qualities:List<Quality>)

    private var qualitySelected: Quality? =null
    private lateinit var recordingState:VideoRecordEvent
    private val captureLiveStatus = MutableLiveData<String>()
    private var currentRecording: Recording? = null
    private lateinit var videoCapture: VideoCapture<Recorder>
    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }
    private var cameraX: Camera? = null

    private val mInterval: Long = 1000
    private var mHandler: Handler? = null
    private var timeInSeconds = 0L
    private var recordStartTime = 0L
    private lateinit var audioManagerImpl: AudioManagerImpl
    private var cameraMode: CameraMode? = null


    //photo
    private var imageCapture: ImageCapture? = null
    private lateinit var broadcastManager: LocalBroadcastManager
    private var displayId: Int = -1
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var outputDirectory: File

    private lateinit var windowManager: WindowManager

    private lateinit var cameraExecutor: ExecutorService
    val EXTENSION_WHITELIST = arrayOf("JPG")
    val ANIMATION_FAST_MILLIS = 50L
    val ANIMATION_SLOW_MILLIS = 100L


    /** Common */

    //Volume down button receiver used to trigger shutter
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    binding.recordButton.performClick()
                }
            }
        }
    }

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        @SuppressLint("RestrictedApi")
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@HomeFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")

                if(cameraMode == CameraMode.VIDEO){
                    videoCapture.targetRotation = view.display.rotation
                }else if(cameraMode == CameraMode.PHOTO){
                    imageCapture?.targetRotation = view.display.rotation
                }
            }
        } ?: Unit
    }

    private fun setGalleryThumbnail(uri: Uri) {
        // Run the operations in the view's thread
        binding.galleryButton.let { galleryButton ->
            galleryButton.post {
                // Remove thumbnail padding
                //galleryButton.setPadding(resources.getDimension(androidx.camera.core.R.dimen.stroke_small).toInt())

                // Load thumbnail into circular button using Glide
                Glide.with(galleryButton)
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(galleryButton)
            }
        }
    }

    private fun setExposureResource(){
        if(cameraX != null){
            if(cameraX!!.cameraInfo.exposureState.isExposureCompensationSupported){
                binding.exposureSlideLayout.visibility = View.VISIBLE
                val range = cameraX!!.cameraInfo.exposureState.exposureCompensationRange
                val index = cameraX!!.cameraInfo.exposureState.exposureCompensationIndex
                val step = cameraX!!.cameraInfo.exposureState.exposureCompensationStep
                binding.exposureSlide.valueFrom = range.lower.toFloat()
                binding.exposureSlide.valueTo = range.upper.toFloat()
                binding.exposureSlide.stepSize = step.toFloat()
                binding.exposureSlide.value = index.toFloat()

                binding.exposureSlide.addOnChangeListener { slider, value, fromUser ->
                    val ev = value * step.toFloat()
                    cameraX!!.cameraControl.setExposureCompensationIndex(value.toInt())
                }
            }
        }
    }

    private fun exposureChange(){
        if(cameraX != null){
            val index = cameraX!!.cameraInfo.exposureState.exposureCompensationIndex
            binding.exposureSlide.value = index.toFloat()
        }
    }

    private val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Get the camera's current zoom ratio
            val currentZoomRatio = cameraX!!.cameraInfo.zoomState.value?.zoomRatio ?: 0F
            val roundOff = (currentZoomRatio * 10.0).roundToInt() / 10.0

            if(roundOff > 1.0){
                binding.zoomValueLayout.visibility = View.VISIBLE
                binding.zoomValue.text = roundOff.toString() + "x"
            }else{
                binding.zoomValueLayout.visibility = View.GONE
            }
            // Get the pinch gesture's scaling factor
            val delta = detector.scaleFactor

            // Update the camera's zoom ratio. This is an asynchronous operation that returns
            // a ListenableFuture, allowing you to listen to when the operation completes.
            cameraX!!.cameraControl.setZoomRatio(currentZoomRatio * delta)

            // Return true, as the event was handled
            return true
        }
    }

    private fun getCameraSelector(idx: Int) : CameraSelector {
        if (cameraCapabilities.size == 0) {
            Log.i(TAG, "Error: This device does not have any camera, bailing out")
            //Todo check below line
            requireActivity().finish()
        }
        return (cameraCapabilities[idx % cameraCapabilities.size].camSelector)
    }

    init{

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
                                    cameraCapabilities.add(CameraCapability(camSelector, it))
                                }
                        }
                    } catch (exc: java.lang.Exception) {
                        Log.e(TAG, "Camera Face $camSelector is not supported")
                    }
                }
            }
        }
    }

    private fun initQuality(){
        val qualityList = cameraCapabilities[cameraIndex].qualities
        val selector = cameraCapabilities[cameraIndex].camSelector
        var selectedQuality: String? = ""

        if(selector ==  DEFAULT_BACK_CAMERA){
            selectedQuality = SharedPreferenceHelper(requireContext()).getString(SharedPreferenceHelper.back_cam_quality,"")
        }else if(selector == DEFAULT_FRONT_CAMERA){
            selectedQuality = SharedPreferenceHelper(requireContext()).getString(SharedPreferenceHelper.front_cam_quality,"")
        }

        if(selectedQuality!= null && selectedQuality != ""){
            if(selectedQuality == Quality.UHD.toString()){
                qualitySelected = Quality.UHD
            }
            if(selectedQuality == Quality.FHD.toString()){
                qualitySelected = Quality.FHD
            }
            if(selectedQuality == Quality.HD.toString()){
                qualitySelected = Quality.HD
            }
            if(selectedQuality == Quality.SD.toString()){
                qualitySelected = Quality.SD
            }
        } else{
            if(qualityList.contains(Quality.SD)){
                qualitySelected = Quality.SD

            }else if(qualityList.contains(Quality.HD)){
                qualitySelected = Quality.HD

            }else if(qualityList.contains(Quality.FHD)){
                qualitySelected = Quality.FHD

            }else if(qualityList.contains(Quality.UHD)){
                qualitySelected = Quality.UHD

            }
        }

    }

    private fun enableUI(){

        if (cameraCapabilities.size <= 1) {
            binding.recordButton.isEnabled = false
        }

        if(cameraMode == CameraMode.PHOTO){
            binding.recordButton.setBackgroundResource(R.drawable.ic_capture_photo)
            binding.recordTimer.visibility = View.GONE
            //binding.camVideoRepButton.setBackgroundResource(R.drawable.ic_videocam)
            //todo update gallery photo
        }else if(cameraMode == CameraMode.VIDEO){
            binding.recordButton.setBackgroundResource(R.drawable.ic_start_video)
            binding.recordTimer.visibility = View.VISIBLE
            //binding.camVideoRepButton.setBackgroundResource(R.drawable.ic_camera)
            //todo update gallery photo
        }
    }





    /** Video Capture */

    private fun triggerStopRecordStopTimer() {
        Utils.stopRecordStopTimer(mStatusCheckerRecordStart, mHandler!!)
    }

    private var mStatusCheckerRecordStart: Runnable = object : Runnable {
        override fun run() {
            try {
                recordStartTime -= 1
                if(recordStartTime > 0L) {
                    Utils.updateRecordTimerView(recordStartTime, binding.recordStartTimerText)
                }else if(recordStartTime == 0L){
                    mHandler!!.post{triggerStopRecordStopTimer()}
                    binding.startTimer.visibility = View.GONE
                    binding.recordStartTimerText.visibility = View.GONE
                    startRecording()
                }
            } finally {
                mHandler!!.postDelayed(this, mInterval)
            }
        }
    }

    private var mStatusChecker: Runnable = object : Runnable {
        override fun run() {
            try {
                timeInSeconds += 1
                VideoHandler.updateStopWatchView(timeInSeconds, binding.recordTimer)
                //Todo add memory check every 10 seconds
            } finally {
                mHandler!!.postDelayed(this, mInterval)
            }
        }
    }

    private suspend fun bindVideoCapture() {
        //var ori = Utils.getOrientationFromDegrees(this.resources.configuration.orientation)
        //val capabilities = display?.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()

        /*if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            var cap = requireContext().display?.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
            var a = cap
        }else{
            var cap = requireActivity().windowManager.defaultDisplay.hdrCapabilities.supportedHdrTypes ?: intArrayOf()
            var a = cap
        }*/

        //binding.surfaceView.scaleType = PreviewView.ScaleType.FIT_END

        val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).await()
        val cameraSelector = getCameraSelector(cameraIndex)
        if(cameraSelector == DEFAULT_FRONT_CAMERA){
            binding.torchButton.visibility = View.GONE
        }else{
            binding.torchButton.visibility = View.VISIBLE
        }

        val qualitySelector = QualitySelector.from(qualitySelected!!)

        binding.surfaceView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            val orientation = this@HomeFragment.resources.configuration.orientation
            dimensionRatio = qualitySelected!!.getAspectRatioString(qualitySelected!!,
                (orientation == Configuration.ORIENTATION_PORTRAIT))
        }

        val preview = Preview.Builder()
            .setTargetAspectRatio(qualitySelected!!.getAspectRatio(qualitySelected!!))
            .build().apply {
                setSurfaceProvider(binding.surfaceView.surfaceProvider)
            }

        binding.surfaceView.previewStreamState.observe(viewLifecycleOwner, Observer { streamState ->
            when(streamState){
                PreviewView.StreamState.STREAMING -> {
                    binding.progressLayout.visibility = View.GONE
                }
                else -> {}
            }
        })

        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            cameraX = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                videoCapture,
                preview
            )


        } catch (exc: Exception) {
            // we are on main thread, let's reset the controls on the UI.
            Log.e(TAG, "Use case binding failed", exc)
            resetUIAndStats("bindToLifecycle failed: $exc")
        }
        enableUI()

    }

    @SuppressLint("MissingPermission", "RestrictedApi")
    private fun startRecording() {
        if(!SharedPreferenceHelper(requireContext()).getBoolean(SharedPreferenceHelper.song_record_setting,false)){
            audioManagerImpl.requestAudioFocus()
        }

        if(SharedPreferenceHelper(requireContext()).getBoolean(torch_setting, false)
            && binding.torchButton.visibility == View.VISIBLE){
            Utils.torchSetup(true, cameraX)
        }
        //todo uncomment for ad
        //binding.adView.visibility = View.INVISIBLE

        val name = "CameraX-VR-camera-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis())
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")

        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            requireActivity().contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        //videoCapture.targetRotation = Surface.ROTATION_270

        currentRecording = videoCapture.output
            .prepareRecording(requireActivity(), mediaStoreOutput)
            .apply { if (SharedPreferenceHelper(requireContext()).getBoolean(audio_record_setting,true)) withAudioEnabled() }
            .start(mainThreadExecutor, captureListener)

        Log.i(TAG, "Recording started")
        VideoHandler.startTimer(mStatusChecker)
    }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            recordingState = event

        updateVideoUI(event)

    }

    private fun initVideoUI(){
        viewLifecycleOwner.lifecycleScope.launch {
            if (lScopeInstance != null) {
                lScopeInstance!!.await()
                lScopeInstance = null
            }
            enableUI()
            initQuality()

            binding.surfaceView.post {
                // Keep track of the display in which this view is attached
                displayId = binding.surfaceView.display.displayId
            }

            homeViewModel!!.getVideoContentDetails(requireContext(), false).observe(viewLifecycleOwner,
                Observer {
                    if(it.isNotEmpty()){
                        setGalleryThumbnail(it[0].uri)
                    }
                })

            bindVideoCapture()
            setExposureResource()
        }
    }

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    fun initVideoRecordingUI(){

        VideoHandler.initStopWatch(binding.recordTimer)

        binding.videoText.setTextColor(ContextCompat.getColor(requireContext(), R.color.pink_600))
        binding.photoText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        /*binding.camVideoRepButton.setOnClickListener {
            if(cameraMode == CameraMode.VIDEO){
                cameraMode = CameraMode.PHOTO
            }else if(cameraMode == CameraMode.PHOTO){
                cameraMode = CameraMode.VIDEO
            }

            if(cameraMode == CameraMode.PHOTO){
                initPhotoCaptureUI()
                initPhotoUI()
            }else if(cameraMode == CameraMode.VIDEO){
                initVideoRecordingUI()
                initVideoUI()
            }
        }*/

        binding.galleryButton.setOnClickListener {
            if(cameraMode == CameraMode.PHOTO){
                findNavController().navigate(HomeFragmentDirections.actionNavigationHomeToGalleryFragment(0,
                    Uri.EMPTY,ViewerMode.PHOTO))

            }else if(cameraMode == CameraMode.VIDEO){
                findNavController().navigate(HomeFragmentDirections.actionNavigationHomeToGalleryFragment(0,
                    Uri.EMPTY,ViewerMode.VIDEO))
            }
        }

        binding.switchCameraButton.setOnClickListener {
            binding.progressLayout.visibility = View.VISIBLE
            cameraIndex = (cameraIndex + 1) % cameraCapabilities.size
            initQuality()
            enableUI()

            viewLifecycleOwner.lifecycleScope.launch {
                bindVideoCapture()
            }

        }

        binding.recordButton.setOnClickListener{
            if (!this@HomeFragment::recordingState.isInitialized ||
                recordingState is VideoRecordEvent.Finalize) {
                enableUI()
                recordStartTime = SharedPreferenceHelper(requireContext()).getInt(video_start_timer,0).toLong() + 1
                if(recordStartTime > 0){
                    Utils.initRecordStartTimer(binding.startTimer, binding.recordStartTimerText, recordStartTime.toString())
                    Utils.startRecordStartTimer(mStatusCheckerRecordStart)
                }else{
                    startRecording()
                }
                /*if(checkMemoryStatus()){
                    startRecording()
                }else{
                    Toast.makeText(context, "Storage less than 1GB! Free up space for recording", Toast.LENGTH_LONG).show()
                }*/
            }else{
                when (recordingState) {
                    is VideoRecordEvent.Start -> {
                        stopVideoClicked()
                    }
                    is VideoRecordEvent.Pause ->{
                        stopVideoClicked()
                    }
                    is VideoRecordEvent.Resume ->{
                        stopVideoClicked()
                    }
                    else -> throw IllegalStateException("recordingState in unknown state")
                }
            }
        }

        binding.pauseButton.setOnClickListener{
            if (this@HomeFragment::recordingState.isInitialized) {
                when(recordingState){
                    is VideoRecordEvent.Start -> {
                        currentRecording?.pause()
                        VideoHandler.stopTimer(mStatusChecker,mHandler!!)
                    }
                    is VideoRecordEvent.Pause ->{
                        currentRecording?.resume()
                        VideoHandler.startTimer(mStatusChecker)
                    }
                    is VideoRecordEvent.Resume ->{
                        currentRecording?.pause()
                        VideoHandler.stopTimer(mStatusChecker,mHandler!!)
                    }
                    else -> throw IllegalStateException("recordingState in unknown state")
                }
            }
        }

        binding.torchButton.setOnClickListener {
            if(cameraX!!.cameraInfo.hasFlashUnit()){
                if(!SharedPreferenceHelper(requireContext()).getBoolean(torch_setting, false)){
                    SharedPreferenceHelper(requireContext()).putBoolean(torch_setting,true)
                    binding.torchButton.setBackgroundResource(R.drawable.ic_flash_on)
                }else{
                    SharedPreferenceHelper(requireContext()).putBoolean(torch_setting,false)
                    binding.torchButton.setBackgroundResource(R.drawable.ic_flash_off)
                }
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(context, listener)
        binding.surfaceView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            var dx: Float = binding.focusPoint.x
            var dy: Float = binding.focusPoint.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dx -= event.x
                    dy -= event.y
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    // Get the MeteringPointFactory from PreviewView
                    val factory = binding.surfaceView.meteringPointFactory

                    // Create a MeteringPoint from the tap coordinates
                    val point = factory.createPoint(event.x, event.y)

                    // Create a MeteringAction from the MeteringPoint, you can configure it to specify the metering mode
                    val action = FocusMeteringAction.Builder(point).build()

                    // Trigger the focus and metering. The method returns a ListenableFuture since the operation
                    // is asynchronous. You can use it get notified when the focus is successful or if it fails.
                    cameraX!!.cameraControl.startFocusAndMetering(action)
                    exposureChange()
                    //adjustFocusPoint(dx + event.x, dy + event.y)
                    Utils.adjustFocusPoint(event.x, event.y, binding.focusPoint)


                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener true
            }
        }

    }

    private fun stopVideoClicked(){
        if (currentRecording == null || recordingState is VideoRecordEvent.Finalize) {
            return
        }

        val recording = currentRecording
        if (recording != null) {
            recording.stop()
            currentRecording = null
            Utils.torchSetup(false, cameraX)
            VideoHandler.stopTimer(mStatusChecker,mHandler!!)
            VideoHandler.initStopWatch(binding.recordTimer)
            timeInSeconds = 0L
            if(!SharedPreferenceHelper(requireContext()).getBoolean(SharedPreferenceHelper.song_record_setting,false)){
                audioManagerImpl.releaseAudioFocus()
            }
            //todo uncomment for ad
            //binding.adView.visibility = View.VISIBLE
        }

        binding.recordButton.setBackgroundResource(R.drawable.ic_start_video)
        binding.pauseButton.setBackgroundResource(R.drawable.ic_pause)
        binding.pauseButton.visibility = View.GONE
        binding.switchCameraButton.visibility = View.VISIBLE
        binding.videoStatus.text = ""
    }

    private fun updateVideoUI(event: VideoRecordEvent) {
        val state = if (event is VideoRecordEvent.Status) recordingState.getNameString()
        else event.getNameString()

        when (event) {
            is VideoRecordEvent.Status -> {
            }
            is VideoRecordEvent.Start -> {
                showVideoUI(UiState.RECORDING, "RECORDING")
            }
            is VideoRecordEvent.Finalize-> {
                showVideoUI(UiState.FINALIZED, "")

            }
            is VideoRecordEvent.Pause -> {
                binding.pauseButton.setBackgroundResource(R.drawable.ic_resume)
                binding.videoStatus.text = "PAUSED"
            }
            is VideoRecordEvent.Resume -> {
                binding.pauseButton.setBackgroundResource(R.drawable.ic_pause)
                binding.videoStatus.text = "RECORDING"
            }
        }

        val stats = event.recordingStats
        val size = stats.numBytesRecorded / 1000
        val time = TimeUnit.NANOSECONDS.toSeconds(stats.recordedDurationNanos)
        var text = "${state}: recorded ${size}KB, in ${time}second"
        if(event is VideoRecordEvent.Finalize)
            text = "${text}\nFile saved to: ${event.outputResults.outputUri}"

        captureLiveStatus.value = text
        Log.i(TAG, "recording event: $text")

    }

    private fun showVideoUI(state: UiState, status: String){
        when(state) {
            UiState.IDLE -> {
                binding.recordButton.setBackgroundResource(R.drawable.ic_start_video)
                binding.recordButton.isEnabled = true
                binding.switchCameraButton.visibility = View.VISIBLE
                binding.pauseButton.visibility = View.GONE
                binding.videoStatus.text = ""
            }
            UiState.RECORDING -> {
                binding.recordButton.setBackgroundResource(R.drawable.ic_stop)
                binding.recordButton.isEnabled = true
                binding.switchCameraButton.visibility = View.GONE
                binding.pauseButton.visibility = View.VISIBLE
                binding.videoStatus.text = "RECORDING"

            }
            UiState.FINALIZED -> {
                binding.recordButton.setBackgroundResource(R.drawable.ic_start_video)
                binding.switchCameraButton.visibility = View.VISIBLE
                binding.pauseButton.visibility = View.GONE
                binding.videoStatus.text = ""


                homeViewModel!!.getVideoContentDetails(requireContext(), false).observe(viewLifecycleOwner, Observer {
                    mHandler!!.post{
                        if(it.isNotEmpty()){
                            setGalleryThumbnail(it[0].uri)
                        }
                    }
                })
            }
            else -> {
                val errorMsg = "Error: showUI($state) is not supported"
                Log.e(TAG, errorMsg)
                return
            }
        }
        binding.videoStatus.text = status

    }




    /** Photo Capture */

    private fun bindPhotoCapture() {

        val rotation = requireView().display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = getCameraSelector(cameraIndex)

        binding.surfaceView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            val orientation = this@HomeFragment.resources.configuration.orientation
            dimensionRatio = qualitySelected!!.getAspectRatioString(qualitySelected!!,
                (orientation == Configuration.ORIENTATION_PORTRAIT))
        }
        // Preview
        val preview = Preview.Builder()
            .setTargetAspectRatio(qualitySelected!!.getAspectRatio(qualitySelected!!))
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(qualitySelected!!.getAspectRatio(qualitySelected!!))
            .setTargetRotation(rotation)
            .build()

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        if (cameraX != null) {
            // Must remove observers from the previous camera instance
            removeCameraStateObservers(cameraX!!.cameraInfo)
        }

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            cameraX = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture)

            preview.setSurfaceProvider(binding.surfaceView.surfaceProvider)
            observeCameraState(cameraX?.cameraInfo!!)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = "CameraX-Photo-"+SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        var outputOptions: ImageCapture.OutputFileOptions? = null

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }

            val metadata = ImageCapture.Metadata().apply {
                // Mirror image when using the front camera
                val cameraSelector = getCameraSelector(cameraIndex)
                isReversedHorizontal = cameraSelector == DEFAULT_FRONT_CAMERA
            }

            outputOptions = ImageCapture.OutputFileOptions
                .Builder(requireContext().contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues)
                .setMetadata(metadata)
                .build()
        }else{
            /*val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                //https://stackoverflow.com/questions/60798804/store-image-via-android-media-store-in-new-folder
            }*/

            val directory = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + File.separator+ "CameraX-Image")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val photoFile:File = if(directory.exists()){
                createFile(directory, name, PHOTO_EXTENSION)
            }else{
                createFile(requireContext().filesDir, name, PHOTO_EXTENSION)
            }

            val metadata = ImageCapture.Metadata().apply {
                // Mirror image when using the front camera
                val cameraSelector = getCameraSelector(cameraIndex)
                isReversedHorizontal = cameraSelector == DEFAULT_FRONT_CAMERA
            }

            outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()

        }

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    if(output.savedUri != null){
                        setGalleryThumbnail(output.savedUri!!)
                    }
                    //Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun removeCameraStateObservers(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.removeObservers(viewLifecycleOwner)
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(viewLifecycleOwner, Observer { cameraState ->
            run {
                when (cameraState.type) {
                    CameraState.Type.PENDING_OPEN -> {

                    }
                    CameraState.Type.OPENING -> {

                    }
                    CameraState.Type.OPEN -> {

                    }
                    CameraState.Type.CLOSING -> {

                    }
                    CameraState.Type.CLOSED -> {

                    }
                }
            }

            cameraState.error?.let { error ->
                when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> {
                        // Make sure to setup the use cases properly
                        Toast.makeText(context,
                            "Stream config error",
                            Toast.LENGTH_SHORT).show()
                    }
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        // Close the camera or ask user to close another camera app that's using the
                        // camera
                        Toast.makeText(context,
                            "Camera in use",
                            Toast.LENGTH_SHORT).show()
                    }
                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // Close another open camera in the app, or ask the user to close another
                        // camera app that's using the camera
                        Toast.makeText(context,
                            "Max cameras in use",
                            Toast.LENGTH_SHORT).show()
                    }
                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
                        Toast.makeText(context,
                            "Other recoverable error",
                            Toast.LENGTH_SHORT).show()
                    }
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        // Ask the user to enable the device's cameras
                        Toast.makeText(context,
                            "Camera disabled",
                            Toast.LENGTH_SHORT).show()
                    }
                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        // Ask the user to reboot the device to restore camera function
                        Toast.makeText(context,
                            "Fatal error",
                            Toast.LENGTH_SHORT).show()
                    }
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        // Ask the user to disable the "Do Not Disturb" mode, then reopen the camera
                        Toast.makeText(context,
                            "Do not disturb mode enabled",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun updateCameraUi() {

        homeViewModel!!.getPhotoContentDetails(requireContext(), false).observe(viewLifecycleOwner,
            Observer {
                if(it.isNotEmpty()){
                    setGalleryThumbnail(it[0].uri)
                }
        })

    }

    private fun initPhotoUI(){

        viewLifecycleOwner.lifecycleScope.launch {
            if (lScopeInstance != null) {
                lScopeInstance!!.await()
                lScopeInstance = null
            }
            enableUI()
            initQuality()

            // Initialize our background executor
            cameraExecutor = Executors.newSingleThreadExecutor()

            broadcastManager = LocalBroadcastManager.getInstance(requireContext())

            // Set up the intent filter that will receive events from our main activity
            val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
            broadcastManager.registerReceiver(volumeDownReceiver, filter)

            // Every time the orientation of device changes, update rotation for use cases
            displayManager.registerDisplayListener(displayListener, null)

            //Initialize WindowManager to retrieve display metrics
            windowManager = requireActivity().windowManager

            // Determine the output directory
            outputDirectory = MainActivity.getOutputDirectory(requireContext())

            // Wait for the views to be properly laid out
            binding.surfaceView.post {
                // Keep track of the display in which this view is attached
                displayId = binding.surfaceView.display.displayId
                // Build UI controls
                updateCameraUi()
                // Set up the camera and its use cases
                setUpCamera()
            }

            setExposureResource()
        }

    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            // CameraProvider
            cameraProvider = cameraProviderFuture.get()
            bindPhotoCapture()

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /*override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Rebind the camera with the updated display metrics
        bindCameraUseCases()
    }*/

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    fun initPhotoCaptureUI(){

        binding.videoText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.photoText.setTextColor(ContextCompat.getColor(requireContext(), R.color.pink_600))

       /* binding.camVideoRepButton.setOnClickListener {
            if(cameraMode == CameraMode.VIDEO){
                cameraMode = CameraMode.PHOTO
            }else if(cameraMode == CameraMode.PHOTO){
                cameraMode = CameraMode.VIDEO
            }

            if(cameraMode == CameraMode.PHOTO){
                initPhotoCaptureUI()
                initPhotoUI()
            }else if(cameraMode == CameraMode.VIDEO){
                initVideoRecordingUI()
                initVideoUI()
            }
        }*/

        binding.galleryButton.setOnClickListener {
            if(cameraMode == CameraMode.PHOTO){
                findNavController().navigate(HomeFragmentDirections.actionNavigationHomeToGalleryFragment(0,
                    Uri.EMPTY,ViewerMode.PHOTO))

            }else if(cameraMode == CameraMode.VIDEO){
                findNavController().navigate(HomeFragmentDirections.actionNavigationHomeToGalleryFragment(0,
                    Uri.EMPTY,ViewerMode.VIDEO))
            }
        }

        binding.switchCameraButton.setOnClickListener {
            binding.progressLayout.visibility = View.VISIBLE
            cameraIndex = (cameraIndex + 1) % cameraCapabilities.size
            initQuality()
            enableUI()

            viewLifecycleOwner.lifecycleScope.launch {
                bindPhotoCapture()
            }

        }

        binding.recordButton.setOnClickListener{
            takePhoto()
            // Display flash animation to indicate that photo was captured
            binding.root.postDelayed({
                binding.root.foreground = ColorDrawable(Color.WHITE)
                binding.root.postDelayed(
                    { binding.root.foreground = null }, ANIMATION_FAST_MILLIS)
            }, ANIMATION_SLOW_MILLIS)
        }

        binding.torchButton.setOnClickListener {
            if(cameraX!!.cameraInfo.hasFlashUnit()){
                if(!SharedPreferenceHelper(requireContext()).getBoolean(torch_setting, false)){
                    SharedPreferenceHelper(requireContext()).putBoolean(torch_setting,true)
                    binding.torchButton.setBackgroundResource(R.drawable.ic_flash_on)
                }else{
                    SharedPreferenceHelper(requireContext()).putBoolean(torch_setting,false)
                    binding.torchButton.setBackgroundResource(R.drawable.ic_flash_off)
                }
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(context, listener)
        binding.surfaceView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            var dx: Float = binding.focusPoint.x
            var dy: Float = binding.focusPoint.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dx -= event.x
                    dy -= event.y
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    // Get the MeteringPointFactory from PreviewView
                    val factory = binding.surfaceView.meteringPointFactory

                    // Create a MeteringPoint from the tap coordinates
                    val point = factory.createPoint(event.x, event.y)

                    // Create a MeteringAction from the MeteringPoint, you can configure it to specify the metering mode
                    val action = FocusMeteringAction.Builder(point).build()

                    // Trigger the focus and metering. The method returns a ListenableFuture since the operation
                    // is asynchronous. You can use it get notified when the focus is successful or if it fails.
                    cameraX!!.cameraControl.startFocusAndMetering(action)
                    exposureChange()
                    //adjustFocusPoint(dx + event.x, dy + event.y)
                    Utils.adjustFocusPoint(event.x, event.y, binding.focusPoint)


                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener true
            }
        }

    }



    /** Default section */

    private fun resetUIAndStats(reason: String){
        enableUI()
        showVideoUI(UiState.IDLE, "")
        cameraIndex = 0
        initQuality()
    }

    private fun initHomeFragment(){

        try{
            homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
            mHandler = Handler(Looper.getMainLooper())
            audioManagerImpl = AudioManagerImpl(requireContext())
            if(cameraMode == null){
                cameraMode = CameraMode.PHOTO
            }

            if(SharedPreferenceHelper(requireContext()).getBoolean(grid_setting, true)){
                binding.gridInclude.gridLayout.visibility = View.VISIBLE
                binding.gridButton.setBackgroundResource(R.drawable.ic_grid_on)
            }else{
                binding.gridInclude.gridLayout.visibility = View.GONE
                binding.gridButton.setBackgroundResource(R.drawable.ic_grid_off)
            }

            binding.gridButton.setOnClickListener {
                if(SharedPreferenceHelper(requireContext()).getBoolean(grid_setting, true)){
                    SharedPreferenceHelper(requireContext()).putBoolean(grid_setting, false)
                    binding.gridInclude.gridLayout.visibility = View.GONE
                    binding.gridButton.setBackgroundResource(R.drawable.ic_grid_off)
                }else{
                    SharedPreferenceHelper(requireContext()).putBoolean(grid_setting, true)
                    binding.gridInclude.gridLayout.visibility = View.VISIBLE
                    binding.gridButton.setBackgroundResource(R.drawable.ic_grid_on)                }
            }

        }catch (e: Exception){
            Log.e("VR", e.stackTraceToString())
        }

        if(cameraMode == CameraMode.PHOTO){
            initPhotoCaptureUI()
            initPhotoUI()
        }else if(cameraMode == CameraMode.VIDEO){
            initVideoRecordingUI()
            initVideoUI()
        }

        binding.videoText.setOnClickListener {
            if(cameraMode == CameraMode.PHOTO){
                cameraMode = CameraMode.VIDEO
                initVideoRecordingUI()
                initVideoUI()
            }

        }

        binding.photoText.setOnClickListener {
            if(cameraMode == CameraMode.VIDEO){
                cameraMode = CameraMode.PHOTO
                initPhotoCaptureUI()
                initPhotoUI()
            }

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        navController = findNavController()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //AdViewHandler.initiateAd(binding.adView, requireContext())
        initHomeFragment()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                requireActivity().finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            findNavController().navigate(
                HomeFragmentDirections.actionNavigationHomeToPermissionsFragment2()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        // Shut down our background executor
        cameraExecutor.shutdown()

        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }

    companion object {
        // default Quality selection if no input from UI
        const val DEFAULT_QUALITY_IDX = 0
        private const val PHOTO_EXTENSION = ".jpg"
        val TAG:String = HomeFragment::class.java.simpleName
        private const val FILENAME_FORMAT = "dd-MM-yyyy-HH-mm-ss-SSS"

        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, "CameraX-Photo-" +SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension)
    }


}