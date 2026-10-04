package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FrankfurterApi {

    @GET("currencies")
    suspend fun getCurrencies(): Map<String, String>

    @GET("latest")
    suspend fun getLatestRates(
        @Query("base") base: String,
        @Query("symbols") symbols: String? = null
    ): FrankfurterLatestResponse

    @GET("latest")
    suspend fun convertAmount(
        @Query("amount") amount: Double,
        @Query("from") from: String,
        @Query("to") to: String
    ): FrankfurterLatestResponse

    @GET("{dateRange}")
    suspend fun getTimeSeries(
        @Path("dateRange") dateRange: String,
        @Query("base") base: String,
        @Query("symbols") symbols: String
    ): FrankfurterTimeSeriesResponse
}
