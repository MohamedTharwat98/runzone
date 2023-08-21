package com.example.runzone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.DataSourcesRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.Math.ceil
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit



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

    private val startButton: Button by lazy {
        findViewById<Button>(R.id.startButton)
    }

     var maxHR = 0

     var heartRateIntensity = 0

     var missionType = ""

    private var hrDisposable: Disposable? = null

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
                val age = editTextAge.text.toString()
                // Handle the entered age here
                try {
                    val ageInt = age.toInt()
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

        if (missionType=="story") {
            val imageView = findViewById<ImageView>(R.id.bgImageView)
            imageView.setImageResource(R.drawable.missionbg3)
        } else {
            val imageView = findViewById<ImageView>(R.id.bgImageView)
            imageView.setImageResource(R.drawable.directmission1)
        }

        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.
        initializeLogging()

        //checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)
        requestPermissionForPolarSensor()
        connectToPolarSensor()
        enableFeaturesToPolarSensor()

        val maxHeartRateText = findViewById<TextView>(R.id.maxHRTextView)
        maxHeartRateText.text = "${maxHR} bpm \n HEART RATE"


        handler = Handler(Looper.getMainLooper())


        startButton.setOnClickListener {
            if (!isRunning) {
                startTimer()
                startButton.text = "Stop"
            } else {
                stopTimer()
                startButton.text = "Start"
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

                            val heartRateText = findViewById<TextView>(R.id.heartRateTextView)
                            heartRateText.text = "${sample.hr} bpm \n HEART RATE"

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



}