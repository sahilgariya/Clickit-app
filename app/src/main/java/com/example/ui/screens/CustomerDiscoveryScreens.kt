package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
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
import com.example.ui.theme.ClickitBurgundy
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
                    placeholder = { Text("Search laundry, tailoring, footwear repair...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search local service providers",
                            tint = ClickitMutedText
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.testTag("home_search_clear")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = ClickitMutedText)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClickitPeach.copy(alpha = 0.7f),
                        unfocusedBorderColor = ClickitSubtleLine,
                        focusedTextColor = ClickitIvory,
                        unfocusedTextColor = ClickitIvory,
                        focusedContainerColor = ClickitSlateDeep,
                        unfocusedContainerColor = ClickitSlateDeep
                    ),
                    shape = RoundedCornerShape(28.dp),
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
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ClickitPeach,
                                selectedLabelColor = ClickitMidnight,
                                containerColor = ClickitSlateDeep,
                                labelColor = ClickitIvory
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategory == null,
                                borderColor = ClickitSubtleLine
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
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ClickitPeach,
                                selectedLabelColor = ClickitMidnight,
                                containerColor = ClickitSlateDeep,
                                labelColor = ClickitIvory
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategory == cat,
                                borderColor = ClickitSubtleLine
                            ),
                            modifier = Modifier.testTag("home_filter_${cat.key}")
                        )
                    }
                }
            }
        }

        item {
            Surface(
                color = ClickitSlateDeep,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ClickitSubtleLine),
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
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ClickitPeach.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Service address",
                                tint = ClickitPeach,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Deliver to ${defaultAddress?.label ?: "Your Area"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = ClickitIvory
                            )
                            Text(
                                text = defaultAddress?.formattedSummary() ?: "Tap to set pickup & delivery address",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitMutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text("Change", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = ClickitPeach)
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(18.dp))
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
                                listOf(ClickitMidnight.copy(alpha = 0.92f), ClickitMidnight.copy(alpha = 0.35f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(18.dp)
                        .fillMaxWidth(0.84f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = ClickitSuccessContainer, shape = RoundedCornerShape(50)) {
                            Text(
                                text = "FREE DELIVERY >5 KG",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ClickitSuccess,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(color = ClickitSlate.copy(alpha = 0.9f), shape = RoundedCornerShape(50)) {
                            Text(
                                text = "BATCH ₹2/CLOTH",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClickitPeachSoft,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Trusted local care at your door",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = ClickitIvory
                    )
                    Text(
                        text = "OTP sealed bags • Free delivery on 5+ kg loads",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClickitMutedText
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
                    val isSelected = selectedCategory == category
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ClickitSlate else ClickitSlateDeep
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isSelected) ClickitPeach else ClickitSubtleLine),
                        modifier = Modifier
                            .width(156.dp)
                            .testTag("category_card_${category.key}")
                            .clickable { onCategoryChange(if (isSelected) null else category) }
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
            val sectionTitle = selectedCategory?.let { cat ->
                val label = if (cat == ServiceCategory.FOOTWEAR) "Footwear Repair" else cat.label
                "$label Providers"
            } ?: "Nearby Shops"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sectionTitle, style = MaterialTheme.typography.titleLarge, color = ClickitIvory)
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
                            title = if (hasFilters) "No matching providers" else "No shops yet",
                            subtitle = if (hasFilters) {
                                "Try clearing the search or switching categories."
                            } else {
                                "Load verified local laundry, tailoring & footwear repair shops to get started."
                            },
                            actionLabel = when {
                                hasFilters -> "Reset Filters"
                                onLoadStarterShops != null -> "Load Local Providers"
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
                    focusedBorderColor = ClickitPeach.copy(alpha = 0.7f),
                    unfocusedBorderColor = ClickitSubtleLine,
                    focusedTextColor = ClickitIvory,
                    unfocusedTextColor = ClickitIvory,
                    focusedContainerColor = ClickitSlateDeep,
                    unfocusedContainerColor = ClickitSlateDeep
                ),
                shape = RoundedCornerShape(28.dp),
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
                            title = if (hasFilters) "No matching providers" else "No providers available yet",
                            subtitle = if (hasFilters) {
                                "Try clearing filters or searching another neighborhood service."
                            } else {
                                "Load verified local laundry, tailoring & footwear repair shops."
                            },
                            actionLabel = when {
                                hasFilters -> "Reset Filters"
                                onLoadStarterShops != null -> "Load Local Providers"
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopSummaryCard(
    shop: Shop,
    onClick: () -> Unit
) {
    val loadRatio = (shop.activeLoad.toFloat() / shop.dailyCapacity.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val isBusyToday = loadRatio >= 0.8f
    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
        border = BorderStroke(1.dp, ClickitSubtleLine),
        shape = RoundedCornerShape(16.dp),
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
                        text = when {
                            !shop.openStatus -> "Paused"
                            isBusyToday -> "Busy Today"
                            else -> "Ready • Fast Slot"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            !shop.openStatus -> ClickitPeachSoft
                            isBusyToday -> ClickitWarning
                            else -> ClickitSuccess
                        }
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

            Text(shop.shopName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = ClickitIvory)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (shop.supportsClickitBatch || shop.supportsWorkerDelivery) {
                    Surface(color = ClickitSuccessContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "Free delivery >5 kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClickitSuccess,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (shop.verified) {
                    Surface(color = ClickitSlate, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "QR Tagged • Area Rider",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClickitInfo,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

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
        urgentService: Boolean
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
            if (prefillBooking != null) {
                Surface(
                    color = ClickitBurgundy.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ClickitPeach.copy(alpha = 0.6f)),
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
                            tint = ClickitPeach,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Re-ordering from ${prefillBooking.parcelCode}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = ClickitPeach
                            )
                            Text(
                                text = prefillBooking.itemsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = ClickitPeachSoft,
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

                val parsedWeightKg = weightText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                val weightPresets = listOf("2.5", "4.0", "5.5", "7.5")
                val carePresets = listOf("Delicate Wash", "No Bleach", "Light Starch", "Separate Whites", "Handle With Care")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Load Weight (kg)", style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                        Text(
                            text = "Free delivery above 5 kg",
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
                                label = {
                                    Text(
                                        text = if (isFreePreset) "$preset kg • FREE" else "$preset kg",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isFreePreset) ClickitSuccess else ClickitPeach,
                                    selectedLabelColor = ClickitMidnight,
                                    containerColor = ClickitSlateDeep,
                                    labelColor = if (isFreePreset) ClickitSuccess else ClickitIvory
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isFreePreset) ClickitSuccess.copy(alpha = 0.45f) else ClickitSubtleLine
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
                            modifier = Modifier
                                .weight(0.38f)
                                .testTag("booking_weight_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ClickitPeach,
                                unfocusedBorderColor = ClickitSubtleLine,
                                focusedTextColor = ClickitIvory,
                                unfocusedTextColor = ClickitIvory
                            )
                        )
                        OutlinedTextField(
                            value = itemNotes,
                            onValueChange = { itemNotes = it },
                            label = { Text("Care Notes (optional)") },
                            placeholder = { Text("Delicate wash, separate whites...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.62f)
                                .testTag("booking_notes_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ClickitPeach,
                                unfocusedBorderColor = ClickitSubtleLine,
                                focusedTextColor = ClickitIvory,
                                unfocusedTextColor = ClickitIvory
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
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ClickitInfoContainer,
                                    selectedLabelColor = ClickitInfo,
                                    containerColor = ClickitSlateDeep,
                                    labelColor = ClickitMutedText
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = active,
                                    borderColor = ClickitSubtleLine
                                )
                            )
                        }
                    }

                    if (selectedFulfilment != FulfilmentMethod.SHOP_DROP) {
                        val unlockedFreeDelivery = parsedWeightKg > FREE_DELIVERY_WEIGHT_THRESHOLD_KG
                        val remainingKg = (FREE_DELIVERY_WEIGHT_THRESHOLD_KG - parsedWeightKg).coerceAtLeast(0.0)
                        val progress = (parsedWeightKg / FREE_DELIVERY_WEIGHT_THRESHOLD_KG).toFloat().coerceIn(0f, 1f)
                        Surface(
                            color = if (unlockedFreeDelivery) ClickitSuccessContainer else ClickitSlateDeep,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (unlockedFreeDelivery) ClickitSuccess.copy(alpha = 0.45f) else ClickitSubtleLine
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
                                        tint = if (unlockedFreeDelivery) ClickitSuccess else ClickitPeach,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = when {
                                            unlockedFreeDelivery -> "FREE pickup & delivery unlocked (>5 kg)!"
                                            parsedWeightKg == FREE_DELIVERY_WEIGHT_THRESHOLD_KG ->
                                                "Add 0.1+ kg (above 5 kg) for FREE delivery"
                                            else ->
                                                "Add ${"%.1f".format(remainingKg)} kg more (above 5 kg) for FREE delivery"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (unlockedFreeDelivery) ClickitSuccess else ClickitIvory
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = if (unlockedFreeDelivery) ClickitSuccess else ClickitPeach,
                                    trackColor = ClickitSlate,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                            }
                        }
                    }
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

                Surface(
                    color = if (urgentService) ClickitWarningContainer else ClickitSlateDeep,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (urgentService) ClickitWarning.copy(alpha = 0.6f) else ClickitSubtleLine
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
                                tint = if (urgentService) ClickitWarning else ClickitPeach,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Need it fast? Emergency Service (+25%)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ClickitIvory
                                )
                                Text(
                                    text = "Urgent pickup & priority turnaround • Priced upfront",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClickitMutedText
                                )
                            }
                        }
                        Switch(
                            checked = urgentService,
                            onCheckedChange = { urgentService = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ClickitMidnight,
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

                val hasAddressForFulfilment = selectedFulfilment == FulfilmentMethod.SHOP_DROP ||
                    selectedAddress != null ||
                    (quickHouseNo.isNotBlank() && quickStreet.isNotBlank() && quickLocality.isNotBlank())

                Button(
                    onClick = {
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
                            effectiveWeightKg,
                            selectedFulfilment,
                            selectedSlotIndex,
                            batchOptIn,
                            resolvedAddress,
                            itemNotes,
                            urgentService
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
                            feeQuote.isFreeDeliveryByWeight ->
                                "Confirm • ${formatInr(feeQuote.totalMinor)} (Free Delivery)"
                            else -> "Confirm Booking • ${formatInr(feeQuote.totalMinor)}"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
