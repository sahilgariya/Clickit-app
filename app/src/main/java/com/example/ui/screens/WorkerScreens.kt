package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.FREE_DELIVERY_WEIGHT_THRESHOLD_KG
import com.example.data.FulfilmentMethod
import com.example.data.ServiceCategory
import com.example.data.Shop
import com.example.data.ShopService
import com.example.data.formatInr
import com.example.ui.UiState
import com.example.ui.components.BookingStatusBadge
import com.example.ui.components.CategoryBadge
import com.example.ui.components.DestructiveConfirmDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.NeonIconBadge
import com.example.ui.theme.ClickitBorder
import com.example.ui.theme.ClickitBorderStrong
import com.example.ui.theme.ClickitBrandTeal
import com.example.ui.theme.ClickitBrandTealDark
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
fun WorkerDashboardScreen(
    ownedShopsState: UiState<List<Shop>>,
    workerBookingsState: UiState<List<Booking>>,
    onToggleShopOpen: (String, Boolean) -> Unit,
    onOpenJobsTab: () -> Unit,
    onOpenDukaanTab: () -> Unit
) {
    val shops = (ownedShopsState as? UiState.Success)?.data ?: emptyList()
    val bookings = (workerBookingsState as? UiState.Success)?.data ?: emptyList()
    val primaryShop = shops.firstOrNull()

    val pendingRequests = bookings.filter { BookingStatus.fromRaw(it.status) == BookingStatus.REQUESTED }
    val activeJobs = bookings.filter {
        val st = BookingStatus.fromRaw(it.status)
        st != BookingStatus.REQUESTED && st != BookingStatus.COMPLETED && st != BookingStatus.CANCELLED
    }
    val totalWorkerShareMinor = bookings
        .filter { BookingStatus.fromRaw(it.status) == BookingStatus.COMPLETED }
        .sumOf { it.serviceSubtotalMinor }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (primaryShop == null) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Storefront,
                    title = "No shop yet",
                    subtitle = "Add your shop to get started.",
                    actionLabel = "Create Shop",
                    onAction = onOpenDukaanTab
                )
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, ClickitBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeonIconBadge(
                                icon = Icons.Filled.Storefront,
                                tint = ClickitBrandTeal,
                                size = 42.dp,
                                iconSize = 20.dp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SHOP", style = MaterialTheme.typography.labelSmall, color = ClickitBrandTeal)
                                Text(primaryShop.shopName, style = MaterialTheme.typography.titleLarge, color = ClickitTextPrimary)
                                Text("${primaryShop.locality} • ${primaryShop.hoursSummary}", style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                            }
                            Switch(
                                checked = primaryShop.openStatus,
                                onCheckedChange = { onToggleShopOpen(primaryShop.id, it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ClickitBrandTeal),
                                modifier = Modifier.testTag("worker_shop_open_switch")
                            )
                        }

                        val capRatio = (activeJobs.size.toFloat() / primaryShop.dailyCapacity.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Daily Capacity", style = MaterialTheme.typography.labelSmall, color = ClickitTextSecondary)
                                Text("${activeJobs.size} / ${primaryShop.dailyCapacity} jobs", style = MaterialTheme.typography.labelSmall, color = ClickitTextPrimary)
                            }
                            LinearProgressIndicator(
                                progress = { capRatio },
                                color = ClickitBrandTeal,
                                trackColor = ClickitBorder,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WorkerMetricCard(
                    title = "Requests",
                    value = pendingRequests.size.toString(),
                    accent = ClickitWarning,
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenJobsTab)
                )
                WorkerMetricCard(
                    title = "Active",
                    value = activeJobs.size.toString(),
                    accent = ClickitBrandTeal,
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenJobsTab)
                )
                WorkerMetricCard(
                    title = "Settled",
                    value = formatInr(totalWorkerShareMinor),
                    accent = ClickitSuccess,
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenDukaanTab)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Requests", style = MaterialTheme.typography.titleLarge, color = ClickitTextPrimary)
                TextButton(onClick = onOpenJobsTab) {
                    Text("View All", color = ClickitBrandTeal)
                }
            }
        }

        if (pendingRequests.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.CheckCircle,
                    title = "No requests",
                    subtitle = "Orders appear here.",
                    actionLabel = "My Shop",
                    onAction = onOpenDukaanTab
                )
            }
        } else {
            items(pendingRequests.take(5), key = { it.id }) { bk ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitWarning.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenJobsTab)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${bk.shopName} • ${bk.parcelCode}", style = MaterialTheme.typography.titleSmall, color = ClickitBrandTeal)
                            Text(formatInr(bk.serviceSubtotalMinor), style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                        }
                        Text("${bk.customerName}: ${bk.itemsSummary}", style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                        Text("Slot: ${bk.slotWindowLabel}", style = MaterialTheme.typography.labelSmall, color = ClickitBrandTeal)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerMetricCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ClickitBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = ClickitTextSecondary)
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = accent)
        }
    }
}

@Composable
fun WorkerJobsScreen(
    workerBookingsState: UiState<List<Booking>>,
    isBusy: Boolean,
    onAccept: (Booking) -> Unit,
    onSendQuote: (Booking, Long, String, () -> Unit) -> Unit,
    onReject: (Booking, String) -> Unit,
    onReceiveItems: (Booking) -> Unit,
    onUpdateMilestone: (Booking, String) -> Unit,
    onMarkReady: (Booking) -> Unit,
    onRetry: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Requests", "In Progress", "Ready & Done")

    var quoteTarget by remember { mutableStateOf<Booking?>(null) }
    var rejectTarget by remember { mutableStateOf<Booking?>(null) }

    if (quoteTarget != null) {
        val bk = quoteTarget!!
        var amountRupees by remember(bk.id) { mutableStateOf((bk.serviceSubtotalMinor / 100L).toString()) }
        var reason by remember(bk.id) { mutableStateOf("Actual item count / fabric delicacy adjustment") }
        var quoteError by remember(bk.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!isBusy) quoteTarget = null },
            containerColor = ClickitLightSurface,
            title = { Text("Update Quote (${bk.parcelCode})", color = ClickitTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountRupees,
                        onValueChange = { amountRupees = it; quoteError = null },
                        label = { Text("Amount (₹) *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it; quoteError = null },
                        label = { Text("Reason *") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (quoteError != null) {
                        Text(quoteError!!, style = MaterialTheme.typography.bodySmall, color = ClickitCrimson)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedRupees = amountRupees.trim().toDoubleOrNull()
                        val minor = parsedRupees?.let { kotlin.math.round(it * 100.0).toLong() } ?: 0L
                        when {
                            parsedRupees == null || minor < 100L -> quoteError = "Enter amount of ₹1 or more."
                            reason.trim().length !in 3..200 -> quoteError = "Enter a reason (3–200 chars)."
                            else -> onSendQuote(bk, minor, reason.trim()) { quoteTarget = null }
                        }
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White)
                ) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = { quoteTarget = null }, enabled = !isBusy) { Text("Cancel", color = ClickitTextSecondary) }
            }
        )
    }

    if (rejectTarget != null) {
        DestructiveConfirmDialog(
            title = "Decline ${rejectTarget!!.parcelCode}?",
            message = "Customer will be notified.",
            confirmLabel = "Decline",
            onConfirm = {
                onReject(rejectTarget!!, "Shop capacity full for requested window")
                rejectTarget = null
            },
            onDismiss = { rejectTarget = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                FilterChip(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    shape = RoundedCornerShape(8.dp),
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ClickitBrandTeal,
                        selectedLabelColor = Color.White,
                        containerColor = ClickitLightSurface,
                        labelColor = ClickitTextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedTab == idx,
                        borderColor = ClickitBorder
                    )
                )
            }
        }

        when (workerBookingsState) {
            is UiState.Loading -> LoadingStateView("Loading...")
            is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorRetryCard(workerBookingsState.message, onRetry = onRetry) }
            is UiState.Success -> {
                val filtered = workerBookingsState.data.filter { bk ->
                    val st = BookingStatus.fromRaw(bk.status)
                    when (selectedTab) {
                        0 -> st == BookingStatus.REQUESTED || st == BookingStatus.QUOTE_PENDING
                        1 -> st in listOf(
                            BookingStatus.ACCEPTED, BookingStatus.CONFIRMED,
                            BookingStatus.PICKUP_SCHEDULED, BookingStatus.PICKED_UP,
                            BookingStatus.RECEIVED_BY_WORKER, BookingStatus.IN_PROGRESS
                        )
                        else -> st in listOf(
                            BookingStatus.READY, BookingStatus.RETURN_SCHEDULED,
                            BookingStatus.OUT_FOR_DELIVERY, BookingStatus.DELIVERED,
                            BookingStatus.COMPLETED, BookingStatus.CANCELLED
                        )
                    }
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Filled.Inventory,
                            title = "No jobs",
                            subtitle = "Orders appear here."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { bk ->
                            WorkerJobCard(
                                booking = bk,
                                isBusy = isBusy,
                                onAccept = { onAccept(bk) },
                                onReviseQuote = { quoteTarget = bk },
                                onReject = { rejectTarget = bk },
                                onReceiveItems = { onReceiveItems(bk) },
                                onUpdateMilestone = { ms -> onUpdateMilestone(bk, ms) },
                                onMarkReady = { onMarkReady(bk) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkerJobCard(
    booking: Booking,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onReviseQuote: () -> Unit,
    onReject: () -> Unit,
    onReceiveItems: () -> Unit,
    onUpdateMilestone: (String) -> Unit,
    onMarkReady: () -> Unit
) {
    val st = BookingStatus.fromRaw(booking.status)
    val isFreeBulk = booking.weightKg > FREE_DELIVERY_WEIGHT_THRESHOLD_KG &&
        booking.fulfilmentMethod != FulfilmentMethod.SHOP_DROP.key &&
        booking.deliveryFeeMinor == 0L
    val isUrgent = booking.itemsSummary.contains("URGENT +25%", ignoreCase = true)
    var checkedQcTags by remember(booking.id) { mutableStateOf(setOf<String>()) }
    val qcItems = listOf("QR Tagged", "Condition Logged", "Stains Noted", "Tamper-Sealed")

    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ClickitBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("worker_job_${booking.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryBadge(booking.category)
                    Text(booking.parcelCode, style = MaterialTheme.typography.titleSmall, color = ClickitBrandTeal)
                    if (isUrgent) {
                        Surface(color = ClickitWarningContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "URGENT +25%",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClickitWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isFreeBulk) {
                        Surface(color = ClickitSuccessContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "FREE >5 KG",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClickitSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                BookingStatusBadge(booking.status)
            }

            Text("${booking.customerName} • ${booking.shopName}", style = MaterialTheme.typography.titleMedium, color = ClickitTextPrimary)
            Text(booking.itemsSummary, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
            Text(
                text = "${booking.itemCount} pcs • ${booking.weightKg} kg • ${formatInr(booking.serviceSubtotalMinor)} • ${booking.slotWindowLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = ClickitBrandTealDark
            )

            when (st) {
                BookingStatus.REQUESTED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAccept,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("worker_accept_${booking.id}")
                        ) { Text("Accept") }
                        OutlinedButton(
                            onClick = onReviseQuote,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ClickitBorderStrong),
                            modifier = Modifier.weight(1f)
                        ) { Text("Quote", color = ClickitBrandTeal) }
                        OutlinedButton(
                            onClick = onReject,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ClickitCrimson.copy(alpha = 0.55f))
                        ) { Text("Decline", color = ClickitCrimson) }
                    }
                }
                BookingStatus.CONFIRMED, BookingStatus.PICKUP_SCHEDULED, BookingStatus.PICKED_UP -> {
                    Text("Intake Check", style = MaterialTheme.typography.labelSmall, color = ClickitTextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        qcItems.forEach { qc ->
                            val selected = qc in checkedQcTags
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    checkedQcTags = if (selected) checkedQcTags - qc else checkedQcTags + qc
                                },
                                shape = RoundedCornerShape(8.dp),
                                label = { Text(qc, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ClickitSuccessContainer,
                                    selectedLabelColor = ClickitSuccess,
                                    containerColor = ClickitLightSurface,
                                    labelColor = ClickitTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = ClickitBorder
                                )
                            )
                        }
                    }
                    Button(
                        onClick = onReceiveItems,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("worker_receive_${booking.id}")
                    ) {
                        Text("Confirm Receipt (${booking.itemCount})")
                    }
                }
                BookingStatus.RECEIVED_BY_WORKER, BookingStatus.IN_PROGRESS -> {
                    val milestones = when (ServiceCategory.fromRaw(booking.category)) {
                        ServiceCategory.LAUNDRY -> listOf("Sorting & Pre-treat", "Washing / Steam Press", "Quality Check")
                        ServiceCategory.TAILORING -> listOf("Measuring & Marking", "Stitching & Overlock", "Steam Finish")
                        ServiceCategory.FOOTWEAR -> listOf("Deep Scrub & Prep", "Sole Bond / Repair", "Polish & QC")
                    } + listOf("Delay Flagged • New ETA shared")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        milestones.forEach { step ->
                            val isDelayFlag = step.startsWith("Delay")
                            FilterChip(
                                selected = booking.milestone == step,
                                onClick = { onUpdateMilestone(step) },
                                shape = RoundedCornerShape(8.dp),
                                label = {
                                    Text(
                                        text = if (isDelayFlag) "Flag Delay" else step,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isDelayFlag) ClickitWarningContainer else ClickitBrandTealTint,
                                    selectedLabelColor = if (isDelayFlag) ClickitWarning else ClickitBrandTeal,
                                    containerColor = ClickitLightSurface,
                                    labelColor = if (isDelayFlag) ClickitWarning else ClickitTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = booking.milestone == step,
                                    borderColor = if (isDelayFlag) ClickitWarning.copy(alpha = 0.45f) else ClickitBorder
                                )
                            )
                        }
                    }
                    Text("Quality Check", style = MaterialTheme.typography.labelSmall, color = ClickitTextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        qcItems.forEach { qc ->
                            val selected = qc in checkedQcTags
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    checkedQcTags = if (selected) checkedQcTags - qc else checkedQcTags + qc
                                },
                                shape = RoundedCornerShape(8.dp),
                                label = { Text(qc, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ClickitSuccessContainer,
                                    selectedLabelColor = ClickitSuccess,
                                    containerColor = ClickitLightSurface,
                                    labelColor = ClickitTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = ClickitBorder
                                )
                            )
                        }
                    }
                    Button(
                        onClick = onMarkReady,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClickitSuccess, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("worker_ready_${booking.id}")
                    ) {
                        Text("Mark Ready")
                    }
                }
                else -> {
                    Text("Milestone: ${booking.milestone}", style = MaterialTheme.typography.bodySmall, color = ClickitBrandTeal)
                }
            }
        }
    }
}

@Composable
fun WorkerDukaanScreen(
    ownedShopsState: UiState<List<Shop>>,
    ownedServicesState: UiState<List<ShopService>>,
    onSaveShop: (Shop, () -> Unit) -> Unit,
    onSaveService: (String, ShopService, () -> Unit) -> Unit,
    onToggleServiceActive: (String, String, Boolean) -> Unit
) {
    val shops = (ownedShopsState as? UiState.Success)?.data ?: emptyList()
    val services = (ownedServicesState as? UiState.Success)?.data ?: emptyList()
    val primaryShop = shops.firstOrNull()

    var showShopEditor by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<ShopService?>(null) }
    var showAddService by remember { mutableStateOf(false) }

    if (showShopEditor) {
        val baseShop = primaryShop ?: Shop(startingPriceMinor = 1500L, startingPriceUnit = "cloth")
        var name by remember(baseShop.id) { mutableStateOf(baseShop.shopName) }
        var category by remember(baseShop.id) { mutableStateOf(ServiceCategory.fromRaw(baseShop.category)) }
        var locality by remember(baseShop.id) { mutableStateOf(baseShop.locality) }
        var addressSummary by remember(baseShop.id) { mutableStateOf(baseShop.addressSummary) }
        var tagline by remember(baseShop.id) { mutableStateOf(baseShop.tagline) }
        var hours by remember(baseShop.id) { mutableStateOf(baseShop.hoursSummary) }
        var capacity by remember(baseShop.id) { mutableStateOf(baseShop.dailyCapacity.toString()) }
        var startPrice by remember(baseShop.id) { mutableStateOf((baseShop.startingPriceMinor / 100L).coerceAtLeast(1L).toString()) }
        var shopError by remember(baseShop.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showShopEditor = false },
            containerColor = ClickitLightSurface,
            title = { Text(if (primaryShop == null) "Create Shop" else "Edit Shop", color = ClickitTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it; shopError = null }, label = { Text("Shop Name *") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ServiceCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                shape = RoundedCornerShape(8.dp),
                                label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ClickitBrandTeal,
                                    selectedLabelColor = Color.White,
                                    containerColor = ClickitLightSurface,
                                    labelColor = ClickitTextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = category == cat,
                                    borderColor = ClickitBorder
                                )
                            )
                        }
                    }
                    OutlinedTextField(value = locality, onValueChange = { locality = it; shopError = null }, label = { Text("Area *") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = addressSummary, onValueChange = { addressSummary = it; shopError = null }, label = { Text("Address *") }, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Hours") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startPrice,
                            onValueChange = { startPrice = it; shopError = null },
                            label = { Text("Min ₹ *") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = capacity,
                            onValueChange = { capacity = it; shopError = null },
                            label = { Text("Capacity *") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (shopError != null) {
                        Text(shopError!!, style = MaterialTheme.typography.bodySmall, color = ClickitCrimson)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPrice = startPrice.trim().toDoubleOrNull()
                        val priceMinor = parsedPrice?.let { kotlin.math.round(it * 100.0).toLong() } ?: 0L
                        val cap = capacity.trim().toLongOrNull()
                        when {
                            name.trim().length !in 2..80 -> shopError = "Shop name must be 2–80 chars."
                            locality.trim().length !in 1..80 -> shopError = "Area is required."
                            addressSummary.trim().length !in 1..160 -> shopError = "Address is required."
                            parsedPrice == null || priceMinor < 100L -> shopError = "Minimum price must be ₹1+."
                            cap == null || cap !in 1L..500L -> shopError = "Capacity must be 1–500."
                            else -> onSaveShop(
                                baseShop.copy(
                                    shopName = name.trim(),
                                    category = category.key,
                                    locality = locality.trim(),
                                    addressSummary = addressSummary.trim(),
                                    tagline = tagline.trim().take(140),
                                    hoursSummary = hours.trim().ifBlank { "9:00 AM – 8:00 PM" },
                                    startingPriceMinor = priceMinor,
                                    dailyCapacity = cap
                                )
                            ) { showShopEditor = false }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showShopEditor = false }) { Text("Cancel", color = ClickitTextSecondary) }
            }
        )
    }

    if ((showAddService || editingService != null) && primaryShop != null) {
        val base = editingService ?: ShopService(
            shopId = primaryShop.id,
            category = primaryShop.category,
            unit = "cloth",
            priceMinor = 1500L,
            minQty = 1L,
            turnaroundHours = 24L,
            active = true
        )
        var name by remember(base.id) { mutableStateOf(base.name) }
        var desc by remember(base.id) { mutableStateOf(base.description) }
        var unit by remember(base.id) { mutableStateOf(base.unit) }
        var priceRupees by remember(base.id) { mutableStateOf((base.priceMinor / 100L).toString()) }
        var turnaround by remember(base.id) { mutableStateOf(base.turnaroundHours.toString()) }
        var svcError by remember(base.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = {
                showAddService = false
                editingService = null
            },
            containerColor = ClickitLightSurface,
            title = { Text(if (editingService == null) "Add Service" else "Edit Service", color = ClickitTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it; svcError = null }, label = { Text("Name *") }, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = unit, onValueChange = { unit = it; svcError = null }, label = { Text("Unit (cloth/kg/pair) *") }, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = priceRupees, onValueChange = { priceRupees = it; svcError = null }, label = { Text("Price (₹) *") }, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = turnaround, onValueChange = { turnaround = it; svcError = null }, label = { Text("Hours *") }, shape = RoundedCornerShape(10.dp))
                    if (svcError != null) {
                        Text(svcError!!, style = MaterialTheme.typography.bodySmall, color = ClickitCrimson)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPrice = priceRupees.trim().toDoubleOrNull()
                        val minor = parsedPrice?.let { kotlin.math.round(it * 100.0).toLong() } ?: 0L
                        val hoursVal = turnaround.trim().toLongOrNull()
                        when {
                            name.trim().length !in 2..80 -> svcError = "Service name must be 2–80 chars."
                            unit.trim().length !in 1..24 -> svcError = "Unit is required."
                            parsedPrice == null || minor !in 100L..10000000L -> svcError = "Price must be ₹1 or more."
                            hoursVal == null || hoursVal !in 1L..720L -> svcError = "Turnaround must be 1–720 hours."
                            else -> onSaveService(
                                primaryShop.id,
                                base.copy(
                                    name = name.trim(),
                                    description = desc.trim().take(200),
                                    unit = unit.trim(),
                                    priceMinor = minor,
                                    turnaroundHours = hoursVal
                                )
                            ) {
                                showAddService = false
                                editingService = null
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddService = false
                    editingService = null
                }) { Text("Cancel", color = ClickitTextSecondary) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (primaryShop == null) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Storefront,
                    title = "No shop yet",
                    subtitle = "Add your shop to start receiving orders.",
                    actionLabel = "Create Shop",
                    onAction = { showShopEditor = true }
                )
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ClickitBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryBadge(primaryShop.category)
                            IconButton(onClick = { showShopEditor = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit shop", tint = ClickitBrandTeal)
                            }
                        }
                        Text(primaryShop.shopName, style = MaterialTheme.typography.titleLarge, color = ClickitTextPrimary)
                        Text(primaryShop.tagline, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                        Text("Hours: ${primaryShop.hoursSummary} • Capacity: ${primaryShop.dailyCapacity}/day", style = MaterialTheme.typography.labelSmall, color = ClickitBrandTeal)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Services (${services.size})", style = MaterialTheme.typography.titleMedium, color = ClickitTextPrimary)
                    TextButton(onClick = { showAddService = true }, modifier = Modifier.testTag("add_service_button")) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = ClickitBrandTeal)
                        Spacer(Modifier.width(4.dp))
                        Text("Add", color = ClickitBrandTeal)
                    }
                }
            }

            items(services, key = { it.id }) { svc ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(svc.name, style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                            Text("${formatInr(svc.priceMinor)}/${svc.unit} • ${svc.turnaroundHours}h", style = MaterialTheme.typography.labelMedium, color = ClickitBrandTeal)
                            Text(svc.description, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                        }
                        IconButton(onClick = { editingService = svc }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit price", tint = ClickitTextPrimary)
                        }
                        Switch(
                            checked = svc.active,
                            onCheckedChange = { onToggleServiceActive(primaryShop.id, svc.id, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ClickitBrandTeal)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerEarningsScreen(workerBookingsState: UiState<List<Booking>>) {
    val bookings = (workerBookingsState as? UiState.Success)?.data ?: emptyList()
    val completed = bookings.filter { BookingStatus.fromRaw(it.status) == BookingStatus.COMPLETED }
    val totalGross = completed.sumOf { it.totalMinor }
    val totalWorkerNet = completed.sumOf { it.serviceSubtotalMinor }
    val totalPlatform = completed.sumOf { it.platformFeeMinor }

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
                        Text("Earnings", style = MaterialTheme.typography.titleSmall, color = ClickitTextSecondary)
                    }
                    Text(formatInr(totalWorkerNet), style = MaterialTheme.typography.displaySmall, color = ClickitBrandTeal)
                    Text(
                        text = "Gross: ${formatInr(totalGross)} • Fees: ${formatInr(totalPlatform)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClickitTextSecondary
                    )
                }
            }
        }

        if (completed.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "No earnings yet",
                    subtitle = "Completed jobs appear here."
                )
            }
        } else {
            items(completed, key = { it.id }) { bk ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${bk.parcelCode} • ${bk.customerName}", style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                            Text("+${formatInr(bk.serviceSubtotalMinor)}", style = MaterialTheme.typography.titleSmall, color = ClickitSuccess)
                        }
                        Text(bk.itemsSummary, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                        HorizontalDivider(color = ClickitBorder, modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Paid: ${formatInr(bk.totalMinor)} (Delivery ${formatInr(bk.deliveryFeeMinor)} • Fee ${formatInr(bk.platformFeeMinor)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClickitTextSecondary
                        )
                    }
                }
            }
        }
    }
}
