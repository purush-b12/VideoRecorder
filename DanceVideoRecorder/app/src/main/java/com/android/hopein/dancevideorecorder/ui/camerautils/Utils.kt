package com.android.hopein.dancevideorecorder.ui.camerautils

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.core.Camera
import java.util.concurrent.TimeUnit

object Utils {

    fun initRecordStartTimer(startTimer: LinearLayout, recordStartTimerText: TextView,recordStartTime: String ) {
        startTimer.visibility = View.VISIBLE
        recordStartTimerText.visibility = View.VISIBLE
        recordStartTimerText.text = recordStartTime
    }

    fun startRecordStartTimer(mStatusCheckerRecordStart: Runnable) {
        mStatusCheckerRecordStart.run()
    }

    fun stopRecordStopTimer(mStatusCheckerRecordStart: Runnable, mHandler: Handler) {
        mHandler?.removeCallbacks(mStatusCheckerRecordStart)
    }

    fun updateRecordTimerView(timeInSeconds: Long, recordStartTimerText: TextView) {
        val formattedTime = getFormattedRecordStartTimer((timeInSeconds * 1000))
        Log.e("formattedTime", formattedTime)
        recordStartTimerText.text = formattedTime
    }

    private fun getFormattedRecordStartTimer(ms: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms)
        return "$seconds"
    }

    fun torchSetup(boolean: Boolean, cameraX: Camera?){
        cameraX?.cameraControl?.enableTorch(boolean)
    }

    fun adjustFocusPoint(x: Float, y: Float, focusPoint: ImageView){
        focusPoint.x = x
        focusPoint.y = y
    }

    fun getOrientationFromDegrees(orientation: Int): Int {
        return when {
            orientation == OrientationEventListener.ORIENTATION_UNKNOWN -> {
                Surface.ROTATION_0
            }
            orientation >= 315 || orientation < 45 -> {
                Surface.ROTATION_0 //portrait
            }
            orientation < 135 -> {
                //Surface.ROTATION_90
                Surface.ROTATION_270 //landscape
            }
            orientation < 225 -> {
                Surface.ROTATION_180
            }
            else -> {
                //Surface.ROTATION_270
                Surface.ROTATION_90
            }
        }
    }

    private fun checkMemoryStatus(context: Context): Boolean{
         /*context.getExternalFilesDir("null")
         val stat = StatFs(Environment.getExternalStorageDirectory())
         val mbAvailable = stat.availableBlocksLong * stat.blockSizeLong / (1024 * 1024)
         return mbAvailable >= 1024*/
        return false
    }
}