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
import java.lang.Math.ceil
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit


const val TAG = "BasicRecordingApi"

/**
 * This enum is used to define actions that can be performed after a successful sign in to Fit.
 * One of these values is passed to the Fit sign-in, and returned in a successful callback, allowing
 * subsequent execution of the desired action.
 */
enum class FitActionRequestCode {
    SUBSCRIBE,
    CANCEL_SUBSCRIPTION,
    DUMP_SUBSCRIPTIONS
}

/**
 * This sample demonstrates the Recording API of the Google Fit platform. It allows users to start
 * and stop a subscription to a given data type, as well as read the current daily step total.
 *
 */
open class HeartRateActivity : AppCompatActivity() {
    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_LOCATION_SAMPLE, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_SPEED_SUMMARY, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .build()
    }

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private var dataPointListener: OnDataPointListener? = null


    var seconds: Int = 0
    var isRunning: Boolean = false
    lateinit var handler: Handler

    val timerTextView: TextView by lazy {
        findViewById<TextView>(R.id.timerTextView)
    }

    private val startButton: Button by lazy {
        findViewById<Button>(R.id.startButton)
    }

     var maxHR = 0;

     var heartRateIntensity = 0;

    public var missionType = "";


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

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
                    Toast.makeText(this,"Your maximum Heart Rate Beat per minute is : ${maxHR}",Toast.LENGTH_SHORT).show()
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

        checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)

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



    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkPermissionsAndRun(fitActionRequestCode: FitActionRequestCode) {
        if (permissionApproved()) {
            fitSignIn(fitActionRequestCode)
        } else {
            requestRuntimePermissions(fitActionRequestCode)
        }
    }

    /**
     * Checks that the user is signed in, and if so, executes the specified function. If the user is
     * not signed in, initiates the sign in flow, specifying the post-sign in function to execute.
     *
     * @param requestCode The request code corresponding to the action to perform after sign in.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun fitSignIn(requestCode: FitActionRequestCode) {
        if (oAuthPermissionsApproved()) {
            performActionForRequestCode(requestCode)
        } else {
            requestCode.let {
                GoogleSignIn.requestPermissions(
                    this,
                    it.ordinal,
                    getGoogleAccount(), fitnessOptions
                )
            }
        }
    }

    /**
     * Handles the callback from the OAuth sign in flow, executing the post sign in function
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (resultCode) {
            RESULT_OK -> {
                val postSignInAction = FitActionRequestCode.values()[requestCode]
                postSignInAction.let {
                    performActionForRequestCode(postSignInAction)
                }
            }

            else -> oAuthErrorMsg(requestCode, resultCode)
        }
    }

    /**
     * Runs the desired method, based on the specified request code. The request code is typically
     * passed to the Fit sign-in flow, and returned with the success callback. This allows the
     * caller to specify which method, post-sign-in, should be called.
     *
     * @param requestCode The code corresponding to the action to perform.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun performActionForRequestCode(requestCode: FitActionRequestCode) =
        when (requestCode) {
            FitActionRequestCode.SUBSCRIBE -> subscribe()
            FitActionRequestCode.CANCEL_SUBSCRIPTION -> cancelSubscription()
            FitActionRequestCode.DUMP_SUBSCRIPTIONS -> dumpSubscriptionsList()
        }

    private fun oAuthErrorMsg(requestCode: Int, resultCode: Int) {
        val message = """
            There was an error signing into Fit. Check the troubleshooting section of the README
            for potential issues.
            Request code was: $requestCode
            Result code was: $resultCode
        """.trimIndent()
        Log.e(TAG, message)
        Toast.makeText(this, "There was an error signing into Fit", Toast.LENGTH_LONG)
            .show()
    }

    private fun oAuthPermissionsApproved() =
        GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)

    /**
     * Gets a Google account for use in creating the Fitness client. This is achieved by either
     * using the last signed-in account, or if necessary, prompting the user to sign in.
     * `getAccountForExtension` is recommended over `getLastSignedInAccount` as the latter can
     * return `null` if there has been no sign in before.
     */
    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

    /**
     * Subscribes to an available [DataType]. Subscriptions can exist across application
     * instances (so data is recorded even after the application closes down).  When creating
     * a new subscription, it may already exist from a previous invocation of this app.  If
     * the subscription already exists, the method is a no-op.  However, you can check this with
     * a special success code.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        // [START subscribe_to_datatype]
        /*
        Fitness.getRecordingClient(this, getGoogleAccount())
            .subscribe(DataType.TYPE_CALORIES_EXPENDED)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully subscribed!")
                Toast.makeText(this, "Successfully subscribed!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Log.i(TAG, "There was a problem subscribing.")
                Toast.makeText(this, "There was a problem subscribing.", Toast.LENGTH_LONG).show()
            }
        // [END subscribe_to_datatype]

        val startTime = System.currentTimeMillis()
        val session = Session.Builder()
            .setName("sessionName")
            .setIdentifier("UniqueIdentifierHere")
            .setDescription("Morning run")
            .setActivity(FitnessActivities.RUNNING)
            .setStartTime(startTime, TimeUnit.MILLISECONDS)
            .build()

        // 3. Use the Sessions client to start a session:
        Fitness.getSessionsClient(this, getGoogleAccount())
            .startSession(session)
            .addOnSuccessListener {
                Log.i(TAG, "Session started successfully!")
                Toast.makeText(this, "Session started successfully!", Toast.LENGTH_LONG).show()
                readDate()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was an error starting the session", e)
                Toast.makeText(this, "There was an error starting the session", Toast.LENGTH_LONG)
                    .show()
            }
            */

    }

    /**
     * Reads fitness data by using a [DataReadRequest].  It is possible to read data in
     * readData() method.
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun readAllData () {
        readData(DataType.TYPE_HEART_RATE_BPM, Field.FIELD_BPM)
        readData(DataType.TYPE_STEP_COUNT_DELTA, Field.FIELD_STEPS)
        readData(DataType.TYPE_SPEED, Field.FIELD_SPEED)
        readData(DataType.TYPE_DISTANCE_DELTA, Field.FIELD_DISTANCE)

        updateHeartRateIntensity(readData(DataType.TYPE_HEART_RATE_BPM, Field.FIELD_BPM))
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun readData(dataType: DataType, field: Field) : String {
        // Read the data that's been collected throughout the past week.
        val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
        //val startTime = endTime.minusWeeks(1)
        //val startTime = endTime.minusHours(1) //last 1 hour
        val startTime = endTime.minusMinutes(30) //last 30 minutes
        Log.i(TAG, "Range Start: $startTime")
        Log.i(TAG, "Range End: $endTime")

        val readRequest =
            DataReadRequest.Builder()
                // The data request can specify multiple data types to return,
                // effectively combining multiple data queries into one call.
                // This example demonstrates aggregating only one data type.
                .aggregate(dataType)
                // Analogous to a "Group By" in SQL, defines how data should be
                // aggregated.
                // bucketByTime allows for a time span, whereas bucketBySession allows
                // bucketing by <a href="/fit/android/using-sessions">sessions</a>.
                //.bucketByTime(1, TimeUnit.DAYS) last one week
                //.bucketByTime(1, TimeUnit.DAYS)  // Update time span to 1 day
                .bucketByTime(30, TimeUnit.MINUTES)  // Update time span to 30 minutes
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()

        var returnValue = ""

        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { response ->
                // The aggregate query puts datasets into buckets, so flatten into a
                // single list of datasets
                //read last element
                val lastBucket = response.buckets.last()
                val lastDataSet = lastBucket.dataSets.last()
                if (lastDataSet.isEmpty && field == Field.FIELD_BPM) {
                    val heartRateText = findViewById<TextView>(R.id.heartRateTextView)
                    heartRateText.text = "No data \n HEART RATE"
                    return@addOnSuccessListener
                } else if (lastDataSet.isEmpty && field == Field.FIELD_STEPS) {
                    val stepsCountText = findViewById<TextView>(R.id.stepsTextView)
                    stepsCountText.text = "No data \n STEPS"
                    return@addOnSuccessListener
                } else if (lastDataSet.isEmpty && field == Field.FIELD_SPEED) {
                    val speedText = findViewById<TextView>(R.id.speedTextView)
                    speedText.text = "No data \n SPEED"
                    return@addOnSuccessListener
                } else if (lastDataSet.isEmpty && field == Field.FIELD_DISTANCE) {
                    val distanceText = findViewById<TextView>(R.id.distanceTextView)
                    distanceText.text = "No data \n DISTANCE"
                    return@addOnSuccessListener
                }
                val lastDataPoint = lastDataSet.dataPoints.last()
                var lastValue = ""
                if (field == Field.FIELD_BPM) {
                    lastValue =
                        lastDataPoint.getValue(lastDataPoint.dataType.fields[0]).toString()
                    val heartRateText = findViewById<TextView>(R.id.heartRateTextView)
                    heartRateText.text = "${lastValue} bpm \n HEART RATE"
                } else if (field == Field.FIELD_STEPS) {
                    lastValue = lastDataPoint.getValue(Field.FIELD_STEPS).toString()
                    val stepsCountText = findViewById<TextView>(R.id.stepsTextView)
                    stepsCountText.text = "${lastValue} \n STEPS"
                } else if (field == Field.FIELD_SPEED) {
                    val value = lastDataPoint.getValue((lastDataPoint.dataType.fields[0])).asFloat()
                    lastValue = ceil(value.toDouble()).toString()
                    //number 0 is the average speed, number 1 is the max speed, number 2 is the min speed
                    //DataType{com.google.speed.summary[average(f), max(f), min(f)]}
                    val speedText = findViewById<TextView>(R.id.speedTextView)
                    speedText.text = "${lastValue} m/s \n SPEED"
                } else if (field == Field.FIELD_DISTANCE) {
                    val value = lastDataPoint.getValue(lastDataPoint.dataType.fields[0]).asFloat()
                    lastValue = ceil(value.toDouble()).toString()
                    val distanceText = findViewById<TextView>(R.id.distanceTextView)
                    distanceText.text = "${lastValue} m \n DISTANCE"
                }

                val lastStartTime = lastDataPoint.getStartTimeString()
                val lastEndTime = lastDataPoint.getEndTimeString()

                returnValue = lastValue

            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was an error reading data from Google Fit", e)
                Toast.makeText(
                    this,
                    "There was an error reading data from Google Fit",
                    Toast.LENGTH_SHORT
                ).show()
            }

        return returnValue

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateHeartRateIntensity (heartRate : String) {
            if (heartRate == "") {
                Toast.makeText(this,"Can not read heart rate data !",Toast.LENGTH_SHORT).show()
            } else {
               heartRateIntensity = heartRate.toInt() / maxHR
            }

    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun DataPoint.getStartTimeString() = Instant.ofEpochSecond(this.getStartTime(TimeUnit.SECONDS))
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime().toString()

    @RequiresApi(Build.VERSION_CODES.O)
    fun DataPoint.getEndTimeString() = Instant.ofEpochSecond(this.getEndTime(TimeUnit.SECONDS))
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime().toString()


    /**
     * Fetches a list of all active subscriptions and log it. Since the logger for this sample
     * also prints to the screen, we can see what is happening in this way.
     */
    private fun dumpSubscriptionsList() {
        // [START list_current_subscriptions]
        Fitness.getRecordingClient(this, getGoogleAccount())
            .listSubscriptions(DataType.TYPE_CALORIES_EXPENDED)
            .addOnSuccessListener { subscriptions ->
                for (subscription in subscriptions) {
                    val dataType = subscription.dataType!!
                    Log.i(TAG, "Active subscription for data type: ${dataType.name}")
                    Toast.makeText(
                        this,
                        "Active subscription for data type: ${dataType.name}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (subscriptions.isEmpty()) {
                    Log.i(TAG, "No active subscriptions")
                    Toast.makeText(this, "No active subscriptions", Toast.LENGTH_LONG).show()
                }
            }
        // [END list_current_subscriptions]
    }

    /**
     * Cancels the TYPE_CALORIES_EXPENDED subscription by calling unsubscribe on that [DataType].
     */
    private fun cancelSubscription() {
        val dataTypeStr = DataType.TYPE_CALORIES_EXPENDED.toString()
        Log.i(TAG, "Unsubscribing from data type: $dataTypeStr")

        // Invoke the Recording API to unsubscribe from the data type and specify a callback that
        // will check the result.
        // [START unsubscribe_from_datatype]
        Fitness.getRecordingClient(this, getGoogleAccount())
            .unsubscribe(DataType.TYPE_CALORIES_EXPENDED)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully unsubscribed for data type: $dataTypeStr")
            }
            .addOnFailureListener {
                // Subscription not removed
                Log.i(TAG, "Failed to unsubscribe for data type: $dataTypeStr")
            }
        // [END unsubscribe_from_datatype]
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

    private fun permissionApproved(): Boolean {
        val approved = if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            true
        }
        return approved
    }

    private fun requestRuntimePermissions(requestCode: FitActionRequestCode) {
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            )

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        requestCode.let {
            if (shouldProvideRationale) {
                Log.i(TAG, "Displaying permission rationale to provide additional context.")
                // Request permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    requestCode.ordinal
                )
            } else {
                Log.i(TAG, "Requesting permission")
                // Request permission. It's possible this can be auto answered if device policy
                // sets the permission in a given state or the user denied the permission
                // previously and checked "Never ask again".
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    requestCode.ordinal
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when {
            grantResults.isEmpty() -> {
                // If user interaction was interrupted, the permission request
                // is cancelled and you receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
            }

            grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                // Permission was granted.
                val fitActionRequestCode = FitActionRequestCode.values()[requestCode]
                fitActionRequestCode.let {
                    fitSignIn(fitActionRequestCode)
                }
            }

            else -> {
                // Permission denied.

                // In this Activity we've chosen to notify the user that they
                // have rejected a core permission for the app since it makes the Activity useless.
                // We're communicating this message in a Snackbar since this is a sample app, but
                // core permissions would typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.

                // Build intent that displays the App settings screen.
                Toast.makeText(this, "Permission Denied !!!!!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    /** Finds available data sources and attempts to register on a specific [DataType].  */
    private fun findFitnessDataSources() { // [START find_data_sources]
        Fitness.getSensorsClient(this, getGoogleAccount())
            .findDataSources(
                DataSourcesRequest.Builder()
                    .setDataTypes(DataType.TYPE_LOCATION_SAMPLE)
                    .setDataSourceTypes(DataSource.TYPE_RAW)
                    .build()
            )
            .addOnSuccessListener { dataSources ->
                for (dataSource in dataSources) {
                    Log.i(TAG, "Data source found: $dataSource")
                    Toast.makeText(this, "Data source found: $dataSource", Toast.LENGTH_SHORT)
                        .show()
                    Log.i(TAG, "Data Source type: " + dataSource.dataType.name)
                    Toast.makeText(
                        this,
                        "Data Source type: " + dataSource.dataType.name,
                        Toast.LENGTH_SHORT
                    ).show()
                    // Let's register a listener to receive Activity data!
                    if (dataSource.dataType == DataType.TYPE_LOCATION_SAMPLE && dataPointListener == null) {
                        Log.i(TAG, "Data source for TYPE_LOCATION_SAMPLE found!  Registering.")
                        Toast.makeText(
                            this,
                            "Data source for TYPE_LOCATION_SAMPLE found!  Registering.",
                            Toast.LENGTH_SHORT
                        ).show()
                        registerFitnessDataListener(dataSource, DataType.TYPE_LOCATION_SAMPLE)
                    }
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "failed", e) }
        // [END find_data_sources]
    }

    /**
     * Registers a listener with the Sensors API for the provided [DataSource] and [DataType] combo.
     */
    private fun registerFitnessDataListener(dataSource: DataSource, dataType: DataType) {
        // [START register_data_listener]
        dataPointListener = OnDataPointListener { dataPoint ->
            Toast.makeText(this, "Data Point Works !!!!!", Toast.LENGTH_SHORT).show()
            for (field in dataPoint.dataType.fields) {
                val value = dataPoint.getValue(field)
                Log.i(TAG, "Detected DataPoint field: ${field.name}")
                Toast.makeText(this, "Detected DataPoint field: ${field.name}", Toast.LENGTH_SHORT)
                    .show()
                Log.i(TAG, "Detected DataPoint value: $value")
                Toast.makeText(this, "Detected DataPoint value: $value", Toast.LENGTH_SHORT).show()
            }
        }

        Fitness.getSensorsClient(this, getGoogleAccount())
            .add(
                SensorRequest.Builder()
                    .setDataSource(dataSource) // Optional but recommended for custom data sets.
                    .setDataType(dataType) // Can't be omitted.
                    .setSamplingRate(10, TimeUnit.SECONDS)
                    .build(),
                dataPointListener!!
            )
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Listener registered!")
                    Toast.makeText(this, "Listener registered!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Listener not registered.", task.exception)
                    Toast.makeText(this, "Listener not registered.", Toast.LENGTH_SHORT).show()
                }
            }
        // [END register_data_listener]
    }


    /** Unregisters the listener with the Sensors API.  */
    private fun unregisterFitnessDataListener() {
        if (dataPointListener == null) {
            // This code only activates one listener at a time.  If there's no listener, there's
            // nothing to unregister.
            return
        }
        // [START unregister_data_listener]
        // Waiting isn't actually necessary as the unregister call will complete regardless,
        // even if called from within onStop, but a callback can still be added in order to
        // inspect the results.
        Fitness.getSensorsClient(this, getGoogleAccount())
            .remove(dataPointListener!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result!!) {
                    Log.i(TAG, "Listener was removed!")
                } else {
                    Log.i(TAG, "Listener was not removed.")
                }
            }
        // [END unregister_data_listener]
    }


}