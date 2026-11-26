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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AppNotification
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.BookingTimelineEvent
import com.example.data.DeliveryTask
import com.example.data.FREE_DELIVERY_WEIGHT_THRESHOLD_KG
import com.example.data.FulfilmentMethod
import com.example.data.formatInr
import com.example.ui.UiState
import com.example.ui.components.BookingStatusBadge
import com.example.ui.components.CategoryBadge
import com.example.ui.components.DestructiveConfirmDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.GarmentScanTagCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.OtpCustodyCard
import com.example.ui.components.PriceBreakdownCard
import com.example.ui.components.SmartBatchLiveMap
import com.example.ui.components.SmartBatchVisualTracker
import com.example.ui.components.TrustProtectionStrip
import com.example.ui.theme.ClickitBorder
import com.example.ui.theme.ClickitBorderStrong
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
fun CustomerBookingsScreen(
    bookingsState: UiState<List<Booking>>,
    orderHistoryState: UiState<List<Booking>>,
    onBookingClick: (Booking) -> Unit,
    onReorderBooking: (Booking) -> Unit,
    onExploreClick: () -> Unit,
    onOpenLiveTracker: (() -> Unit)? = null,
    onRetry: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Active", "History", "Cancelled")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        ),
                        modifier = Modifier.testTag("bookings_tab_$idx")
                    )
                }
            }
            if (onOpenLiveTracker != null && selectedTab == 0) {
                OutlinedButton(
                    onClick = onOpenLiveTracker,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ClickitBorderStrong),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("bookings_live_tracker_button")
                ) {
                    Text("Live", style = MaterialTheme.typography.labelMedium, color = ClickitBrandTeal)
                }
            }
        }

        if (selectedTab == 1) {
            OrderHistoryList(
                orderHistoryState = orderHistoryState,
                onOrderClick = onBookingClick,
                onReorderClick = onReorderBooking,
                onExploreClick = onExploreClick,
                onRetry = onRetry,
                modifier = Modifier.weight(1f)
            )
        } else {
            when (bookingsState) {
                is UiState.Loading -> LoadingStateView("Loading...")
                is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorRetryCard(bookingsState.message, onRetry = onRetry) }
                is UiState.Success -> {
                    val filtered = bookingsState.data.filter { bk ->
                        val st = BookingStatus.fromRaw(bk.status)
                        when (selectedTab) {
                            0 -> st.isActive()
                            else -> st == BookingStatus.CANCELLED || st == BookingStatus.DISPUTED
                        }
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                title = "No orders",
                                subtitle = "None in ${tabs[selectedTab].lowercase()}.",
                                actionLabel = "Explore",
                                onAction = onExploreClick
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { booking ->
                                if (selectedTab == 0) {
                                    ActiveOrderStatusTrackerCard(
                                        booking = booking,
                                        onClick = { onBookingClick(booking) }
                                    )
                                } else {
                                    CustomerBookingCard(booking = booking, onClick = { onBookingClick(booking) })
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
private fun CustomerBookingCard(booking: Booking, onClick: () -> Unit) {
    val freeByWeight = booking.weightKg > FREE_DELIVERY_WEIGHT_THRESHOLD_KG &&
        booking.fulfilmentMethod != FulfilmentMethod.SHOP_DROP.key &&
        booking.deliveryFeeMinor == 0L
    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ClickitBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_booking_${booking.id}")
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryBadge(booking.category)
                    Text(booking.parcelCode, style = MaterialTheme.typography.labelMedium, color = ClickitBrandTeal)
                    if (freeByWeight) {
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
            Text(booking.shopName, style = MaterialTheme.typography.titleMedium, color = ClickitTextPrimary)
            Text(booking.itemsSummary, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(booking.milestone, style = MaterialTheme.typography.labelSmall, color = ClickitBrandTeal)
                Text(
                    text = "${formatInr(booking.totalMinor)} • ${booking.paymentStatus.uppercase()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ClickitTextPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomerBookingDetailSheet(
    booking: Booking,
    timelineState: UiState<List<BookingTimelineEvent>>,
    deliveryTask: DeliveryTask? = null,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onRespondQuote: (Boolean) -> Unit,
    onPay: () -> Unit,
    onCancel: (String) -> Unit,
    onConfirmDeliveryAndRate: (Int, String) -> Unit,
    onReportIssue: (String) -> Unit,
    onSimulatePickupAlert: (() -> Unit)? = null,
    onRetry: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCancelConfirm by remember { mutableStateOf(false) }
    var ratingStars by remember(booking.id) { mutableIntStateOf(if (booking.ratingStars > 0) booking.ratingStars.toInt() else 5) }
    var reviewText by remember(booking.id) { mutableStateOf(booking.reviewText) }
    var issueText by remember { mutableStateOf("") }
    var showIssueBox by remember { mutableStateOf(false) }

    val status = BookingStatus.fromRaw(booking.status)

    if (showCancelConfirm) {
        DestructiveConfirmDialog(
            title = "Cancel ${booking.parcelCode}?",
            message = "Paid orders are refunded automatically.",
            confirmLabel = "Cancel Order",
            onConfirm = {
                showCancelConfirm = false
                onCancel("Cancelled by customer")
            },
            onDismiss = { showCancelConfirm = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ClickitLightSurface,
        contentColor = ClickitTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(booking.shopName, style = MaterialTheme.typography.headlineSmall, color = ClickitTextPrimary)
                    Text("${booking.parcelCode} • ${booking.slotWindowLabel}", style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                }
                BookingStatusBadge(booking.status)
            }

            if (status != BookingStatus.CANCELLED && status != BookingStatus.DISPUTED) {
                SmartBatchVisualTracker(
                    status = status,
                    isSmartBatch = booking.batchOptIn || booking.fulfilmentMethod == FulfilmentMethod.CLICKIT_PICKUP_RETURN.key,
                    slotWindowLabel = booking.slotWindowLabel
                )
            }

            GarmentScanTagCard(
                parcelCode = booking.parcelCode,
                itemCount = booking.itemCount,
                weightKg = booking.weightKg,
                slotWindowLabel = booking.slotWindowLabel,
                isUrgent = booking.itemsSummary.contains("URGENT +25%", ignoreCase = true),
                isDelayed = booking.milestone.contains("Delay", ignoreCase = true)
            )

            if (status.isActive() && (booking.fulfilmentMethod == FulfilmentMethod.CLICKIT_PICKUP_RETURN.key || deliveryTask != null)) {
                SmartBatchLiveMap(
                    booking = booking,
                    task = deliveryTask,
                    mapHeight = 200.dp,
                    headerTitle = "Route Map"
                )
            }

            if (status == BookingStatus.QUOTE_PENDING) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitWarningContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitWarning.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Updated Quote", style = MaterialTheme.typography.titleSmall, color = ClickitWarning)
                        Text(
                            text = booking.quoteNote.ifBlank { "Updated by shop." },
                            style = MaterialTheme.typography.bodySmall,
                            color = ClickitTextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onRespondQuote(true) },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White),
                                modifier = Modifier.weight(1f).testTag("accept_quote_button")
                            ) {
                                Text("Accept ${formatInr(booking.totalMinor)}")
                            }
                            OutlinedButton(
                                onClick = { onRespondQuote(false) },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ClickitCrimson.copy(alpha = 0.55f)),
                                modifier = Modifier.weight(1f).testTag("decline_quote_button")
                            ) {
                                Text("Decline", color = ClickitCrimson)
                            }
                        }
                    }
                }
            }

            if (status in listOf(BookingStatus.PICKUP_SCHEDULED, BookingStatus.CONFIRMED)) {
                OtpCustodyCard(
                    parcelCode = booking.parcelCode,
                    otpLabel = "Pickup OTP",
                    otpCode = booking.handoverOtp,
                    helperNote = "Share on pickup"
                )
            }

            if (status in listOf(BookingStatus.READY, BookingStatus.RETURN_SCHEDULED, BookingStatus.OUT_FOR_DELIVERY, BookingStatus.DELIVERED)) {
                OtpCustodyCard(
                    parcelCode = booking.parcelCode,
                    otpLabel = "Delivery OTP",
                    otpCode = booking.deliveryOtp,
                    helperNote = "Share on delivery"
                )
            }

            val isFreeByWeight = booking.weightKg > FREE_DELIVERY_WEIGHT_THRESHOLD_KG &&
                booking.fulfilmentMethod != FulfilmentMethod.SHOP_DROP.key &&
                booking.deliveryFeeMinor == 0L
            val isUrgentOrder = booking.itemsSummary.contains("URGENT +25%", ignoreCase = true)

            PriceBreakdownCard(
                serviceSubtotalMinor = booking.serviceSubtotalMinor,
                deliveryFeeMinor = booking.deliveryFeeMinor,
                platformFeeMinor = booking.platformFeeMinor,
                totalMinor = booking.totalMinor,
                quoteStatus = booking.quoteStatus,
                isFreeDeliveryByWeight = isFreeByWeight,
                isUrgentService = isUrgentOrder
            )

            TrustProtectionStrip(compact = true)

            if (booking.paymentStatus == "pending" && status != BookingStatus.CANCELLED) {
                Button(
                    onClick = onPay,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pay_booking_button")
                ) {
                    Icon(Icons.Filled.Payment, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pay ${formatInr(booking.totalMinor)}")
                }
            } else if (booking.paymentStatus == "paid") {
                Surface(
                    color = ClickitSuccessContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitSuccess.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = ClickitSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Paid • ${booking.paymentRef.ifBlank { "RuPay" }}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ClickitSuccess
                        )
                    }
                }
            }

            if (status.isActive() && onSimulatePickupAlert != null) {
                OutlinedButton(
                    onClick = onSimulatePickupAlert,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitBorderStrong),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("detail_simulate_batch_alert_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = ClickitBrandTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Test Alert", color = ClickitBrandTeal)
                }
            }

            if (status in listOf(BookingStatus.READY, BookingStatus.OUT_FOR_DELIVERY, BookingStatus.DELIVERED) ||
                (status == BookingStatus.COMPLETED && booking.ratingStars == 0L)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitSuccess.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Rate Service", style = MaterialTheme.typography.titleSmall, color = ClickitSuccess)
                        Row {
                            (1..5).forEach { star ->
                                IconButton(onClick = { ratingStars = star }) {
                                    Icon(
                                        imageVector = if (star <= ratingStars) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = "$star stars",
                                        tint = ClickitWarning
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = { reviewText = it },
                            label = { Text("Feedback") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ClickitBrandTeal,
                                unfocusedBorderColor = ClickitBorder,
                                focusedTextColor = ClickitTextPrimary,
                                unfocusedTextColor = ClickitTextPrimary
                            )
                        )
                        Button(
                            onClick = { onConfirmDeliveryAndRate(ratingStars, reviewText) },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_receipt_button")
                        ) {
                            Text(if (status == BookingStatus.COMPLETED) "Submit" else "Complete")
                        }
                    }
                }
            }

            Text("Updates", style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
            when (timelineState) {
                is UiState.Loading -> LoadingStateView("Loading...")
                is UiState.Error -> ErrorRetryCard(timelineState.message, onRetry = onRetry)
                is UiState.Success -> {
                    timelineState.data.forEach { ev ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                color = ClickitBrandTeal,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(top = 5.dp).size(8.dp)
                            ) {}
                            Column {
                                Text(ev.title, style = MaterialTheme.typography.labelLarge, color = ClickitTextPrimary)
                                Text(ev.detail, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                            }
                        }
                    }
                }
            }

            if (status in listOf(BookingStatus.DELIVERED, BookingStatus.COMPLETED)) {
                OutlinedButton(
                    onClick = {
                        onReportIssue("Re-Service Request: Free touch-up / re-press requested for ${booking.parcelCode}")
                    },
                    enabled = !isBusy,
                    border = BorderStroke(1.dp, ClickitSuccess.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("request_reservice_button")
                ) {
                    Icon(Icons.Filled.Replay, contentDescription = null, tint = ClickitSuccess, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Request Touch-Up", color = ClickitSuccess)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (status in listOf(BookingStatus.REQUESTED, BookingStatus.ACCEPTED, BookingStatus.QUOTE_PENDING, BookingStatus.CONFIRMED, BookingStatus.PICKUP_SCHEDULED)) {
                    OutlinedButton(
                        onClick = { showCancelConfirm = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ClickitCrimson.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("cancel_booking_button")
                    ) {
                        Text("Cancel", color = ClickitCrimson)
                    }
                }
                OutlinedButton(
                    onClick = { showIssueBox = !showIssueBox },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitBorderStrong),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("support_booking_button")
                ) {
                    Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = ClickitBrandTeal, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Help", color = ClickitTextPrimary)
                }
            }

            if (showIssueBox) {
                val quickIssues = listOf(
                    "Re-press / Touch-up",
                    "Missing item check",
                    "Delivery timing",
                    "Billing question"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickIssues.forEach { preset ->
                        val selected = issueText.startsWith(preset)
                        FilterChip(
                            selected = selected,
                            onClick = { issueText = "$preset for ${booking.parcelCode}" },
                            shape = RoundedCornerShape(8.dp),
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ClickitBrandTeal,
                                selectedLabelColor = Color.White,
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
                OutlinedTextField(
                    value = issueText,
                    onValueChange = { issueText = it },
                    label = { Text("Issue details") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClickitBrandTeal,
                        unfocusedBorderColor = ClickitBorder,
                        focusedTextColor = ClickitTextPrimary,
                        unfocusedTextColor = ClickitTextPrimary
                    )
                )
                Button(
                    onClick = {
                        if (issueText.trim().length >= 5) {
                            onReportIssue(issueText.trim())
                            issueText = ""
                            showIssueBox = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Send")
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun CustomerActivityScreen(
    notificationsState: UiState<List<AppNotification>>,
    onNotificationClick: (AppNotification) -> Unit,
    onRetry: () -> Unit = {}
) {
    when (notificationsState) {
        is UiState.Loading -> LoadingStateView("Loading...")
        is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorRetryCard(notificationsState.message, onRetry = onRetry) }
        is UiState.Success -> {
            if (notificationsState.data.isEmpty()) {
                Box(Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.NotificationsNone,
                        title = "No notifications",
                        subtitle = "Order updates appear here."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notificationsState.data, key = { it.id }) { notif ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (notif.readFlag) ClickitLightSurface else ClickitBrandTealTint
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (notif.readFlag) ClickitBorder else ClickitBrandTeal.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNotificationClick(notif) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(notif.title, style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                                    if (!notif.readFlag) {
                                        Text("NEW", style = MaterialTheme.typography.labelSmall, color = ClickitBrandTeal)
                                    }
                                }
                                Text(notif.message, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

