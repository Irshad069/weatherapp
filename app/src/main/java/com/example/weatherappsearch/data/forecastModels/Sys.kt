package com.example.weatherappsearch.data.forecastModels


import com.google.gson.annotations.SerializedName

data class Sys(
    @SerializedName("pod")
    val pod: String
)