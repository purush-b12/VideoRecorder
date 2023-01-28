package com.android.hopein.dancevideorecorder.ui.videos

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.hopein.dancevideorecorder.common.CommonUtils
import com.android.hopein.dancevideorecorder.sdk.repository.RepositoryImpl

class VideosViewModel : ViewModel() {

    private val videoContentLiveData: MutableLiveData<List<CommonUtils.Video>> = MutableLiveData<List<CommonUtils.Video>>()
    private val photoContentLiveData: MutableLiveData<List<CommonUtils.Photo>> = MutableLiveData<List<CommonUtils.Photo>>()


    fun getVideoContentDetails(context: Context, getAllMediaList: Boolean): LiveData<List<CommonUtils.Video>> {
        RepositoryImpl.videoContent(videoContentLiveData,context, getAllMediaList)

        return videoContentLiveData
    }

    fun getPhotoContentDetails(context: Context, getAllMediaList: Boolean): LiveData<List<CommonUtils.Photo>> {
        RepositoryImpl.photoContent(photoContentLiveData,context, getAllMediaList, 0)
        return photoContentLiveData
    }
}