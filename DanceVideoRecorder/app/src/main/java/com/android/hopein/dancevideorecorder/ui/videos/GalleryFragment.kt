package com.android.hopein.dancevideorecorder.ui.videos

import android.app.RecoverableSecurityException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.android.hopein.dancevideorecorder.R
import com.android.hopein.dancevideorecorder.common.CommonUtils
import com.android.hopein.dancevideorecorder.databinding.FragmentGalleryBinding
import com.android.hopein.dancevideorecorder.ui.common.ViewerMode
import com.android.hopein.dancevideorecorder.ui.home.HomeViewModel

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null

    private val binding get() = _binding!!

    var videoList: MutableList<CommonUtils.Video>? = null
    var photoList: MutableList<CommonUtils.Photo>? = null

    private val args: GalleryFragmentArgs by navArgs()

    var homeViewModel: HomeViewModel? = null
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var lPosition: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root


        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        })

        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        lPosition = args.position
        val mode = args.mode

        if(mode == ViewerMode.PHOTO){
            binding.mediaViewPager.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }else if(mode == ViewerMode.VIDEO){
            binding.mediaViewPager.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

        }

        intentSenderLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){
            if(it.resultCode == -1){
                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    //myHandler.post {
                    if(mode == ViewerMode.PHOTO){
                        deleteMedia(photoList!!.get(binding.mediaViewPager.currentItem).uri)
                    }else if(mode == ViewerMode.VIDEO){
                        deleteMedia(videoList!!.get(binding.mediaViewPager.currentItem).uri)
                    }
                    //}

                }else{
                    photoList!!.removeAt(binding.mediaViewPager.currentItem)
                    binding.mediaViewPager.apply {
                        offscreenPageLimit = 2
                        adapter = PhotoGalleryAdapter(requireActivity() as AppCompatActivity)
                        setCurrentItem(lPosition,false)
                    }
                    if(photoList!!.size == 0){
                        Navigation.findNavController(requireActivity(), R.id.navigation_home).popBackStack()
                    }
                }

            }
        }

        if(mode == ViewerMode.PHOTO){
            homeViewModel!!.getPhotoContentDetails(requireContext(), false).observe(viewLifecycleOwner,
                Observer {
                    photoList = it as MutableList<CommonUtils.Photo>?

                    binding.mediaViewPager.apply {
                        offscreenPageLimit = 2
                        adapter = PhotoGalleryAdapter(requireActivity() as AppCompatActivity)
                        setCurrentItem(lPosition,false)
                    }

                })
        }else if(mode == ViewerMode.VIDEO){
            homeViewModel!!.getVideoContentDetails(requireContext(), false).observe(viewLifecycleOwner,
                Observer {
                    videoList = it as MutableList<CommonUtils.Video>?

                    binding.mediaViewPager.apply {
                        adapter = VideoGalleryAdapter(requireActivity() as AppCompatActivity)
                        setCurrentItem(lPosition,false)
                    }
                })
        }

        binding.shareButton.setOnClickListener {
            if(mode == ViewerMode.PHOTO){
                sharePhotoClickListener(photoList!!.get(binding.mediaViewPager.currentItem).uri)
            }else if(mode == ViewerMode.VIDEO){
                shareVideoClickListener(videoList!!.get(binding.mediaViewPager.currentItem).uri)
            }
        }

        binding.backButton.setOnClickListener {
            //todo add videofragement also through args
            //Navigation.findNavController(requireActivity(), R.id.navigation_home).popBackStack()
            findNavController().popBackStack()

        }

        binding.deleteButton.setOnClickListener {

            if(mode == ViewerMode.PHOTO){
                lPosition = binding.mediaViewPager.currentItem
                deleteMedia(photoList!!.get(binding.mediaViewPager.currentItem).uri)
            }else if(mode == ViewerMode.VIDEO){
                lPosition = binding.mediaViewPager.currentItem
                deleteMedia(videoList!!.get(binding.mediaViewPager.currentItem).uri)
            }

        }

    }

    inner class PhotoGalleryAdapter(activity: AppCompatActivity): FragmentStateAdapter(activity) {
        override fun getItemCount(): Int {
            return photoList!!.size
        }

        override fun createFragment(position: Int): Fragment {
            return photoList?.get(position)?.let { PhotoViewFragment.create(it.uri) } as Fragment
        }
    }

    inner class VideoGalleryAdapter(activity: AppCompatActivity): FragmentStateAdapter(activity) {
        override fun getItemCount(): Int {
            return videoList!!.size
        }

        override fun createFragment(position: Int): Fragment {
            return videoList?.get(position)?.let { VideoViewFragment.create(it.uri) } as Fragment
        }
    }

    private fun sharePhotoClickListener(uri: Uri) {

        val builder: StrictMode.VmPolicy.Builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(Intent.createChooser(intent,"Share to:"))

        }catch (e: Exception){
            Log.e("vR", e.stackTraceToString())
        }

    }

    private fun shareVideoClickListener(uri: Uri) {

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

    private fun deleteMedia(uri: Uri){

         try{
            val count = requireContext().contentResolver.delete(uri, null, null)
            if(count>0){
                lPosition = binding.mediaViewPager.currentItem
                photoList!!.removeAt(binding.mediaViewPager.currentItem)
                //binding.mediaViewPager.adapter!!.notifyDataSetChanged()
                binding.mediaViewPager.apply {
                    offscreenPageLimit = 2
                    adapter = PhotoGalleryAdapter(requireActivity() as AppCompatActivity)
                    setCurrentItem(lPosition,false)
                }
                if(photoList!!.size == 0){
                    //Navigation.findNavController(requireActivity(), R.id.navigation_home).popBackStack()
                    findNavController().popBackStack()
                }
            }
        }catch (e: Exception){
            Log.e("vR", e.stackTraceToString())

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

}

