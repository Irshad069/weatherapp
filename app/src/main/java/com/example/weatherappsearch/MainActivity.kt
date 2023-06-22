package com.example.weatherappsearch

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherappsearch.adapter.RvAdapter
import com.example.weatherappsearch.data.forecastModels.ForecastData
import com.example.weatherappsearch.databinding.ActivityMainBinding
import com.example.weatherappsearch.databinding.BottomSheetLayoutBinding
import com.example.weatherappsearch.utils.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding
    private lateinit var sheetLayoutBinding:BottomSheetLayoutBinding
    private lateinit var dialog:BottomSheetDialog
    private lateinit var pollutionFragment:PollutionFragment
    private var city: String = "islamabad"
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sheetLayoutBinding = BottomSheetLayoutBinding.inflate(layoutInflater)
        dialog = BottomSheetDialog(this,R.style.BottomSheetTheme)
        dialog.setContentView(sheetLayoutBinding.root)
        setContentView(binding.root)

        pollutionFragment = PollutionFragment()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query!= null){
                    city = query
                }
                getCurrentWeather(city)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })

        fetchLocation()
        getCurrentWeather(city)
        binding.tvForecast.setOnClickListener {

            openDialog()
        }

        binding.tvLocation.setOnClickListener {
            fetchLocation()
        }
    }


    @SuppressLint("SuspiciousIndentation")
    private fun fetchLocation() {
        val task: Task<Location> = fusedLocationProviderClient.lastLocation
            if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
            task.addOnSuccessListener {
                val geocoder= Geocoder(this,Locale.getDefault())
                if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU){
                    geocoder.getFromLocation(it.latitude, it.longitude)
                }else{
                    val address = geocoder.getFromLocation(it.latitude,it.longitude,1) as List<Address>
                    city = address[0].locality
                }
                getCurrentWeather(city)
            }
    }

    private fun openDialog() {
        getForecast()
        sheetLayoutBinding.rvForecast.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@MainActivity,1,RecyclerView.HORIZONTAL,false)

        }
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun getForecast() {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getForecast(
                    city,
                    "metric",
                    applicationContext.getString(R.string.api_key)
                )
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "app error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
                    val data = response.body()!!

                    var forecastArray = arrayListOf<ForecastData>()
                    forecastArray = data.list as ArrayList<ForecastData>
                    val adapter = RvAdapter(forecastArray)
                    sheetLayoutBinding.rvForecast.adapter = adapter
                    sheetLayoutBinding.tvSheet.text = "Five day forecast in ${data.city.name}"
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getCurrentWeather(city: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getCurrentWeather(city,"metric",applicationContext.getString(R.string.api_key))
            }catch (e: IOException){
                Toast.makeText(applicationContext, "app error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            if (response.isSuccessful && response.body()!=null) {
                withContext(Dispatchers.Main) {
                    val data = response.body()!!
                    val iconId = data.weather[0].icon
                    val imgUrl = "https://openweathermap.org/image/wn/$iconId.png"
                    Picasso.get().load(imgUrl).into(binding.imgWeather)

                    binding.tvSunset.text =
                        dateFormatConverter(
                            data.sys.sunset.toLong()
                        )

                    binding.tvSunrise.text =
                        dateFormatConverter(
                            data.sys.sunrise.toLong()
                        )

                    binding.apply {
                        tvStatus.text = data.weather[0].description
                        tvWind.text = "${data.wind.speed.toString()}KM/H"
                        tvLocation.text = "${data.name}\n${data.sys.country}"
                        tvTemp.text = "${data.main.temp.toInt()} °C"
                        tvFeelsLike.text = "Feels like: ${data.main.feelsLike.toInt()} °C"
                        tvMinTemp.text = "Min Temp: ${data.main.tempMin.toInt()} °C"
                        tvMaxTemp.text = "Max Temp: ${data.main.tempMax.toInt()} °C"
                        tvHumidity.text = "${data.main.humidity}%"
                        tvPressure.text = "${data.main.pressure} hpa"
                        tvUpdateTime.text = "Last Update ${
                            SimpleDateFormat(
                                "hh:mm a", Locale.ENGLISH
                            ).format(data.dt * 1000)
                        }"

                        getPollution(data.coord.lat, data.coord.lon)

                    }
                }
            }
        }
    }

    private fun getPollution(lat: Double, lon: Double) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getPollution(
                    lat,
                    lon,
                    "metric",
                    applicationContext.getString(R.string.api_key)
                )
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "app error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
                    val data = response.body()!!

                    val num = data.list[0].main.aqi
                    binding.tvAirQual.text = when(num){
                        1 -> getString(R.string.good)
                        2 -> getString(R.string.fair)
                        3 -> getString(R.string.moderate)
                        4 -> getString(R.string.poor)
                        5 -> getString(R.string.very_poor)
                        else -> "no data"
                    }
                    binding.layoutPollution.setOnClickListener {
                        val bundle = Bundle()
                        bundle.putDouble("co",data.list[0].components.co)
                        bundle.putDouble("nh3",data.list[0].components.nh3)
                        bundle.putDouble("no",data.list[0].components.no)
                        bundle.putDouble("no2",data.list[0].components.no2)
                        bundle.putDouble("o3",data.list[0].components.o3)
                        bundle.putDouble("pm10",data.list[0].components.pm10)
                        bundle.putDouble("pm2_5",data.list[0].components.pm25)
                        bundle.putDouble("so2",data.list[0].components.so2)

                        pollutionFragment.arguments = bundle
                        supportFragmentManager.beginTransaction().apply {
                            replace(R.id.frameLayout,pollutionFragment)
                                .addToBackStack(null)
                                .commit()
                        }

                    }
                }
            }
        }
    }

    private fun dateFormatConverter(date: Long): String {

        return SimpleDateFormat(
            "hh:mm a",
            Locale.ENGLISH
        ).format(Date(date * 1000))
    }
}

private fun Any.getFromLocation(latitude: Double, longitude: Double) {

}

