package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.DeliveryTask
import com.example.data.FulfilmentMethod
import com.example.data.formatInr
import com.example.ui.UiState
import com.example.ui.components.BookingStatusBadge
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.SmartBatchLiveMap
import com.example.ui.components.SmartBatchVisualTracker
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

private enum class ActiveOrderFilter(val label: String) {
    ALL("All Active"),
    NEEDS_ACTION("Needs Action"),
    IN_SERVICE("In Service"),
    IN_TRANSIT("Transit & Ready")
}

private fun Booking.needsUserAttention(): Boolean {
    val st = BookingStatus.fromRaw(status)
    return st == BookingStatus.QUOTE_PENDING ||
        st == BookingStatus.DELIVERED ||
        st == BookingStatus.DISPUTED ||
        paymentStatus.equals("pending", ignoreCase = true)
}

@Composable
fun ActiveOrdersScreen(
    activeOrdersState: UiState<List<Booking>>,
    customerTasksState: UiState<List<DeliveryTask>> = UiState.Success(emptyList()),
    onOrderClick: (Booking) -> Unit,
    onExploreClick: () -> Unit,
    onBack: () -> Unit,
    onSimulatePickupAlert: (Booking?) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    var selectedFilter by remember { mutableIntStateOf(0) }
    val filters = ActiveOrderFilter.entries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClickitMidnight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("active_orders_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ClickitIvory
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Active Orders",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = ClickitIvory
                            )
                            LiveStatusBadge()
                        }
                        Text(
                            text = "Real-time order milestones synced from Firestore",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClickitMutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (activeOrdersState is UiState.Success) {
                    Surface(
                        color = ClickitSlateDeep,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, ClickitPeach.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "${activeOrdersState.data.size} live",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ClickitPeach,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            when (activeOrdersState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingStateView("Syncing active order statuses...")
                    }
                }
                is UiState.Error -> {
                    Box(Modifier.padding(16.dp)) {
                        ErrorRetryCard(message = activeOrdersState.message, onRetry = onRetry)
                    }
                }
                is UiState.Success -> {
                    val orders = activeOrdersState.data
                    val targetForSimulation = orders.firstOrNull {
                        val st = BookingStatus.fromRaw(it.status)
                        st == BookingStatus.READY || st == BookingStatus.RETURN_SCHEDULED
                    } ?: orders.firstOrNull()

                    OutlinedButton(
                        onClick = { onSimulatePickupAlert(targetForSimulation) },
                        border = BorderStroke(1.dp, ClickitPeach.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(48.dp)
                            .testTag("simulate_smart_batch_alert_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = ClickitPeach,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Simulate Smart Batch Pickup Alert",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = ClickitPeach
                        )
                    }

                    if (orders.isEmpty()) {
                        Box(Modifier.padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.Filled.LocalShipping,
                                title = "No active orders right now",
                                subtitle = "Book a nearby service from Explore to track pickup, progress, and delivery live.",
                                actionLabel = "Explore Services",
                                onAction = onExploreClick
                            )
                        }
                    } else {
                        val attentionCount = orders.count { it.needsUserAttention() }
                        val inServiceCount = orders.count {
                            val st = BookingStatus.fromRaw(it.status)
                            st == BookingStatus.RECEIVED_BY_WORKER || st == BookingStatus.IN_PROGRESS
                        }
                        val transitCount = orders.count {
                            val st = BookingStatus.fromRaw(it.status)
                            st in listOf(
                                BookingStatus.PICKUP_SCHEDULED,
                                BookingStatus.PICKED_UP,
                                BookingStatus.READY,
                                BookingStatus.RETURN_SCHEDULED,
                                BookingStatus.OUT_FOR_DELIVERY
                            )
                        }

                        // Summary strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OrderStatusSummaryChip(
                                label = "Needs Action",
                                count = attentionCount,
                                accent = if (attentionCount > 0) ClickitWarning else ClickitMutedText,
                                modifier = Modifier.weight(1f)
                            )
                            OrderStatusSummaryChip(
                                label = "In Service",
                                count = inServiceCount,
                                accent = ClickitPeach,
                                modifier = Modifier.weight(1f)
                            )
                            OrderStatusSummaryChip(
                                label = "In Transit",
                                count = transitCount,
                                accent = ClickitInfo,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Filter chips
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filters.size) { index ->
                                val filter = filters[index]
                                FilterChip(
                                    selected = selectedFilter == index,
                                    onClick = { selectedFilter = index },
                                    label = { Text(filter.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ClickitPeach,
                                        selectedLabelColor = ClickitMidnight
                                    ),
                                    modifier = Modifier.testTag("active_filter_${filter.name.lowercase()}")
                                )
                            }
                        }

                        val filtered = orders.filter { booking ->
                            val st = BookingStatus.fromRaw(booking.status)
                            when (filters[selectedFilter]) {
                                ActiveOrderFilter.ALL -> true
                                ActiveOrderFilter.NEEDS_ACTION -> booking.needsUserAttention()
                                ActiveOrderFilter.IN_SERVICE -> st == BookingStatus.RECEIVED_BY_WORKER || st == BookingStatus.IN_PROGRESS
                                ActiveOrderFilter.IN_TRANSIT -> st in listOf(
                                    BookingStatus.PICKUP_SCHEDULED,
                                    BookingStatus.PICKED_UP,
                                    BookingStatus.READY,
                                    BookingStatus.RETURN_SCHEDULED,
                                    BookingStatus.OUT_FOR_DELIVERY
                                )
                            }
                        }

                        if (filtered.isEmpty()) {
                            Box(Modifier.padding(16.dp)) {
                                EmptyStateCard(
                                    icon = Icons.Filled.Schedule,
                                    title = "No orders in ${filters[selectedFilter].label}",
                                    subtitle = "Switch back to All Active to see your ongoing bookings.",
                                    actionLabel = "Show All Active",
                                    onAction = { selectedFilter = 0 }
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val tasksList = (customerTasksState as? UiState.Success)?.data.orEmpty()
                                val mapBooking = filtered.firstOrNull { bk ->
                                    tasksList.any { it.bookingId == bk.id && it.status !in listOf("handed_over", "delivered", "exception") }
                                } ?: filtered.firstOrNull {
                                    it.fulfilmentMethod == FulfilmentMethod.CLICKIT_PICKUP_RETURN.key
                                } ?: filtered.firstOrNull()
                                val mapTask = mapBooking?.let { bk ->
                                    tasksList.firstOrNull { it.bookingId == bk.id && it.status !in listOf("handed_over", "delivered", "exception") }
                                        ?: tasksList.firstOrNull { it.bookingId == bk.id }
                                }
                                if (mapBooking != null) {
                                    item(key = "live_smart_batch_map") {
                                        SmartBatchLiveMap(
                                            booking = mapBooking,
                                            task = mapTask,
                                            mapHeight = 220.dp
                                        )
                                    }
                                }

                                items(filtered, key = { it.id }) { booking ->
                                    ActiveOrderStatusTrackerCard(
                                        booking = booking,
                                        onClick = { onOrderClick(booking) }
                                    )
                                }
                                item { Spacer(Modifier.height(16.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveOrdersHomeSection(
    activeOrdersState: UiState<List<Booking>>,
    onOrderClick: (Booking) -> Unit,
    onViewAllClick: () -> Unit,
    onRetry: () -> Unit = {}
) {
    when (activeOrdersState) {
        is UiState.Loading -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                border = BorderStroke(1.dp, ClickitSlate),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = ClickitPeach,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Checking active orders...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClickitMutedText
                    )
                }
            }
        }
        is UiState.Error -> {
            ErrorRetryCard(
                message = activeOrdersState.message,
                onRetry = onRetry
            )
        }
        is UiState.Success -> {
            val activeOrders = activeOrdersState.data
            if (activeOrders.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Active Orders (${activeOrders.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = ClickitIvory
                            )
                            LiveStatusBadge()
                        }
                        TextButton(
                            onClick = onViewAllClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_active_orders_screen_button")
                        ) {
                            Text("Live Tracker", color = ClickitPeach)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = ClickitPeach,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    ActiveOrderStatusTrackerCard(
                        booking = activeOrders.first(),
                        onClick = { onOrderClick(activeOrders.first()) }
                    )

                    if (activeOrders.size > 1) {
                        OutlinedButton(
                            onClick = onViewAllClick,
                            border = BorderStroke(1.dp, ClickitPeach.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("view_all_active_orders_button")
                        ) {
                            Text(
                                text = "View all ${activeOrders.size} active orders",
                                color = ClickitPeach
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveOrderStatusTrackerCard(
    booking: Booking,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = BookingStatus.fromRaw(booking.status)
    val needsAttention = booking.needsUserAttention()
    val borderColor = when {
        status == BookingStatus.DISPUTED -> ClickitCrimson
        status == BookingStatus.QUOTE_PENDING -> ClickitWarning
        else -> ClickitPeach.copy(alpha = 0.45f)
    }

    val pickupOtpVisible = status in listOf(BookingStatus.PICKUP_SCHEDULED, BookingStatus.CONFIRMED) &&
        booking.handoverOtp.isNotBlank()
    val deliveryOtpVisible = status in listOf(
        BookingStatus.READY,
        BookingStatus.RETURN_SCHEDULED,
        BookingStatus.OUT_FOR_DELIVERY,
        BookingStatus.DELIVERED
    ) && booking.deliveryOtp.isNotBlank()

    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_order_card_${booking.id}")
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top bar: category + parcel code + status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryBadge(booking.category)
                    Text(
                        text = booking.parcelCode,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = ClickitPeach
                    )
                }
                BookingStatusBadge(booking.status)
            }

            // Shop & Items summary
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = booking.shopName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ClickitIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = booking.itemsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = ClickitMutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Progress stepper or dispute notice
            if (status == BookingStatus.DISPUTED) {
                Surface(
                    color = ClickitBurgundy.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ClickitCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ReportProblem,
                            contentDescription = null,
                            tint = ClickitPeachSoft,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Support review active — tap to view updates",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClickitIvory
                        )
                    }
                }
            } else {
                SmartBatchVisualTracker(
                    status = status,
                    isSmartBatch = booking.batchOptIn || booking.fulfilmentMethod == FulfilmentMethod.CLICKIT_PICKUP_RETURN.key,
                    slotWindowLabel = booking.slotWindowLabel
                )
            }

            // Current milestone & action callouts
            Surface(
                color = ClickitMidnight.copy(alpha = 0.75f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ClickitSubtleLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = booking.milestone.ifBlank { status.label },
                            style = MaterialTheme.typography.labelLarge,
                            color = ClickitInfo,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatInr(booking.totalMinor),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ClickitPeach
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = ClickitMutedText,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = booking.slotWindowLabel.ifBlank { "Scheduled" },
                                style = MaterialTheme.typography.labelSmall,
                                color = ClickitMutedText
                            )
                        }

                        val payColor = if (booking.paymentStatus == "paid") ClickitSuccess else ClickitWarning
                        val payBg = if (booking.paymentStatus == "paid") ClickitSuccessContainer else ClickitWarningContainer
                        val payLabel = if (booking.paymentStatus == "paid") {
                            booking.paymentRef.takeIf { it.isNotBlank() }?.let { "Paid • RuPay" } ?: "Paid • RuPay"
                        } else {
                            "Pay via RuPay"
                        }
                        Surface(color = payBg, shape = RoundedCornerShape(50)) {
                            Text(
                                text = payLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = payColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Inline OTP or quote approval badge
            if (status == BookingStatus.QUOTE_PENDING || pickupOtpVisible || deliveryOtpVisible || needsAttention) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (status == BookingStatus.QUOTE_PENDING) {
                        Surface(
                            color = ClickitWarningContainer,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ClickitWarning.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = ClickitWarning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Review Quote",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ClickitWarning
                                )
                            }
                        }
                    }

                    if (pickupOtpVisible) {
                        OtpInlinePill(label = "Pickup OTP", code = booking.handoverOtp)
                    } else if (deliveryOtpVisible) {
                        OtpInlinePill(label = "Delivery OTP", code = booking.deliveryOtp)
                    }

                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Track",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ClickitPeach
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open order details",
                            tint = ClickitPeach,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpInlinePill(label: String, code: String) {
    Surface(
        color = ClickitInfoContainer,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ClickitInfo.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = ClickitInfo,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "$label: $code",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ClickitInfo
            )
        }
    }
}

@Composable
private fun OrderStatusSummaryChip(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ClickitSlateDeep,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ClickitSlate),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ClickitMutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiveStatusBadge() {
    val transition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        color = ClickitSuccessContainer,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, ClickitSuccess.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(ClickitSuccess)
            )
            Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ClickitSuccess
            )
        }
    }
}
