package com.example.runzone

import com.github.mikephil.charting.data.BarEntry
import java.util.Date

class Session(
    var duration: String,
    var date: String,
    var distance: String,
    var maxHR: Float = 0F,
    var age: Int = 0,
    var missionType: String,
    var zone1: Float,
    var zone2: Float,
    var zone3: Float,
    var zone4: Float
) {
    // You can add additional methods or code here if needed
}
