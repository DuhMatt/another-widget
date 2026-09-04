package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.tommasoberlose.anotherwidget.R
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.databinding.ActivityCustomLocationBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.location.LocationRepository
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.CustomLocationViewModel
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import kotlin.coroutines.resume

class CustomLocationActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: CustomLocationViewModel
    private lateinit var binding: ActivityCustomLocationBinding

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            requestCurrentLocation()
        } else {
            showLocationError()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(CustomLocationViewModel::class.java)
        binding = ActivityCustomLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .registerDefault(R.layout.custom_location_item) { item, injector ->
                when (item) {
                    is String -> {
                        injector
                            .text(R.id.text, getString(R.string.custom_location_gps))
                            .clicked(R.id.text) {
                                requirePermission()
                            }
                    }
                    is Address -> {
                        injector
                            .text(R.id.text, item.getAddressLine(0) ?: "")
                            .clicked(R.id.item) {
                                Preferences.bulk {
                                    customLocationLat = item.latitude.toString()
                                    customLocationLon = item.longitude.toString()
                                    customLocationAdd = item.getAddressLine(0) ?: ""
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                }
                            }
                    }
                }
            }
            .attachTo(binding.listView)


        viewModel.addresses.observe(this, Observer {
            adapter.updateData(listOf("Default") + it)
        })

        setupListener()
        subscribeUi(binding, viewModel)

        binding.location.requestFocus()

    }

    private var searchJob: Job? = null

    private fun subscribeUi(binding: ActivityCustomLocationBinding, viewModel: CustomLocationViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.addresses.observe(this, Observer {
            adapter.updateData(listOf("Default") + it)
            binding.loader.visibility = View.INVISIBLE
        })

        viewModel.locationInput.observe(this, Observer { location ->
            binding.loader.visibility = View.VISIBLE
            searchJob?.cancel()
            searchJob = lifecycleScope.launch(Dispatchers.IO) {
                delay(200)
                val list = if (location == null || location == "") {
                    viewModel.addresses.value!!
                } else {
                    geocode(location)
                }
                withContext(Dispatchers.Main) {
                    viewModel.addresses.value = list
                    binding.loader.visibility = View.INVISIBLE
                }

            }
            binding.clearSearch.isVisible = location.isNotBlank()
        })
    }

    private suspend fun geocode(query: String): List<Address> {
        val geocoder = Geocoder(this@CustomLocationActivity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(query, 10, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: List<Address>) {
                        if (continuation.isActive) continuation.resume(addresses)
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resume(emptyList())
                    }
                })
            }
        }

        @Suppress("DEPRECATION")
        return try {
            withContext(Dispatchers.IO) {
                geocoder.getFromLocationName(query, 10).orEmpty()
            }
        } catch (ignored: Exception) {
            emptyList()
        }
    }

    private fun requirePermission() {
        if (hasLocationPermission()) {
            requestCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCurrentLocation() {
        binding.loader.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val location = LocationRepository(this@CustomLocationActivity).getCurrentLocation()
                if (location == null) {
                    showLocationError()
                } else {
                    Preferences.bulk {
                        customLocationLat = location.latitude.toString()
                        customLocationLon = location.longitude.toString()
                        customLocationAdd = ""
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } catch (exception: Exception) {
                Log.w("CustomLocationActivity", "Unable to obtain current location", exception)
                showLocationError()
            } finally {
                binding.loader.visibility = View.INVISIBLE
            }
        }
    }

    private fun showLocationError() {
        Toast.makeText(this, R.string.weather_location_unavailable, Toast.LENGTH_LONG).show()
    }

    private fun setupListener() {
        binding.actionBack.setOnClickListener {
            onBackPressed()
        }

        binding.clearSearch.setOnClickListener {
            viewModel.locationInput.value = ""
        }
    }
}
