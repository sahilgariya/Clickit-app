package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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

data class NormalizedPoint(
    val xFraction: Float,
    val yFraction: Float
)

data class ProjectedRoutePoints(
    val pickup: NormalizedPoint,
    val drop: NormalizedPoint,
    val agent: NormalizedPoint?
)

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

fun projectRoutePoints(
    pickupPoint: GeoPointPair,
    dropPoint: GeoPointPair,
    agentPoint: GeoPointPair?,
    focusOnAgent: Boolean = false
): ProjectedRoutePoints {
    val allPoints = buildList {
        add(pickupPoint)
        add(dropPoint)
        if (agentPoint != null) add(agentPoint)
    }

    val minLat = allPoints.minOf { it.lat }
    val maxLat = allPoints.maxOf { it.lat }
    val minLng = allPoints.minOf { it.lng }
    val maxLng = allPoints.maxOf { it.lng }

    val baseLatSpan = (maxLat - minLat).coerceAtLeast(0.004)
    val baseLngSpan = (maxLng - minLng).coerceAtLeast(0.004)
    val centerLat = if (focusOnAgent && agentPoint != null) agentPoint.lat else (minLat + maxLat) / 2.0
    val centerLng = if (focusOnAgent && agentPoint != null) agentPoint.lng else (minLng + maxLng) / 2.0
    val paddingFactor = if (focusOnAgent && agentPoint != null) 0.72 else 1.38
    val latSpan = baseLatSpan * paddingFactor
    val lngSpan = baseLngSpan * paddingFactor

    fun normalize(point: GeoPointPair): NormalizedPoint {
        val rawX = ((point.lng - (centerLng - lngSpan / 2.0)) / lngSpan).toFloat()
        val rawY = (1.0 - (point.lat - (centerLat - latSpan / 2.0)) / latSpan).toFloat()
        return NormalizedPoint(
            xFraction = rawX.coerceIn(0.14f, 0.86f),
            yFraction = rawY.coerceIn(0.20f, 0.80f)
        )
    }

    val pickupNorm = normalize(pickupPoint)
    var dropNorm = normalize(dropPoint)
    if (abs(pickupNorm.xFraction - dropNorm.xFraction) < 0.08f &&
        abs(pickupNorm.yFraction - dropNorm.yFraction) < 0.08f
    ) {
        dropNorm = NormalizedPoint(
            xFraction = (pickupNorm.xFraction + 0.42f).coerceIn(0.16f, 0.84f),
            yFraction = (pickupNorm.yFraction - 0.18f).coerceIn(0.22f, 0.78f)
        )
    }

    return ProjectedRoutePoints(
        pickup = pickupNorm,
        drop = dropNorm,
        agent = agentPoint?.let(::normalize)
    )
}

@Composable
fun SmartBatchLiveMap(
    booking: Booking?,
    task: DeliveryTask?,
    modifier: Modifier = Modifier,
    mapHeight: Dp = 228.dp,
    headerTitle: String = "Smart Batch Live Route"
) {
    val telemetry = remember(booking, task) {
        resolveSmartBatchRouteTelemetry(booking = booking, task = task)
    }

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
    val radarScale by pulseTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_scale"
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

            SmartBatchRouteCanvas(
                telemetry = telemetry,
                agentBearing = task?.agentBearing?.toFloat() ?: 0f,
                radarScale = radarScale,
                gpsPulseAlpha = gpsPulseAlpha,
                mapHeight = mapHeight
            )

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
                                .background(ClickitInfo)
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
private fun SmartBatchRouteCanvas(
    telemetry: SmartBatchRouteTelemetry,
    agentBearing: Float,
    radarScale: Float,
    gpsPulseAlpha: Float,
    mapHeight: Dp
) {
    var focusOnAgent by remember { mutableStateOf(false) }
    val projected = remember(telemetry, focusOnAgent) {
        projectRoutePoints(
            pickupPoint = telemetry.pickupPoint,
            dropPoint = telemetry.dropPoint,
            agentPoint = telemetry.agentPoint,
            focusOnAgent = focusOnAgent
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(mapHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF1E2440), ClickitMidnight, Color(0xFF101326))
                )
            )
            .border(1.dp, ClickitSlate, RoundedCornerShape(14.dp))
            .testTag("smart_batch_route_canvas")
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStepX = size.width / 6f
            val gridStepY = size.height / 4f
            val gridColor = ClickitSlate.copy(alpha = 0.28f)
            for (i in 1..5) {
                drawLine(
                    color = gridColor,
                    start = Offset(gridStepX * i, 0f),
                    end = Offset(gridStepX * i, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            for (j in 1..3) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, gridStepY * j),
                    end = Offset(size.width, gridStepY * j),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val pickupOffset = Offset(
                x = projected.pickup.xFraction * size.width,
                y = projected.pickup.yFraction * size.height
            )
            val dropOffset = Offset(
                x = projected.drop.xFraction * size.width,
                y = projected.drop.yFraction * size.height
            )
            val agentOffset = projected.agent?.let {
                Offset(
                    x = it.xFraction * size.width,
                    y = it.yFraction * size.height
                )
            }

            // Concentric hub rings around destination
            drawCircle(
                color = ClickitInfo.copy(alpha = 0.14f),
                radius = 28.dp.toPx(),
                center = dropOffset,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = ClickitPeach.copy(alpha = 0.14f),
                radius = 26.dp.toPx(),
                center = pickupOffset,
                style = Stroke(width = 1.dp.toPx())
            )

            // Planned dashed route
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
            if (agentOffset != null) {
                drawLine(
                    color = ClickitSuccess,
                    start = pickupOffset,
                    end = agentOffset,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = ClickitPeach.copy(alpha = 0.85f),
                    start = agentOffset,
                    end = dropOffset,
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = dashEffect
                )

                // Pulsing live GPS radar ring & heading vector
                drawCircle(
                    color = ClickitSuccess.copy(alpha = (1f - (radarScale - 0.75f) / 0.7f).coerceIn(0.08f, 0.45f)),
                    radius = 22.dp.toPx() * radarScale,
                    center = agentOffset
                )
                val rad = Math.toRadians(agentBearing.toDouble() - 90.0)
                val headingTip = Offset(
                    x = agentOffset.x + (cos(rad) * 26.dp.toPx()).toFloat(),
                    y = agentOffset.y + (sin(rad) * 26.dp.toPx()).toFloat()
                )
                drawLine(
                    color = ClickitSuccess.copy(alpha = gpsPulseAlpha),
                    start = agentOffset,
                    end = headingTip,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            } else {
                drawLine(
                    color = ClickitPeach.copy(alpha = 0.85f),
                    start = pickupOffset,
                    end = dropOffset,
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = dashEffect
                )
            }
        }

        // Pickup badge positioned dynamically at projected coordinates
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = ((projected.pickup.xFraction * widthPx) - 36f).roundToInt(),
                        y = ((projected.pickup.yFraction * heightPx) - 22f).roundToInt()
                    )
                }
        ) {
            MapPinBadge(
                icon = Icons.Filled.LocationOn,
                containerColor = ClickitPeach,
                contentColor = ClickitMidnight,
                label = "Pickup"
            )
        }

        // Drop / Shop Hub badge positioned dynamically
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = ((projected.drop.xFraction * widthPx) - 32f).roundToInt(),
                        y = ((projected.drop.yFraction * heightPx) - 22f).roundToInt()
                    )
                }
        ) {
            MapPinBadge(
                icon = Icons.Filled.Storefront,
                containerColor = ClickitInfoContainer,
                contentColor = ClickitInfo,
                label = "Drop"
            )
        }

        // Live Delivery Agent marker positioned from Firestore GPS telemetry
        if (projected.agent != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((projected.agent.xFraction * widthPx) - 24f).roundToInt(),
                            y = ((projected.agent.yFraction * heightPx) - 24f).roundToInt()
                        )
                    }
            ) {
                MapAgentMarker()
            }
        }

        // Live GPS coordinates chip (top-left)
        Surface(
            color = ClickitMidnight.copy(alpha = 0.86f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ClickitSubtleLine),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = if (telemetry.agentPoint != null) {
                    String.format(
                        Locale.getDefault(),
                        "GPS %.4f, %.4f",
                        telemetry.agentPoint.lat,
                        telemetry.agentPoint.lng
                    )
                } else {
                    "Route Telemetry • No Key Needed"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (telemetry.agentPoint != null) ClickitSuccess else ClickitMutedText,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }

        // Recenter / Focus Agent button (bottom-end)
        Surface(
            color = ClickitMidnight.copy(alpha = 0.88f),
            shape = CircleShape,
            border = BorderStroke(1.dp, ClickitPeach.copy(alpha = 0.6f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        ) {
            IconButton(
                onClick = {
                    focusOnAgent = if (telemetry.hasLiveAgentGps) !focusOnAgent else false
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("map_recenter_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = if (focusOnAgent) "Fit full route" else "Focus delivery agent",
                    tint = if (focusOnAgent) ClickitSuccess else ClickitPeach,
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
private fun MapAgentMarker() {
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
