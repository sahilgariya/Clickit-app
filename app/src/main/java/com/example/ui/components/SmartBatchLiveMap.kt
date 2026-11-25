package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.Booking
import com.example.data.DeliveryTask
import com.example.data.GeoPointPair
import com.example.data.computeDistanceKm
import com.example.data.estimateEtaMinutes
import com.example.data.isValidCoordinate
import com.example.data.maskAddressForOffer
import com.example.data.resolveZoneCoordinates
import com.example.ui.theme.ClickitInfo
import com.example.ui.theme.ClickitInfoContainer
import com.example.ui.theme.ClickitIvory
import com.example.ui.theme.ClickitMidnight
import com.example.ui.theme.ClickitMutedText
import com.example.ui.theme.ClickitPeach
import com.example.ui.theme.ClickitSlate
import com.example.ui.theme.ClickitSlateDeep
import com.example.ui.theme.ClickitSubtleLine
import com.example.ui.theme.ClickitSuccess
import com.example.ui.theme.ClickitSuccessContainer
import com.example.ui.theme.ClickitWarning
import com.example.ui.theme.ClickitWarningContainer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class SmartBatchRouteTelemetry(
    val pickupPoint: GeoPointPair,
    val dropPoint: GeoPointPair,
    val agentPoint: GeoPointPair?,
    val pickupLabel: String,
    val dropLabel: String,
    val distanceKm: Double,
    val etaMinutes: Int,
    val hasLiveAgentGps: Boolean
)

fun isMapsApiKeyConfigured(apiKey: String = BuildConfig.MAPS_API_KEY): Boolean {
    val trimmed = apiKey.trim()
    return trimmed.isNotEmpty() &&
        !trimmed.equals("YOUR_GOOGLE_MAPS_API_KEY", ignoreCase = true) &&
        !trimmed.startsWith("MY_", ignoreCase = true)
}

fun resolveSmartBatchRouteTelemetry(
    booking: Booking?,
    task: DeliveryTask?
): SmartBatchRouteTelemetry {
    val customerZone = booking?.addressSummary?.takeIf { it.isNotBlank() }?.let(::maskAddressForOffer)
        ?: task?.pickupZone?.takeIf { it.isNotBlank() }
        ?: "Indiranagar, Bengaluru"
    val shopName = task?.shopName?.takeIf { it.isNotBlank() }
        ?: booking?.shopName?.takeIf { it.isNotBlank() }
        ?: "Local Service Hub"

    val pickupLabel = task?.pickupZone?.takeIf { it.isNotBlank() }
        ?: if (task?.taskType == "return") shopName else customerZone
    val dropLabel = task?.dropZone?.takeIf { it.isNotBlank() }
        ?: if (task?.taskType == "return") customerZone else shopName

    val pickupPoint = if (isValidCoordinate(task?.pickupLat, task?.pickupLng)) {
        GeoPointPair(task!!.pickupLat!!, task.pickupLng!!)
    } else {
        resolveZoneCoordinates(pickupLabel, booking?.parcelCode ?: task?.parcelCode.orEmpty())
    }

    val dropPoint = if (isValidCoordinate(task?.dropLat, task?.dropLng)) {
        GeoPointPair(task!!.dropLat!!, task.dropLng!!)
    } else {
        resolveZoneCoordinates(dropLabel, booking?.shopId ?: task?.bookingId.orEmpty())
    }

    val agentPoint = if (isValidCoordinate(task?.agentLat, task?.agentLng)) {
        GeoPointPair(task!!.agentLat!!, task.agentLng!!)
    } else {
        null
    }

    val distanceTarget = if (task?.status == "picked_up") dropPoint else pickupPoint
    val distanceOrigin = agentPoint ?: pickupPoint
    val distanceDest = if (agentPoint != null) distanceTarget else dropPoint
    val distanceKm = computeDistanceKm(
        distanceOrigin.lat,
        distanceOrigin.lng,
        distanceDest.lat,
        distanceDest.lng
    )
    val etaMinutes = estimateEtaMinutes(distanceKm)

    return SmartBatchRouteTelemetry(
        pickupPoint = pickupPoint,
        dropPoint = dropPoint,
        agentPoint = agentPoint,
        pickupLabel = pickupLabel,
        dropLabel = dropLabel,
        distanceKm = distanceKm,
        etaMinutes = etaMinutes,
        hasLiveAgentGps = agentPoint != null
    )
}

private const val CLICKIT_DARK_MAP_STYLE = """
[
  {"elementType":"geometry","stylers":[{"color":"#161822"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#9ba1b8"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#0f1018"}]},
  {"featureType":"administrative","elementType":"geometry","stylers":[{"color":"#2c3044"}]},
  {"featureType":"poi","elementType":"geometry","stylers":[{"color":"#1f2230"}]},
  {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#1a2925"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#272b3d"}]},
  {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#1a1d29"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#3a3142"}]},
  {"featureType":"transit","elementType":"geometry","stylers":[{"color":"#222638"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0b101d"}]}
]
"""

@Composable
fun SmartBatchLiveMap(
    booking: Booking?,
    task: DeliveryTask?,
    modifier: Modifier = Modifier,
    mapHeight: Dp = 228.dp,
    headerTitle: String = "Smart Batch Live Map"
) {
    val telemetry = remember(booking, task) {
        resolveSmartBatchRouteTelemetry(booking = booking, task = task)
    }
    val hasApiKey = remember { isMapsApiKeyConfigured() }

    val pickupLatLng = LatLng(telemetry.pickupPoint.lat, telemetry.pickupPoint.lng)
    val dropLatLng = LatLng(telemetry.dropPoint.lat, telemetry.dropPoint.lng)
    val agentLatLng = telemetry.agentPoint?.let { LatLng(it.lat, it.lng) }

    val pulseTransition = rememberInfiniteTransition(label = "agent_gps_pulse")
    val gpsPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gps_pulse_alpha"
    )

    val lastUpdatedLabel = remember(task?.locationUpdatedAt) {
        task?.locationUpdatedAt?.toDate()?.let { date ->
            SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(date)
        }
    }

    Surface(
        color = ClickitSlateDeep,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (telemetry.hasLiveAgentGps) ClickitSuccess.copy(alpha = 0.55f) else ClickitPeach.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_batch_live_map")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top telemetry header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = null,
                        tint = ClickitPeach,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ClickitIvory,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val parcelTag = task?.parcelCode?.takeIf { it.isNotBlank() }
                            ?: booking?.parcelCode?.takeIf { it.isNotBlank() }
                            ?: "Active Batch"
                        val stopInfo = if (task != null) {
                            "$parcelTag • Stop ${task.batchStopIndex}/${task.batchTotalStops}"
                        } else {
                            parcelTag
                        }
                        Text(
                            text = stopInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = ClickitMutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = if (telemetry.hasLiveAgentGps) ClickitSuccessContainer else ClickitWarningContainer,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        1.dp,
                        if (telemetry.hasLiveAgentGps) ClickitSuccess.copy(alpha = 0.5f)
                        else ClickitWarning.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(if (telemetry.hasLiveAgentGps) gpsPulseAlpha else 1f)
                                .clip(CircleShape)
                                .background(if (telemetry.hasLiveAgentGps) ClickitSuccess else ClickitWarning)
                        )
                        Text(
                            text = if (telemetry.hasLiveAgentGps) "LIVE GPS" else "AWAITING GPS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (telemetry.hasLiveAgentGps) ClickitSuccess else ClickitWarning
                        )
                    }
                }
            }

            // Distance & ETA telemetry strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = ClickitMidnight.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitSubtleLine),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            "%.1f km • ~%d min ETA",
                            telemetry.distanceKm,
                            telemetry.etaMinutes
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = ClickitInfo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    color = ClickitMidnight.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitSubtleLine)
                ) {
                    Text(
                        text = if (lastUpdatedLabel != null) {
                            "Fix $lastUpdatedLabel"
                        } else if (task?.partnerId?.isNotBlank() == true) {
                            "Agent assigned"
                        } else {
                            "Batch open"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ClickitMutedText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Map Canvas or Graceful API-Key Placeholder
            if (hasApiKey) {
                SmartBatchGoogleMapCanvas(
                    pickupLatLng = pickupLatLng,
                    dropLatLng = dropLatLng,
                    agentLatLng = agentLatLng,
                    pickupLabel = telemetry.pickupLabel,
                    dropLabel = telemetry.dropLabel,
                    parcelCode = task?.parcelCode ?: booking?.parcelCode.orEmpty(),
                    agentBearing = task?.agentBearing?.toFloat() ?: 0f,
                    mapHeight = mapHeight
                )
            } else {
                Surface(
                    color = ClickitMidnight,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ClickitSlate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("maps_api_key_notice_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Configure MAPS_API_KEY in AI Studio Secrets",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ClickitPeach
                        )
                        Text(
                            text = "Live GPS coordinates sync from Firestore now. Add your Google Maps Android API key in Secrets to display interactive map tiles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClickitMutedText
                        )
                        if (agentLatLng != null) {
                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    "Agent GPS: %.5f, %.5f",
                                    agentLatLng.latitude,
                                    agentLatLng.longitude
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ClickitSuccess
                            )
                        }
                    }
                }
            }

            // Route stops footer
            Surface(
                color = ClickitMidnight.copy(alpha = 0.72f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ClickitSubtleLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ClickitPeach)
                        )
                        Text(
                            text = telemetry.pickupLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = ClickitIvory,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = " → ",
                        style = MaterialTheme.typography.labelSmall,
                        color = ClickitMutedText
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ClickitSuccess)
                        )
                        Text(
                            text = telemetry.dropLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = ClickitIvory,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartBatchGoogleMapCanvas(
    pickupLatLng: LatLng,
    dropLatLng: LatLng,
    agentLatLng: LatLng?,
    pickupLabel: String,
    dropLabel: String,
    parcelCode: String,
    agentBearing: Float,
    mapHeight: Dp
) {
    val scope = rememberCoroutineScope()
    val initialCenter = agentLatLng ?: pickupLatLng
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, 13.5f)
    }

    val mapStyle = remember { MapStyleOptions(CLICKIT_DARK_MAP_STYLE) }
    val mapProperties = remember(mapStyle) {
        MapProperties(
            mapStyleOptions = mapStyle,
            isTrafficEnabled = false
        )
    }
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false
        )
    }

    val pickupMarkerState = rememberMarkerState(key = "pickup", position = pickupLatLng)
    val dropMarkerState = rememberMarkerState(key = "drop", position = dropLatLng)
    val agentMarkerState = rememberMarkerState(key = "agent", position = agentLatLng ?: pickupLatLng)

    LaunchedEffect(pickupLatLng) {
        pickupMarkerState.position = pickupLatLng
    }
    LaunchedEffect(dropLatLng) {
        dropMarkerState.position = dropLatLng
    }
    LaunchedEffect(agentLatLng) {
        if (agentLatLng != null) {
            agentMarkerState.position = agentLatLng
        }
    }

    suspend fun fitRouteBounds() {
        val builder = LatLngBounds.Builder()
            .include(pickupLatLng)
            .include(dropLatLng)
        if (agentLatLng != null) builder.include(agentLatLng)
        val bounds = builder.build()
        runCatching {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 96),
                durationMs = 650
            )
        }
    }

    LaunchedEffect(pickupLatLng, dropLatLng, agentLatLng) {
        fitRouteBounds()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(mapHeight)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, ClickitSlate, RoundedCornerShape(14.dp))
            .testTag("smart_batch_google_map_canvas")
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {
            // Planned route polyline
            val routePoints = remember(pickupLatLng, dropLatLng, agentLatLng) {
                if (agentLatLng != null) {
                    listOf(pickupLatLng, agentLatLng, dropLatLng)
                } else {
                    listOf(pickupLatLng, dropLatLng)
                }
            }
            Polyline(
                points = routePoints,
                color = Color(ClickitPeach.toArgb()),
                width = 9f,
                pattern = listOf(Dash(24f), Gap(12f))
            )

            if (agentLatLng != null) {
                Polyline(
                    points = listOf(pickupLatLng, agentLatLng),
                    color = Color(ClickitSuccess.toArgb()),
                    width = 10f
                )
            }

            // Pickup marker
            MarkerComposable(
                keys = arrayOf(pickupLatLng, pickupLabel),
                state = pickupMarkerState,
                title = "Pickup • $pickupLabel"
            ) {
                MapPinBadge(
                    icon = Icons.Filled.LocationOn,
                    containerColor = ClickitPeach,
                    contentColor = ClickitMidnight,
                    label = "Pickup"
                )
            }

            // Drop / Shop marker
            MarkerComposable(
                keys = arrayOf(dropLatLng, dropLabel),
                state = dropMarkerState,
                title = "Drop • $dropLabel"
            ) {
                MapPinBadge(
                    icon = Icons.Filled.Storefront,
                    containerColor = ClickitInfoContainer,
                    contentColor = ClickitInfo,
                    label = "Drop"
                )
            }

            // Real-time Delivery Agent marker
            if (agentLatLng != null) {
                MarkerComposable(
                    keys = arrayOf(agentLatLng, agentBearing),
                    state = agentMarkerState,
                    title = "Delivery Partner • $parcelCode",
                    snippet = "Live Smart Batch Agent"
                ) {
                    MapAgentMarker(bearingDegrees = agentBearing)
                }
            }
        }

        // Recenter camera button
        Surface(
            color = ClickitMidnight.copy(alpha = 0.88f),
            shape = CircleShape,
            border = BorderStroke(1.dp, ClickitPeach.copy(alpha = 0.6f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        ) {
            IconButton(
                onClick = { scope.launch { fitRouteBounds() } },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("map_recenter_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Recenter route map",
                    tint = ClickitPeach,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MapPinBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    label: String
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.5.dp, ClickitMidnight),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

@Composable
private fun MapAgentMarker(bearingDegrees: Float) {
    Surface(
        color = ClickitSuccess,
        shape = CircleShape,
        border = BorderStroke(2.dp, ClickitMidnight),
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier.padding(7.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = "Live Delivery Agent",
                tint = ClickitMidnight,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
