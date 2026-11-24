package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.DeliveryTask
import com.example.data.formatInr
import com.example.ui.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.NeonIconBadge
import com.example.ui.theme.ClickitBurgundy
import com.example.ui.theme.ClickitCrimson
import com.example.ui.theme.ClickitInfo
import com.example.ui.theme.ClickitIvory
import com.example.ui.theme.ClickitMidnight
import com.example.ui.theme.ClickitMutedText
import com.example.ui.theme.ClickitPeach
import com.example.ui.theme.ClickitPeachSoft
import com.example.ui.theme.ClickitSlate
import com.example.ui.theme.ClickitSlateDeep
import com.example.ui.theme.ClickitSubtleLine
import com.example.ui.theme.ClickitSuccess

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
                colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                border = BorderStroke(1.dp, if (online) ClickitSuccess else ClickitSlate)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        NeonIconBadge(
                            icon = Icons.AutoMirrored.Filled.DirectionsBike,
                            tint = if (online) ClickitSuccess else ClickitPeach,
                            glowColor = if (online) ClickitSuccess else ClickitPeach,
                            size = 42.dp,
                            iconSize = 20.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (online) "Online • Accepting Smart Batches" else "Offline • Shift Paused",
                                style = MaterialTheme.typography.titleSmall,
                                color = ClickitIvory
                            )
                            Text(
                                text = "Vehicle: 2-Wheeler • Max 25 kg • ${activeTasks.size} active stops",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitMutedText
                            )
                        }
                    }
                    Switch(
                        checked = online,
                        onCheckedChange = onToggleOnline,
                        colors = SwitchDefaults.colors(checkedThumbColor = ClickitMidnight, checkedTrackColor = ClickitPeach),
                        modifier = Modifier.testTag("partner_online_switch")
                    )
                }
            }
        }

        item {
            Text("Open Tasks", style = MaterialTheme.typography.titleLarge, color = ClickitIvory)
            Text(
                text = "Full address unlocks after accept.",
                style = MaterialTheme.typography.bodySmall,
                color = ClickitMutedText
            )
        }

        if (!online) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                    title = "Offline",
                    subtitle = "Go online for pickup tasks."
                )
            }
        } else {
            when (offeredTasksState) {
                is UiState.Loading -> item { LoadingStateView("Finding tasks...") }
                is UiState.Error -> item { ErrorRetryCard(offeredTasksState.message, onRetry = onRetry) }
                is UiState.Success -> {
                    if (offeredTasksState.data.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.LocalShipping,
                                title = "No open tasks",
                                subtitle = "New pickup tasks appear here."
                            )
                        }
                    } else {
                        items(offeredTasksState.data, key = { it.id }) { task ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                                border = BorderStroke(1.dp, ClickitInfo.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().testTag("task_offer_${task.id}")
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(color = ClickitMidnight, shape = RoundedCornerShape(8.dp)) {
                                            Text(
                                                text = "${task.taskType.uppercase()} • ${task.batchId}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ClickitInfo,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = "Payout ${formatInr(task.payoutMinor)}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = ClickitPeach
                                        )
                                    }

                                    Text("${task.pickupZone} -> ${task.dropZone}", style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                                    Text(
                                        text = "Parcel ${task.parcelCode} • ${task.itemCount} items • ~${task.estimatedWeightKg} kg • ${task.timeWindowLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ClickitMutedText
                                    )

                                    Button(
                                        onClick = { onAcceptTask(task) },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight),
                                        modifier = Modifier.fillMaxWidth().testTag("accept_task_${task.id}")
                                    ) {
                                        Text("Accept Task Offer")
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

@Composable
fun PartnerRouteScreen(
    partnerTasksState: UiState<List<DeliveryTask>>,
    partnerBookingsState: UiState<List<Booking>>,
    isBusy: Boolean,
    onAdvanceTask: (DeliveryTask, String, String?, String) -> Unit,
    onReportException: (DeliveryTask, String) -> Unit,
    onRetry: () -> Unit = {}
) {
    val tasks = (partnerTasksState as? UiState.Success)?.data ?: emptyList()
    val bookings = (partnerBookingsState as? UiState.Success)?.data ?: emptyList()
    val active = tasks.filter { it.status !in listOf("handed_over", "delivered", "exception") }

    var otpDialogTask by remember { mutableStateOf<Pair<DeliveryTask, Booking?>?>(null) }
    var exceptionTarget by remember { mutableStateOf<DeliveryTask?>(null) }

    if (otpDialogTask != null) {
        val (task, booking) = otpDialogTask!!
        val expectedOtp = if (task.taskType == "pickup") booking?.handoverOtp ?: "" else booking?.deliveryOtp ?: ""
        var enteredOtp by remember { mutableStateOf(expectedOtp) }
        var otpError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { otpDialogTask = null },
            containerColor = ClickitSlateDeep,
            title = {
                Text(
                    text = if (task.taskType == "pickup") "Verify Pickup Handover OTP" else "Verify Customer Delivery OTP",
                    color = ClickitIvory
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${task.itemCount} items • Parcel ${task.parcelCode} • OTP: $expectedOtp",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClickitMutedText
                    )
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = {
                            enteredOtp = it
                            otpError = null
                        },
                        label = { Text("4-digit OTP *") },
                        singleLine = true
                    )
                    if (otpError != null) {
                        Text(otpError!!, style = MaterialTheme.typography.bodySmall, color = ClickitPeachSoft)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (expectedOtp.isNotBlank() && enteredOtp.trim() != expectedOtp) {
                            otpError = "OTP mismatch. Check with customer before taking custody."
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
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight)
                ) { Text("Verify & Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { otpDialogTask = null }) { Text("Cancel", color = ClickitIvory) }
            }
        )
    }

    if (exceptionTarget != null) {
        var reason by remember { mutableStateOf("Customer unavailable") }
        AlertDialog(
            onDismissRequest = { exceptionTarget = null },
            containerColor = ClickitSlateDeep,
            title = { Text("Flag Stop Exception", color = ClickitIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Other completed stops in this batch remain untouched.", style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason *") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportException(exceptionTarget!!, reason)
                        exceptionTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitCrimson, contentColor = ClickitIvory)
                ) { Text("Report Exception") }
            },
            dismissButton = {
                TextButton(onClick = { exceptionTarget = null }) { Text("Cancel", color = ClickitIvory) }
            }
        )
    }

    when (partnerTasksState) {
        is UiState.Loading -> LoadingStateView("Loading active batch route...")
        is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorRetryCard(partnerTasksState.message, onRetry = onRetry) }
        is UiState.Success -> {
            if (active.isEmpty()) {
                Box(Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.LocalShipping,
                        title = "No active stops in route",
                        subtitle = "Accept a pickup or return offer from the Offers tab to start your route."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(active, key = { it.id }) { task ->
                        val linkedBooking = bookings.firstOrNull { it.id == task.bookingId }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                            border = BorderStroke(1.dp, ClickitPeach)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Stop ${task.batchStopIndex} • ${task.parcelCode}", style = MaterialTheme.typography.titleSmall, color = ClickitPeach)
                                    Text(task.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = ClickitInfo)
                                }
                                Text("${task.taskType.replaceFirstChar { it.uppercase() }}: ${task.pickupZone} -> ${task.dropZone}", style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                                if (linkedBooking != null) {
                                    Text("Stop Address: ${linkedBooking.addressSummary}", style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                                }
                                Text("${task.itemCount} items • ~${task.estimatedWeightKg} kg • ${task.timeWindowLabel}", style = MaterialTheme.typography.bodySmall, color = ClickitPeachSoft)

                                when (task.status) {
                                    "accepted" -> {
                                        Button(
                                            onClick = {
                                                onAdvanceTask(task, "arrived", null, "Partner arrived at stop (${task.parcelCode})")
                                            },
                                            enabled = !isBusy,
                                            colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight),
                                            modifier = Modifier.fillMaxWidth().testTag("partner_arrived_${task.id}")
                                        ) { Text("Mark Arrived at Stop") }
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
                                            colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight),
                                            modifier = Modifier.fillMaxWidth().testTag("partner_collect_${task.id}")
                                        ) {
                                            Text(if (task.taskType == "pickup") "Verify OTP & Collect Parcel" else "Collect Sealed Return from Shop")
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
                                            colors = ButtonDefaults.buttonColors(containerColor = ClickitSuccess, contentColor = ClickitMidnight),
                                            modifier = Modifier.fillMaxWidth().testTag("partner_complete_${task.id}")
                                        ) {
                                            Text(if (task.taskType == "pickup") "Confirm Handover to Worker" else "Verify Customer OTP & Deliver")
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { exceptionTarget = task },
                                    border = BorderStroke(1.dp, ClickitCrimson),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.ReportProblem, contentDescription = null, tint = ClickitPeachSoft)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Report Stop Exception", color = ClickitPeachSoft)
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
                colors = CardDefaults.cardColors(containerColor = ClickitBurgundy.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, ClickitPeach)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeonIconBadge(
                            icon = Icons.Filled.AccountBalanceWallet,
                            tint = ClickitPeach,
                            glowColor = ClickitPeach,
                            size = 38.dp,
                            iconSize = 19.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Delivery Partner Payouts", style = MaterialTheme.typography.titleSmall, color = ClickitPeachSoft)
                    }
                    Text(formatInr(totalPayout), style = MaterialTheme.typography.displaySmall, color = ClickitIvory)
                    Text("${completed.size} completed custody handovers", style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                }
            }
        }

        if (completed.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.CheckCircle,
                    title = "No completed tasks yet",
                    subtitle = "Complete a pickup handover or customer delivery to see payouts here."
                )
            }
        } else {
            items(completed, key = { it.id }) { t ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                    border = BorderStroke(1.dp, ClickitSlate)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${t.parcelCode} • ${t.taskType.uppercase()}", style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                            Text("+${formatInr(t.payoutMinor)}", style = MaterialTheme.typography.titleSmall, color = ClickitSuccess)
                        }
                        HorizontalDivider(color = ClickitSubtleLine, modifier = Modifier.padding(vertical = 6.dp))
                        Text("${t.pickupZone} -> ${t.dropZone}", style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                    }
                }
            }
        }
    }
}
