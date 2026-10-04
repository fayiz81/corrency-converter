package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FrankfurterLatestResponse(
    @Json(name = "amount") val amount: Double = 1.0,
    @Json(name = "base") val base: String,
    @Json(name = "date") val date: String,
    @Json(name = "rates") val rates: Map<String, Double> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class FrankfurterTimeSeriesResponse(
    @Json(name = "amount") val amount: Double = 1.0,
    @Json(name = "base") val base: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String,
    @Json(name = "rates") val rates: Map<String, Map<String, Double>> = emptyMap()
)
