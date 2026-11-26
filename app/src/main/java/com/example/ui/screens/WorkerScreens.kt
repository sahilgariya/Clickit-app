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
import com.example.ui.theme.ClickitBurgundy
import com.example.ui.theme.ClickitCrimson
import com.example.ui.theme.ClickitInfo
import com.example.ui.theme.ClickitInfoContainer
import com.example.ui.theme.ClickitIvory
import com.example.ui.theme.ClickitMidnight
import com.example.ui.theme.ClickitMutedText
import com.example.ui.theme.ClickitPeach
import com.example.ui.theme.ClickitPeachSoft
import com.example.ui.theme.ClickitSlate
import com.example.ui.theme.ClickitSlateDeep
import com.example.ui.theme.ClickitSubtleLine
import com.example.ui.theme.ClickitSuccess
import com.example.ui.theme.ClickitSuccessContainer
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
                    subtitle = "Create your shop to receive orders.",
                    actionLabel = "Create Shop",
                    onAction = onOpenDukaanTab
                )
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                    border = BorderStroke(1.dp, ClickitSubtleLine),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeonIconBadge(
                                icon = Icons.Filled.Storefront,
                                tint = ClickitPeach,
                                glowColor = if (primaryShop.openStatus) ClickitSuccess else ClickitPeach,
                                size = 44.dp,
                                iconSize = 22.dp,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("MY SHOP", style = MaterialTheme.typography.labelSmall, color = ClickitPeach)
                                Text(primaryShop.shopName, style = MaterialTheme.typography.titleLarge, color = ClickitIvory)
                                Text("${primaryShop.locality} • ${primaryShop.hoursSummary}", style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                            }
                            Switch(
                                checked = primaryShop.openStatus,
                                onCheckedChange = { onToggleShopOpen(primaryShop.id, it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = ClickitMidnight, checkedTrackColor = ClickitPeach),
                                modifier = Modifier.testTag("worker_shop_open_switch")
                            )
                        }

                        val capRatio = (activeJobs.size.toFloat() / primaryShop.dailyCapacity.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Today's Workload Capacity", style = MaterialTheme.typography.labelSmall, color = ClickitMutedText)
                                Text("${activeJobs.size} / ${primaryShop.dailyCapacity} jobs", style = MaterialTheme.typography.labelSmall, color = ClickitIvory)
                            }
                            LinearProgressIndicator(
                                progress = { capRatio },
                                color = ClickitPeach,
                                trackColor = ClickitSlate,
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WorkerMetricCard(
                    title = "New Requests",
                    value = pendingRequests.size.toString(),
                    accent = ClickitWarning,
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenJobsTab)
                )
                WorkerMetricCard(
                    title = "Active Jobs",
                    value = activeJobs.size.toString(),
                    accent = ClickitInfo,
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
                Text("Pending Requests", style = MaterialTheme.typography.titleLarge, color = ClickitIvory)
                TextButton(onClick = onOpenJobsTab) {
                    Text("All Jobs", color = ClickitPeach)
                }
            }
        }

        if (pendingRequests.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.CheckCircle,
                    title = "No pending requests",
                    subtitle = "New orders appear here.",
                    actionLabel = "My Shop",
                    onAction = onOpenDukaanTab
                )
            }
        } else {
            items(pendingRequests.take(5), key = { it.id }) { bk ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                    border = BorderStroke(1.dp, ClickitWarning),
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenJobsTab)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${bk.shopName} • ${bk.parcelCode}", style = MaterialTheme.typography.titleSmall, color = ClickitPeach)
                            Text(formatInr(bk.serviceSubtotalMinor), style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                        }
                        Text("${bk.customerName}: ${bk.itemsSummary}", style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                        Text("Slot: ${bk.slotWindowLabel}", style = MaterialTheme.typography.labelSmall, color = ClickitInfo)
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
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
        border = BorderStroke(1.dp, ClickitSubtleLine),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = ClickitMutedText)
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = accent)
        }
    }
}

@Composable
fun WorkerJobsScreen(
    workerBookingsState: UiState<List<Booking>>,
    isBusy: Boolean,
    onAccept: (Booking) -> Unit,
    onSendQuote: (Booking, Long, String) -> Unit,
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
        var amountRupees by remember { mutableStateOf((bk.serviceSubtotalMinor / 100L).toString()) }
        var reason by remember { mutableStateOf("Actual item count / fabric delicacy adjustment") }
        AlertDialog(
            onDismissRequest = { quoteTarget = null },
            containerColor = ClickitSlateDeep,
            title = { Text("Revise Quote (${bk.parcelCode})", color = ClickitIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountRupees,
                        onValueChange = { amountRupees = it },
                        label = { Text("New Subtotal (₹) *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason *") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minor = ((amountRupees.toDoubleOrNull() ?: 0.0) * 100.0).toLong()
                        if (minor >= 100L && reason.trim().length >= 3) {
                            onSendQuote(bk, minor, reason)
                            quoteTarget = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight)
                ) { Text("Send Quote") }
            },
            dismissButton = {
                TextButton(onClick = { quoteTarget = null }) { Text("Cancel", color = ClickitIvory) }
            }
        )
    }

    if (rejectTarget != null) {
        DestructiveConfirmDialog(
            title = "Decline Booking ${rejectTarget!!.parcelCode}?",
            message = "Customer will be notified immediately that your shop cannot fulfill this slot.",
            confirmLabel = "Decline Request",
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
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ClickitPeach,
                        selectedLabelColor = ClickitMidnight
                    )
                )
            }
        }

        when (workerBookingsState) {
            is UiState.Loading -> LoadingStateView("Loading worker jobs...")
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
                            title = "No jobs in ${tabs[selectedTab]}",
                            subtitle = "New orders appear here."
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
        colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
        border = BorderStroke(1.dp, ClickitSubtleLine),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("worker_job_${booking.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryBadge(booking.category)
                    Text(booking.parcelCode, style = MaterialTheme.typography.titleSmall, color = ClickitPeach)
                    if (isUrgent) {
                        Surface(color = ClickitWarningContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "URGENT +25%",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClickitWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isFreeBulk) {
                        Surface(color = ClickitSuccessContainer, shape = RoundedCornerShape(8.dp)) {
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

            Text("${booking.customerName} • ${booking.shopName}", style = MaterialTheme.typography.titleMedium, color = ClickitIvory)
            Text(booking.itemsSummary, style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
            Text(
                text = "${booking.itemCount} pcs • ${booking.weightKg} kg • Worker Share: ${formatInr(booking.serviceSubtotalMinor)} • ${booking.slotWindowLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = ClickitPeachSoft
            )

            when (st) {
                BookingStatus.REQUESTED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAccept,
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight),
                            modifier = Modifier.weight(1f).testTag("worker_accept_${booking.id}")
                        ) { Text("Accept") }
                        OutlinedButton(
                            onClick = onReviseQuote,
                            enabled = !isBusy,
                            border = BorderStroke(1.dp, ClickitPeach),
                            modifier = Modifier.weight(1f)
                        ) { Text("Revise Quote", color = ClickitPeach) }
                        OutlinedButton(
                            onClick = onReject,
                            enabled = !isBusy,
                            border = BorderStroke(1.dp, ClickitCrimson)
                        ) { Text("Decline", color = ClickitPeachSoft) }
                    }
                }
                BookingStatus.CONFIRMED, BookingStatus.PICKUP_SCHEDULED, BookingStatus.PICKED_UP -> {
                    Text("Intake & QC Check", style = MaterialTheme.typography.labelSmall, color = ClickitMutedText)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        qcItems.forEach { qc ->
                            val selected = qc in checkedQcTags
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    checkedQcTags = if (selected) checkedQcTags - qc else checkedQcTags + qc
                                },
                                label = { Text(qc, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ClickitSuccessContainer,
                                    selectedLabelColor = ClickitSuccess,
                                    containerColor = ClickitMidnight,
                                    labelColor = ClickitMutedText
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = ClickitSubtleLine
                                )
                            )
                        }
                    }
                    Button(
                        onClick = onReceiveItems,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight),
                        modifier = Modifier.fillMaxWidth().testTag("worker_receive_${booking.id}")
                    ) {
                        Text("Confirm Item Receipt (${booking.itemCount} pcs)")
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
                                label = {
                                    Text(
                                        text = if (isDelayFlag) "Flag Delay" else step,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isDelayFlag) ClickitWarningContainer else ClickitInfoContainer,
                                    selectedLabelColor = if (isDelayFlag) ClickitWarning else ClickitInfo,
                                    containerColor = ClickitMidnight,
                                    labelColor = if (isDelayFlag) ClickitWarning else ClickitMutedText
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = booking.milestone == step,
                                    borderColor = if (isDelayFlag) ClickitWarning.copy(alpha = 0.45f) else ClickitSubtleLine
                                )
                            )
                        }
                    }
                    Text("Pre-Seal QC Checklist", style = MaterialTheme.typography.labelSmall, color = ClickitMutedText)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        qcItems.forEach { qc ->
                            val selected = qc in checkedQcTags
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    checkedQcTags = if (selected) checkedQcTags - qc else checkedQcTags + qc
                                },
                                label = { Text(qc, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ClickitSuccessContainer,
                                    selectedLabelColor = ClickitSuccess,
                                    containerColor = ClickitMidnight,
                                    labelColor = ClickitMutedText
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = ClickitSubtleLine
                                )
                            )
                        }
                    }
                    Button(
                        onClick = onMarkReady,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = ClickitSuccess, contentColor = ClickitMidnight),
                        modifier = Modifier.fillMaxWidth().testTag("worker_ready_${booking.id}")
                    ) {
                        Text("Mark Work Ready & Seal Parcel")
                    }
                }
                else -> {
                    Text("Milestone: ${booking.milestone}", style = MaterialTheme.typography.bodySmall, color = ClickitInfo)
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
        AlertDialog(
            onDismissRequest = { showShopEditor = false },
            containerColor = ClickitSlateDeep,
            title = { Text(if (primaryShop == null) "Create Shop" else "Edit Shop", color = ClickitIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Shop Name *") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ServiceCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ClickitPeach,
                                    selectedLabelColor = ClickitMidnight
                                )
                            )
                        }
                    }
                    OutlinedTextField(value = locality, onValueChange = { locality = it }, label = { Text("Area / Locality *") }, singleLine = true)
                    OutlinedTextField(value = addressSummary, onValueChange = { addressSummary = it }, label = { Text("Shop Address *") })
                    OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, singleLine = true)
                    OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Hours") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startPrice,
                            onValueChange = { startPrice = it },
                            label = { Text("Min ₹ *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = capacity,
                            onValueChange = { capacity = it },
                            label = { Text("Capacity *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceMinor = ((startPrice.toDoubleOrNull() ?: 15.0) * 100.0).toLong().coerceAtLeast(100L)
                        onSaveShop(
                            baseShop.copy(
                                shopName = name,
                                category = category.key,
                                locality = locality,
                                addressSummary = addressSummary,
                                tagline = tagline,
                                hoursSummary = hours,
                                startingPriceMinor = priceMinor,
                                dailyCapacity = capacity.toLongOrNull()?.coerceIn(1L, 500L) ?: 30L
                            )
                        ) { showShopEditor = false }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showShopEditor = false }) { Text("Cancel", color = ClickitIvory) }
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
        var name by remember { mutableStateOf(base.name) }
        var desc by remember { mutableStateOf(base.description) }
        var unit by remember { mutableStateOf(base.unit) }
        var priceRupees by remember { mutableStateOf((base.priceMinor / 100L).toString()) }
        var turnaround by remember { mutableStateOf(base.turnaroundHours.toString()) }
        AlertDialog(
            onDismissRequest = {
                showAddService = false
                editingService = null
            },
            containerColor = ClickitSlateDeep,
            title = { Text(if (editingService == null) "Add Service" else "Edit Price & Service", color = ClickitIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Service Name *") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Inclusions / Description") })
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Pricing Unit (cloth/kg/pair) *") })
                    OutlinedTextField(value = priceRupees, onValueChange = { priceRupees = it }, label = { Text("Price (₹) *") })
                    OutlinedTextField(value = turnaround, onValueChange = { turnaround = it }, label = { Text("Turnaround (hours) *") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minor = ((priceRupees.toDoubleOrNull() ?: 15.0) * 100.0).toLong().coerceAtLeast(100L)
                        onSaveService(
                            primaryShop.id,
                            base.copy(
                                name = name,
                                description = desc,
                                unit = unit,
                                priceMinor = minor,
                                turnaroundHours = turnaround.toLongOrNull()?.coerceIn(1L, 720L) ?: 24L
                            )
                        ) {
                            showAddService = false
                            editingService = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight)
                ) { Text("Save Service") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddService = false
                    editingService = null
                }) { Text("Cancel", color = ClickitIvory) }
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
                    subtitle = "Create your shop to start receiving orders.",
                    actionLabel = "Create Shop",
                    onAction = { showShopEditor = true }
                )
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                    border = BorderStroke(1.dp, ClickitSlate)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryBadge(primaryShop.category)
                            IconButton(onClick = { showShopEditor = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit shop", tint = ClickitPeach)
                            }
                        }
                        Text(primaryShop.shopName, style = MaterialTheme.typography.titleLarge, color = ClickitIvory)
                        Text(primaryShop.tagline, style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                        Text("Hours: ${primaryShop.hoursSummary} • Capacity: ${primaryShop.dailyCapacity}/day", style = MaterialTheme.typography.labelSmall, color = ClickitPeachSoft)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Service Catalogue (${services.size})", style = MaterialTheme.typography.titleMedium, color = ClickitIvory)
                    TextButton(onClick = { showAddService = true }, modifier = Modifier.testTag("add_service_button")) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = ClickitPeach)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Service", color = ClickitPeach)
                    }
                }
            }

            items(services, key = { it.id }) { svc ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                    border = BorderStroke(1.dp, ClickitSlate)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(svc.name, style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                            Text("${formatInr(svc.priceMinor)}/${svc.unit} • ${svc.turnaroundHours}h", style = MaterialTheme.typography.labelMedium, color = ClickitPeach)
                            Text(svc.description, style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                        }
                        IconButton(onClick = { editingService = svc }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit price", tint = ClickitIvory)
                        }
                        Switch(
                            checked = svc.active,
                            onCheckedChange = { onToggleServiceActive(primaryShop.id, svc.id, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ClickitMidnight, checkedTrackColor = ClickitPeach)
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
                        Text("Settled Worker Earnings", style = MaterialTheme.typography.titleSmall, color = ClickitPeachSoft)
                    }
                    Text(formatInr(totalWorkerNet), style = MaterialTheme.typography.displaySmall, color = ClickitIvory)
                    Text(
                        text = "Gross Customer Billing: ${formatInr(totalGross)} • Platform Fees: ${formatInr(totalPlatform)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClickitMutedText
                    )
                }
            }
        }

        if (completed.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "No settled jobs yet",
                    subtitle = "Job-wise immutable INR financial breakdowns appear once bookings complete."
                )
            }
        } else {
            items(completed, key = { it.id }) { bk ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                    border = BorderStroke(1.dp, ClickitSlate)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${bk.parcelCode} • ${bk.customerName}", style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                            Text("+${formatInr(bk.serviceSubtotalMinor)}", style = MaterialTheme.typography.titleSmall, color = ClickitSuccess)
                        }
                        Text(bk.itemsSummary, style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                        HorizontalDivider(color = ClickitSubtleLine, modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Customer Paid: ${formatInr(bk.totalMinor)} (Delivery ${formatInr(bk.deliveryFeeMinor)} • Clickit ${formatInr(bk.platformFeeMinor)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClickitMutedText
                        )
                    }
                }
            }
        }
    }
}
