package com.android.hopein.dancevideorecorder.ui.videos

import android.net.Uri

interface VideoOnclickListener {
    fun deleteClickListener(uri: Uri)
    fun shareClickListener(uri: Uri)
    fun videoPlayListener(uri: Uri)

}