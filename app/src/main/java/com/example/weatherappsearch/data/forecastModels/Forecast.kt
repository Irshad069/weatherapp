package com.example.weatherappsearch.data.forecastModels


import com.google.gson.annotations.SerializedName

data class Forecast(
    @SerializedName("city")
    val city: City,
    @SerializedName("cnt")
    val cnt: Int,
    @SerializedName("cod")
    val cod: String,
    @SerializedName("list")
    val list: List<ForecastData>,
    @SerializedName("message")
    val message: Int
)