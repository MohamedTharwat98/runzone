package com.example.runzone.model

import com.google.gson.annotations.SerializedName
data class HeartRateDataResponse(
    @SerializedName("insertedDataPoint")
    val insertedDataPoint: List<InsertedDataPoint>
)

data class InsertedDataPoint(
    @SerializedName("startTimeNanos") val startTimeNanos: String,
    @SerializedName("endTimeNanos") val endTimeNanos: String,
    @SerializedName("dataTypeName") val dataTypeName: String,
    @SerializedName("originDataSourceId") val originDataSourceId: String,
    @SerializedName("value") val values: List<Value>,
    @SerializedName("modifiedTimeMillis") val modifiedTimeMillis: String
)

data class Value(
    @SerializedName("fpVal") val heartRate: Float,
    @SerializedName("mapVal") val mapVal: List<Any>
)
