package com.example.runzone

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.alpha
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
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
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.UUID


const val TAG = "HeartRateActivity"

/**
 * This sample demonstrates the Recording API of the Google Fit platform. It allows users to start
 * and stop a subscription to a given data type, as well as read the current daily step total.
 *
 */
open class HeartRateActivity : AppCompatActivity() {

    lateinit var api: PolarBleApi

    private val PERMISSION_REQUEST_CODE = 1


    var seconds: Int = 0
    var isRunning: Boolean = false
    lateinit var handler: Handler

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

    var zoneNumber = 0

    var inZone = false

    var zoneTimeSeconds = 0

    var zoneTimeMinutes = 0

    lateinit var warning : MediaPlayer

    private lateinit var heartRateChart: BarChart
    private lateinit var dataSet: BarDataSet
    private val entries = ArrayList<BarEntry>()

    private val blinkHandlers = mutableMapOf<View, Handler>() // Store handlers for blinking animations



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        api = PolarBleApiDefaultImpl.defaultImplementation(this,
            setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO)
        )

        showAgeInputDialog();


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAgeInputDialog() {
        val popupView = layoutInflater.inflate(R.layout.age_box, null)
        val editTextAge = popupView.findViewById<EditText>(R.id.editTextAge)

        val alertDialogBuilder = AlertDialog.Builder(this)
            .setView(popupView)
            .setTitle("Enter Your Age")
            .setPositiveButton("Start Mission") { dialog, which ->
                var age = editTextAge.text.toString()
                // Handle the entered age here
                try {
                    val ageInt = age.toInt()
                    runnersAge = ageInt
                    maxHR = 220 - ageInt
                    startMission()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this,"Your age must be a valid number ",Toast.LENGTH_SHORT).show()
                }

            }
            .setNegativeButton("Cancel", null)

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMission () {
        setContentView(R.layout.activity_mission)

        missionType = intent.getStringExtra("missionType").toString()

        handler = Handler(Looper.getMainLooper())

        isRunning = true

        inZone = true

        startButton.isChecked = true

        startTimer()

        // to be continued !!
        warning = MediaPlayer.create(this, R.raw.narratorslowdown)

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

        val session = Session(
            duration = "",
            date = Date().toString(),
            maxHR = maxHR.toFloat(),
            missionType = missionType,
            age = runnersAge,
            zone0 = 0F,
            zone1 = 0F,
            zone2 = 0F,
            zone3 = 0F,
            zone4 = 0F
        )



        stopButton.setOnClickListener {
            session.zone0 = entries.get(0).y
            session.zone1 = entries.get(1).y
            session.zone2 = entries.get(2).y
            session.zone3 = entries.get(3).y
            session.zone4 = entries.get(4).y
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            val timer = String.format("%02d:%02d:%02d", hours, minutes, secs)
            session.duration = timer

            val databaseRef: DatabaseReference = FirebaseDatabase.getInstance("https://plucky-balm-389709-default-rtdb.europe-west1.firebasedatabase.app/").getReference("sessions")
            val sessionKey: String? = databaseRef.push().key

            if (sessionKey != null) {
                databaseRef.child(sessionKey).setValue(session)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Session data saved successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error saving session data: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

        }


        startButton.setOnClickListener {
            if (!isRunning) {
                startTimer()
                startButton.isChecked = true
            } else {
                if (warning.isPlaying) {
                    warning.pause()
                }
                stopTimer()
                startButton.isChecked = false
            }
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

    private val timerRunnable = Runnable { }



    private fun requestPermissionForPolarSensor () {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
        }

        // callback is invoked after granted or denied permissions
        fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        }

    }


    public override fun onDestroy() {
        super.onDestroy()
        api.shutDown()
    }


    private fun enableFeaturesToPolarSensor () {
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

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
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

            override fun hrNotificationReceived(identifier: String, data: PolarHrData.PolarHrSample) {
                //deprecated
            }

            override fun polarFtpFeatureReady(identifier: String) {
                //deprecated
            }

            override fun streamingFeaturesReady(identifier: String, features: Set<PolarBleApi.PolarDeviceDataType>) {
                //deprecated
            }

            override fun hrFeatureReady(identifier: String) {
                //deprecated
            }
        })
    }



    private fun connectToPolarSensor () {
        try {

            api.connectToDevice("AD4A0229")
        } catch (a: PolarInvalidArgument) {
            Toast.makeText(this,"connectToPolarSensor failed !",Toast.LENGTH_SHORT).show()
            a.printStackTrace()
        }
    }

    private fun connectToNearbyPolarSensor () {
            api.autoConnectToDevice(-50, null, null).subscribe()
    }

    private fun searchForPolarSensor () {
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

                            if(isRunning && inZone) {
                                val heartRateText = findViewById<TextView>(R.id.heartRateTextView)
                                heartRateText.text = "${sample.hr} bpm \n HEART RATE"
                                currentHr = sample.hr
                            } else {
                                currentHr = sample.hr
                            }

                            checkZone()


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
       xAxis.valueFormatter = IndexAxisValueFormatter(arrayOf("Label1", "Label2", "Label3", "Label4", "Label5")) // Replace with your labels

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
                   if(isRunning && inZone) {
                       val heartRateIntensity = if (currentHr >= 0) {
                           (currentHr.toFloat() / maxHR.toFloat()) * 100
                       } else {
                           0.0
                       }

                       val barIndex: Float
                       val newBarEntry: BarEntry
                       if (heartRateIntensity.toFloat() <= 57) {
                           // Update the first bar's value (index 0)
                           barData.getDataSetByIndex(0).apply {
                               barIndex = 0F
                               val currentBarValue = entries[barIndex.toInt()].y
                               val updatedBarValue = currentBarValue + 1
                               newBarEntry = BarEntry(barIndex, updatedBarValue)
                               entries[barIndex.toInt()] = newBarEntry
                           }
                       } else if (heartRateIntensity.toFloat() > 57 && heartRateIntensity.toFloat() < 63) {
                           barData.getDataSetByIndex(1).apply {
                               barIndex = 1F
                               val currentBarValue = entries[barIndex.toInt()].y
                               val updatedBarValue = currentBarValue + 1
                               newBarEntry = BarEntry(barIndex, updatedBarValue)
                               entries[barIndex.toInt()] = newBarEntry
                           }
                       } else if (heartRateIntensity.toFloat() > 64 && heartRateIntensity.toFloat() < 76) {
                           barData.getDataSetByIndex(2).apply {
                               barIndex = 2F
                               val currentBarValue = entries[barIndex.toInt()].y
                               val updatedBarValue = currentBarValue + 1
                               newBarEntry = BarEntry(barIndex, updatedBarValue)
                               entries[barIndex.toInt()] = newBarEntry
                           }
                       } else if (heartRateIntensity.toFloat() > 76 && heartRateIntensity.toFloat() < 95) {
                           barData.getDataSetByIndex(3).apply {
                               barIndex = 3F
                               val currentBarValue = entries[barIndex.toInt()].y
                               val updatedBarValue = currentBarValue + 1
                               newBarEntry = BarEntry(barIndex, updatedBarValue)
                               entries[barIndex.toInt()] = newBarEntry
                           }
                       } else if (heartRateIntensity.toFloat() > 95 && heartRateIntensity.toFloat() < 100) {
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

    fun blinkSections (sectionNumber : Int) {
        val section0 : View = findViewById(R.id.section0)
        val section1 : View = findViewById(R.id.section1)
        val section2 : View = findViewById(R.id.section2)
        val section3 : View = findViewById(R.id.section3)
        val section4 : View = findViewById(R.id.section4)
        // Stop blinking for the previous section
        if (sectionNumber > 0) {
            stopBlinking(when (sectionNumber - 1) {
                0 -> section0
                1 -> section1
                2 -> section2
                3 -> section3
                else -> section4
            })
        }
        when (sectionNumber){
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


    fun checkZone () {

        val heartRateIntensity = if (currentHr >= 0) {
            (currentHr.toFloat() / maxHR.toFloat()) * 100
        } else {
            0.0
        }

        val hrIntensityText = findViewById<TextView>(R.id.hrIntensityTextView)
        hrIntensityText.text = "${heartRateIntensity.toInt()} % \n HR-INTENSITY"


        if (zoneNumber == 0) {
            if (heartRateIntensity.toFloat() <= 57 && !inZone) {
                inZone = true
                startTimer()
                Toast.makeText(this,"Right Zone Again !",Toast.LENGTH_SHORT).show()
            } else if (heartRateIntensity.toFloat() >= 57 && inZone)
            {
                inZone = false
                stopTimer()
                Toast.makeText(this,"Wrong Zone !",Toast.LENGTH_SHORT).show()
                playWarning(heartRateIntensity.toInt())
            }
        }


    }

    open fun playWarning (hrIntensity: Int) {
        if (zoneNumber == 0 && hrIntensity > 57) {
            Toast.makeText(this,"Playing warning !",Toast.LENGTH_SHORT).show()
            //warning = MediaPlayer.create(this, R.raw.narratorslowdown)
            warning.start()
        }
    }


}