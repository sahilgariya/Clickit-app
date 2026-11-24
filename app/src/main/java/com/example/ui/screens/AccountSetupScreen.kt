package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SavedAddress
import com.example.data.ServiceCategory
import com.example.data.Shop
import com.example.data.UserRole
import com.example.ui.components.ClickitBrandLogoMark
import com.example.ui.components.ClickitWordmark
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountSetupScreen(
    initialDisplayName: String,
    isBusy: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onSetupCustomer: (displayName: String, locality: String, phone: String, address: SavedAddress?) -> Unit,
    onSetupWorker: (
        displayName: String,
        locality: String,
        phone: String,
        shop: Shop,
        serviceName: String,
        servicePriceMinor: Long,
        serviceUnit: String
    ) -> Unit,
    onSetupPartner: (displayName: String, locality: String, phone: String) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedRole by remember { mutableStateOf(UserRole.CUSTOMER) }
    var displayName by remember { mutableStateOf(initialDisplayName.takeIf { it != "Clickit Neighbor" } ?: "") }
    var locality by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Customer optional address
    var addAddressNow by remember { mutableStateOf(false) }
    var houseNo by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }

    // Shop Owner fields
    var shopName by remember { mutableStateOf("") }
    var shopCategory by remember { mutableStateOf(ServiceCategory.LAUNDRY) }
    var shopAddress by remember { mutableStateOf("") }
    var firstServiceName by remember { mutableStateOf("Steam Press") }
    var firstServicePrice by remember { mutableStateOf("15") }
    var firstServiceUnit by remember { mutableStateOf("cloth") }

    var validationError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(errorMessage)
            onClearError()
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ClickitPeach,
        unfocusedBorderColor = ClickitSlate,
        focusedTextColor = ClickitIvory,
        unfocusedTextColor = ClickitIvory,
        focusedLabelColor = ClickitPeach,
        unfocusedLabelColor = ClickitMutedText,
        focusedContainerColor = ClickitMidnight,
        unfocusedContainerColor = ClickitMidnight
    )

    Scaffold(
        containerColor = ClickitMidnight,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClickitBrandLogoMark(size = 42.dp)
                        Spacer(Modifier.width(10.dp))
                        ClickitWordmark(fontSizeSp = 22f)
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("setup_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Switch Google account",
                            tint = ClickitMutedText
                        )
                    }
                }

                Text(
                    text = "Set up your account",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ClickitIvory
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    UserRole.entries.forEach { role ->
                        val selected = selectedRole == role
                        val roleIcon = when (role) {
                            UserRole.CUSTOMER -> Icons.Filled.ShoppingCart
                            UserRole.WORKER -> Icons.Filled.Storefront
                            UserRole.PARTNER -> Icons.AutoMirrored.Filled.DirectionsBike
                        }
                        Surface(
                            color = if (selected) ClickitBurgundy else ClickitSlateDeep,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.2.dp, if (selected) ClickitPeach else ClickitSlate),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_role_${role.key}")
                                .minimumInteractiveComponentSize()
                                .clickable {
                                    selectedRole = role
                                    validationError = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NeonIconBadge(
                                    icon = roleIcon,
                                    tint = if (selected) ClickitPeach else ClickitIvory,
                                    glowColor = if (selected) ClickitPeach else ClickitInfo,
                                    size = 42.dp,
                                    iconSize = 20.dp,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(role.label, style = MaterialTheme.typography.titleMedium, color = ClickitIvory)
                                    Text(role.subtitle, style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                                }
                                if (selected) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = ClickitPeach)
                                }
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                    border = BorderStroke(1.dp, ClickitSlate),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it; validationError = null },
                            label = { Text("Your Name *") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("setup_name_input")
                        )

                        OutlinedTextField(
                            value = locality,
                            onValueChange = { locality = it; validationError = null },
                            label = {
                                Text(
                                    when (selectedRole) {
                                        UserRole.PARTNER -> "Delivery Zone / City *"
                                        else -> "Area / City *"
                                    }
                                )
                            },
                            placeholder = { Text("e.g. Koramangala, Bengaluru") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("setup_locality_input")
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone (optional)") },
                            placeholder = { Text("+91 ...") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("setup_phone_input")
                        )

                        when (selectedRole) {
                            UserRole.CUSTOMER -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { addAddressNow = !addAddressNow }
                                ) {
                                    Checkbox(
                                        checked = addAddressNow,
                                        onCheckedChange = { addAddressNow = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = ClickitPeach,
                                            checkmarkColor = ClickitMidnight
                                        )
                                    )
                                    Text("Add pickup address now", style = MaterialTheme.typography.bodyMedium, color = ClickitIvory)
                                }

                                if (addAddressNow) {
                                    OutlinedTextField(
                                        value = houseNo,
                                        onValueChange = { houseNo = it; validationError = null },
                                        label = { Text("House / Flat No *") },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = street,
                                        onValueChange = { street = it; validationError = null },
                                        label = { Text("Street / Road *") },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = postalCode,
                                        onValueChange = { postalCode = it; validationError = null },
                                        label = { Text("PIN Code *") },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            UserRole.WORKER -> {
                                Text("Shop Details", style = MaterialTheme.typography.titleSmall, color = ClickitPeach)

                                OutlinedTextField(
                                    value = shopName,
                                    onValueChange = { shopName = it; validationError = null },
                                    label = { Text("Shop Name *") },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier.fillMaxWidth().testTag("setup_shop_name_input")
                                )

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ServiceCategory.entries.forEach { cat ->
                                        FilterChip(
                                            selected = shopCategory == cat,
                                            onClick = {
                                                shopCategory = cat
                                                when (cat) {
                                                    ServiceCategory.LAUNDRY -> {
                                                        if (firstServiceName == "Trouser Hemming" || firstServiceName == "Sneaker Clean") {
                                                            firstServiceName = "Steam Press"
                                                            firstServiceUnit = "cloth"
                                                        }
                                                    }
                                                    ServiceCategory.TAILORING -> {
                                                        if (firstServiceName == "Steam Press" || firstServiceName == "Sneaker Clean") {
                                                            firstServiceName = "Trouser Hemming"
                                                            firstServiceUnit = "alteration"
                                                        }
                                                    }
                                                    ServiceCategory.FOOTWEAR -> {
                                                        if (firstServiceName == "Steam Press" || firstServiceName == "Trouser Hemming") {
                                                            firstServiceName = "Sneaker Clean"
                                                            firstServiceUnit = "pair"
                                                        }
                                                    }
                                                }
                                            },
                                            label = { Text(cat.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ClickitPeach,
                                                selectedLabelColor = ClickitMidnight
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = shopAddress,
                                    onValueChange = { shopAddress = it; validationError = null },
                                    label = { Text("Shop Address *") },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier.fillMaxWidth().testTag("setup_shop_address_input")
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = firstServiceName,
                                        onValueChange = { firstServiceName = it; validationError = null },
                                        label = { Text("First Service *") },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    OutlinedTextField(
                                        value = firstServicePrice,
                                        onValueChange = { firstServicePrice = it; validationError = null },
                                        label = { Text("Price (₹) *") },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }

                                OutlinedTextField(
                                    value = firstServiceUnit,
                                    onValueChange = { firstServiceUnit = it; validationError = null },
                                    label = { Text("Unit (cloth / kg / pair) *") },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            UserRole.PARTNER -> {
                                Text(
                                    text = "You will see pickup and return tasks in your zone right after setup.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClickitMutedText
                                )
                            }
                        }

                        if (validationError != null) {
                            Surface(
                                color = ClickitCrimson.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ClickitCrimson)
                            ) {
                                Text(
                                    text = validationError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClickitPeachSoft,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val nameClean = displayName.trim()
                        val locClean = locality.trim()
                        if (nameClean.isEmpty()) {
                            validationError = "Enter your name."
                            return@Button
                        }
                        if (locClean.isEmpty()) {
                            validationError = "Enter your area or city."
                            return@Button
                        }
                        when (selectedRole) {
                            UserRole.CUSTOMER -> {
                                val addr = if (addAddressNow) {
                                    if (houseNo.isBlank() || street.isBlank() || postalCode.trim().length < 3) {
                                        validationError = "Fill House No, Street, and PIN Code."
                                        return@Button
                                    }
                                    val cityPart = locClean.substringAfter(",", locClean).trim().ifBlank { locClean }
                                    val areaPart = locClean.substringBefore(",").trim().ifBlank { locClean }
                                    SavedAddress(
                                        label = "Home",
                                        houseNo = houseNo.trim(),
                                        street = street.trim(),
                                        locality = areaPart.take(60),
                                        city = cityPart.take(50),
                                        postalCode = postalCode.trim(),
                                        defaultAddress = true
                                    )
                                } else null
                                onSetupCustomer(nameClean, locClean, phone.trim(), addr)
                            }
                            UserRole.WORKER -> {
                                if (shopName.trim().length < 2) {
                                    validationError = "Enter shop name (min 2 chars)."
                                    return@Button
                                }
                                if (shopAddress.trim().isEmpty()) {
                                    validationError = "Enter shop address."
                                    return@Button
                                }
                                if (firstServiceName.trim().length < 2) {
                                    validationError = "Enter your first service name."
                                    return@Button
                                }
                                val priceMinor = ((firstServicePrice.toDoubleOrNull() ?: 0.0) * 100.0).toLong()
                                if (priceMinor < 100L) {
                                    validationError = "Price must be at least ₹1."
                                    return@Button
                                }
                                val shop = Shop(
                                    shopName = shopName.trim(),
                                    category = shopCategory.key,
                                    locality = locClean,
                                    addressSummary = shopAddress.trim(),
                                    startingPriceMinor = priceMinor,
                                    startingPriceUnit = firstServiceUnit.trim().ifBlank { "item" }
                                )
                                onSetupWorker(
                                    nameClean,
                                    locClean,
                                    phone.trim(),
                                    shop,
                                    firstServiceName.trim(),
                                    priceMinor,
                                    firstServiceUnit.trim().ifBlank { "item" }
                                )
                            }
                            UserRole.PARTNER -> {
                                onSetupPartner(nameClean, locClean, phone.trim())
                            }
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClickitPeach,
                        contentColor = ClickitMidnight
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("complete_setup_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = ClickitMidnight,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
