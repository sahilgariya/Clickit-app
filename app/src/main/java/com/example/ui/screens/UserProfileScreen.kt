package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.SavedAddress
import com.example.data.SupportTicket
import com.example.data.UserProfile
import com.example.data.UserRole
import com.example.data.decodeDataUriToImageBitmap
import com.example.ui.UiState
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.LoadingStateView
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
import com.example.ui.theme.ClickitSubtleLine
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun UserProfileAvatar(
    displayName: String,
    photoUrl: String,
    previewUri: Uri? = null,
    size: Dp = 88.dp,
    modifier: Modifier = Modifier
) {
    val initials = remember(displayName) {
        displayName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "C" }
    }
    val dataBitmap = remember(photoUrl) {
        if (photoUrl.startsWith("data:image/")) decodeDataUriToImageBitmap(photoUrl) else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(ClickitBurgundy.copy(alpha = 0.65f), ClickitSlateDeep, ClickitMidnight)
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    listOf(ClickitPeach, ClickitCrimson.copy(alpha = 0.85f), ClickitInfo.copy(alpha = 0.7f))
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            previewUri != null -> {
                AsyncImage(
                    model = previewUri,
                    contentDescription = "$displayName profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            dataBitmap != null -> {
                Image(
                    bitmap = dataBitmap,
                    contentDescription = "$displayName profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            photoUrl.isNotBlank() -> {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "$displayName profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = ClickitPeach
                )
            }
        }
    }
}

@Composable
fun ProfileAndSupportScreen(
    profileState: UiState<UserProfile?>,
    addressesState: UiState<List<SavedAddress>>,
    ticketsState: UiState<List<SupportTicket>>,
    activeRole: UserRole,
    isBusy: Boolean = false,
    onOpenRoleSwitcher: () -> Unit,
    onUpdatePhoto: (Uri) -> Unit,
    onSaveProfile: (String, String, String, Uri?, Boolean, () -> Unit) -> Unit,
    onSaveAddress: (SavedAddress, () -> Unit) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onCreateTicket: (String, String, String) -> Unit,
    onRetry: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val profile = (profileState as? UiState.Success)?.data
    val addresses = (addressesState as? UiState.Success)?.data ?: emptyList()
    val tickets = (ticketsState as? UiState.Success)?.data ?: emptyList()
    val authUser = remember { Firebase.auth.currentUser }
    val userEmail = authUser?.email.orEmpty()
    val fallbackPhoto = authUser?.photoUrl?.toString().orEmpty()

    var showAddressDialog by remember { mutableStateOf(false) }
    var showTicketDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val quickPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdatePhoto(uri)
        }
    }

    if (showEditProfileDialog && profile != null) {
        var name by remember(profile.id) { mutableStateOf(profile.displayName) }
        var locality by remember(profile.id) { mutableStateOf(profile.activeLocality) }
        var phone by remember(profile.id) { mutableStateOf(profile.phoneMasked) }
        var pendingPhotoUri by remember(profile.id) { mutableStateOf<Uri?>(null) }
        var clearPhoto by remember(profile.id) { mutableStateOf(false) }
        var validationError by remember { mutableStateOf<String?>(null) }

        val dialogPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                pendingPhotoUri = uri
                clearPhoto = false
            }
        }

        val currentDisplayPhoto = if (clearPhoto) "" else profile.photoUrl.ifBlank { fallbackPhoto }
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClickitPeach,
            unfocusedBorderColor = ClickitSlate,
            focusedTextColor = ClickitIvory,
            unfocusedTextColor = ClickitIvory,
            focusedLabelColor = ClickitPeach,
            unfocusedLabelColor = ClickitMutedText
        )

        AlertDialog(
            onDismissRequest = { if (!isBusy) showEditProfileDialog = false },
            containerColor = ClickitSlateDeep,
            title = { Text("Edit Profile", color = ClickitIvory) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserProfileAvatar(
                        displayName = name.ifBlank { profile.displayName },
                        photoUrl = currentDisplayPhoto,
                        previewUri = pendingPhotoUri,
                        size = 84.dp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                dialogPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isBusy,
                            border = BorderStroke(1.dp, ClickitPeach),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dialog_pick_photo_button")
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = ClickitPeach, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Change Photo", color = ClickitPeach)
                        }
                        if (pendingPhotoUri != null || currentDisplayPhoto.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    pendingPhotoUri = null
                                    clearPhoto = true
                                },
                                enabled = !isBusy,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Remove", color = ClickitPeachSoft)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationError = null
                        },
                        label = { Text("Display Name *") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_display_name_input")
                    )
                    OutlinedTextField(
                        value = locality,
                        onValueChange = {
                            locality = it
                            validationError = null
                        },
                        label = { Text("Area / City *") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone (optional)") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = ClickitPeachSoft,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            name.trim().isEmpty() -> validationError = "Enter your display name."
                            name.trim().length > 60 -> validationError = "Name must be 60 characters or fewer."
                            locality.trim().isEmpty() -> validationError = "Enter your area or city."
                            else -> onSaveProfile(
                                name.trim(),
                                locality.trim(),
                                phone.trim(),
                                pendingPhotoUri,
                                clearPhoto
                            ) { showEditProfileDialog = false }
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight),
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = ClickitMidnight,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditProfileDialog = false },
                    enabled = !isBusy
                ) {
                    Text("Cancel", color = ClickitIvory)
                }
            }
        )
    }

    if (showAddressDialog) {
        var label by remember { mutableStateOf("Home") }
        var houseNo by remember { mutableStateOf("") }
        var street by remember { mutableStateOf("") }
        val defaultArea = profile?.activeLocality?.substringBefore(",")?.trim().orEmpty()
        val defaultCity = profile?.activeLocality?.substringAfter(",", "")?.trim().orEmpty()
        var locality by remember { mutableStateOf(defaultArea) }
        var city by remember { mutableStateOf(defaultCity) }
        var postal by remember { mutableStateOf("") }
        var instructions by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            containerColor = ClickitSlateDeep,
            title = { Text("Add Address", color = ClickitIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (Home/Work) *") }, singleLine = true)
                    OutlinedTextField(value = houseNo, onValueChange = { houseNo = it }, label = { Text("House / Flat No *") }, singleLine = true)
                    OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("Street / Road *") }, singleLine = true)
                    OutlinedTextField(value = locality, onValueChange = { locality = it }, label = { Text("Locality *") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City *") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = postal, onValueChange = { postal = it }, label = { Text("PIN *") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("Instructions") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveAddress(
                            SavedAddress(
                                label = label,
                                houseNo = houseNo,
                                street = street,
                                locality = locality,
                                city = city.ifBlank { locality },
                                postalCode = postal,
                                instructions = instructions,
                                defaultAddress = addresses.isEmpty()
                            )
                        ) { showAddressDialog = false }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight)
                ) { Text("Save Address") }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) { Text("Cancel", color = ClickitIvory) }
            }
        )
    }

    if (showTicketDialog) {
        var category by remember { mutableStateOf("Order & Pickup") }
        var subject by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTicketDialog = false },
            containerColor = ClickitSlateDeep,
            title = { Text("Contact Clickit Support", color = ClickitIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject *") })
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Details *") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateTicket(category, subject, description)
                        showTicketDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight)
                ) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showTicketDialog = false }) { Text("Cancel", color = ClickitIvory) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            when (profileState) {
                is UiState.Loading -> LoadingStateView("Loading profile...")
                is UiState.Error -> ErrorRetryCard(message = profileState.message, onRetry = onRetry)
                is UiState.Success -> {
                    val userProfile = profileState.data
                    val resolvedPhoto = userProfile?.photoUrl?.ifBlank { fallbackPhoto } ?: fallbackPhoto
                    val displayName = userProfile?.displayName?.ifBlank { "Clickit User" } ?: "Clickit User"

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                        border = BorderStroke(1.dp, ClickitPeach.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    UserProfileAvatar(
                                        displayName = displayName,
                                        photoUrl = resolvedPhoto,
                                        size = 84.dp,
                                        modifier = Modifier
                                            .testTag("profile_avatar")
                                            .clickable(enabled = !isBusy) {
                                                quickPhotoPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                    )
                                    Surface(
                                        color = ClickitPeach,
                                        shape = CircleShape,
                                        border = BorderStroke(2.dp, ClickitSlateDeep),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("change_profile_photo_button")
                                            .clickable(enabled = !isBusy) {
                                                quickPhotoPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isBusy) {
                                                CircularProgressIndicator(
                                                    color = ClickitMidnight,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.CameraAlt,
                                                    contentDescription = "Update profile picture",
                                                    tint = ClickitMidnight,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = ClickitIvory,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (userEmail.isNotBlank()) {
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ClickitMutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!userProfile?.activeLocality.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.LocationOn,
                                                contentDescription = null,
                                                tint = ClickitPeach,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = userProfile!!.activeLocality,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ClickitPeachSoft,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (!userProfile?.phoneMasked.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Phone,
                                                contentDescription = null,
                                                tint = ClickitMutedText,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = userProfile!!.phoneMasked,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ClickitMutedText
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { showEditProfileDialog = true },
                                enabled = userProfile != null && !isBusy,
                                border = BorderStroke(1.dp, ClickitPeach.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, tint = ClickitPeach, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Edit Name & Photo", color = ClickitPeach)
                            }

                            HorizontalDivider(color = ClickitSubtleLine)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable(onClick = onOpenRoleSwitcher),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    NeonIconBadge(
                                        icon = Icons.Filled.SwapHoriz,
                                        tint = ClickitPeach,
                                        glowColor = ClickitInfo,
                                        size = 38.dp,
                                        iconSize = 18.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Mode: ${activeRole.label}", style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                                        Text("Customer • Shop • Delivery", style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                                    }
                                }
                                Text("Switch", style = MaterialTheme.typography.labelLarge, color = ClickitPeach)
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
                Text("Saved Addresses", style = MaterialTheme.typography.titleMedium, color = ClickitIvory)
                TextButton(onClick = { showAddressDialog = true }, modifier = Modifier.testTag("add_address_button")) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = ClickitPeach)
                    Spacer(Modifier.width(4.dp))
                    Text("Add", color = ClickitPeach)
                }
            }
        }

        items(addresses, key = { it.id }) { addr ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                border = BorderStroke(1.dp, if (addr.defaultAddress) ClickitPeach else ClickitSlate)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = ClickitPeach)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(addr.label, style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                        Text(addr.formattedSummary(), style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                    }
                    IconButton(onClick = { onDeleteAddress(addr.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete address", tint = ClickitPeachSoft)
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
                Text("Support & Disputes", style = MaterialTheme.typography.titleMedium, color = ClickitIvory)
                TextButton(onClick = { showTicketDialog = true }, modifier = Modifier.testTag("new_ticket_button")) {
                    Text("New Ticket", color = ClickitPeach)
                }
            }
        }

        if (tickets.isEmpty()) {
            item {
                Text(
                    text = "No open support tickets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ClickitMutedText
                )
            }
        } else {
            items(tickets, key = { it.id }) { t ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
                    border = BorderStroke(1.dp, ClickitSlate)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.subject, style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                            Text(t.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = ClickitPeach)
                        }
                        Text(t.description, style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSignOut,
                border = BorderStroke(1.dp, ClickitCrimson),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ClickitPeachSoft)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = ClickitPeachSoft)
            }
        }
    }
}
