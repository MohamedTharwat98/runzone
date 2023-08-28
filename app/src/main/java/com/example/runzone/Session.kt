package com.example.runzone

import com.github.mikephil.charting.data.BarEntry
import java.util.Date

class Session(
    var duration: String,
    var date: String,
    var avgHR: Float = 0F,
    var maxHR: Float = 0F,
    var age: Int = 0,
    var missionType: String,
    var zone0: Float,
    var zone1: Float,
    var zone2: Float,
    var zone3: Float,
    var zone4: Float
) {
    // You can add additional methods or code here if needed
}
