package com.example.runzone
import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.runzone.SharedData.SharedData.token
import com.example.runzone.model.HeartRateDataResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.TimeZone


class HeartRateActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS_CODE = 1001
    private val GOOGLE_FIT_API_BASE_URL = "https://fitness.googleapis.com/fitness/v1/"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heart_rate)

        // Check for required permissions
        if (checkPermissions()) {
            readHeartRateData()
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BODY_SENSORS
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BODY_SENSORS),
            REQUEST_PERMISSIONS_CODE
        )
    }

    private fun readHeartRateData() {

        val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain: Interceptor.Chain ->
            val original = chain.request()
            val requestBuilder: Request.Builder = original.newBuilder()
                .header("Authorization", "Bearer $token")

            val request: Request = requestBuilder.build()
            val response = chain.proceed(request)

            response
        }).build()


        val retrofit = Retrofit.Builder()
            .baseUrl(GOOGLE_FIT_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()



        val googleFitApi = retrofit.create(GoogleFitApi::class.java)

        val heartRateDataCall = googleFitApi.getHeartRateData()

        val requestUrl = heartRateDataCall.request().url().toString() // Get the complete URL of the request

        Log.d(TAG, "Request URL: $requestUrl")

        heartRateDataCall.enqueue(object : Callback<HeartRateDataResponse> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(
                call: Call<HeartRateDataResponse>,
                response: Response<HeartRateDataResponse>
            ) {
                if (response.isSuccessful) {
                    val heartRateData = response.body()
                    getLastHeartBeat(heartRateData)
                    // Process heart rate data here
                } else {
                    Toast.makeText(
                        this@HeartRateActivity,
                        "${response.code()}: ${response.message()}",    // Show error message
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<HeartRateDataResponse>, t: Throwable) {
                Toast.makeText(
                    this@HeartRateActivity,
                    "Failed to fetch heart rate data: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getLastHeartBeat(heartRateData: HeartRateDataResponse?) {
        /*heartRateData?.insertedDataPoint?.forEach() {
            Log.d("Bucket", "Inserted Data points: ${Gson().toJson(it)}")
            Toast.makeText(this@HeartRateActivity, "Inserted Data points: : ${Gson().toJson(it)}", Toast.LENGTH_SHORT).show()

        }*/
        val lastHeartBeat = heartRateData?.insertedDataPoint?.last()
        val lastHeartBeatValue = lastHeartBeat?.values?.last()
        val lastHeartBeatRate = lastHeartBeatValue?.heartRate
        val lastHeartBeatTime = lastHeartBeat?.modifiedTimeMillis
        val lastHeartBeatTimeConverted = convertMillisecondsToDate(lastHeartBeatTime!!)

        Log.d(TAG, "Last heart beat: $lastHeartBeatRate")
        Toast.makeText(this@HeartRateActivity, "Last heart beat: $lastHeartBeatRate", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Last heart beat time: $lastHeartBeatTimeConverted")
        Toast.makeText(this@HeartRateActivity, "Last heart beat time: $lastHeartBeatTimeConverted", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun convertMillisecondsToDate(milliseconds: String): String {
        val millis = milliseconds.toLong()
        val instant = Instant.ofEpochMilli(millis)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return dateTime.format(formatter)
    }



}

interface GoogleFitApi {
    // Define API endpoints here
    // For example:
    @GET("users/me/dataSources/derived:com.google.heart_rate.bpm:com.google.android.gms:merge_heart_rate_bpm/dataPointChanges")
    fun getHeartRateData(): Call<HeartRateDataResponse>
}

