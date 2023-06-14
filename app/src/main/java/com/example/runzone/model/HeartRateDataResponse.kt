package com.example.runzone.model

import com.google.gson.annotations.SerializedName

data class HeartRateDataResponse(
    @SerializedName("bucket")
    val buckets: List<Bucket>
)

data class Bucket(
    @SerializedName("startTimeMillis") val startTimeMillis: Long,
    @SerializedName("endTimeMillis") val endTimeMillis: Long,
    @SerializedName("dataset") val datasets: List<Dataset>
)

data class Dataset(
    @SerializedName("point") val dataPoints: List<DataPoint>
)

data class DataPoint(
    @SerializedName("startTimeNanos") val startTimeNanos: Long,
    @SerializedName("endTimeNanos") val endTimeNanos: Long,
    @SerializedName("value") val value: Value
)

data class Value(
    @SerializedName("fpVal") val heartRate: Float
)
