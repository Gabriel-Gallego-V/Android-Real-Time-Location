package com.rtlocation.vlocator

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var outputTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var cepTextView: TextView
    private var serviceBound = false
    private var isTracking = false
    private var service: LocationService? = null


    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Permissão concedida
            Log.d(TAG, "Location Permission Granted")
            service?.subscribeToLocationUpdates()
        } else {
            // Permissão negada
            Snackbar.make(findViewById(R.id.activity_main), "Permissão de localização negada", Snackbar.LENGTH_SHORT).show()
        }
    }

    // Chame este método para solicitar permissão
    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double, context: Context): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: "Cidade não encontrada"
                val neighborhood = address.subLocality ?: "Bairro não encontrado"
                "$neighborhood, $city"
            } else {
                "Endereço não encontrado"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Erro ao buscar endereço"
        }
    }
    fun getPostalCodeFromNominatim(latitude: Double, longitude: Double): String? {
        val url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$latitude&lon=$longitude"
        val result = URL(url).readText()

        val jsonObject = JSONObject(result)
        val address = jsonObject.getJSONObject("address")
        return address.optString("postcode", "CEP não encontrado")
    }
    fun updatePostalCode(latitude: Double, longitude: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val cep = getPostalCodeFromNominatim(latitude, longitude)
            withContext(Dispatchers.Main) {
                cepTextView.text = cep
            }
        }
    }


    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Broadcast Received")
            val location = intent?.getParcelableExtra<Location>(LocationService.EXTRA_LOCATION)

            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude

                Log.d(TAG, "Attempting to update TextView with Location: Lat: $latitude, Long: $longitude")

                runOnUiThread {
                    // Atualiza a coordenada de localização
                    outputTextView.visibility = View.VISIBLE
                    outputTextView.text = getString(R.string.location_format, latitude.toString(), longitude.toString())

                    // Obtém o endereço (cidade/bairro) a partir das coordenadas
                    val address = getAddressFromLocation(latitude, longitude, applicationContext)
                    locationTextView.text = address

                    
                    val cep = updatePostalCode(latitude, longitude)
                    cepTextView.text = cep.toString()

                    Log.d(TAG, "Update complete")
                }
            } else {
                Log.d(TAG, "No location found in the intent")
            }
        }
    }


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "Service Connected")
            val localBinder = binder as LocationService.LocalBinder
            service = localBinder.service
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service Disconnected")
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestLocationPermission()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputTextView = findViewById(R.id.GeoCoord)
        locationTextView = findViewById(R.id.AproxLocale)
        cepTextView = findViewById(R.id.CEPViewer)
        Log.d(TAG, "TextViews Initialized")

        val button = findViewById<Button>(R.id.button_start_stop)
        button.setOnClickListener {
            // Alterna a captura de localização
            if (serviceBound) {
                if (isTracking) {
                    service?.unsubscribeToLocationUpdates()
                    isTracking = false
                    button.text = "Iniciar Captura de Localização"
                } else {
                    service?.subscribeToLocationUpdates()
                    isTracking = true
                    button.text = "Parar Captura de Localização"
                }
            } else {
                Snackbar.make(findViewById(R.id.activity_main), "Serviço não está disponível", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Registra o receiver
        val filter = IntentFilter(LocationService.LOCATION_BROADCAST)
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, filter)
        Log.d(TAG, "BroadcastReceiver Registered")

        // Vincula o serviço
        val intent = Intent(this, LocationService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
        Log.d(TAG, "BroadcastReceiver Unregistered")
    }
    companion object {
        private const val TAG = "MainActivity"
    }
}
