package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationClient(private val context: Context) {

  private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(context) }

  fun hasLocationPermission(): Boolean {
    val coarse =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val fine =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return coarse || fine
  }

  @SuppressLint("MissingPermission")
  suspend fun getCurrentCoordinates(): Pair<Double, Double>? {
    if (!hasLocationPermission()) return null

    // Try FusedLocationProviderClient with timeout, falling back to last known location
    val fusedResult = withTimeoutOrNull(4500L) {
      suspendCancellableCoroutine<Location?> { cont ->
        val cts = CancellationTokenSource()
        cont.invokeOnCancellation { cts.cancel() }
        fusedClient
          .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
          .addOnSuccessListener { location ->
            if (location != null) {
              if (cont.isActive) cont.resume(location)
            } else {
              fusedClient.lastLocation
                .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
          }
          .addOnFailureListener {
            fusedClient.lastLocation
              .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
              .addOnFailureListener { if (cont.isActive) cont.resume(null) }
          }
      }
    }

    if (fusedResult != null) {
      return fusedResult.latitude to fusedResult.longitude
    }

    // Secondary fallback via platform LocationManager for emulators without GMS location
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    val best =
      providers
        .mapNotNull { provider ->
          runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull { it.time }

    return best?.let { it.latitude to it.longitude }
  }
}
