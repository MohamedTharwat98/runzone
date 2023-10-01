package com.example.runzone

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi

class DirectFeedbackMission : HeartRateActivity (){
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Add your custom code here for ChildActivity1's onCreate method

        warningSlowDown = MediaPlayer.create(this, R.raw.runzoneslowdown)
        warningSpeedUp = MediaPlayer.create(this,R.raw.runezonespeedup)

        zone0Audio = MediaPlayer.create(this, R.raw.runzonezone0)
        zone1Audio = MediaPlayer.create(this, R.raw.runzonezone1)
        zone2Audio = MediaPlayer.create(this, R.raw.runzonezone2)
        zone3Audio = MediaPlayer.create(this, R.raw.runzonezone3)
        zone4Audio = MediaPlayer.create(this, R.raw.runzonezone4)
        endAudio = MediaPlayer.create(this, R.raw.runzoneend)
        pausedAudio= MediaPlayer()

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