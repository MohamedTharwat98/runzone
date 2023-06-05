package com.example.runzone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HeartRateActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HeartRateActivity"
        private const val REQUEST_PERMISSIONS = 1
    }

    private lateinit var heartRateTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heart_rate)

        // Initialize UI elements
        heartRateTextView = findViewById(R.id.heartRateTextView)

        // Check and request necessary permissions
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val permissionGranted = PackageManager.PERMISSION_GRANTED
        if (ContextCompat.checkSelfPermission(this, permission) != permissionGranted) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_PERMISSIONS)
        } else {
            accessGoogleFit()
        }
    }

    private fun accessGoogleFit() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        val client = Fitness.getHistoryClient(this, account!!)

        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_WEEK, -1) // Set the start time to 24 hours ago

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        client.readData(readRequest)
            .addOnSuccessListener { response: DataReadResponse? ->
                handleDataReadSuccess(response)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error reading data from Google Fit", e)
            }
    }


    private fun handleDataReadSuccess(response: DataReadResponse?) {
        response?.let {
            if (it.status.isSuccess) {
                val dataSets = it.getDataSets()
                // Process the heart rate data sets
                for (dataSet in dataSets) {
                    processHeartRateDataSet(dataSet)
                }
            } else {
                Log.e(TAG, "Error reading data from Google Fit: ${it.status.statusMessage}")
            }
        }
    }

    private fun processHeartRateDataSet(dataSet: DataSet) {
        val dataPoints = dataSet.dataPoints
        for (dataPoint in dataPoints) {
            val field = dataPoint.dataType.fields[0]
            val heartRate = dataPoint.getValue(field).asFloat()
            // Process heart rate value
            Log.d(TAG, "Heart Rate: $heartRate bpm")
            runOnUiThread {
                heartRateTextView.text = "Heart Rate: $heartRate bpm"
            }
        }
    }
}
