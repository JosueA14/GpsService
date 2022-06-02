package cr.ac.gpsservice

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import cr.ac.gpsservice.databinding.ActivityMapsBinding
import cr.ac.gpsservice.db.LocationDatabase
import cr.ac.gpsservice.entity.Location
import cr.ac.gpsservice.service.GpsService

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var binding: ActivityMapsBinding
    private val SOLICITA_GPS = 1

    companion object{
        lateinit var mMap: GoogleMap
        lateinit var mLocationClient: FusedLocationProviderClient //proveedor de localización de Google
        lateinit var mLocationRequest: LocationRequest
        lateinit var mLocationCallback: LocationCallback
        lateinit var locationDatabase: LocationDatabase
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationDatabase = LocationDatabase.getInstance(this)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        validaPermisos()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        this.iniciaServicio()
        this.recupererPuntos()

    }

    /**
     * Obtener los puntos de la ubicacion que estan en la BD y mostrarlos en el mapa
     */
    fun recupererPuntos(){
        var locations = locationDatabase.locationDao.query()

        for (location in locations) {
            var currentLocation = LatLng(location.latitude, location.longitude)
            mMap.addMarker(MarkerOptions().position(currentLocation).title("Marker"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
        }
    }
    /**
     * Hace un filtro del broadcast GPS (com.example.gpsservice.GPS_EVENT)
     * e inicia el servicio (startService) GpsServise
     */
    fun iniciaServicio(){

        var filter = IntentFilter()
        var progreso = ProgressReceiver()
        filter.addAction(GpsService.gps)

        registerReceiver(progreso, filter)


        startService(Intent(this, GpsService::class.java))

    }
    /**
     * Valida los permisos de ACCESS_FINE_LOCATION Y ACCESS_COARSE_LOCATION
     * si no tiene permisos solicita al usuario permisos (requestPermissions)
     */
    fun validaPermisos(){
        //¿Tengo permiso de gps?
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // No TENGO PERMISOS
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                SOLICITA_GPS
            )

        } else {
            mLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback, null
            )
        }
    }

    /**
     * Validar que se le dieron los permisos al app, en caso contrario salir
     */
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SOLICITA_GPS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // el usuario dio permisos
                    mLocationClient.requestLocationUpdates(
                        mLocationRequest,
                        mLocationCallback, null
                    )

                } else {
                    // el usuario no dio permisos de GPS
                    System.exit(1)
                }
            }
        }
    }


    /**
     * Es la clase para recibir los mensajes de broadcast de gps
     */
    class ProgressReceiver : BroadcastReceiver(){
        /**
         * Se obtiene el parametro enviado por el servicio (Location)
         * Coloca  en el mapa la localizacón
         * Mueve la cámara a esa localización
         */
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action.equals(GpsService.gps)) {
                val mLocation: Location? =
                    intent!!.getSerializableExtra("gps") as Location?

                val currentLocation = mLocation?.let { LatLng(it.latitude, it.longitude) }
                mMap.addMarker(MarkerOptions().position(currentLocation).title("Marker"))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
                if (mLocation != null) {
                    locationDatabase.locationDao.insert(mLocation)
                }
            }
        }
    }
}