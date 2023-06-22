package com.example.weatherappsearch.data.pollutionModels


import com.google.gson.annotations.SerializedName

data class Pollution(
    @SerializedName("components")
    val components: Components,
    @SerializedName("dt")
    val dt: Int,
    @SerializedName("main")
    val main: Main
)