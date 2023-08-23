package com.example.runzone

import com.github.mikephil.charting.data.BarEntry
import java.util.Date

class Session(
    var duration: String,
    var date: Date,
    var avgHR: Float = 0F,
    var maxHR: Float = 0F,
    var age: Int = 0,
    var missionType: String,
    var chartEntries: ArrayList<BarEntry>
) {
    // You can add additional methods or code here if needed
}
