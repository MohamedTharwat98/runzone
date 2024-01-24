package com.example.runzone

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi


class EscapeFromDystopia : HeartRateActivity (){
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        warningSlowDown = MediaPlayer.create(this, R.raw.narratorslowdown)
        warningSpeedUp = MediaPlayer.create(this,R.raw.narratorspeedup)
        zone1Audio = MediaPlayer.create(this, R.raw.escapefromdystopiazone1)
        zone2Audio = MediaPlayer.create(this, R.raw.escapefromdystopiazone2)
        zone3Part1Audio = MediaPlayer.create(this, R.raw.escapefromdystopiazone3part1)
        zone4Part1Audio = MediaPlayer.create(this, R.raw.escapefromdystopiazone4part1)
        zone3Part2Audio = MediaPlayer.create(this, R.raw.escapefromdystopiazone3part2)
        zone4Part2Audio = MediaPlayer.create(this, R.raw.escapefromdystopiazone4part2)
        endAudio = MediaPlayer.create(this, R.raw.escapefromdystopiaend)
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

    override fun playAudio( mediaPlayer: MediaPlayer, resId: Int) {
        // Stop and reset the MediaPlayer before playing again
        mediaPlayer.stop()
        mediaPlayer.reset()

        // Set the data source (assuming it's the same every time)
        mediaPlayer.setDataSource(this, Uri.parse("android.resource://$packageName/${resId}"))

        //warningSpeedUp = MediaPlayer.create(this,R.raw.narratorspeedup)

        // Prepare and start the MediaPlayer
        mediaPlayer.prepareAsync()

        mediaPlayer.setOnErrorListener { mp, what, extra ->
            Log.e("checkZone", "MediaPlayer error: $what, $extra")
            false // return false to indicate that the error is not handled
        }
        mediaPlayer.setOnPreparedListener(MediaPlayer.OnPreparedListener {
            Log.d("checkZone", "warningSpeedUp is prepared")
            mediaPlayer.start()
        })
    }
}