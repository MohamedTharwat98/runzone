package com.example.runzone
import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.runzone.SharedData.SharedData
import com.example.runzone.SharedData.SharedData.token
import com.example.runzone.model.HeartRateDataResponse
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


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
            override fun onResponse(
                call: Call<HeartRateDataResponse>,
                response: Response<HeartRateDataResponse>
            ) {
                if (response.isSuccessful) {
                    val heartRateData = response.body()
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
}

interface GoogleFitApi {
    // Define API endpoints here
    // For example:
    @GET("users/me/dataSources/derived:com.google.heart_rate.bpm:com.google.android.gms:merge_heart_rate_bpm/dataPointChanges")
    fun getHeartRateData(): Call<HeartRateDataResponse>
}

