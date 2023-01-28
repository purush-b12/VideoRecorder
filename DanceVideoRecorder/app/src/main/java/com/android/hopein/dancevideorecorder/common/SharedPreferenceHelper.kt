package com.android.hopein.dancevideorecorder.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

class SharedPreferenceHelper constructor(context: Context) {

    fun getString(key: String?, defValue: String?): String? {
        synchronized(this) { return sharedPrefs.getString(key, defValue) }
    }

    fun putString(key: String?, value: String?) {
        synchronized(this) {
            editor.putString(key, value)
            editor.apply()
        }
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        synchronized(this) { return sharedPrefs.getBoolean(key, defValue) }
    }

    fun putBoolean(key: String?, value: Boolean) {
        synchronized(this) {
            editor.putBoolean(key, value)
            editor.apply()
        }
    }

    fun getInt(key: String?, defValue: Int): Int {
        synchronized(this) { return sharedPrefs.getInt(key, defValue) }
    }

    fun putInt(key: String?, value: Int) {
        synchronized(this) {
            editor.putInt(key, value)
            editor.apply()
        }
    }

    companion object {
        private lateinit var sharedPrefs: SharedPreferences
        private lateinit var editor: SharedPreferences.Editor
        private const val PP_PREFERENCES = "PP_PREFERENCES"
        const val audio_record_setting = "audio_record"
        const val song_record_setting = "song_record"
        const val front_cam_quality = "front_cam_quality"
        const val back_cam_quality = "back_cam_quality"
        const val torch_setting = "torch_setting"
        const val video_start_timer = "video_start_timer"
        const val grid_setting = "grid_setting"
    }

    init {
        sharedPrefs = context.getSharedPreferences(PP_PREFERENCES, Context.MODE_PRIVATE)
        editor = sharedPrefs.edit()
    }


}