package com.android.hopein.dancevideorecorder.sdk.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log


class AudioManagerImpl constructor(private val context: Context){

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var onAudioFocusChange: AudioManager.OnAudioFocusChangeListener? =null
    private var hasAudioFocus: Boolean = false

    init{

        try{
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            onAudioFocusChange = AudioManager.OnAudioFocusChangeListener { focusChange ->

                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        hasAudioFocus = true
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        hasAudioFocus = false
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        hasAudioFocus = false
                    }
                }

            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(onAudioFocusChange!!)
                .build()
        }catch (e: Exception){
            Log.e("VR", e.stackTraceToString())
        }

    }

    fun requestAudioFocus() : Int?{
        val focusRequest: Int? = audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        if(focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            hasAudioFocus = true
        }
        return focusRequest
    }

    fun releaseAudioFocus() : Int? {
        var focusRequest: Int? = null
        if (audioFocusRequest != null) {
            if(hasAudioFocus){
                focusRequest = audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
            }
            audioFocusRequest = null
        }

        return focusRequest
    }

}