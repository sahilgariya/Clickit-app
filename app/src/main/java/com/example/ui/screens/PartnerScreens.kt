package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.DeliveryTask
import com.example.data.FREE_DELIVERY_WEIGHT_THRESHOLD_KG
import com.example.data.LiveLocationTracker
import com.example.data.formatInr
import com.example.ui.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.NeonIconBadge
import com.example.ui.components.SmartBatchLiveMap
import com.example.ui.theme.ClickitBorder
import com.example.ui.theme.ClickitBrandTeal
import com.example.ui.theme.ClickitBrandTealTint
import com.example.ui.theme.ClickitCrimson
import com.example.ui.theme.ClickitLightSurface
import com.example.ui.theme.ClickitSuccess
import com.example.ui.theme.ClickitSuccessContainer
import com.example.ui.theme.ClickitTextPrimary
import com.example.ui.theme.ClickitTextSecondary
import com.example.ui.theme.ClickitWarning
import com.example.ui.theme.ClickitWarningContainer

@Composable
fun PartnerOffersScreen(
    online: Boolean,
    offeredTasksState: UiState<List<DeliveryTask>>,
    partnerTasksState: UiState<List<DeliveryTask>>,
    isBusy: Boolean,
    onToggleOnline: (Boolean) -> Unit,
    onAcceptTask: (DeliveryTask) -> Unit,
    onRetry: () -> Unit = {}
) {
    val activeTasks = (partnerTasksState as? UiState.Success)?.data?.filter {
        it.status !in listOf("handed_over", "delivered", "exception")
    } ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (online) ClickitSuccess.copy(alpha = 0.5f) else ClickitBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        NeonIconBadge(
                            icon = Icons.AutoMirrored.Filled.DirectionsBike,
                            tint = if (online) ClickitSuccess else ClickitBrandTeal,
                            size = 40.dp,
                            iconSize = 20.dp,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (online) "Online" else "Offline",
                                style = MaterialTheme.typography.titleSmall,
                                color = ClickitTextPrimary
                            )
                            Text(
                                text = "2-Wheeler • ${activeTasks.size} stops",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = online,
                        onCheckedChange = onToggleOnline,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ClickitBrandTeal),
                        modifier = Modifier.testTag("partner_online_switch")
                    )
                }
            }
        }

        item {
            Text("Tasks", style = MaterialTheme.typography.titleLarge, color = ClickitTextPrimary)
        }

        if (!online) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                    title = "Offline",
                    subtitle = "Go online for tasks."
                )
            }
        } else {
            when (offeredTasksState) {
                is UiState.Loading -> item { LoadingStateView("Loading...") }
                is UiState.Error -> item { ErrorRetryCard(offeredTasksState.message, onRetry = onRetry) }
                is UiState.Success -> {
                    if (offeredTasksState.data.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.LocalShipping,
                                title = "No tasks",
                                subtitle = "Tasks appear here."
                            )
                        }
                    } else {
                        items(offeredTasksState.data, key = { it.id }) { task ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = BorderStroke(1.dp, ClickitBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("task_offer_${task.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Surface(color = ClickitBrandTealTint, shape = RoundedCornerShape(6.dp)) {
                                                Text(
                                                    text = "${task.taskType.uppercase()} • ${task.batchId}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ClickitBrandTeal,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            if (task.estimatedWeightKg > FREE_DELIVERY_WEIGHT_THRESHOLD_KG) {
                                                Surface(color = ClickitSuccessContainer, shape = RoundedCornerShape(6.dp)) {
                                                    Text(
                                                        text = ">5 KG BULK",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ClickitSuccess,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Payout ${formatInr(task.payoutMinor)}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = ClickitBrandTeal
                                        )
                                    }

                                    Text("${task.pickupZone} -> ${task.dropZone}", style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                                    Text(
                                        text = "Parcel ${task.parcelCode} • ${task.itemCount} items • ~${task.estimatedWeightKg} kg • ${task.timeWindowLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ClickitTextSecondary
                                    )

                                    Button(
                                        onClick = { onAcceptTask(task) },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("accept_task_${task.id}")
                                    ) {
                                        Text("Accept")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PartnerRouteScreen(
    partnerTasksState: UiState<List<DeliveryTask>>,
    partnerBookingsState: UiState<List<Booking>>,
    isBusy: Boolean,
    onAdvanceTask: (DeliveryTask, String, String?, String) -> Unit,
    onReportException: (DeliveryTask, String) -> Unit,
    onUpdateLiveLocation: (String, Double, Double, Double?) -> Unit = { _, _, _, _ -> },
    onRetry: () -> Unit = {}
) {
    val context = LocalContext.current
    val tasks = (partnerTasksState as? UiState.Success)?.data ?: emptyList()
    val bookings = (partnerBookingsState as? UiState.Success)?.data ?: emptyList()
    val active = tasks.filter { it.status !in listOf("handed_over", "delivered", "exception") }

    var hasLocationPermission by remember {
        mutableStateOf(LiveLocationTracker.hasLocationPermission(context))
    }
    var shareLiveGps by rememberSaveable { mutableStateOf(true) }
    var permissionDeniedNote by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        permissionDeniedNote = !granted
        if (granted) shareLiveGps = true
    }

    LaunchedEffect(shareLiveGps, hasLocationPermission, active.map { it.id }) {
        if (shareLiveGps && hasLocationPermission && active.isNotEmpty()) {
            LiveLocationTracker.observeDeviceLocation(context).collect { fix ->
                active.forEach { task ->
                    onUpdateLiveLocation(task.id, fix.lat, fix.lng, fix.bearing)
                }
            }
        }
    }

    var otpDialogTask by remember { mutableStateOf<Pair<DeliveryTask, Booking?>?>(null) }
    var exceptionTarget by remember { mutableStateOf<DeliveryTask?>(null) }

    if (otpDialogTask != null) {
        val (task, booking) = otpDialogTask!!
        val expectedOtp = if (task.taskType == "pickup") booking?.handoverOtp ?: "" else booking?.deliveryOtp ?: ""
        var enteredOtp by remember(task.id) { mutableStateOf("") }
        var otpError by remember(task.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!isBusy) otpDialogTask = null },
            containerColor = ClickitLightSurface,
            title = {
                Text(
                    text = if (task.taskType == "pickup") "Pickup OTP" else "Delivery OTP",
                    color = ClickitTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${task.itemCount} items • Parcel ${task.parcelCode}. Ask customer for OTP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClickitTextSecondary
                    )
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = {
                            enteredOtp = it.filter { ch -> ch.isDigit() }.take(8)
                            otpError = null
                        },
                        label = { Text("4-digit OTP *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("partner_otp_input")
                    )
                    if (otpError != null) {
                        Text(otpError!!, style = MaterialTheme.typography.bodySmall, color = ClickitCrimson)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanOtp = enteredOtp.trim()
                        if (cleanOtp.length !in 4..8 || !cleanOtp.all { it.isDigit() }) {
                            otpError = "Enter 4-digit OTP from customer."
                        } else if (expectedOtp.isNotBlank() && cleanOtp != expectedOtp) {
                            otpError = "Invalid OTP. Verify with customer."
                        } else {
                            if (task.taskType == "pickup") {
                                onAdvanceTask(
                                    task,
                                    "picked_up",
                                    BookingStatus.PICKED_UP.key,
                                    "Picked up parcel ${task.parcelCode} (${task.itemCount} pcs)"
                                )
                            } else {
                                onAdvanceTask(
                                    task,
                                    "delivered",
                                    BookingStatus.DELIVERED.key,
                                    "Delivered parcel ${task.parcelCode} with OTP proof"
                                )
                            }
                            otpDialogTask = null
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White)
                ) { Text("Verify") }
            },
            dismissButton = {
                TextButton(onClick = { otpDialogTask = null }) { Text("Cancel", color = ClickitTextSecondary) }
            }
        )
    }

    if (exceptionTarget != null) {
        var reason by remember(exceptionTarget!!.id) { mutableStateOf("Customer unavailable") }
        var reasonError by remember(exceptionTarget!!.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!isBusy) exceptionTarget = null },
            containerColor = ClickitLightSurface,
            title = { Text("Report Issue", color = ClickitTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Other stops remain unaffected.", style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                    OutlinedTextField(
                        value = reason,
                        onValueChange = {
                            reason = it
                            reasonError = null
                        },
                        label = { Text("Reason *") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (reasonError != null) {
                        Text(reasonError!!, style = MaterialTheme.typography.bodySmall, color = ClickitCrimson)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reason.trim().length !in 3..160) {
                            reasonError = "Enter a reason (3–160 chars)."
                        } else {
                            onReportException(exceptionTarget!!, reason.trim())
                            exceptionTarget = null
                        }
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitCrimson, contentColor = Color.White)
                ) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { exceptionTarget = null }, enabled = !isBusy) { Text("Cancel", color = ClickitTextSecondary) }
            }
        )
    }

    when (partnerTasksState) {
        is UiState.Loading -> LoadingStateView("Loading...")
        is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorRetryCard(partnerTasksState.message, onRetry = onRetry) }
        is UiState.Success -> {
            if (active.isEmpty()) {
                Box(Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.LocalShipping,
                        title = "No active stops",
                        subtitle = "Accept a task in Offers."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "partner_gps_card") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (shareLiveGps && hasLocationPermission) ClickitSuccess.copy(alpha = 0.5f) else ClickitBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.GpsFixed,
                                            contentDescription = null,
                                            tint = if (shareLiveGps && hasLocationPermission) ClickitSuccess else ClickitBrandTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (hasLocationPermission && shareLiveGps) {
                                                    "GPS Active"
                                                } else if (!hasLocationPermission) {
                                                    "Enable GPS"
                                                } else {
                                                    "GPS Paused"
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                color = ClickitTextPrimary
                                            )
                                            Text(
                                                text = "Live customer tracking",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ClickitTextSecondary
                                            )
                                        }
                                    }
                                    if (hasLocationPermission) {
                                        Switch(
                                            checked = shareLiveGps,
                                            onCheckedChange = { shareLiveGps = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = ClickitSuccess
                                            ),
                                            modifier = Modifier.testTag("partner_gps_switch")
                                        )
                                    }
                                }
                                if (!hasLocationPermission) {
                                    Button(
                                        onClick = {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ClickitBrandTeal,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("grant_partner_gps_button")
                                    ) {
                                        Text("Enable GPS")
                                    }
                                    if (permissionDeniedNote) {
                                        Text(
                                            text = "Enable location in Settings.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ClickitCrimson
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val primaryTask = active.first()
                    val primaryBooking = bookings.firstOrNull { it.id == primaryTask.bookingId }
                    item(key = "partner_route_map") {
                        SmartBatchLiveMap(
                            booking = primaryBooking,
                            task = primaryTask,
                            mapHeight = 210.dp,
                            headerTitle = "Route Map"
                        )
                    }

                    items(active, key = { it.id }) { task ->
                        val linkedBooking = bookings.firstOrNull { it.id == task.bookingId }
                        val isHeavyBulk = (linkedBooking?.weightKg ?: task.estimatedWeightKg) > FREE_DELIVERY_WEIGHT_THRESHOLD_KG
                        val isUrgent = linkedBooking?.itemsSummary?.contains("URGENT +25%", ignoreCase = true) == true
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, ClickitBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Stop ${task.batchStopIndex} • ${task.parcelCode}", style = MaterialTheme.typography.titleSmall, color = ClickitBrandTeal)
                                    Text(task.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = ClickitBrandTeal)
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(color = ClickitBrandTealTint, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = "Scan QR & OTP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ClickitBrandTeal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (isUrgent) {
                                        Surface(color = ClickitWarningContainer, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "URGENT +25%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ClickitWarning,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    if (isHeavyBulk) {
                                        Surface(color = ClickitSuccessContainer, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "FREE >5 KG BULK",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ClickitSuccess,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                                Text("${task.taskType.replaceFirstChar { it.uppercase() }}: ${task.pickupZone} -> ${task.dropZone}", style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                                if (linkedBooking != null) {
                                    Text(linkedBooking.addressSummary, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                                }
                                Text("${task.itemCount} items • ~${task.estimatedWeightKg} kg • ${task.timeWindowLabel}", style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)

                                when (task.status) {
                                    "accepted" -> {
                                        Button(
                                            onClick = {
                                                onAdvanceTask(task, "arrived", null, "Partner arrived at stop (${task.parcelCode})")
                                            },
                                            enabled = !isBusy,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White),
                                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("partner_arrived_${task.id}")
                                        ) { Text("Arrived") }
                                    }
                                    "arrived" -> {
                                        Button(
                                            onClick = {
                                                if (task.taskType == "pickup") {
                                                    otpDialogTask = task to linkedBooking
                                                } else {
                                                    onAdvanceTask(
                                                        task,
                                                        "picked_up",
                                                        BookingStatus.OUT_FOR_DELIVERY.key,
                                                        "Return parcel out for delivery (${task.parcelCode})"
                                                    )
                                                }
                                            },
                                            enabled = !isBusy,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White),
                                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("partner_collect_${task.id}")
                                        ) {
                                            Text(if (task.taskType == "pickup") "Collect Parcel" else "Collect Return")
                                        }
                                    }
                                    "picked_up" -> {
                                        Button(
                                            onClick = {
                                                if (task.taskType == "pickup") {
                                                    onAdvanceTask(
                                                        task,
                                                        "handed_over",
                                                        BookingStatus.RECEIVED_BY_WORKER.key,
                                                        "Handed over to ${task.shopName}"
                                                    )
                                                } else {
                                                    otpDialogTask = task to linkedBooking
                                                }
                                            },
                                            enabled = !isBusy,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ClickitSuccess, contentColor = Color.White),
                                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("partner_complete_${task.id}")
                                        ) {
                                            Text(if (task.taskType == "pickup") "Hand Over" else "Deliver")
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { exceptionTarget = task },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ClickitCrimson.copy(alpha = 0.55f)),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Filled.ReportProblem, contentDescription = null, tint = ClickitCrimson)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Report Issue", color = ClickitCrimson)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PartnerEarningsScreen(partnerTasksState: UiState<List<DeliveryTask>>) {
    val tasks = (partnerTasksState as? UiState.Success)?.data ?: emptyList()
    val completed = tasks.filter { it.status in listOf("handed_over", "delivered") }
    val totalPayout = completed.sumOf { it.payoutMinor }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ClickitBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeonIconBadge(
                            icon = Icons.Filled.AccountBalanceWallet,
                            tint = ClickitBrandTeal,
                            size = 38.dp,
                            iconSize = 19.dp,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Payouts", style = MaterialTheme.typography.titleSmall, color = ClickitTextSecondary)
                    }
                    Text(formatInr(totalPayout), style = MaterialTheme.typography.displaySmall, color = ClickitBrandTeal)
                    Text("${completed.size} completed", style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                }
            }
        }

        if (completed.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.CheckCircle,
                    title = "No payouts yet",
                    subtitle = "Completed tasks appear here."
                )
            }
        } else {
            items(completed, key = { it.id }) { t ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${t.parcelCode} • ${t.taskType.uppercase()}", style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                            Text("+${formatInr(t.payoutMinor)}", style = MaterialTheme.typography.titleSmall, color = ClickitSuccess)
                        }
                        HorizontalDivider(color = ClickitBorder, modifier = Modifier.padding(vertical = 6.dp))
                        Text("${t.pickupZone} -> ${t.dropZone}", style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                    }
                }
            }
        }
    }
}
