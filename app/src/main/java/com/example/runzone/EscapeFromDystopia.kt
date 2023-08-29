package com.example.runzone

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.RequiresApi

class EscapeFromDystopia : HeartRateActivity (){
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    // Override the timerRunnable here
    private val customTimerRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            // Custom implementation for timerRunnable
            // ...
            seconds++
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60

            timerTextView.text = String.format("%02d:%02d:%02d", hours, minutes, secs)
            timerTextView.text = " ${timerTextView.text} \n Time"
            handler.postDelayed(this, 1000)

            updateZone(minutes,secs)

        }
    }





    override fun startTimer() {
        isRunning = true
        handler.postDelayed(customTimerRunnable, 1000)
    }

    override fun stopTimer() {
        isRunning = false
        handler.removeCallbacks(customTimerRunnable)
    }
}