package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.data.DefaultPickupSlots
import com.example.data.FREE_DELIVERY_WEIGHT_THRESHOLD_KG
import com.example.data.FulfilmentMethod
import com.example.data.SavedAddress
import com.example.data.ServiceCategory
import com.example.data.Shop
import com.example.data.ShopService
import com.example.data.calculateBookingFees
import com.example.data.formatInr
import com.example.ui.UiState
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.PriceBreakdownCard
import com.example.ui.components.SmartBatchCapacityBar
import com.example.ui.components.TrustProtectionStrip
import com.example.ui.theme.ClickitBorder
import com.example.ui.theme.ClickitBrandTeal
import com.example.ui.theme.ClickitBrandTealDark
import com.example.ui.theme.ClickitBrandTealTint
import com.example.ui.theme.ClickitCrimson
import com.example.ui.theme.ClickitInfo
import com.example.ui.theme.ClickitInfoContainer
import com.example.ui.theme.ClickitLightBg
import com.example.ui.theme.ClickitLightSurface
import com.example.ui.theme.ClickitLightSurfaceVar
import com.example.ui.theme.ClickitSuccess
import com.example.ui.theme.ClickitSuccessContainer
import com.example.ui.theme.ClickitTextPrimary
import com.example.ui.theme.ClickitTextSecondary
import com.example.ui.theme.ClickitWarning
import com.example.ui.theme.ClickitWarningContainer

fun filterShops(
    shops: List<Shop>,
    searchQuery: String,
    selectedCategory: ServiceCategory?,
    batchOnly: Boolean = false
): List<Shop> = shops.filter { shop ->
    val query = searchQuery.trim()
    val categoryLabel = ServiceCategory.fromRaw(shop.category).label
    val matchesQuery = query.isBlank() ||
        shop.shopName.contains(query, ignoreCase = true) ||
        shop.locality.contains(query, ignoreCase = true) ||
        shop.tagline.contains(query, ignoreCase = true) ||
        shop.category.contains(query, ignoreCase = true) ||
        categoryLabel.contains(query, ignoreCase = true) ||
        (shop.category == ServiceCategory.FOOTWEAR.key &&
            ("footwear repair".contains(query, ignoreCase = true) || "cobbler".contains(query, ignoreCase = true)))
    val matchesCat = selectedCategory == null || shop.category == selectedCategory.key
    val matchesBatch = !batchOnly || shop.supportsClickitBatch
    matchesQuery && matchesCat && matchesBatch
}

fun matchServiceForReorder(
    services: List<ShopService>,
    itemsSummary: String
): ShopService? {
    if (services.isEmpty()) return null
    val normalizedSummary = itemsSummary.trim().lowercase()
    return services.firstOrNull { svc ->
        svc.name.isNotBlank() && normalizedSummary.contains(svc.name.trim().lowercase())
    } ?: services.first()
}

fun extractNotesFromSummary(itemsSummary: String): String {
    val idx = itemsSummary.indexOf(" • ")
    return if (idx >= 0) itemsSummary.substring(idx + 3).trim() else ""
}

@Composable
fun CustomerHomeScreen(
    addressesState: UiState<List<SavedAddress>>,
    shopsState: UiState<List<Shop>>,
    activeOrdersState: UiState<List<Booking>>,
    searchQuery: String,
    selectedCategory: ServiceCategory?,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (ServiceCategory?) -> Unit,
    onExploreClick: () -> Unit,
    onShopClick: (Shop) -> Unit,
    onBookingClick: (Booking) -> Unit,
    onViewActiveOrdersClick: () -> Unit,
    onManageAddressesClick: () -> Unit,
    onLoadStarterShops: (() -> Unit)? = null,
    onRetry: () -> Unit = {}
) {
    val defaultAddress = (addressesState as? UiState.Success)?.data?.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search local service providers",
                            tint = ClickitTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.testTag("home_search_clear")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = ClickitTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClickitBrandTeal,
                        unfocusedBorderColor = ClickitBorder,
                        focusedTextColor = ClickitTextPrimary,
                        unfocusedTextColor = ClickitTextPrimary,
                        focusedContainerColor = ClickitLightSurface,
                        unfocusedContainerColor = ClickitLightSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onCategoryChange(null) },
                            label = { Text("All") },
                            shape = RoundedCornerShape(8.dp),
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
                            modifier = Modifier.testTag("home_filter_all")
                        )
                    }
                    items(ServiceCategory.entries) { cat ->
                        val chipLabel = if (cat == ServiceCategory.FOOTWEAR) "Footwear Repair" else cat.label
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategoryChange(if (selectedCategory == cat) null else cat) },
                            label = { Text(chipLabel) },
                            shape = RoundedCornerShape(8.dp),
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
                            modifier = Modifier.testTag("home_filter_${cat.key}")
                        )
                    }
                }
            }
        }

        item {
            Surface(
                color = ClickitLightSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ClickitBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_address_bar")
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onManageAddressesClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ClickitBrandTealTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Service address",
                                tint = ClickitBrandTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = defaultAddress?.label ?: "Address",
                                style = MaterialTheme.typography.labelLarge,
                                color = ClickitTextPrimary
                            )
                            Text(
                                text = defaultAddress?.formattedSummary() ?: "Set address",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ClickitBrandTeal
                    )
                }
            }
        }

        item {
            Surface(
                color = ClickitBrandTealTint,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ClickitBrandTeal.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExploreClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "FREE DELIVERY >5 KG",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ClickitBrandTealDark
                        )
                        Text(
                            text = "Laundry, tailoring & footwear",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ClickitTextPrimary
                        )
                    }
                    Surface(
                        color = ClickitBrandTeal,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Explore",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
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
            Text("Services", style = MaterialTheme.typography.titleLarge, color = ClickitTextPrimary)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ServiceCategory.entries) { category ->
                    val catIcon = when (category) {
                        ServiceCategory.LAUNDRY -> Icons.Filled.LocalLaundryService
                        ServiceCategory.TAILORING -> Icons.Filled.ContentCut
                        ServiceCategory.FOOTWEAR -> Icons.Filled.CleaningServices
                    }
                    val isSelected = selectedCategory == category
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ClickitBrandTealTint else ClickitLightSurface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) ClickitBrandTeal else ClickitBorder),
                        modifier = Modifier
                            .width(156.dp)
                            .testTag("category_card_${category.key}")
                            .clickable { onCategoryChange(if (isSelected) null else category) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSelected) ClickitBrandTeal else ClickitLightSurfaceVar),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = category.label,
                                    tint = if (isSelected) Color.White else ClickitBrandTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ClickitTextPrimary
                                )
                                Text(
                                    text = category.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClickitTextSecondary,
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
            val sectionTitle = selectedCategory?.let { cat ->
                val label = if (cat == ServiceCategory.FOOTWEAR) "Footwear Repair" else cat.label
                "$label Providers"
            } ?: "Nearby Shops"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sectionTitle, style = MaterialTheme.typography.titleLarge, color = ClickitTextPrimary)
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelLarge,
                    color = ClickitBrandTeal,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onExploreClick)
                )
            }
        }

        when (shopsState) {
            is UiState.Loading -> item { LoadingStateView("Loading shops...") }
            is UiState.Error -> item { ErrorRetryCard(shopsState.message, onRetry = onRetry) }
            is UiState.Success -> {
                val filteredShops = filterShops(
                    shops = shopsState.data,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory
                )
                if (filteredShops.isEmpty()) {
                    item {
                        val hasFilters = searchQuery.isNotBlank() || selectedCategory != null
                        EmptyStateCard(
                            icon = Icons.Filled.Storefront,
                            title = if (hasFilters) "No results" else "No shops",
                            subtitle = if (hasFilters) {
                                "Try another search or category."
                            } else {
                                "Load local shops to start."
                            },
                            actionLabel = when {
                                hasFilters -> "Reset"
                                onLoadStarterShops != null -> "Load Shops"
                                else -> null
                            },
                            onAction = when {
                                hasFilters -> {
                                    {
                                        onSearchChange("")
                                        onCategoryChange(null)
                                    }
                                }
                                onLoadStarterShops != null -> onLoadStarterShops
                                else -> null
                            }
                        )
                    }
                } else {
                    items(filteredShops, key = { it.id }) { shop ->
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
    onLoadStarterShops: (() -> Unit)? = null,
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
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ClickitTextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = ClickitTextSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClickitBrandTeal,
                    unfocusedBorderColor = ClickitBorder,
                    focusedTextColor = ClickitTextPrimary,
                    unfocusedTextColor = ClickitTextPrimary,
                    focusedContainerColor = ClickitLightSurface,
                    unfocusedContainerColor = ClickitLightSurface
                ),
                shape = RoundedCornerShape(10.dp),
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
                        shape = RoundedCornerShape(8.dp),
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
                        )
                    )
                }
                items(ServiceCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryChange(if (selectedCategory == cat) null else cat) },
                        label = { Text(cat.label) },
                        shape = RoundedCornerShape(8.dp),
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
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = batchOnly,
                        onClick = { onBatchOnlyChange(!batchOnly) },
                        label = { Text("Smart Batch") },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ClickitBrandTeal,
                            selectedLabelColor = Color.White,
                            containerColor = ClickitLightSurface,
                            labelColor = ClickitTextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = batchOnly,
                            borderColor = ClickitBorder
                        )
                    )
                }
            }
        }

        when (shopsState) {
            is UiState.Loading -> LoadingStateView("Loading...")
            is UiState.Error -> Box(Modifier.padding(16.dp)) {
                ErrorRetryCard(shopsState.message, onRetry = onRetry)
            }
            is UiState.Success -> {
                val filtered = filterShops(
                    shops = shopsState.data,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    batchOnly = batchOnly
                )
                if (filtered.isEmpty()) {
                    val hasFilters = searchQuery.isNotBlank() || selectedCategory != null || batchOnly
                    Box(Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Filled.Search,
                            title = if (hasFilters) "No results" else "No shops",
                            subtitle = if (hasFilters) {
                                "Try clearing filters."
                            } else {
                                "Load local shops to start."
                            },
                            actionLabel = when {
                                hasFilters -> "Reset"
                                onLoadStarterShops != null -> "Load Shops"
                                else -> null
                            },
                            onAction = when {
                                hasFilters -> {
                                    {
                                        onSearchChange("")
                                        onCategoryChange(null)
                                        onBatchOnlyChange(false)
                                    }
                                }
                                onLoadStarterShops != null -> onLoadStarterShops
                                else -> null
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopSummaryCard(
    shop: Shop,
    onClick: () -> Unit
) {
    val loadRatio = (shop.activeLoad.toFloat() / shop.dailyCapacity.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val isBusyToday = loadRatio >= 0.8f
    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ClickitBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_card_${shop.id}")
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = when {
                            !shop.openStatus -> "Paused"
                            isBusyToday -> "Busy"
                            else -> "Open"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            !shop.openStatus -> ClickitCrimson
                            isBusyToday -> ClickitWarning
                            else -> ClickitSuccess
                        }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = ClickitWarning, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${shop.ratingAvg} (${shop.ratingCount})",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClickitTextPrimary
                    )
                }
            }

            Text(
                text = shop.shopName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ClickitTextPrimary
            )

            if (shop.supportsClickitBatch || shop.supportsWorkerDelivery) {
                Text(
                    text = "Free delivery >5 kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClickitSuccess
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(shop.locality, style = MaterialTheme.typography.labelMedium, color = ClickitTextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = ClickitTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${shop.turnaroundLabel} • From ${formatInr(shop.startingPriceMinor)}/${shop.startingPriceUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClickitTextSecondary
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open storefront",
                    tint = ClickitTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShopBookingBottomSheet(
    shop: Shop,
    servicesState: UiState<List<ShopService>>,
    addresses: List<SavedAddress>,
    customerName: String,
    isBusy: Boolean,
    prefillBooking: Booking? = null,
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
        notes: String,
        urgentService: Boolean,
        saveNewAddress: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val services = (servicesState as? UiState.Success)?.data ?: emptyList()

    var selectedServiceId by remember(shop.id, prefillBooking?.id) { mutableStateOf<String?>(null) }
    val selectedService = services.firstOrNull { it.id == selectedServiceId } ?: services.firstOrNull()
    var quantity by remember(shop.id, prefillBooking?.id) {
        mutableIntStateOf(prefillBooking?.itemCount?.toInt()?.coerceAtLeast(1) ?: 1)
    }
    var hasAppliedPrefill by remember(shop.id, prefillBooking?.id) { mutableStateOf(false) }

    LaunchedEffect(services, prefillBooking?.id) {
        if (!hasAppliedPrefill && services.isNotEmpty()) {
            val matched = if (prefillBooking != null) {
                matchServiceForReorder(services, prefillBooking.itemsSummary)
            } else {
                services.firstOrNull()
            }
            if (matched != null) {
                selectedServiceId = matched.id
                val minQty = matched.minQty.toInt().coerceAtLeast(1)
                quantity = prefillBooking?.itemCount?.toInt()?.coerceAtLeast(minQty) ?: minQty
                hasAppliedPrefill = true
            }
        }
    }

    var weightText by remember(shop.id, prefillBooking?.id) {
        mutableStateOf(
            prefillBooking?.weightKg?.takeIf { it > 0.0 }?.toString() ?: "2.5"
        )
    }
    var itemNotes by remember(shop.id, prefillBooking?.id) {
        mutableStateOf(prefillBooking?.let { extractNotesFromSummary(it.itemsSummary) } ?: "")
    }
    val availableFulfilments = remember(shop) {
        buildList {
            if (shop.supportsClickitBatch) add(FulfilmentMethod.CLICKIT_PICKUP_RETURN)
            if (shop.supportsWorkerDelivery) add(FulfilmentMethod.WORKER_DELIVERY)
            if (shop.supportsShopDrop) add(FulfilmentMethod.SHOP_DROP)
        }.ifEmpty { listOf(FulfilmentMethod.SHOP_DROP) }
    }
    var selectedFulfilment by remember(shop.id, prefillBooking?.id) {
        val preferred = prefillBooking?.let { FulfilmentMethod.fromRaw(it.fulfilmentMethod) }
        mutableStateOf(
            if (preferred != null && preferred in availableFulfilments) {
                preferred
            } else {
                when {
                    shop.supportsClickitBatch -> FulfilmentMethod.CLICKIT_PICKUP_RETURN
                    shop.supportsWorkerDelivery -> FulfilmentMethod.WORKER_DELIVERY
                    else -> FulfilmentMethod.SHOP_DROP
                }
            }
        )
    }
    var selectedSlotIndex by remember(shop.id, prefillBooking?.id) {
        val matchedSlot = prefillBooking?.let { bk ->
            DefaultPickupSlots.indexOfFirst { it.windowLabel == bk.slotWindowLabel }.takeIf { it >= 0 }
        } ?: 0
        mutableIntStateOf(matchedSlot)
    }
    var batchOptIn by remember(shop.id, prefillBooking?.id) {
        mutableStateOf(prefillBooking?.batchOptIn ?: true)
    }
    var urgentService by remember(shop.id, prefillBooking?.id) {
        mutableStateOf(prefillBooking?.itemsSummary?.contains("URGENT +25%", ignoreCase = true) == true)
    }
    var selectedAddressId by remember(shop.id) { mutableStateOf<String?>(null) }
    val selectedAddress = addresses.firstOrNull { it.id == selectedAddressId } ?: addresses.firstOrNull()
    var quickHouseNo by remember(shop.id) { mutableStateOf("") }
    var quickStreet by remember(shop.id) { mutableStateOf("") }
    var quickLocality by remember(shop.id) { mutableStateOf(shop.locality) }
    var quickPostal by remember(shop.id) { mutableStateOf("") }
    var saveQuickAddress by remember(shop.id) { mutableStateOf(true) }

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
            if (prefillBooking != null) {
                Surface(
                    color = ClickitBrandTealTint,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClickitBrandTeal.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            contentDescription = null,
                            tint = ClickitBrandTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Re-ordering from ${prefillBooking.parcelCode}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = ClickitBrandTeal
                            )
                            Text(
                                text = prefillBooking.itemsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    CategoryBadge(shop.category)
                    Spacer(Modifier.height(4.dp))
                    Text(shop.shopName, style = MaterialTheme.typography.headlineSmall, color = ClickitTextPrimary)
                    Text("${shop.locality} • ${shop.hoursSummary}", style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = ClickitTextPrimary)
                }
            }

            HorizontalDivider(color = ClickitBorder)

            Text("Service", style = MaterialTheme.typography.titleMedium, color = ClickitTextPrimary)
            when (servicesState) {
                is UiState.Loading -> LoadingStateView("Loading...")
                is UiState.Error -> ErrorRetryCard(servicesState.message, onRetry = onRetry)
                is UiState.Success -> {
                    if (services.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Filled.Storefront,
                            title = "No services",
                            subtitle = "Unavailable right now."
                        )
                    } else {
                        services.forEach { svc ->
                            val isSelected = selectedService?.id == svc.id
                            Surface(
                                color = if (isSelected) ClickitBrandTealTint else ClickitLightSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSelected) ClickitBrandTeal else ClickitBorder),
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
                                        colors = RadioButtonDefaults.colors(selectedColor = ClickitBrandTeal)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(svc.name, style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                                        Text("${svc.turnaroundHours}h turnaround", style = MaterialTheme.typography.labelSmall, color = ClickitTextSecondary)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${formatInr(svc.priceMinor)}/${svc.unit}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ClickitBrandTeal
                                        )
                                        Text(
                                            text = "${svc.turnaroundHours}h ETA",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ClickitTextSecondary
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
                        Text("Quantity (${svc.unit}s)", style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                        Text("Minimum $minQty ${svc.unit}", style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (quantity > minQty) quantity-- },
                            modifier = Modifier.testTag("qty_minus_button")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", tint = ClickitBrandTeal)
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = ClickitTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.testTag("qty_plus_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase quantity", tint = ClickitBrandTeal)
                        }
                    }
                }

                val parsedWeightKg = weightText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                val weightPresets = listOf("2.5", "4.0", "5.5", "7.5")
                val carePresets = listOf("Delicate Wash", "No Bleach", "Light Starch", "Separate Whites", "Handle With Care")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Weight (kg)", style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                        Text(
                            text = "Free >5 kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClickitSuccess
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weightPresets.forEach { preset ->
                            val isFreePreset = (preset.toDoubleOrNull() ?: 0.0) > FREE_DELIVERY_WEIGHT_THRESHOLD_KG
                            val isSelected = weightText.trim() == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { weightText = preset },
                                shape = RoundedCornerShape(8.dp),
                                label = {
                                    Text(
                                        text = if (isFreePreset) "$preset kg • FREE" else "$preset kg",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isFreePreset) ClickitSuccess else ClickitBrandTeal,
                                    selectedLabelColor = Color.White,
                                    containerColor = ClickitLightSurface,
                                    labelColor = if (isFreePreset) ClickitSuccess else ClickitTextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isFreePreset) ClickitSuccess.copy(alpha = 0.45f) else ClickitBorder
                                ),
                                modifier = Modifier.testTag("weight_preset_${preset.replace(".", "_")}")
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Weight (kg)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.38f)
                                .testTag("booking_weight_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ClickitBrandTeal,
                                unfocusedBorderColor = ClickitBorder,
                                focusedTextColor = ClickitTextPrimary,
                                unfocusedTextColor = ClickitTextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = itemNotes,
                            onValueChange = { itemNotes = it },
                            label = { Text("Notes") },
                            placeholder = { Text("Care notes") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.62f)
                                .testTag("booking_notes_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ClickitBrandTeal,
                                unfocusedBorderColor = ClickitBorder,
                                focusedTextColor = ClickitTextPrimary,
                                unfocusedTextColor = ClickitTextPrimary
                            )
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        carePresets.forEach { tag ->
                            val active = itemNotes.contains(tag, ignoreCase = true)
                            FilterChip(
                                selected = active,
                                onClick = {
                                    val parts = itemNotes.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .toMutableList()
                                    if (active) {
                                        parts.removeAll { it.equals(tag, ignoreCase = true) }
                                    } else {
                                        parts.add(tag)
                                    }
                                    itemNotes = parts.joinToString(", ")
                                },
                                shape = RoundedCornerShape(8.dp),
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ClickitBrandTealTint,
                                    selectedLabelColor = ClickitBrandTealDark,
                                    containerColor = ClickitLightSurface,
                                    labelColor = ClickitTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = active,
                                    borderColor = ClickitBorder
                                )
                            )
                        }
                    }

                    if (selectedFulfilment != FulfilmentMethod.SHOP_DROP) {
                        val unlockedFreeDelivery = parsedWeightKg > FREE_DELIVERY_WEIGHT_THRESHOLD_KG
                        val remainingKg = (FREE_DELIVERY_WEIGHT_THRESHOLD_KG - parsedWeightKg).coerceAtLeast(0.0)
                        val progress = (parsedWeightKg / FREE_DELIVERY_WEIGHT_THRESHOLD_KG).toFloat().coerceIn(0f, 1f)
                        Surface(
                            color = if (unlockedFreeDelivery) ClickitSuccessContainer else ClickitLightBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (unlockedFreeDelivery) ClickitSuccess.copy(alpha = 0.45f) else ClickitBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("free_delivery_weight_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (unlockedFreeDelivery) Icons.Filled.CheckCircle else Icons.Filled.LocalShipping,
                                        contentDescription = null,
                                        tint = if (unlockedFreeDelivery) ClickitSuccess else ClickitBrandTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = when {
                                            unlockedFreeDelivery -> "Free delivery unlocked"
                                            parsedWeightKg == FREE_DELIVERY_WEIGHT_THRESHOLD_KG ->
                                                "Add 0.1+ kg for free delivery"
                                            else ->
                                                "Add ${"%.1f".format(remainingKg)} kg for free delivery"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (unlockedFreeDelivery) ClickitSuccess else ClickitTextPrimary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = if (unlockedFreeDelivery) ClickitSuccess else ClickitBrandTeal,
                                    trackColor = ClickitBorder,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                            }
                        }
                    }
                }

                Text("Delivery", style = MaterialTheme.typography.titleMedium, color = ClickitTextPrimary)
                availableFulfilments.forEach { method ->
                    val selected = selectedFulfilment == method
                    Surface(
                        color = if (selected) ClickitBrandTealTint else ClickitLightSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (selected) ClickitBrandTeal else ClickitBorder),
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
                                colors = RadioButtonDefaults.colors(selectedColor = ClickitBrandTeal)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(method.label, style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                                Text(method.subtitle, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                            }
                        }
                    }
                }

                if (selectedFulfilment == FulfilmentMethod.CLICKIT_PICKUP_RETURN) {
                    DefaultPickupSlots.forEachIndexed { idx, slot ->
                        val isSlotSelected = selectedSlotIndex == idx
                        Surface(
                            color = if (isSlotSelected) ClickitBrandTealTint else ClickitLightSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSlotSelected) ClickitBrandTeal else ClickitBorder),
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
                                    Text(slot.windowLabel, style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                                    Text(slot.statusLabel, style = MaterialTheme.typography.labelSmall, color = ClickitBrandTeal)
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
                                colors = CheckboxDefaults.colors(checkedColor = ClickitBrandTeal, checkmarkColor = Color.White)
                            )
                            Text(
                                text = "Smart Batch pickup (₹2/cloth)",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitTextPrimary
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
                                        shape = RoundedCornerShape(8.dp),
                                        label = { Text(addr.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ClickitBrandTeal,
                                            selectedLabelColor = Color.White,
                                            containerColor = ClickitLightSurface,
                                            labelColor = ClickitTextPrimary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selectedAddress?.id == addr.id,
                                            borderColor = ClickitBorder
                                        )
                                    )
                                }
                            }
                        }
                        if (selectedAddress != null) {
                            Text(
                                text = "Pickup: ${selectedAddress.label} • ${selectedAddress.locality}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (selectedFulfilment != FulfilmentMethod.SHOP_DROP && addresses.isEmpty()) {
                    Text("Pickup Address", style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                    OutlinedTextField(
                        value = quickHouseNo,
                        onValueChange = { quickHouseNo = it },
                        label = { Text("House / Flat No *") },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = quickStreet,
                            onValueChange = { quickStreet = it },
                            label = { Text("Street *") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ClickitBrandTeal,
                                unfocusedBorderColor = ClickitBorder,
                                focusedTextColor = ClickitTextPrimary,
                                unfocusedTextColor = ClickitTextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = quickLocality,
                            onValueChange = { quickLocality = it },
                            label = { Text("Area *") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ClickitBrandTeal,
                                unfocusedBorderColor = ClickitBorder,
                                focusedTextColor = ClickitTextPrimary,
                                unfocusedTextColor = ClickitTextPrimary
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = quickPostal,
                            onValueChange = { quickPostal = it.take(12) },
                            label = { Text(if (saveQuickAddress) "PIN *" else "PIN") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.42f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ClickitBrandTeal,
                                unfocusedBorderColor = ClickitBorder,
                                focusedTextColor = ClickitTextPrimary,
                                unfocusedTextColor = ClickitTextPrimary
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(0.58f)
                                .clickable { saveQuickAddress = !saveQuickAddress }
                        ) {
                            Checkbox(
                                checked = saveQuickAddress,
                                onCheckedChange = { saveQuickAddress = it },
                                colors = CheckboxDefaults.colors(checkedColor = ClickitBrandTeal, checkmarkColor = Color.White)
                            )
                            Text(
                                text = "Save address",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitTextPrimary
                            )
                        }
                    }
                }

                Surface(
                    color = if (urgentService) ClickitWarningContainer else ClickitLightBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (urgentService) ClickitWarning.copy(alpha = 0.5f) else ClickitBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("urgent_service_card")
                        .clickable { urgentService = !urgentService }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "Emergency service",
                                tint = if (urgentService) ClickitWarning else ClickitBrandTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Urgent (+25%)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ClickitTextPrimary
                                )
                                Text(
                                    text = "Priority pickup & return",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClickitTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = urgentService,
                            onCheckedChange = { urgentService = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ClickitWarning
                            ),
                            modifier = Modifier.testTag("urgent_service_switch")
                        )
                    }
                }

                val slot = DefaultPickupSlots[selectedSlotIndex]
                val effectiveWeightKg = weightText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 2.5
                val feeQuote = calculateBookingFees(
                    unitPriceMinor = svc.priceMinor,
                    quantity = quantity,
                    weightKg = effectiveWeightKg,
                    fulfilment = selectedFulfilment,
                    batchOptIn = batchOptIn,
                    batchEligible = slot.batchEligible,
                    urgentService = urgentService
                )

                PriceBreakdownCard(
                    serviceSubtotalMinor = feeQuote.serviceSubtotalMinor,
                    deliveryFeeMinor = feeQuote.deliveryFeeMinor,
                    platformFeeMinor = feeQuote.platformFeeMinor,
                    totalMinor = feeQuote.totalMinor,
                    quoteStatus = "awaiting_worker_confirmation",
                    isFreeDeliveryByWeight = feeQuote.isFreeDeliveryByWeight,
                    savedDeliveryMinor = feeQuote.originalDeliveryFeeMinor,
                    emergencySurchargeMinor = feeQuote.emergencySurchargeMinor,
                    isUrgentService = feeQuote.isUrgentService
                )

                TrustProtectionStrip(compact = true)

                val hasQuickAddressFields = quickHouseNo.isNotBlank() && quickStreet.isNotBlank() && quickLocality.isNotBlank()
                val hasValidQuickSavePin = !saveQuickAddress || quickPostal.trim().length in 3..12
                val hasAddressForFulfilment = selectedFulfilment == FulfilmentMethod.SHOP_DROP ||
                    selectedAddress != null ||
                    (hasQuickAddressFields && hasValidQuickSavePin)

                Button(
                    onClick = {
                        val areaText = quickLocality.trim()
                        val resolvedAddress = selectedAddress ?: if (selectedFulfilment != FulfilmentMethod.SHOP_DROP) {
                            SavedAddress(
                                label = "Home",
                                houseNo = quickHouseNo.trim(),
                                street = quickStreet.trim(),
                                locality = areaText.substringBefore(",").trim().ifBlank { areaText },
                                city = areaText.substringAfter(",", "").trim().ifBlank { areaText },
                                postalCode = quickPostal.trim()
                            )
                        } else null
                        onSubmitBooking(
                            svc,
                            quantity,
                            effectiveWeightKg,
                            selectedFulfilment,
                            selectedSlotIndex,
                            batchOptIn,
                            resolvedAddress,
                            itemNotes,
                            urgentService,
                            selectedAddress == null && saveQuickAddress && quickPostal.trim().length in 3..12
                        )
                    },
                    enabled = !isBusy && shop.openStatus && hasAddressForFulfilment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClickitBrandTeal,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_booking_button")
                ) {
                    Text(
                        text = when {
                            !shop.openStatus -> "Shop Paused"
                            !hasAddressForFulfilment -> "Add Address"
                            else -> "Confirm • ${formatInr(feeQuote.totalMinor)}"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
