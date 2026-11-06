package com.primestrider.kotlinstarter.feature.example.sdk.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.component.SegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.component.SwitchRow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.permission.PermissionGate
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val UPDATE_INTERVAL_MILLIS = 5_000L
private const val MIN_DISTANCE_METERS = 5f
private const val FIX_DURATION_MILLIS = 10_000L
private const val FIX_TIMEOUT_MILLIS = 15_000L

private enum class Accuracy(
    val label: Int,
    val priority: Int,
) {
    LOW(R.string.sdk_location_accuracy_low, Priority.PRIORITY_LOW_POWER),
    BALANCED(R.string.sdk_location_accuracy_balanced, Priority.PRIORITY_BALANCED_POWER_ACCURACY),
    HIGH(R.string.sdk_location_accuracy_high, Priority.PRIORITY_HIGH_ACCURACY),
}

private data class Fix(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val time: Long,
)

private data class Place(
    val street: String?,
    val district: String?,
    val city: String?,
    val region: String?,
    val postalCode: String?,
    val country: String?,
)

private val FixSaver =
    Saver<Fix?, List<Double>>(
        save = { it?.let { fix -> listOf(fix.latitude, fix.longitude, fix.accuracy.toDouble(), fix.time.toDouble()) }.orEmpty() },
        restore = { if (it.isEmpty()) null else Fix(it[0], it[1], it[2].toFloat(), it[3].toLong()) },
    )

private val PlaceSaver =
    Saver<Place?, List<String>>(
        save = {
            it
                ?.let { p ->
                    listOf(p.street, p.district, p.city, p.region, p.postalCode, p.country).map { v -> v.orEmpty() }
                }.orEmpty()
        },
        restore = { if (it.isEmpty()) null else Place(it[0], it[1], it[2], it[3], it[4], it[5]) },
    )

/** Current position (one-off or live) with the fused location provider, and the address of the last one-off fix. */
@Composable
fun LocationScreen(onBack: () -> Unit) {
    ExamplePageScaffold(
        title = R.string.sdk_location_title,
        subtitle = R.string.sdk_location_subtitle,
        onBack = onBack,
    ) {
        PermissionGate(
            permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            reason = stringResource(R.string.sdk_location_reason),
            // Approximate access (coarse only) is enough; High accuracy then runs at balanced power.
            requireAll = false,
        ) {
            LocationContent()
        }
    }
}

// Only composed inside the PermissionGate, so the location permissions are granted here.
@SuppressLint("MissingPermission")
@Composable
private fun LocationContent() {
    val context = LocalContext.current
    val locale = currentLocale()
    val scope = rememberCoroutineScope()
    val client = remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    var accuracy by rememberSaveable { mutableStateOf(Accuracy.BALANCED) }
    var watch by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var fix by rememberSaveable(stateSaver = FixSaver) { mutableStateOf<Fix?>(null) }
    var place by rememberSaveable(stateSaver = PlaceSaver) { mutableStateOf<Place?>(null) }
    val geocoderPresent = remember { Geocoder.isPresent() }
    val accuracyLabels = Accuracy.entries.associateWith { stringResource(it.label) }
    val failure = stringResource(R.string.sdk_location_error)

    // Live updates run only while the switch is on and the screen is in the foreground (STARTED); a new
    // accuracy restarts them, and going to the background or leaving the screen stops them.
    ForegroundWatchEffect(watch, accuracy) {
        val request =
            LocationRequest
                .Builder(
                    effectivePriority(accuracy.priority, fineGranted(context)),
                    UPDATE_INTERVAL_MILLIS,
                ).setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
                .build()
        val callback =
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { fix = it.toFix() }
                }
            }
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            error = failure
        }
        return@ForegroundWatchEffect { client.removeLocationUpdates(callback) }
    }

    SdkSection(R.string.sdk_location_position_title, R.string.sdk_location_position_description) {
        SegmentedChoice(
            options = Accuracy.entries,
            selected = accuracy,
            onSelect = { accuracy = it },
            label = { accuracyLabels.getValue(it) },
        )
        SwitchRow(label = stringResource(R.string.sdk_location_watch), checked = watch, onCheckedChange = { watch = it })
        AppButton(
            stringResource(if (loading) R.string.sdk_location_locating else R.string.sdk_location_get),
            onClick = {
                loading = true
                error = null
                scope.launch {
                    try {
                        val location =
                            client.currentLocation(effectivePriority(accuracy.priority, fineGranted(context))) ?: throw IOException(failure)
                        fix = location.toFix()
                        place = if (geocoderPresent) geocode(context, locale, location) else null
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = e.message?.takeIf { it.isNotBlank() } ?: failure
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
        )
        error?.let { Banner(title = it, variant = BannerVariant.ERROR, onDismiss = { error = null }) }
        val timeFormat = remember(locale) { DateFormat.getTimeInstance(DateFormat.MEDIUM, locale) }
        InfoCard(
            listOf(
                stringResource(R.string.sdk_location_latitude) to fix?.let { formatCoordinate(it.latitude) },
                stringResource(R.string.sdk_location_longitude) to fix?.let { formatCoordinate(it.longitude) },
                stringResource(R.string.sdk_location_accuracy) to fix?.let { formatAccuracy(it.accuracy) },
                stringResource(R.string.sdk_location_updated) to fix?.let { timeFormat.format(Date(it.time)) },
            ),
        )
    }
    SdkSection(R.string.sdk_location_address_title, R.string.sdk_location_address_description) {
        InfoCard(
            listOf(
                stringResource(R.string.sdk_location_street) to place?.street,
                stringResource(R.string.sdk_location_district) to place?.district,
                stringResource(R.string.sdk_location_city) to place?.city,
                stringResource(R.string.sdk_location_region) to place?.region,
                stringResource(R.string.sdk_location_postal_code) to place?.postalCode,
                stringResource(R.string.sdk_location_country) to place?.country,
            ),
        )
        if (!geocoderPresent) {
            Text(
                stringResource(R.string.sdk_location_no_geocoder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Location.toFix() = Fix(latitude, longitude, accuracy, time.takeIf { it > 0 } ?: System.currentTimeMillis())

@SuppressLint("MissingPermission")
private fun fineGranted(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** One fix, or null when none arrives: the request expires after [FIX_DURATION_MILLIS], and the timeout cancels the token too. */
@SuppressLint("MissingPermission")
private suspend fun com.google.android.gms.location.FusedLocationProviderClient.currentLocation(priority: Int): Location? {
    val source = CancellationTokenSource()
    val request =
        CurrentLocationRequest
            .Builder()
            .setPriority(priority)
            .setDurationMillis(FIX_DURATION_MILLIS)
            .build()
    return withTimeoutOrNull(FIX_TIMEOUT_MILLIS) { getCurrentLocation(request, source.token).await(source) }
        .also { source.cancel() }
}

private suspend fun <T> Task<T>.await(source: CancellationTokenSource): T? =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
        continuation.invokeOnCancellation { source.cancel() }
    }

/** The first address for [location]: the listener form on Android 13+, otherwise the blocking call on the IO dispatcher. */
private suspend fun geocode(
    context: Context,
    locale: Locale,
    location: Location,
): Place? {
    val geocoder = Geocoder(context, locale)
    val address =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.firstAddress(location)
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
                } catch (e: IOException) {
                    null
                }
            }
        }
    return address?.let {
        Place(it.thoroughfare, it.subLocality, it.locality, it.adminArea, it.postalCode, it.countryName)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private suspend fun Geocoder.firstAddress(location: Location): Address? =
    suspendCancellableCoroutine { continuation ->
        getFromLocation(
            location.latitude,
            location.longitude,
            1,
            object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    continuation.resume(addresses.firstOrNull())
                }

                override fun onError(errorMessage: String?) {
                    continuation.resume(null)
                }
            },
        )
    }

@ThemePreviews
@Composable
private fun LocationScreenPreview() {
    PreviewTheme {
        LocationScreen(onBack = {})
    }
}
