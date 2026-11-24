package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.DefaultPickupSlots
import com.example.data.FulfilmentMethod
import com.example.data.SavedAddress
import com.example.data.ServiceCategory
import com.example.data.Shop
import com.example.data.ShopService
import com.example.data.formatInr
import com.example.ui.UiState
import com.example.ui.components.BookingStatusBadge
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.NeonIconBadge
import com.example.ui.components.PriceBreakdownCard
import com.example.ui.components.SmartBatchCapacityBar
import com.example.ui.theme.ClickitBurgundy
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
fun CustomerHomeScreen(
    addressesState: UiState<List<SavedAddress>>,
    shopsState: UiState<List<Shop>>,
    activeOrdersState: UiState<List<Booking>>,
    onCategoryClick: (ServiceCategory) -> Unit,
    onExploreClick: () -> Unit,
    onShopClick: (Shop) -> Unit,
    onBookingClick: (Booking) -> Unit,
    onViewActiveOrdersClick: () -> Unit,
    onManageAddressesClick: () -> Unit,
    onRetry: () -> Unit = {}
) {
    val defaultAddress = (addressesState as? UiState.Success)?.data?.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = ClickitSlateDeep,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ClickitSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_address_bar")
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onManageAddressesClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        NeonIconBadge(
                            icon = Icons.Filled.LocationOn,
                            tint = ClickitPeach,
                            glowColor = ClickitPeach,
                            size = 36.dp,
                            iconSize = 18.dp,
                            contentDescription = "Service address"
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = defaultAddress?.label ?: "Add Address",
                                style = MaterialTheme.typography.titleSmall,
                                color = ClickitIvory
                            )
                            Text(
                                text = defaultAddress?.formattedSummary() ?: "Tap to add address",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitMutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text("Change", style = MaterialTheme.typography.labelMedium, color = ClickitPeach)
                }
            }
        }

        item {
            Surface(
                color = ClickitSlateDeep,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ClickitSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_trigger")
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onExploreClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search services", tint = ClickitMutedText)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Search laundry, tailor, cobbler...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClickitMutedText
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onExploreClick)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_home),
                    contentDescription = "Clickit local services",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(ClickitMidnight.copy(alpha = 0.88f), ClickitMidnight.copy(alpha = 0.25f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(18.dp)
                        .fillMaxWidth(0.75f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(color = ClickitBurgundy, shape = RoundedCornerShape(50)) {
                        Text(
                            text = "SMART BATCH • ₹2/CLOTH",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClickitPeachSoft,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Local Dukaans nearby",
                        style = MaterialTheme.typography.headlineSmall,
                        color = ClickitIvory
                    )
                }
            }
        }

        item {
            ActiveOrdersHomeSection(
                activeOrdersState = activeOrdersState,
                onOrderClick = onBookingClick,
                onViewAllClick = onViewActiveOrdersClick,
                onRetry = onRetry
            )
        }

        item {
            Text("Services", style = MaterialTheme.typography.titleLarge, color = ClickitIvory)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ServiceCategory.entries) { category ->
                    val imgRes = when (category) {
                        ServiceCategory.LAUNDRY -> R.drawable.img_cat_laundry
                        ServiceCategory.TAILORING -> R.drawable.img_cat_tailoring
                        ServiceCategory.FOOTWEAR -> R.drawable.img_cat_footwear
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                        border = BorderStroke(1.dp, ClickitSlate),
                        modifier = Modifier
                            .width(156.dp)
                            .testTag("category_card_${category.key}")
                            .clickable { onCategoryClick(category) }
                    ) {
                        Column {
                            Image(
                                painter = painterResource(id = imgRes),
                                contentDescription = category.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(88.dp)
                            )
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(category.label, style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                                Text(
                                    category.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClickitMutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nearby Shops", style = MaterialTheme.typography.titleLarge, color = ClickitIvory)
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelLarge,
                    color = ClickitPeach,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onExploreClick)
                )
            }
        }

        when (shopsState) {
            is UiState.Loading -> item { LoadingStateView("Finding nearby local shops...") }
            is UiState.Error -> item { ErrorRetryCard(shopsState.message, onRetry = onRetry) }
            is UiState.Success -> {
                if (shopsState.data.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Storefront,
                            title = "No shops yet",
                            subtitle = "Local shops will appear here."
                        )
                    }
                } else {
                    items(shopsState.data, key = { it.id }) { shop ->
                        ShopSummaryCard(shop = shop, onClick = { onShopClick(shop) })
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerExploreScreen(
    shopsState: UiState<List<Shop>>,
    searchQuery: String,
    selectedCategory: ServiceCategory?,
    batchOnly: Boolean,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (ServiceCategory?) -> Unit,
    onBatchOnlyChange: (Boolean) -> Unit,
    onShopClick: (Shop) -> Unit,
    onRetry: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search shop name, locality, or craft...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClickitPeach,
                    unfocusedBorderColor = ClickitSlate,
                    focusedTextColor = ClickitIvory,
                    unfocusedTextColor = ClickitIvory,
                    focusedContainerColor = ClickitSlateDeep,
                    unfocusedContainerColor = ClickitSlateDeep
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategoryChange(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ClickitPeach,
                            selectedLabelColor = ClickitMidnight
                        )
                    )
                }
                items(ServiceCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryChange(if (selectedCategory == cat) null else cat) },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ClickitPeach,
                            selectedLabelColor = ClickitMidnight
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = batchOnly,
                        onClick = { onBatchOnlyChange(!batchOnly) },
                        label = { Text("Smart Batch") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ClickitInfo,
                            selectedLabelColor = ClickitMidnight
                        )
                    )
                }
            }
        }

        when (shopsState) {
            is UiState.Loading -> LoadingStateView("Loading marketplace catalogue...")
            is UiState.Error -> Box(Modifier.padding(16.dp)) {
                ErrorRetryCard(shopsState.message, onRetry = onRetry)
            }
            is UiState.Success -> {
                val filtered = shopsState.data.filter { shop ->
                    val matchesQuery = searchQuery.isBlank() ||
                        shop.shopName.contains(searchQuery, ignoreCase = true) ||
                        shop.locality.contains(searchQuery, ignoreCase = true) ||
                        shop.tagline.contains(searchQuery, ignoreCase = true)
                    val matchesCat = selectedCategory == null || shop.category == selectedCategory.key
                    val matchesBatch = !batchOnly || shop.supportsClickitBatch
                    matchesQuery && matchesCat && matchesBatch
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Filled.Search,
                            title = "No matching providers",
                            subtitle = "Try clearing filters or searching another neighborhood service.",
                            actionLabel = "Reset Filters",
                            onAction = {
                                onSearchChange("")
                                onCategoryChange(null)
                                onBatchOnlyChange(false)
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { shop ->
                            ShopSummaryCard(shop = shop, onClick = { onShopClick(shop) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopSummaryCard(
    shop: Shop,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
        border = BorderStroke(1.dp, ClickitSlate),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_card_${shop.id}")
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    CategoryBadge(shop.category)
                    Spacer(Modifier.width(8.dp))
                    if (shop.verified) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = "Verified Shop",
                            tint = ClickitSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = if (shop.openStatus) "Open Now" else "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (shop.openStatus) ClickitSuccess else ClickitPeachSoft
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = ClickitPeach, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${shop.ratingAvg} (${shop.ratingCount})",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClickitIvory
                    )
                }
            }

            Text(shop.shopName, style = MaterialTheme.typography.titleMedium, color = ClickitIvory)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(shop.locality, style = MaterialTheme.typography.labelMedium, color = ClickitIvory)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = ClickitMutedText, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${shop.turnaroundLabel} • From ${formatInr(shop.startingPriceMinor)}/${shop.startingPriceUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClickitPeachSoft
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open storefront",
                    tint = ClickitPeach
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopBookingBottomSheet(
    shop: Shop,
    servicesState: UiState<List<ShopService>>,
    addresses: List<SavedAddress>,
    customerName: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {},
    onSubmitBooking: (
        service: ShopService,
        quantity: Int,
        weightKg: Double,
        fulfilment: FulfilmentMethod,
        slotIndex: Int,
        batchOptIn: Boolean,
        address: SavedAddress?,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val services = (servicesState as? UiState.Success)?.data ?: emptyList()

    var selectedServiceId by remember(shop.id) { mutableStateOf<String?>(null) }
    val selectedService = services.firstOrNull { it.id == selectedServiceId } ?: services.firstOrNull()
    var quantity by remember(shop.id, selectedService?.id) {
        mutableIntStateOf(selectedService?.minQty?.toInt()?.coerceAtLeast(1) ?: 1)
    }
    var weightText by remember(shop.id) { mutableStateOf("2.5") }
    var itemNotes by remember(shop.id) { mutableStateOf("") }
    var selectedFulfilment by remember(shop.id) {
        mutableStateOf(
            when {
                shop.supportsClickitBatch -> FulfilmentMethod.CLICKIT_PICKUP_RETURN
                shop.supportsWorkerDelivery -> FulfilmentMethod.WORKER_DELIVERY
                else -> FulfilmentMethod.SHOP_DROP
            }
        )
    }
    var selectedSlotIndex by remember(shop.id) { mutableIntStateOf(0) }
    var batchOptIn by remember(shop.id) { mutableStateOf(true) }
    var selectedAddressId by remember(shop.id) { mutableStateOf<String?>(null) }
    val selectedAddress = addresses.firstOrNull { it.id == selectedAddressId } ?: addresses.firstOrNull()
    var quickHouseNo by remember(shop.id) { mutableStateOf("") }
    var quickStreet by remember(shop.id) { mutableStateOf("") }
    var quickLocality by remember(shop.id) { mutableStateOf(shop.locality) }

    val availableFulfilments = remember(shop) {
        buildList {
            if (shop.supportsClickitBatch) add(FulfilmentMethod.CLICKIT_PICKUP_RETURN)
            if (shop.supportsWorkerDelivery) add(FulfilmentMethod.WORKER_DELIVERY)
            if (shop.supportsShopDrop) add(FulfilmentMethod.SHOP_DROP)
        }.ifEmpty { listOf(FulfilmentMethod.SHOP_DROP) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ClickitMidnight,
        contentColor = ClickitIvory
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
                    CategoryBadge(shop.category)
                    Spacer(Modifier.height(4.dp))
                    Text(shop.shopName, style = MaterialTheme.typography.headlineSmall, color = ClickitIvory)
                    Text("${shop.locality} • ${shop.hoursSummary}", style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = ClickitIvory)
                }
            }

            HorizontalDivider(color = ClickitSubtleLine)

            Text("1. Service", style = MaterialTheme.typography.titleMedium, color = ClickitPeach)
            when (servicesState) {
                is UiState.Loading -> LoadingStateView("Loading services...")
                is UiState.Error -> ErrorRetryCard(servicesState.message, onRetry = onRetry)
                is UiState.Success -> {
                    if (services.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Filled.Storefront,
                            title = "No active services",
                            subtitle = "Check back soon."
                        )
                    } else {
                        services.forEach { svc ->
                            val isSelected = selectedService?.id == svc.id
                            Surface(
                                color = if (isSelected) ClickitBurgundy.copy(alpha = 0.55f) else ClickitSlateDeep,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isSelected) ClickitPeach else ClickitSlate),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("service_option_${svc.id}")
                                    .clickable {
                                        selectedServiceId = svc.id
                                        quantity = svc.minQty.toInt().coerceAtLeast(1)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedServiceId = svc.id
                                            quantity = svc.minQty.toInt().coerceAtLeast(1)
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = ClickitPeach)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(svc.name, style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                                        Text("${svc.turnaroundHours}h turnaround", style = MaterialTheme.typography.labelSmall, color = ClickitMutedText)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${formatInr(svc.priceMinor)}/${svc.unit}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ClickitPeach
                                        )
                                        Text(
                                            text = "${svc.turnaroundHours}h ETA",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ClickitMutedText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedService != null) {
                val svc = selectedService
                val minQty = svc.minQty.toInt().coerceAtLeast(1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Quantity (${svc.unit}s)", style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                        Text("Minimum $minQty ${svc.unit}", style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (quantity > minQty) quantity-- },
                            modifier = Modifier.testTag("qty_minus_button")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", tint = ClickitPeach)
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = ClickitIvory,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.testTag("qty_plus_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase quantity", tint = ClickitPeach)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Est. Weight (kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.4f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClickitPeach,
                            unfocusedBorderColor = ClickitSlate,
                            focusedTextColor = ClickitIvory,
                            unfocusedTextColor = ClickitIvory
                        )
                    )
                    OutlinedTextField(
                        value = itemNotes,
                        onValueChange = { itemNotes = it },
                        label = { Text("Item Notes (optional)") },
                        placeholder = { Text("Separate whites, delicate hem...") },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClickitPeach,
                            unfocusedBorderColor = ClickitSlate,
                            focusedTextColor = ClickitIvory,
                            unfocusedTextColor = ClickitIvory
                        )
                    )
                }

                Text("2. Pickup & Delivery", style = MaterialTheme.typography.titleMedium, color = ClickitPeach)
                availableFulfilments.forEach { method ->
                    val selected = selectedFulfilment == method
                    Surface(
                        color = if (selected) ClickitSlate else ClickitSlateDeep,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (selected) ClickitPeach else ClickitSubtleLine),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fulfilment_${method.key}")
                            .clickable { selectedFulfilment = method }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { selectedFulfilment = method },
                                colors = RadioButtonDefaults.colors(selectedColor = ClickitPeach)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(method.label, style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                                Text(method.subtitle, style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                            }
                        }
                    }
                }

                if (selectedFulfilment == FulfilmentMethod.CLICKIT_PICKUP_RETURN) {
                    DefaultPickupSlots.forEachIndexed { idx, slot ->
                        val isSlotSelected = selectedSlotIndex == idx
                        Surface(
                            color = if (isSlotSelected) ClickitSlateDeep else ClickitMidnight,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isSlotSelected) ClickitPeach else ClickitSlate),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slot_${slot.id}")
                                .clickable { selectedSlotIndex = idx }
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(slot.windowLabel, style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                                    Text(slot.statusLabel, style = MaterialTheme.typography.labelSmall, color = ClickitInfo)
                                }
                                if (slot.batchEligible) {
                                    SmartBatchCapacityBar(
                                        filledKg = slot.batchFilledKg,
                                        capacityKg = slot.batchCapacityKg,
                                        stopsCount = slot.batchStopsCount
                                    )
                                }
                            }
                        }
                    }

                    val chosenSlot = DefaultPickupSlots[selectedSlotIndex]
                    if (chosenSlot.batchEligible) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = batchOptIn,
                                onCheckedChange = { batchOptIn = it },
                                colors = CheckboxDefaults.colors(checkedColor = ClickitPeach, checkmarkColor = ClickitMidnight)
                            )
                            Text(
                                text = "Smart Batch pickup (₹2/cloth)",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitIvory
                            )
                        }
                    }

                    if (addresses.isNotEmpty()) {
                        if (addresses.size > 1) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(addresses, key = { it.id }) { addr ->
                                    FilterChip(
                                        selected = selectedAddress?.id == addr.id,
                                        onClick = { selectedAddressId = addr.id },
                                        label = { Text(addr.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ClickitPeach,
                                            selectedLabelColor = ClickitMidnight
                                        )
                                    )
                                }
                            }
                        }
                        if (selectedAddress != null) {
                            Text(
                                text = "Pickup: ${selectedAddress.label} • ${selectedAddress.locality}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitMutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (selectedFulfilment != FulfilmentMethod.SHOP_DROP && addresses.isEmpty()) {
                    Text("Pickup Address", style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                    OutlinedTextField(
                        value = quickHouseNo,
                        onValueChange = { quickHouseNo = it },
                        label = { Text("House / Flat No *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClickitPeach,
                            unfocusedBorderColor = ClickitSlate,
                            focusedTextColor = ClickitIvory,
                            unfocusedTextColor = ClickitIvory
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = quickStreet,
                            onValueChange = { quickStreet = it },
                            label = { Text("Street *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ClickitPeach,
                                unfocusedBorderColor = ClickitSlate,
                                focusedTextColor = ClickitIvory,
                                unfocusedTextColor = ClickitIvory
                            )
                        )
                        OutlinedTextField(
                            value = quickLocality,
                            onValueChange = { quickLocality = it },
                            label = { Text("Area *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ClickitPeach,
                                unfocusedBorderColor = ClickitSlate,
                                focusedTextColor = ClickitIvory,
                                unfocusedTextColor = ClickitIvory
                            )
                        )
                    }
                }

                val slot = DefaultPickupSlots[selectedSlotIndex]
                val subtotal = svc.priceMinor * quantity.toLong()
                val delivery = when (selectedFulfilment) {
                    FulfilmentMethod.SHOP_DROP -> 0L
                    FulfilmentMethod.WORKER_DELIVERY -> 2500L
                    FulfilmentMethod.CLICKIT_PICKUP_RETURN -> {
                        val perCloth = quantity.toLong() * 200L
                        if (batchOptIn && slot.batchEligible) perCloth.coerceAtLeast(1000L) else 2500L
                    }
                }
                val platformFee = 100L
                val total = subtotal + delivery + platformFee

                PriceBreakdownCard(
                    serviceSubtotalMinor = subtotal,
                    deliveryFeeMinor = delivery,
                    platformFeeMinor = platformFee,
                    totalMinor = total,
                    quoteStatus = "awaiting_worker_confirmation"
                )

                val hasAddressForFulfilment = selectedFulfilment == FulfilmentMethod.SHOP_DROP ||
                    selectedAddress != null ||
                    (quickHouseNo.isNotBlank() && quickStreet.isNotBlank() && quickLocality.isNotBlank())

                Button(
                    onClick = {
                        val weight = weightText.toDoubleOrNull() ?: 2.0
                        val resolvedAddress = selectedAddress ?: if (selectedFulfilment != FulfilmentMethod.SHOP_DROP) {
                            SavedAddress(
                                label = "Pickup",
                                houseNo = quickHouseNo.trim(),
                                street = quickStreet.trim(),
                                locality = quickLocality.trim(),
                                city = ""
                            )
                        } else null
                        onSubmitBooking(
                            svc,
                            quantity,
                            weight,
                            selectedFulfilment,
                            selectedSlotIndex,
                            batchOptIn,
                            resolvedAddress,
                            itemNotes
                        )
                    },
                    enabled = !isBusy && shop.openStatus && hasAddressForFulfilment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClickitPeach,
                        contentColor = ClickitMidnight
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_booking_button")
                ) {
                    Text(
                        text = when {
                            !shop.openStatus -> "Shop Currently Paused"
                            !hasAddressForFulfilment -> "Enter Pickup Address"
                            else -> "Confirm Booking • ${formatInr(total)}"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
