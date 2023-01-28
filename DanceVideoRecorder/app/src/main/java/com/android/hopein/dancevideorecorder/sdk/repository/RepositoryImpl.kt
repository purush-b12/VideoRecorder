package com.android.hopein.dancevideorecorder.sdk.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.hopein.dancevideorecorder.common.CommonUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object RepositoryImpl {

    fun videoContent(mutableLiveData: MutableLiveData<List<CommonUtils.Video>>,context: Context, getAllMediaContent: Boolean){

        val videoList = mutableListOf<CommonUtils.Video>()

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )

        // Show only videos that are at least 1 second in duration.
        val selection = "${MediaStore.Video.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS).toString()
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val query = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            if(cursor.moveToFirst()) {
                // Get values of columns for a given video.
                do{
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val duration = cursor.getInt(durationColumn)
                    val minutes: Int = duration / 1000 / 60
                    val seconds = (duration / 1000 % 60)
                    val size = cursor.getInt(sizeColumn)
                    val fileSizeInKB: Int = size / 1024
                    val fileSizeInMB = fileSizeInKB / 1024
                    val date = Date(cursor.getString(dateColumn).toLong())
                    val inputSDF = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    // Stores column values and the contentUri in a local object
                    // that represents the media file.
                    videoList += CommonUtils.Video(contentUri, name,
                        "$minutes:$seconds", fileSizeInMB, inputSDF.format(date))
                }while (cursor.moveToNext())

            }

        }
        val videoListFinal = mutableListOf<CommonUtils.Video>()

        if(getAllMediaContent){
            mutableLiveData.postValue(videoList)
        }else{
            videoList.forEach {
                if(it.name.contains("CameraX-")){
                    //if(it.name.contains("VR-camera-")){
                    videoListFinal.add(it)
                }
            }
            mutableLiveData.postValue(videoListFinal)
        }

    }

    fun photoContent(mutableLiveData: MutableLiveData<List<CommonUtils.Photo>>,context: Context, getAllMediaContent: Boolean, count: Int){

        val list = mutableListOf<CommonUtils.Photo>()

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val selection =
                MediaStore.Images.ImageColumns.RELATIVE_PATH + " like ? "
            val selectionArgs = arrayOf("%Pictures/CameraX-Image%")

            val sortOrder: String = if(count > 0){
                "${MediaStore.Images.Media.DATE_ADDED} DESC" + " limit " + count
            } else{
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            }

            val query = context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            try{
                query?.use { cursor ->
                    // Cache column indices.
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                    if(cursor.count > 0  && cursor.moveToFirst()) {
                        // Get values of columns for a given video.
                        do{
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn)
                            val size = cursor.getInt(sizeColumn)
                            val fileSizeInKB: Int = size / 1024
                            val fileSizeInMB = fileSizeInKB / 1024
                            val date = Date(cursor.getString(dateColumn).toLong())
                            val inputSDF = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

                            val contentUri: Uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )

                            // Stores column values and the contentUri in a local object
                            // that represents the media file.
                            list += CommonUtils.Photo(contentUri, name, fileSizeInMB, inputSDF.format(date))
                        }while (cursor.moveToNext())

                    }


                }
                val listFinal = mutableListOf<CommonUtils.Photo>()

                if(getAllMediaContent){
                    mutableLiveData.postValue(list)
                }else{
                    list.forEach {
                        if(it.name.contains("CameraX-Photo-")){
                            listFinal.add(it)
                        }
                    }
                    mutableLiveData.postValue(listFinal)
                }
            }catch (e: Exception){
                Log.e("Hi camera", e.stackTraceToString())
            }

        } else {
            //todo add count filter
            val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + File.separator+ "CameraX-Image")
            val listFiles:  Array<File> = if (directory.exists()) {
                directory.listFiles { file ->
                    file.name.toString().contains("CameraX-Photo-")
                } as Array<File>

            }else{
                context.filesDir.listFiles { file ->
                    file.name.toString().contains("CameraX-Photo-")
                } as Array<File>
            }

            val listFinal = mutableListOf<CommonUtils.Photo>()
            listFiles.forEach {
                val name = it.name.toString()
                val size = it.length()
                val fileSizeInKB: Long = size / 1024
                val fileSizeInMB = (fileSizeInKB / 1024).toInt()
                val date = Date(it.lastModified())
                val inputSDF = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

                listFinal.add(CommonUtils.Photo(Uri.fromFile(it),name,fileSizeInMB, inputSDF.format(date)))
            }

            mutableLiveData.postValue(listFinal)

        }

    }
}