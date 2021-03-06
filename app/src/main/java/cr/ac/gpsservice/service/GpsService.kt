package cr.ac.gpsservice.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import cr.ac.gpsservice.MapsActivity
import cr.ac.gpsservice.db.LocationDatabase
import cr.ac.gpsservice.entity.Location

class GpsService : IntentService("GpsService") {

    lateinit var  locationCallback: LocationCallback
    lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationDatabase: LocationDatabase

    companion object {
        val gps = "com.example.gpsservice.GPS_EVENT"
    }
    override fun onHandleIntent(intent: Intent?) {
        locationDatabase = LocationDatabase.getInstance(this)
        getLocation()
    }



            /**
             * Inicializa los atributos locationCallback y fusedLocationClient
             * coloca un intervalo de actualizacion de 10000 u una prioridad
             * de PRIORITY_HIGH_ACCURACY
             * recibe la ubicacion de GPS mediante un onLocationResult y envia un
             * broadcast con una instacia de Location y la accion GPS(com.example.gpsservice.GPS_EVENT)
             * ademas guarda la localizacion en la BD
             */
    @SuppressLint("MissingPermission")
    fun getLocation (){
        MapsActivity.mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.equals(null)) {
                    return
                }// dibujar en el mapa los puntos
                for (location in locationResult.locations) {

                    val mLocation = Location(
                        null,
                        location.latitude,
                        location.longitude)

                    val bcIntent = Intent()
                    bcIntent.action = GpsService.gps
                    bcIntent.putExtra("gps", mLocation)
                    sendBroadcast(bcIntent)

                    MapsActivity.locationDatabase.locationDao.insert(Location(
                        null,
                        mLocation.latitude,
                        mLocation.longitude))

                }

            }
        }

        MapsActivity.mLocationClient = LocationServices.getFusedLocationProviderClient(this)
        MapsActivity.mLocationRequest = LocationRequest()
        MapsActivity.mLocationRequest.interval = 10000
        MapsActivity.mLocationRequest.fastestInterval = 5000
        MapsActivity.mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        LocationSettingsRequest.Builder().addLocationRequest(MapsActivity.mLocationRequest)

        MapsActivity.mLocationClient.requestLocationUpdates(
            MapsActivity.mLocationRequest,
            MapsActivity.mLocationCallback,
            null)
        Looper.loop()

    }

}