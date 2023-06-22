package com.example.weatherappsearch.data.pollutionModels


import com.google.gson.annotations.SerializedName

data class Main(
    @SerializedName("aqi")
    val aqi: Int
)