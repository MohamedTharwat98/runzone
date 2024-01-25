package com.example.runzone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.text.DecimalFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import kotlin.math.*

const val TAG = "HeartRateActivity"

open class HeartRateActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var api: PolarBleApi

    private val PERMISSION_REQUEST_CODE = 1


    var seconds: Int = 0
    var isRunning: Boolean = false
    lateinit var handler: Handler
    private lateinit var textToSpeech: TextToSpeech


    val timerTextView: TextView by lazy {
        findViewById<TextView>(R.id.timerTextView)
    }

    private val startButton: ToggleButton by lazy {
        findViewById<ToggleButton>(R.id.startButton)
    }

    private val stopButton: Button by lazy {
        findViewById<Button>(R.id.stopButton)
    }

    var maxHR = 0


    var missionType = ""

    private var hrDisposable: Disposable? = null

    var currentHr = 0

    var runnersAge = 0

    var zoneNumber = 1

    var inZone = false

    lateinit var warningSlowDown: MediaPlayer

    lateinit var warningSpeedUp: MediaPlayer

    lateinit var zone1Audio: MediaPlayer

    lateinit var zone2Audio: MediaPlayer

    lateinit var zone3Part1Audio: MediaPlayer

    lateinit var zone3Part2Audio: MediaPlayer

    lateinit var zone4Part1Audio: MediaPlayer

    lateinit var zone4Part2Audio: MediaPlayer

    lateinit var endAudio: MediaPlayer

    lateinit var pausedAudio: MediaPlayer

    private lateinit var heartRateChart: BarChart
    private lateinit var dataSet: BarDataSet
    private val entries = ArrayList<BarEntry>()

    private val blinkHandlers =
        mutableMapOf<View, Handler>() // Store handlers for blinking animations


    var zone1StartMinutes: Int = 0
    var zone2StartMinutes: Int = 0
    var zone3Part1StartMinutes: Int = 0
    var zone3Part2StartMinutes: Int = 0
    var zone4Part1StartMinutes: Int = 0
    var zone4Part2StartMinutes: Int = 0

    var zone1StartSeconds: Int = 0
    var zone2StartSeconds: Int = 0
    var zone3Part1StartSeconds: Int = 0
    var zone3Part2StartSeconds: Int = 0
    var zone4Part1StartSeconds: Int = 0
    var zone4Part2StartSeconds: Int = 0

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var distanceTextView: TextView

    private var lastLocation: Location? = null
    private var totalDistance: Float = 0.0f
    private var missionStoped: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        api = PolarBleApiDefaultImpl.defaultImplementation(
            this,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
            )
        )

        showAgeInputDialog();


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAgeInputDialog() {
        val popupView = layoutInflater.inflate(R.layout.new_age_box, null)
        val editTextAge = popupView.findViewById<EditText>(R.id.editTextAge)

        val alertDialogBuilder = AlertDialog.Builder(this)
            .setView(popupView)
            .setTitle("Pyramidal Heart Rate Zone Training")
            .setPositiveButton(
                "Start Mission",
                null
            ) // Set the positive button, but don't provide a click listener here
            .setNegativeButton("Cancel", null)


        val alertDialog = alertDialogBuilder.create()

        alertDialog.setOnShowListener { dialog ->
            val startButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            startButton.setOnClickListener {
                val age = editTextAge.text.toString()
                val ageInt = age.toInt()
                runnersAge = ageInt
                maxHR = 220 - ageInt
                // 5% = 1 min
                calculateZonesTime()
                startMission()
                alertDialog.dismiss() // Close the dialog after successful validation

            }
        }

        alertDialog.show()

    }


    /*
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAgeInputDialog() {
        val popupView = layoutInflater.inflate(R.layout.age_box, null)
        val editTextAge = popupView.findViewById<EditText>(R.id.editTextAge)

        setProgressBar(popupView)

        seekBarOnClick(popupView)

        val alertDialogBuilder = AlertDialog.Builder(this)
            .setView(popupView)
            .setTitle("Pyramidal Heart Rate Zone Training")
            .setPositiveButton("Start Mission", null) // Set the positive button, but don't provide a click listener here
            .setNegativeButton("Cancel", null)

        val alertDialog = alertDialogBuilder.create()

        alertDialog.setOnShowListener { dialog ->
            val startButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            startButton.setOnClickListener {
                val age = editTextAge.text.toString()

                try {
                    val ageInt = age.toInt()
                    runnersAge = ageInt
                    maxHR = 220 - ageInt

                    // Validate percentage input
                    val seekBarZone0 = popupView.findViewById<SeekBar>(R.id.seekBarZone0)
                    val seekBarZone1 = popupView.findViewById<SeekBar>(R.id.seekBarZone1)
                    val seekBarZone2 = popupView.findViewById<SeekBar>(R.id.seekBarZone2)
                    val seekBarZone3 = popupView.findViewById<SeekBar>(R.id.seekBarZone3)
                    val seekBarZone4 = popupView.findViewById<SeekBar>(R.id.seekBarZone4)


                    val percentageZone0 = (seekBarZone0.progress  * 10) / 2
                    val percentageZone1 = (seekBarZone1.progress  * 10) / 2
                    val percentageZone2 = (seekBarZone2.progress  * 10) / 2
                    val percentageZone3 = (seekBarZone3.progress  * 10) / 2
                    val percentageZone4 = (seekBarZone4.progress  * 10) / 2


                    val totalPercentage = percentageZone0 + percentageZone1 + percentageZone2 + percentageZone3 + percentageZone4

                    if (totalPercentage != 100) {
                        Toast.makeText(this, "Total percentage must be 100%", Toast.LENGTH_SHORT).show()
                    } else if (percentageZone0 < 5 || percentageZone1 < 5 || percentageZone2 < 5 ||
                        percentageZone3 < 5 || percentageZone4 < 5 ) {
                        Toast.makeText(this, "A zone time can not be less than 5%", Toast.LENGTH_SHORT).show()
                    }else if (percentageZone4 > 10) {
                        Toast.makeText(this, "Time in Zone 4 must be less than or equal to 10%", Toast.LENGTH_SHORT).show()
                    } else {
                        calculateZonesTime(percentageZone0,percentageZone1,percentageZone2,percentageZone3,percentageZone4)
                        startMission()
                        alertDialog.dismiss() // Close the dialog after successful validation
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Your age must be a valid number", Toast.LENGTH_SHORT).show()
                }
            }
        }

        alertDialog.show()
    }*/


    fun seekBarOnClick(popupView: View) {
        val seekBarZone0 = popupView.findViewById<SeekBar>(R.id.seekBarZone0)
        val seekBarZone1 = popupView.findViewById<SeekBar>(R.id.seekBarZone1)
        val seekBarZone2 = popupView.findViewById<SeekBar>(R.id.seekBarZone2)
        val seekBarZone3 = popupView.findViewById<SeekBar>(R.id.seekBarZone3)
        val seekBarZone4 = popupView.findViewById<SeekBar>(R.id.seekBarZone4)

        val percentageZone0Bar = popupView.findViewById<TextView>(R.id.percentageZone0)
        val percentageZone1Bar = popupView.findViewById<TextView>(R.id.percentageZone1)
        val percentageZone2Bar = popupView.findViewById<TextView>(R.id.percentageZone2)
        val percentageZone3Bar = popupView.findViewById<TextView>(R.id.percentageZone3)
        val percentageZone4Bar = popupView.findViewById<TextView>(R.id.percentageZone4)


        seekBarZone0.max = 20
        seekBarZone1.max = 20
        seekBarZone2.max = 20
        seekBarZone3.max = 20
        seekBarZone4.max = 20

        seekBarZone0.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val mappedValue = progress * 5
                val time = calculateZoneTimeSeekBar(mappedValue)
                val zone0Text = popupView.findViewById<TextView>(R.id.zone0TextView)
                zone0Text.text = "Zone 1 : \n $time"
                percentageZone0Bar.text = "$mappedValue%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarZone1.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val mappedValue = progress * 5
                val time = calculateZoneTimeSeekBar(mappedValue)
                val zone1Text = popupView.findViewById<TextView>(R.id.zone1TextView)
                zone1Text.text = "Zone 2 : \n $time"
                percentageZone1Bar.text = "$mappedValue%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarZone2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val mappedValue = progress * 5
                val time = calculateZoneTimeSeekBar(mappedValue)
                val zone2Text = popupView.findViewById<TextView>(R.id.zone2TextView)
                zone2Text.text = "Zone 3 : \n $time"
                percentageZone2Bar.text = "$mappedValue%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarZone3.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val mappedValue = progress * 5
                val time = calculateZoneTimeSeekBar(mappedValue)
                val zone3Text = popupView.findViewById<TextView>(R.id.zone3TextView)
                zone3Text.text = "Zone 4 : \n $time"
                percentageZone3Bar.text = "$mappedValue%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarZone4.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val mappedValue = progress * 5
                val time = calculateZoneTimeSeekBar(mappedValue)
                val zone4Text = popupView.findViewById<TextView>(R.id.zone4TextView)
                zone4Text.text = "Zone 5 : \n $time"
                percentageZone4Bar.text = "$mappedValue%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setProgressBar(popupView: View) {
        val seekBarZone0 = popupView.findViewById<SeekBar>(R.id.seekBarZone0)
        val seekBarZone1 = popupView.findViewById<SeekBar>(R.id.seekBarZone1)
        val seekBarZone2 = popupView.findViewById<SeekBar>(R.id.seekBarZone2)
        val seekBarZone3 = popupView.findViewById<SeekBar>(R.id.seekBarZone3)
        val seekBarZone4 = popupView.findViewById<SeekBar>(R.id.seekBarZone4)

        val percentageZone0Bar = popupView.findViewById<TextView>(R.id.percentageZone0)
        val percentageZone1Bar = popupView.findViewById<TextView>(R.id.percentageZone1)
        val percentageZone2Bar = popupView.findViewById<TextView>(R.id.percentageZone2)
        val percentageZone3Bar = popupView.findViewById<TextView>(R.id.percentageZone3)
        val percentageZone4Bar = popupView.findViewById<TextView>(R.id.percentageZone4)

        val percentageZone0 = seekBarZone0.progress * 10
        val percentageZone1 = seekBarZone1.progress * 10
        val percentageZone2 = seekBarZone2.progress * 10
        val percentageZone3 = seekBarZone3.progress * 10
        val percentageZone4 = seekBarZone4.progress * 10

        percentageZone0Bar.text = "$percentageZone0%"
        percentageZone1Bar.text = "$percentageZone1%"
        percentageZone2Bar.text = "$percentageZone2%"
        percentageZone3Bar.text = "$percentageZone3%"
        percentageZone4Bar.text = "$percentageZone4%"

        val time0 = calculateZoneTimeSeekBar(percentageZone0)
        val zone0Text = popupView.findViewById<TextView>(R.id.zone0TextView)
        zone0Text.text = "Zone 1 : \n $time0"

        val time1 = calculateZoneTimeSeekBar(percentageZone1)
        val zone1Text = popupView.findViewById<TextView>(R.id.zone1TextView)
        zone1Text.text = "Zone 2 : \n $time1"

        val time2 = calculateZoneTimeSeekBar(percentageZone2)
        val zone2Text = popupView.findViewById<TextView>(R.id.zone2TextView)
        zone2Text.text = "Zone 3 : \n $time2"

        val time3 = calculateZoneTimeSeekBar(percentageZone3)
        val zone3Text = popupView.findViewById<TextView>(R.id.zone3TextView)
        zone3Text.text = "Zone 4 : \n $time3"

        val time4 = calculateZoneTimeSeekBar(percentageZone4)
        val zone4Text = popupView.findViewById<TextView>(R.id.zone4TextView)
        zone4Text.text = "Zone 5 : \n $time4"


    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMission() {
        setContentView(R.layout.activity_mission)

        missionType = intent.getStringExtra("missionType").toString()

        handler = Handler(Looper.getMainLooper())


        isRunning = true

        inZone = true

        startButton.isChecked = true

        textToSpeech = TextToSpeech(this, this)

        setCompletionListenerAudio()

        startTimer()

        playAudio(zone1Audio, 1)


        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.
        initializeLogging()

        //checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)
        requestPermissionForPolarSensor()
        connectToPolarSensor()
        enableFeaturesToPolarSensor()

        val maxHeartRateText = findViewById<TextView>(R.id.maxHRTextView)
        maxHeartRateText.text = "${maxHR} bpm \nMAX-HR"

        processChart()

        blinkSections(0)

        val targetZoneText = findViewById<TextView>(R.id.targetZoneTextView)
        targetZoneText.text = "CURRENT TARGET ZONE: 1 \n 50% < INTENSITY < 60% "

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        distanceTextView = findViewById(R.id.distanceTextView)

        // Start tracking location updates
        startLocationUpdates()

        //check every 10 seconds if the user is in the zone
        val checkZoneTimer = Timer()
        checkZoneTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkZone()
            }
        }, 10000, 10000)

        //check every 30 seconds and gives the user feedback
        val percentageFeedbackTimer = Timer()
        percentageFeedbackTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                speak()
            }
        }, 30000, 30000)



        stopButton.setOnClickListener {
            percentageFeedbackTimer.cancel()
            checkZoneTimer.cancel()
            stopMission()
        }


        startButton.setOnClickListener {
            if (!isRunning) {
                startTimer()
                startButton.isChecked = true
                checkAudio()
            } else {
                stopTimer()
                startButton.isChecked = false
                checkAudio()
            }
        }

        Log.d("zonesTime", "Zone 1 =  ${zone1StartMinutes}, ${zone1StartSeconds}")
        Log.d("zonesTime", "Zone 2 =  ${zone2StartMinutes}, ${zone2StartSeconds}")
        Log.d("zonesTime", "Zone 3 part 1 =  ${zone3Part1StartMinutes}, ${zone3Part1StartSeconds}")
        Log.d("zonesTime", "Zone 4 part 1 =  ${zone4Part1StartMinutes}, ${zone4Part1StartSeconds}")
        Log.d("zonesTime", "Zone 3 part 2 =  ${zone3Part2StartMinutes}, ${zone3Part2StartSeconds}")
        Log.d("zonesTime", "Zone 4 part 2 =  ${zone4Part2StartMinutes}, ${zone4Part2StartSeconds}")

    }


    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 1000 // Update every 1 second

        // Check and request location permissions if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    private val MIN_DISTANCE_THRESHOLD = 5 // Adjust as needed

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            for (location in locationResult.locations) {
                if (lastLocation != null) {
                    val distance = lastLocation!!.distanceTo(location)
                    if (distance >= MIN_DISTANCE_THRESHOLD && inZone && isRunning) {
                        totalDistance += distance
                        val distanceInKilometers = totalDistance / 1000.0 // Convert to kilometers
                        distanceTextView.text =
                            String.format("%.2f km \n DISTANCE", distanceInKilometers)
                    }
                }
                lastLocation = location
            }
        }
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }


    fun stopMission() {
        if (!missionStoped) {
            val distanceInKilometers = totalDistance / 1000.0 // Convert to kilometers
            val decimalFormat = DecimalFormat("#.###")
            val roundedNumber = decimalFormat.format(distanceInKilometers).toDouble()
            val session = Session(
                duration = "",
                date = Date().toString(),
                maxHR = maxHR.toFloat(),
                distance = "$roundedNumber km ",
                missionType = missionType,
                age = runnersAge,
                zone1 = 0F,
                zone2 = 0F,
                zone3 = 0F,
                zone4 = 0F
            )
            stopAllMedia()
            session.zone1 = entries.get(0).y
            session.zone2 = entries.get(1).y
            session.zone3 = entries.get(2).y
            session.zone4 = entries.get(3).y
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            val timer = String.format("%02d:%02d:%02d", hours, minutes, secs)
            session.duration = timer

            val databaseRef: DatabaseReference =
                FirebaseDatabase.getInstance("https://plucky-balm-389709-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference("sessions")
            val sessionKey: String? = databaseRef.push().key

            if (sessionKey != null) {
                databaseRef.child(sessionKey).setValue(session)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Session data saved successfully", Toast.LENGTH_SHORT)
                            .show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Error saving session data: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            finish()
            missionStoped = true
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    fun setCompletionListenerAudio() {
        setCompletionListener(warningSlowDown)
        setCompletionListener(warningSpeedUp)
        setCompletionListener(zone1Audio)
        setCompletionListener(zone2Audio)
        setCompletionListener(zone3Part1Audio)
        setCompletionListener(zone4Part1Audio)
        setCompletionListener(zone3Part2Audio)
        setCompletionListener(zone4Part2Audio)
        setCompletionListener(endAudio)
        setCompletionListener(pausedAudio)
    }

    fun setCompletionListener(mediaPlayer: MediaPlayer) {
        // Set a completion listener to detect when playback is finished
        mediaPlayer.setOnCompletionListener { mp ->
            // This callback will be called when the MediaPlayer has finished playing
            // You can perform actions here, such as updating UI or handling the completion event
            // You can also release the MediaPlayer if you're done with it
            mp.stop()
            if (mediaPlayer == pausedAudio) {
                pausedAudio = MediaPlayer()
            }
            if (mediaPlayer == endAudio) {
                stopMission()
            }
        }
    }

    fun checkAudio() {
        if (!isRunning) { //pause
            if (warningSlowDown.isPlaying) {
                warningSlowDown.pause()
                pausedAudio = warningSlowDown
            } else if (warningSpeedUp.isPlaying) {
                warningSpeedUp.pause()
                pausedAudio = warningSpeedUp
            } else if (zone1Audio.isPlaying) {
                zone1Audio.pause()
                pausedAudio = zone1Audio
            } else if (zone2Audio.isPlaying) {
                zone2Audio.pause()
                pausedAudio = zone2Audio
            } else if (zone3Part1Audio.isPlaying) {
                zone3Part1Audio.pause()
                pausedAudio = zone3Part1Audio
            } else if (zone4Part1Audio.isPlaying) {
                zone4Part1Audio.pause()
                pausedAudio = zone4Part1Audio
            } else if (zone3Part2Audio.isPlaying) {
                zone3Part2Audio.pause()
                pausedAudio = zone3Part2Audio
            } else if (zone4Part2Audio.isPlaying) {
                zone4Part2Audio.pause()
                pausedAudio = zone4Part2Audio
            } else if (endAudio.isPlaying) {
                endAudio.pause()
                pausedAudio = endAudio
            }
        } else { //continue
            pausedAudio.start()
        }
    }

    open fun startTimer() {
        isRunning = true
        handler.postDelayed(timerRunnable, 1000)
    }

    open fun stopTimer() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
    }

    private fun stopAllMedia() {
        if (warningSlowDown.isPlaying) {
            warningSlowDown.stop()
            warningSlowDown.release()
        } else if (warningSpeedUp.isPlaying) {
            warningSpeedUp.stop()
            warningSpeedUp.release()
        } else if (zone1Audio.isPlaying) {
            zone1Audio.stop()
            zone1Audio.release()
        } else if (zone2Audio.isPlaying) {
            zone2Audio.stop()
            zone2Audio.release()
        } else if (zone3Part1Audio.isPlaying) {
            zone3Part1Audio.stop()
            zone3Part1Audio.release()
        } else if (zone4Part1Audio.isPlaying) {
            zone4Part1Audio.stop()
            zone4Part1Audio.release()
        } else if (zone3Part2Audio.isPlaying) {
            zone3Part2Audio.stop()
            zone3Part2Audio.release()
        } else if (zone4Part2Audio.isPlaying) {
            zone4Part2Audio.stop()
            zone4Part2Audio.release()
        } else if (endAudio.isPlaying) {
            endAudio.stop()
            endAudio.release()
        } else if (pausedAudio.isPlaying) {
            pausedAudio.stop()
            pausedAudio.release()
        }
    }

    private val timerRunnable = Runnable { }


    private fun requestPermissionForPolarSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ), PERMISSION_REQUEST_CODE
                )
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }

        // callback is invoked after granted or denied permissions
        fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
        ) {
        }

    }


    public override fun onDestroy() {
        api.shutDown()
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        textToSpeech.shutdown()
        super.onDestroy()
    }


    private fun enableFeaturesToPolarSensor() {
        // NOTICE in this code snippet all the features are enabled.
        // You may enable only the features you are interested

        api.setApiCallback(object : PolarBleApiCallback() {


            override fun blePowerStateChanged(powered: Boolean) {
                Log.d("MyApp", "BLE power: $powered")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("MyApp", "CONNECTED: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("MyApp", "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("MyApp", "DISCONNECTED: ${polarDeviceInfo.deviceId}")
            }

            override fun bleSdkFeatureReady(
                identifier: String,
                feature: PolarBleApi.PolarBleSdkFeature
            ) {
                Log.d(TAG, "Polar BLE SDK feature $feature is ready")
                when (feature) {
                    PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING -> {
                        streamHR()
                    }

                    else -> {}
                }
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d("MyApp", "DIS INFO uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d("MyApp", "BATTERY LEVEL: $level")
            }

            override fun hrNotificationReceived(
                identifier: String,
                data: PolarHrData.PolarHrSample
            ) {
                //deprecated
            }

            override fun polarFtpFeatureReady(identifier: String) {
                //deprecated
            }

            override fun streamingFeaturesReady(
                identifier: String,
                features: Set<PolarBleApi.PolarDeviceDataType>
            ) {
                //deprecated
            }

            override fun hrFeatureReady(identifier: String) {
                //deprecated
            }
        })
    }


    private fun connectToPolarSensor() {
        try {

            api.connectToDevice("AD4A0229")
        } catch (a: PolarInvalidArgument) {
            Toast.makeText(this, "connectToPolarSensor failed !", Toast.LENGTH_SHORT).show()
            a.printStackTrace()
        }
    }

    private fun connectToNearbyPolarSensor() {
        api.autoConnectToDevice(-50, null, null).subscribe()
    }

    private fun searchForPolarSensor() {
        api.searchForDevice()
    }

    fun streamHR() {
        val isDisposed = hrDisposable?.isDisposed ?: true
        if (isDisposed) {
            hrDisposable = api.startHrStreaming("AD4A0229")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { hrData: PolarHrData ->
                        for (sample in hrData.samples) {
                            Log.d(TAG, "HR ${sample.hr} RR ${sample.rrsMs}")

                            if (sample.rrsMs.isNotEmpty()) {
                                val rrText = "(${sample.rrsMs.joinToString(separator = "ms, ")}ms)"
                                //textViewRR.text = rrText
                                //Toast.makeText(this,"rrText : ${rrText}",Toast.LENGTH_SHORT).show()
                            }

                            if (isRunning && inZone) {
                                val heartRateText = findViewById<TextView>(R.id.heartRateTextView)
                                heartRateText.text = "${sample.hr} bpm \n HEART RATE"
                                currentHr = sample.hr
                            } else {
                                currentHr = sample.hr
                            }


                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "HR stream failed. Reason $error")
                        hrDisposable = null
                    },
                    { Log.d(TAG, "HR stream complete") }
                )
        } else {
            // NOTE stops streaming if it is "running"
            hrDisposable?.dispose()
            hrDisposable = null
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return true
    }

    /**
     * Initializes a custom log class that outputs both to in-app targets and logcat.
     */
    private fun initializeLogging() {
    }


    private fun processChart() {
        heartRateChart = findViewById(R.id.heartRateChart)

        heartRateChart.getDescription().setEnabled(false)
        heartRateChart.setDrawGridBackground(true)
        heartRateChart.setPinchZoom(true)
        heartRateChart.setDrawBarShadow(true)
        heartRateChart.setDrawValueAboveBar(false)
        heartRateChart.setHighlightFullBarEnabled(false)
        heartRateChart.getAxisLeft().setEnabled(false)
        heartRateChart.getAxisRight().setEnabled(false)
        heartRateChart.getXAxis().setEnabled(false)
        //Get rid of negative Y Axis
        heartRateChart.axisLeft.axisMinimum = 0f

        // Initialize the entries list with default values for each bar
        for (i in 0 until 5) { // Assuming you have 5 bars
            entries.add(BarEntry(i.toFloat(), 0f)) // You can set the initial value as needed
        }


        dataSet = BarDataSet(entries, "Heart Rate")
        dataSet.setDrawValues(false)

        // Set custom colors for each bar
        val colors = intArrayOf(
            Color.GRAY,  // First bar (gray)
            Color.BLUE,  // Second bar (blue)
            Color.GREEN,  // Third bar (green)
            Color.YELLOW,  // Fourth bar (yellow)
            Color.RED // Fifth bar (red)

        )
        dataSet.setColors(*colors)


        val barData = BarData(dataSet)
        barData.setBarWidth(0.9f)
        heartRateChart.data = barData


        // Set a label for the X-axis
        val xAxis = heartRateChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(
            arrayOf(
                "Label1",
                "Label2",
                "Label3",
                "Label4",
                "Label5"
            )
        ) // Replace with your labels

        // Set a label for the Y-axis
        val yAxis = heartRateChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = 700f // Adjust maximum value as needed
        yAxis.setDrawLabels(true) // Ensure labels are drawn on the Y-axis
        yAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()} %" // Customize formatting as needed
            }
        }


        // Start a timer to update the chart every second
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Add a new data point to the chart
                runOnUiThread {
                    if (isRunning && inZone) {
                        val heartRateIntensity = if (currentHr >= 0) {
                            (currentHr.toFloat() / maxHR.toFloat()) * 100
                        } else {
                            0.0
                        }

                        val barIndex: Float
                        val newBarEntry: BarEntry
                        if (heartRateIntensity.toFloat() in 50.0..60.0) {
                            // Update the first bar's value (index 0)
                            barData.getDataSetByIndex(0).apply {
                                barIndex = 0F
                                val currentBarValue = entries[barIndex.toInt()].y
                                val updatedBarValue = currentBarValue + 1
                                newBarEntry = BarEntry(barIndex, updatedBarValue)
                                entries[barIndex.toInt()] = newBarEntry
                            }
                        } else if (heartRateIntensity.toFloat() in 60.0..70.0) {
                            barData.getDataSetByIndex(1).apply {
                                barIndex = 1F
                                val currentBarValue = entries[barIndex.toInt()].y
                                val updatedBarValue = currentBarValue + 1
                                newBarEntry = BarEntry(barIndex, updatedBarValue)
                                entries[barIndex.toInt()] = newBarEntry
                            }
                        } else if (heartRateIntensity.toFloat() in 70.0..80.0) {
                            barData.getDataSetByIndex(2).apply {
                                barIndex = 2F
                                val currentBarValue = entries[barIndex.toInt()].y
                                val updatedBarValue = currentBarValue + 1
                                newBarEntry = BarEntry(barIndex, updatedBarValue)
                                entries[barIndex.toInt()] = newBarEntry
                            }
                        } else if (heartRateIntensity.toFloat() in 80.0..90.0) {
                            barData.getDataSetByIndex(3).apply {
                                barIndex = 3F
                                val currentBarValue = entries[barIndex.toInt()].y
                                val updatedBarValue = currentBarValue + 1
                                newBarEntry = BarEntry(barIndex, updatedBarValue)
                                entries[barIndex.toInt()] = newBarEntry
                            }
                        } else if (heartRateIntensity.toFloat() in 90.0..100.0) {
                            barData.getDataSetByIndex(4).apply {
                                barIndex = 4F
                                val currentBarValue = entries[barIndex.toInt()].y
                                val updatedBarValue = currentBarValue + 1
                                newBarEntry = BarEntry(barIndex, updatedBarValue)
                                entries[barIndex.toInt()] = newBarEntry
                            }
                        }

                        // Get heart rate data from your source
                        dataSet.notifyDataSetChanged()
                        heartRateChart.notifyDataSetChanged()
                        heartRateChart.invalidate() // Refresh the chart
                    }

                }
            }
        }, 0, 1000) // Update every second
    }

    fun blinkSections(sectionNumber: Int) {
        val section0: View = findViewById(R.id.section0)
        val section1: View = findViewById(R.id.section1)
        val section2: View = findViewById(R.id.section2)
        val section3: View = findViewById(R.id.section3)
        val section4: View = findViewById(R.id.section4)
        // Stop blinking for the previous section
        if (sectionNumber > 0) {
            stopBlinking(
                when (sectionNumber - 1) {
                    0 -> section0
                    1 -> section1
                    2 -> section2
                    3 -> section3
                    else -> section4
                }
            )
        }
        when (sectionNumber) {
            0 -> {
                //Shadowed sections
                section1.setBackgroundColor(Color.parseColor("#30889900"))
                section2.setBackgroundColor(Color.parseColor("#30889900"))
                section3.setBackgroundColor(Color.parseColor("#30889900"))
                section4.setBackgroundColor(Color.parseColor("#30889900"))


                blinkSection(section0)
            }

            1 -> {
                //Light sections
                section0.setBackgroundColor(Color.parseColor("#80FFFF00"))
                //Shadowed sections
                section2.setBackgroundColor(Color.parseColor("#30889900"))
                section3.setBackgroundColor(Color.parseColor("#30889900"))
                section4.setBackgroundColor(Color.parseColor("#30889900"))

                blinkSection(section1)
            }

            2 -> {
                //Light sections
                section0.setBackgroundColor(Color.parseColor("#80FFFF00"))
                section1.setBackgroundColor(Color.parseColor("#80FFFF00"))
                //Shadowed sections
                section3.setBackgroundColor(Color.parseColor("#30889900"))
                section4.setBackgroundColor(Color.parseColor("#30889900"))




                blinkSection(section2)
            }

            3 -> {
                //Light sections
                section0.setBackgroundColor(Color.parseColor("#80FFFF00"))
                section1.setBackgroundColor(Color.parseColor("#80FFFF00"))
                section2.setBackgroundColor(Color.parseColor("#80FFFF00"))
                //Shadowed sections
                section4.setBackgroundColor(Color.parseColor("#30889900"))




                blinkSection(section3)
            }

            4 -> {
                //Light sections
                section0.setBackgroundColor(Color.parseColor("#80FFFF00"))
                section1.setBackgroundColor(Color.parseColor("#80FFFF00"))
                section2.setBackgroundColor(Color.parseColor("#80FFFF00"))
                section3.setBackgroundColor(Color.parseColor("#80FFFF00"))


                blinkSection(section4)
            }

        }


    }


    fun blinkSection(section: View) {
        if (blinkHandlers.containsKey(section)) {
            return // Animation is already blinking
        }

        val handler = Handler()
        blinkHandlers[section] = handler

        val runnable = object : Runnable {
            override fun run() {
                section.animate()
                    .alpha(0f)
                    .setDuration(1000)
                    .withEndAction {
                        section.alpha = 1f
                        handler.postDelayed(this, 1000) // Restart the animation
                    }
                    .start()
            }
        }

        handler.post(runnable)
    }

    // Call this function to stop the blinking animation for a section
    fun stopBlinking(section: View) {
        val handler = blinkHandlers[section]
        handler?.removeCallbacksAndMessages(null)
        blinkHandlers.remove(section)
        section.animate().cancel() // Cancel the ongoing animation
        section.alpha = 1f // Restore the alpha value
    }


    fun checkZone() {

        val heartRateIntensity = if (currentHr >= 0) {
            (currentHr.toFloat() / maxHR.toFloat()) * 100
        } else {
            0.0
        }

        Log.d("checkZone", "running")

        val hrIntensityText = findViewById<TextView>(R.id.hrIntensityTextView)
        hrIntensityText.text = "${heartRateIntensity.toInt()} % \n INTENSITY"

        if (!otherMissionAudiosAreOn()) {
            Log.d("checkZone", "otherMissionAudiosAreOn ${otherMissionAudiosAreOn()}")
            if (zoneNumber == 1) {
                if (heartRateIntensity.toFloat() in 50.0..60.0 && !inZone) {
                    inZone = true
                    startTimer()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Right Zone Again !", Toast.LENGTH_SHORT).show()
                    }
                } else if (heartRateIntensity.toFloat() !in 50.0..60.0 && inZone) {
                    inZone = false
                    stopTimer()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Wrong Zone !", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("checkZone", "playWarning")
                    playWarning(heartRateIntensity.toInt())
                } else if (heartRateIntensity.toFloat() !in 50.0..60.0) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Wrong Zone !", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("checkZone", "playWarning")
                    stopTimer()
                    playWarning(heartRateIntensity.toInt())
                }
            }

            if (zoneNumber == 2) {
                if (heartRateIntensity.toFloat() in 60.0..70.0 && !inZone) {
                    inZone = true
                    startTimer()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Right Zone Again !", Toast.LENGTH_SHORT).show()
                    }
                } else if (heartRateIntensity.toFloat() !in 60.0..70.0 && inZone) {
                    inZone = false
                    stopTimer()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Wrong Zone !", Toast.LENGTH_SHORT).show()
                    }
                    playWarning(heartRateIntensity.toInt())
                } else if (heartRateIntensity.toFloat() !in 60.0..70.0) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Wrong Zone !", Toast.LENGTH_SHORT).show()
                    }
                    stopTimer()
                    playWarning(heartRateIntensity.toInt())
                }
            }

            if (zoneNumber == 3) {
                if (heartRateIntensity.toFloat() in 70.0..80.0 && !inZone) {
                    inZone = true
                    startTimer()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Right Zone Again !", Toast.LENGTH_SHORT).show()
                    }
                } else if (heartRateIntensity.toFloat() !in 70.0..80.0 && inZone) {
                    inZone = false
                    stopTimer()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Wrong Zone !", Toast.LENGTH_SHORT).show()
                    }
                    playWarning(heartRateIntensity.toInt())
                } else if (heartRateIntensity.toFloat() !in 70.0..80.0) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Wrong Zone !", Toast.LENGTH_SHORT).show()
                    }
                    stopTimer()
                    playWarning(heartRateIntensity.toInt())
                }
            }

            if (zoneNumber == 4) {
                if (heartRateIntensity.toFloat() in 80.0..90.0 && !inZone) {
                    inZone = true
                    startTimer()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Right Zone Again !", Toast.LENGTH_SHORT).show()
                    }
                } else if (heartRateIntensity.toFloat() !in 80.0..90.0 && inZone) {
                    inZone = false
                    stopTimer()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Wrong Zone !", Toast.LENGTH_SHORT).show()
                    }
                    playWarning(heartRateIntensity.toInt())
                } else if (heartRateIntensity.toFloat() !in 80.0..90.0) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Wrong Zone !", Toast.LENGTH_SHORT).show()
                    }
                    stopTimer()
                    playWarning(heartRateIntensity.toInt())
                }
            }
        }
    }

    open fun playWarning(hrIntensity: Int) {
        if (startButton.isChecked) {
            if (zoneNumber == 1) {
                if (hrIntensity > 60) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Playing warning !", Toast.LENGTH_SHORT).show()
                    }
                    playAudio(warningSlowDown, 10)
                } else if (hrIntensity < 50) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Playing warning !", Toast.LENGTH_SHORT).show()
                    }
                    playAudio(warningSpeedUp, 11)
                }
            }
            if (zoneNumber == 2) {
                if (hrIntensity > 70) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Playing warning !", Toast.LENGTH_SHORT).show()
                    }
                    playAudio(warningSlowDown, 10)
                } else if (hrIntensity < 60) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Playing warning !", Toast.LENGTH_SHORT).show()
                    }
                    playAudio(warningSpeedUp, 11)
                }
            }
            if (zoneNumber == 3) {
                if (hrIntensity > 80) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Playing warning !", Toast.LENGTH_SHORT).show()
                    }
                    playAudio(warningSlowDown, 10)
                } else if (hrIntensity < 70) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Playing warning !", Toast.LENGTH_SHORT).show()
                    }
                    playAudio(warningSpeedUp, 11)
                }
            }

            if (zoneNumber == 4) {
                if (hrIntensity > 90) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Playing warning !", Toast.LENGTH_SHORT).show()
                    }
                    playAudio(warningSlowDown, 10)
                } else if (hrIntensity < 80) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Playing warning !", Toast.LENGTH_SHORT).show()
                    }
                    playAudio(warningSpeedUp, 11)
                }
            }
        }

    }

    open fun playAudio(mediaPlayer: MediaPlayer, resId: Int) {}

    fun updateZone(minutes: Int, secs: Int) {
        val targetZoneText = findViewById<TextView>(R.id.targetZoneTextView)

        if (minutes == zone2StartMinutes && secs == zone2StartSeconds) {
            zoneNumber = 2
            targetZoneText.text = "CURRENT TARGET ZONE: 2 \n 60% < INTENSITY < 70%  "
            blinkSections(1)
            playAudio(zone2Audio, 2)
        }
        if (minutes == zone3Part1StartMinutes && secs == zone3Part1StartSeconds) {
            zoneNumber = 3
            targetZoneText.text = "CURRENT TARGET ZONE: 3 \n 70% < INTENSITY < 80%  "
            blinkSections(2)
            playAudio(zone3Part1Audio, 31)
        }

        if (minutes == zone4Part1StartMinutes && secs == zone4Part1StartSeconds) {
            zoneNumber = 4
            targetZoneText.text = "CURRENT TARGET ZONE: 4 \n 80% < INTENSITY < 90%  "
            blinkSections(3)
            playAudio(zone4Part1Audio, 41)
        }

        if (minutes == zone3Part2StartMinutes && secs == zone3Part2StartSeconds) {
            zoneNumber = 3
            targetZoneText.text = "CURRENT TARGET ZONE: 3 \n 70% < INTENSITY < 80%  "
            blinkSections(2)
            playAudio(zone3Part2Audio, 32)
        }

        if (minutes == zone4Part2StartMinutes && secs == zone4Part2StartSeconds) {
            zoneNumber = 4
            targetZoneText.text = "CURRENT TARGET ZONE: 4 \n 80% < INTENSITY < 90%  "
            blinkSections(3)
            playAudio(zone4Part2Audio, 42)
        }
        if (minutes == 20) {
            playAudio(endAudio, 5)
        }

    }

    fun otherMissionAudiosAreOn(): Boolean {
        if (zone1Audio.isPlaying || zone2Audio.isPlaying || zone3Part1Audio.isPlaying ||

            zone4Part1Audio.isPlaying || zone3Part2Audio.isPlaying || zone4Part2Audio.isPlaying || endAudio.isPlaying || pausedAudio.isPlaying || warningSlowDown.isPlaying || warningSpeedUp.isPlaying || textToSpeech.isSpeaking
        ) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    this,
                    "Can not play warning! Other audio is running!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return true
        }
        return false
    }


    /*
    fun calculateZonesTime(
        percentageZone1: Int,
        percentageZone2: Int,
        percentageZone3: Int,
        percentageZone4: Int
    ) {
        // Total session time in seconds
        val totalSessionTimeSeconds = 20 * 60 // 20 minutes converted to seconds


        // Calculate start times for each zone in seconds
        zone1StartSeconds = 0
        zone2StartSeconds = (percentageZone1 * totalSessionTimeSeconds / 100)
        zone3Part1StartSeconds = zone2StartSeconds + (percentageZone2 * totalSessionTimeSeconds / 100)
        zone4Part1StartSeconds = zone3Part1StartSeconds + ((percentageZone3/2) * totalSessionTimeSeconds / 100)
        zone3Part2StartSeconds = zone4Part1StartSeconds + ((percentageZone4/2) * totalSessionTimeSeconds / 100)
        zone4Part2StartSeconds = zone3Part2StartSeconds + ((percentageZone3/2) * totalSessionTimeSeconds / 100)

        // Convert start times to minutes and remaining seconds
        zone1StartMinutes = zone1StartSeconds / 60
        val zone1StartRemainingSeconds = zone1StartSeconds % 60
        zone2StartMinutes = zone2StartSeconds / 60
        val zone2StartRemainingSeconds = zone2StartSeconds % 60
        zone3Part1StartMinutes = zone3Part1StartSeconds / 60
        val zone3StartRemainingSeconds = zone3Part1StartSeconds % 60
        zone4Part1StartMinutes = zone4Part1StartSeconds / 60
        val zone4StartRemainingSeconds = zone4Part1StartSeconds % 60
        zone3Part2StartMinutes = zone3Part2StartSeconds / 60
        val zone3Part2StartRemainingSeconds = zone3Part2StartSeconds % 60
        zone4Part2StartMinutes = zone4Part2StartSeconds / 60
        val zone4Part2StartRemainingSeconds = zone4Part2StartSeconds % 60

        zone1StartSeconds = zone1StartRemainingSeconds
        zone2StartSeconds = zone2StartRemainingSeconds
        zone3Part1StartSeconds = zone3StartRemainingSeconds
        zone4Part1StartSeconds = zone4StartRemainingSeconds
        zone3Part2StartSeconds = zone3Part2StartRemainingSeconds
        zone4Part2StartSeconds = zone4Part2StartRemainingSeconds

    } */


    fun calculateZonesTime(
    ) {
        zone1StartMinutes = 0
        zone1StartSeconds = 0

        zone2StartMinutes = 3
        zone2StartSeconds = 0

        zone3Part1StartMinutes = 9
        zone3Part1StartSeconds = 0

        zone4Part1StartMinutes = 13
        zone4Part1StartSeconds = 0

        zone3Part2StartMinutes = 14
        zone3Part2StartSeconds = 30

        zone4Part2StartMinutes = 18
        zone4Part2StartSeconds = 30

    }


    fun calculateZoneTimeSeekBar(percentageZone: Int): String {
        // Total session time in minutes
        val totalSessionTime = 20

        // Calculate the time for each zone
        val timeZoneMinutes = ((percentageZone / 100.0) * totalSessionTime).toInt()

        // Calculate the remaining seconds for each zone
        val timeZoneSeconds = ((percentageZone / 100.0) * totalSessionTime * 60).toInt() % 60

        if (timeZoneSeconds == 0) {
            return "$timeZoneMinutes Min"
        } else {
            return "$timeZoneMinutes Min, $timeZoneSeconds Sec"
        }

    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                // Handle the language not supported or missing data
                Log.e("TTS", "Language is not supported")
            }
        } else {
            // Handle TTS initialization failure
            Log.e("TTS", "Initialization failed")
        }
    }

    private fun speak() {
        if (!otherMissionAudiosAreOn() && isRunning && inZone) {
            val timerText = timerTextView.text.toString()
            //extract only the "00:00:00" part from the given string
            val time = timerText.substring(1, 9)
            //convert the time string to time and calculate the percentage of that time in the total time which is 20 minutes
            val timeInSeconds = timeToSeconds(time)
            val percentage = (timeInSeconds / 1200.0) * 100
            //round the percentage to before the decimal point
            val percentageRounded = percentage.roundToInt()
            val text = "You are in zone $zoneNumber. You have completed $percentageRounded percent of the mission. Keep going!"
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun timeToSeconds(time: String): Int {
        Log.d("time", time)
        val timeArray = time.split(":")
        val hours = timeArray[0].toInt()
        val minutes = timeArray[1].toInt()
        val seconds = timeArray[2].toInt()
        return hours * 3600 + minutes * 60 + seconds
    }


}