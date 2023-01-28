package com.android.hopein.dancevideorecorder.ui.videos

import android.app.RecoverableSecurityException
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.android.hopein.dancevideorecorder.R
import com.android.hopein.dancevideorecorder.common.CommonUtils
import com.android.hopein.dancevideorecorder.databinding.FragmentVideosBinding
import com.android.hopein.dancevideorecorder.ui.common.ViewerMode
import com.android.hopein.dancevideorecorder.ui.home.HomeFragmentDirections
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds


class VideosFragment : Fragment(), VideoOnclickListener {

    private var _binding: FragmentVideosBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var adapter: VideoRecyclerViewAdapter
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var deleteUri: Uri? = null
    private var videoList: MutableList<CommonUtils.Video>? = null
    private var photoList: MutableList<CommonUtils.Photo>? = null

    private val myHandler = Handler(Looper.getMainLooper())

    private var currentMode: ViewerMode? = null
    private lateinit var videosViewModel: VideosViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentVideosBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(currentMode == null){
            currentMode = ViewerMode.PHOTO
        }

        val mContext = this
        videosViewModel = ViewModelProvider(this).get(VideosViewModel::class.java)
        updateUI()

        binding.photoViewerTab.setOnClickListener {
            currentMode = ViewerMode.PHOTO
            updateUI()
        }

        binding.videoViewerTab.setOnClickListener {
            currentMode = ViewerMode.VIDEO
            updateUI()
        }

        binding.mediaList.setOnItemClickListener { _, lView, i, _ ->
            if(currentMode == ViewerMode.PHOTO){
                findNavController().navigate(
                    VideosFragmentDirections.actionNavigationVideosToGalleryFragment(i,
                    Uri.EMPTY,ViewerMode.PHOTO))
            }else if(currentMode == ViewerMode.VIDEO){
                findNavController().navigate(
                    VideosFragmentDirections.actionNavigationVideosToGalleryFragment(i,
                        Uri.EMPTY,ViewerMode.VIDEO))
            }

        }

        /*lifecycleScope.launch {
            initiateAd()
        }*/

        intentSenderLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){
            if(it.resultCode == -1){
                if(deleteUri != null){
                    if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                        myHandler.post {
                            this.deleteAndroidQDevice(deleteUri!!)
                            deleteUri = null
                        }
                            //adapter.deleteAndUpdateList(deleteUri!!)

                    }else{
                        myHandler.post {
                            adapter.deleteAndUpdateList(videoList!!)
                        }
                        deleteUri = null
                    }
                }

            }
        }

    }

    private fun updateUI(){
        if(currentMode == ViewerMode.PHOTO){

            binding.photoViewerTab.setBackgroundResource(R.drawable.selection_card_background)
            binding.videoViewerTab.setBackgroundResource(R.color.white)

            videosViewModel.getPhotoContentDetails(requireContext(), false).observe(viewLifecycleOwner) {
                myHandler.post{
                    binding.progressLayout.visibility = View.GONE
                    photoList = it as MutableList<CommonUtils.Photo>?

                    val gridAdapter = GridPhotoAdapter(requireContext(), photoList!!)
                    binding.mediaList.adapter = gridAdapter
                }

            }

        }else if(currentMode == ViewerMode.VIDEO){
            binding.photoViewerTab.setBackgroundResource(R.color.white)
            binding.videoViewerTab.setBackgroundResource(R.drawable.selection_card_background)

            //lifecycleScope.launch(Dispatchers.IO) {
            videosViewModel.getVideoContentDetails(requireContext(), false).observe(viewLifecycleOwner) {
                myHandler.post{
                    binding.progressLayout.visibility = View.GONE
                    videoList = it as MutableList<CommonUtils.Video>?
                    //adapter = VideoRecyclerViewAdapter(requireContext(), it, mContext)
                    //binding.videoList.layoutManager = LinearLayoutManager(requireContext())
                    //binding.videoList.adapter = adapter

                    val gridAdapter = GridVideoAdapter(requireContext(), videoList!!)
                    binding.mediaList.adapter = gridAdapter
                }

            }
            //}

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun initiateAd(){
        MobileAds.initialize(requireContext()) {}
        //ads:adUnitId="ca-app-pub-5655866277681532/2609247934"
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
        binding.adView.visibility = View.VISIBLE

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

    override fun deleteClickListener(uri: Uri) {
        try{
            val count = requireContext().contentResolver.delete(uri, null, null)

            if(count>0){
                var item: CommonUtils.Video? = null
                videoList!!.forEachIndexed { index, it ->
                    if(it.uri == deleteUri){
                        item = it
                    }
                }
                if(item!=null){
                    videoList!!.remove(item)
                }
                adapter.deleteAndUpdateList(videoList!!)
            }
        }catch (e: Exception){
            Log.e("vR", e.stackTraceToString())

            deleteUri = uri

            // android 30 (Andriod 11)
            val intentSender = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    MediaStore.createDeleteRequest(requireContext().contentResolver, listOf(uri)).intentSender
                }
                // android 29 (Andriod 10)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {

                    val recoverableSecurityException = e as? RecoverableSecurityException
                    recoverableSecurityException?.userAction?.actionIntent?.intentSender
                }
                else -> null
            }
            intentSender?.let { sender ->
                intentSenderLauncher.launch(
                    IntentSenderRequest.Builder(sender).build()
                )
            }


        }

    }
    private fun deleteAndroidQDevice(uri: Uri) {
        try{
            val count = requireContext().contentResolver.delete(uri, null, null)

            if(count>0){
                var item: CommonUtils.Video? = null
                videoList!!.forEachIndexed { index, it ->
                    if(it.uri == deleteUri){
                        item = it
                    }
                }
                if(item!=null){
                    videoList!!.remove(item)
                }
                adapter.deleteAndUpdateList(videoList!!)
            }
        }catch (e: Exception){
            Log.e("vR", e.stackTraceToString())
        }

    }

    override fun shareClickListener(uri: Uri) {

        val builder: StrictMode.VmPolicy.Builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "video/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(Intent.createChooser(intent,"Share to:"))

        }catch (e: Exception){
            Log.e("vR", e.stackTraceToString())
        }

    }

    override fun videoPlayListener(uri: Uri) {
        findNavController().navigate(VideosFragmentDirections.actionNavigationVideosToPlayVideoFragment(uri))
    }

}