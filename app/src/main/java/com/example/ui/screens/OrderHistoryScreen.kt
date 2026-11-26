package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.FulfilmentMethod
import com.example.data.ServiceCategory
import com.example.data.formatInr
import com.example.ui.UiState
import com.example.ui.components.BookingStatusBadge
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.ClickitBorder
import com.example.ui.theme.ClickitBorderStrong
import com.example.ui.theme.ClickitBrandTeal
import com.example.ui.theme.ClickitLightBg
import com.example.ui.theme.ClickitLightSurface
import com.example.ui.theme.ClickitSuccess
import com.example.ui.theme.ClickitSuccessContainer
import com.example.ui.theme.ClickitTextPrimary
import com.example.ui.theme.ClickitTextSecondary
import com.example.ui.theme.ClickitWarning
import com.example.ui.theme.ClickitWarningContainer

data class OrderHistoryStats(
    val completedCount: Int,
    val totalSpentMinor: Long,
    val smartBatchCount: Int
)

fun filterOrderHistory(
    orders: List<Booking>,
    category: ServiceCategory?
): List<Booking> = orders.filter { booking ->
    BookingStatus.fromRaw(booking.status) == BookingStatus.COMPLETED &&
        (category == null || booking.category == category.key)
}

fun summarizeOrderHistory(orders: List<Booking>): OrderHistoryStats {
    val completed = orders.filter { BookingStatus.fromRaw(it.status) == BookingStatus.COMPLETED }
    return OrderHistoryStats(
        completedCount = completed.size,
        totalSpentMinor = completed.sumOf { it.totalMinor },
        smartBatchCount = completed.count { it.batchOptIn }
    )
}

@Composable
fun OrderHistoryScreen(
    orderHistoryState: UiState<List<Booking>>,
    onOrderClick: (Booking) -> Unit,
    onReorderClick: (Booking) -> Unit,
    onExploreClick: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClickitLightBg)
    ) {
        Surface(color = ClickitLightSurface, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("order_history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ClickitTextPrimary
                        )
                    }
                    Text(
                        text = "Order History",
                        style = MaterialTheme.typography.titleLarge,
                        color = ClickitTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = ClickitBorder)
            }
        }

        OrderHistoryList(
            orderHistoryState = orderHistoryState,
            onOrderClick = onOrderClick,
            onReorderClick = onReorderClick,
            onExploreClick = onExploreClick,
            onRetry = onRetry,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun OrderHistoryList(
    orderHistoryState: UiState<List<Booking>>,
    onOrderClick: (Booking) -> Unit,
    onReorderClick: (Booking) -> Unit,
    onExploreClick: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<ServiceCategory?>(null) }

    when (orderHistoryState) {
        is UiState.Loading -> Box(modifier = modifier.fillMaxSize()) {
            LoadingStateView("Loading...")
        }
        is UiState.Error -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ErrorRetryCard(orderHistoryState.message, onRetry = onRetry)
        }
        is UiState.Success -> {
            val allCompleted = orderHistoryState.data
            if (allCompleted.isEmpty()) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    EmptyStateCard(
                        icon = Icons.Filled.History,
                        title = "No past orders",
                        subtitle = "Completed orders appear here.",
                        actionLabel = "Explore",
                        onAction = onExploreClick
                    )
                }
            } else {
                val stats = remember(allCompleted) { summarizeOrderHistory(allCompleted) }
                val filtered = remember(allCompleted, selectedCategory) {
                    filterOrderHistory(allCompleted, selectedCategory)
                }

                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OrderHistorySummaryCard(stats = stats)
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    shape = RoundedCornerShape(8.dp),
                                    label = { Text("All (${allCompleted.size})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ClickitBrandTeal,
                                        selectedLabelColor = Color.White,
                                        containerColor = ClickitLightSurface,
                                        labelColor = ClickitTextPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedCategory == null,
                                        borderColor = ClickitBorder
                                    ),
                                    modifier = Modifier.testTag("history_filter_all")
                                )
                            }
                            items(ServiceCategory.entries) { cat ->
                                val chipLabel = if (cat == ServiceCategory.FOOTWEAR) "Footwear Repair" else cat.label
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                    shape = RoundedCornerShape(8.dp),
                                    label = { Text(chipLabel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ClickitBrandTeal,
                                        selectedLabelColor = Color.White,
                                        containerColor = ClickitLightSurface,
                                        labelColor = ClickitTextPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedCategory == cat,
                                        borderColor = ClickitBorder
                                    ),
                                    modifier = Modifier.testTag("history_filter_${cat.key}")
                                )
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                title = "No orders",
                                subtitle = "Try another category.",
                                actionLabel = "Reset",
                                onAction = { selectedCategory = null }
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { booking ->
                            OrderHistoryCard(
                                booking = booking,
                                onOrderClick = { onOrderClick(booking) },
                                onReorderClick = { onReorderClick(booking) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderHistorySummaryCard(stats: OrderHistoryStats) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ClickitBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryStatColumn(
                label = "Completed",
                value = stats.completedCount.toString(),
                accent = ClickitBrandTeal,
                modifier = Modifier.weight(1f)
            )
            HistoryStatColumn(
                label = "Spent",
                value = formatInr(stats.totalSpentMinor),
                accent = ClickitTextPrimary,
                modifier = Modifier.weight(1f)
            )
            HistoryStatColumn(
                label = "Smart Batch",
                value = stats.smartBatchCount.toString(),
                accent = ClickitSuccess,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HistoryStatColumn(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ClickitTextSecondary
        )
    }
}

@Composable
fun OrderHistoryCard(
    booking: Booking,
    onOrderClick: () -> Unit,
    onReorderClick: () -> Unit
) {
    val fulfilmentLabel = FulfilmentMethod.fromRaw(booking.fulfilmentMethod).label

    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ClickitBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_history_card_${booking.id}")
            .clickable(onClick = onOrderClick)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryBadge(booking.category)
                    Text(
                        text = booking.parcelCode,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
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
                Text(
                    text = "$fulfilmentLabel • ${booking.slotWindowLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClickitBrandTeal
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (booking.paymentStatus == "paid") {
                        Surface(
                            color = ClickitSuccessContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = ClickitSuccess,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = booking.paymentRef.ifBlank { "RuPay Paid" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ClickitSuccess,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (booking.ratingStars > 0L) {
                        Surface(
                            color = ClickitWarningContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Rated ${booking.ratingStars} stars",
                                    tint = ClickitWarning,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${booking.ratingStars}.0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ClickitWarning
                                )
                            }
                        }
                    }
                }

                Text(
                    text = formatInr(booking.totalMinor),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ClickitTextPrimary
                )
            }

            HorizontalDivider(color = ClickitBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOrderClick,
                    border = BorderStroke(1.dp, ClickitBorderStrong),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("history_receipt_${booking.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = ClickitTextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Receipt", color = ClickitTextPrimary)
                }

                Button(
                    onClick = onReorderClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClickitBrandTeal,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.25f)
                        .height(48.dp)
                        .testTag("reorder_button_${booking.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Re-order",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
