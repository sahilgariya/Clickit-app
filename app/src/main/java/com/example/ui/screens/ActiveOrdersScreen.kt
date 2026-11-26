package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.ClickitBorder
import com.example.ui.theme.ClickitBorderStrong
import com.example.ui.theme.ClickitBrandTeal
import com.example.ui.theme.ClickitBrandTealTint
import com.example.ui.theme.ClickitCrimson
import com.example.ui.theme.ClickitDangerContainer
import com.example.ui.theme.ClickitLightBg
import com.example.ui.theme.ClickitLightSurface
import com.example.ui.theme.ClickitSuccess
import com.example.ui.theme.ClickitSuccessContainer
import com.example.ui.theme.ClickitTextPrimary
import com.example.ui.theme.ClickitTextSecondary
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
            .background(ClickitLightBg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            Surface(color = ClickitLightSurface, modifier = Modifier.fillMaxWidth()) {
                Column {
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
                                    tint = ClickitTextPrimary
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
                                        color = ClickitTextPrimary
                                    )
                                    LiveStatusBadge()
                                }
                            }
                        }

                        if (activeOrdersState is UiState.Success) {
                            Surface(
                                color = ClickitBrandTealTint,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "${activeOrdersState.data.size} live",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = ClickitBrandTeal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = ClickitBorder)
                }
            }

            when (activeOrdersState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingStateView("Loading...")
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
                        border = BorderStroke(1.dp, ClickitBorderStrong),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(48.dp)
                            .testTag("simulate_smart_batch_alert_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = ClickitBrandTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Test Alert",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = ClickitBrandTeal
                        )
                    }

                    if (orders.isEmpty()) {
                        Box(Modifier.padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.Filled.LocalShipping,
                                title = "No active orders",
                                subtitle = "Browse shops to place an order.",
                                actionLabel = "Explore",
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OrderStatusSummaryChip(
                                label = "Needs Action",
                                count = attentionCount,
                                accent = if (attentionCount > 0) ClickitWarning else ClickitTextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            OrderStatusSummaryChip(
                                label = "In Service",
                                count = inServiceCount,
                                accent = ClickitBrandTeal,
                                modifier = Modifier.weight(1f)
                            )
                            OrderStatusSummaryChip(
                                label = "In Transit",
                                count = transitCount,
                                accent = ClickitBrandTeal,
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
                                    shape = RoundedCornerShape(8.dp),
                                    label = { Text(filter.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ClickitBrandTeal,
                                        selectedLabelColor = Color.White,
                                        containerColor = ClickitLightSurface,
                                        labelColor = ClickitTextPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedFilter == index,
                                        borderColor = ClickitBorder
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
                                    title = "No orders",
                                    subtitle = "None in ${filters[selectedFilter].label}.",
                                    actionLabel = "Show All",
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

                                items(filtered.size, key = { filtered[it].id }) { idx ->
                                    val booking = filtered[idx]
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
                colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, ClickitBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = ClickitBrandTeal,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClickitTextSecondary
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
                                color = ClickitTextPrimary
                            )
                            LiveStatusBadge()
                        }
                        TextButton(
                            onClick = onViewAllClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_active_orders_screen_button")
                        ) {
                            Text("Live Map", color = ClickitBrandTeal)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = ClickitBrandTeal,
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
                            border = BorderStroke(1.dp, ClickitBorderStrong),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("view_all_active_orders_button")
                        ) {
                            Text(
                                text = "View All (${activeOrders.size})",
                                color = ClickitBrandTeal
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
        status == BookingStatus.DISPUTED -> ClickitCrimson.copy(alpha = 0.5f)
        status == BookingStatus.QUOTE_PENDING -> ClickitWarning.copy(alpha = 0.5f)
        else -> ClickitBorder
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
        colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_order_card_${booking.id}")
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = ClickitBrandTeal
                    )
                }
                BookingStatusBadge(booking.status)
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = booking.shopName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ClickitTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = booking.itemsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = ClickitTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (status == BookingStatus.DISPUTED) {
                Surface(
                    color = ClickitDangerContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitCrimson.copy(alpha = 0.4f)),
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
                            tint = ClickitCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Under review",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClickitCrimson
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

            Surface(
                color = ClickitLightBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ClickitBorder),
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
                            color = ClickitBrandTeal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatInr(booking.totalMinor),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ClickitTextPrimary
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
                                tint = ClickitTextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = booking.slotWindowLabel.ifBlank { "Scheduled" },
                                style = MaterialTheme.typography.labelSmall,
                                color = ClickitTextSecondary
                            )
                        }

                        val payColor = if (booking.paymentStatus == "paid") ClickitSuccess else ClickitWarning
                        val payBg = if (booking.paymentStatus == "paid") ClickitSuccessContainer else ClickitWarningContainer
                        val payLabel = if (booking.paymentStatus == "paid") "Paid • RuPay" else "Pay via RuPay"
                        Surface(color = payBg, shape = RoundedCornerShape(6.dp)) {
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

            if (status == BookingStatus.QUOTE_PENDING || pickupOtpVisible || deliveryOtpVisible || needsAttention) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (status == BookingStatus.QUOTE_PENDING) {
                        Surface(
                            color = ClickitWarningContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ClickitWarning.copy(alpha = 0.45f))
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
                            color = ClickitBrandTeal
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open order details",
                            tint = ClickitBrandTeal,
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
        color = ClickitBrandTealTint,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ClickitBrandTeal.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = ClickitBrandTeal,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "$label: $code",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ClickitBrandTeal
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
        color = ClickitLightSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ClickitBorder),
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
                color = ClickitTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiveStatusBadge() {
    Surface(
        color = ClickitSuccessContainer,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, ClickitSuccess.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
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
