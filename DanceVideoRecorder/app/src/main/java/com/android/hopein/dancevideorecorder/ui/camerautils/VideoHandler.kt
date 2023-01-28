package com.android.hopein.dancevideorecorder.ui.camerautils

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import java.util.concurrent.TimeUnit

object VideoHandler {

    fun initStopWatch(textView: TextView) {
        textView.text = "00.00"
    }

    fun startTimer(mStatusChecker: Runnable) {
        mStatusChecker.run()
    }

    fun stopTimer(mStatusChecker: Runnable, mHandler: Handler) {
        mHandler.removeCallbacks(mStatusChecker)
    }


    fun updateStopWatchView(timeInSeconds: Long, textView: TextView) {
        val formattedTime = getFormattedStopWatch((timeInSeconds * 1000))
        Log.e("formattedTime", formattedTime)
        textView.text = formattedTime
    }

    private fun getFormattedStopWatch(ms: Long): String {
        var milliseconds = ms
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        /* return "${if (hours < 10) "0" else ""}$hours:" +
                 "${if (minutes < 10) "0" else ""}$minutes:" +
                 "${if (seconds < 10) "0" else ""}$seconds"*/

        return "${if (minutes < 10) "0" else ""}$minutes:" +
                "${if (seconds < 10) "0" else ""}$seconds"
    }
}