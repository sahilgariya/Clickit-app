package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.Booking
import com.example.data.formatInr
import com.example.ui.theme.ClickitBorder
import com.example.ui.theme.ClickitBrandTeal
import com.example.ui.theme.ClickitBrandTealTint
import com.example.ui.theme.ClickitCrimson
import com.example.ui.theme.ClickitDangerContainer
import com.example.ui.theme.ClickitLightBg
import com.example.ui.theme.ClickitLightSurface
import com.example.ui.theme.ClickitSuccess
import com.example.ui.theme.ClickitTextPrimary
import com.example.ui.theme.ClickitTextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuPayPaymentSheet(
    booking: Booking,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirmPayment: (paymentMethod: String, paymentRef: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 0 = RuPay on UPI, 1 = RuPay Card
    var selectedMode by remember { mutableIntStateOf(0) }
    var upiId by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ClickitBrandTeal,
        unfocusedBorderColor = ClickitBorder,
        focusedTextColor = ClickitTextPrimary,
        unfocusedTextColor = ClickitTextPrimary,
        focusedLabelColor = ClickitBrandTeal,
        unfocusedLabelColor = ClickitTextSecondary,
        focusedContainerColor = ClickitLightSurface,
        unfocusedContainerColor = ClickitLightSurface
    )

    ModalBottomSheet(
        onDismissRequest = { if (!isBusy) onDismiss() },
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
                    Text(
                        text = "Payment",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = ClickitTextPrimary
                    )
                    Text(
                        text = "${booking.shopName} • ${booking.parcelCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClickitTextSecondary
                    )
                }
                Surface(
                    color = ClickitBrandTealTint,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ClickitBrandTeal.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "RuPay",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = ClickitBrandTeal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Surface(
                color = ClickitLightBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ClickitBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", style = MaterialTheme.typography.bodyMedium, color = ClickitTextSecondary)
                    Text(
                        text = formatInr(booking.totalMinor),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = ClickitBrandTeal
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RuPayModeOption(
                    title = "RuPay on UPI",
                    icon = Icons.Filled.AccountBalance,
                    selected = selectedMode == 0,
                    onClick = {
                        selectedMode = 0
                        errorText = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rupay_mode_upi")
                )
                RuPayModeOption(
                    title = "RuPay Card",
                    icon = Icons.Filled.CreditCard,
                    selected = selectedMode == 1,
                    onClick = {
                        selectedMode = 1
                        errorText = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rupay_mode_card")
                )
            }

            if (selectedMode == 0) {
                OutlinedTextField(
                    value = upiId,
                    onValueChange = {
                        upiId = it.trim()
                        errorText = null
                    },
                    label = { Text("UPI ID") },
                    placeholder = { Text("name@oksbi") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rupay_upi_input")
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("@oksbi", "@okaxis", "@ybl", "@rupay").forEach { suffix ->
                        FilterChip(
                            selected = upiId.endsWith(suffix),
                            onClick = {
                                val base = upiId.substringBefore("@").ifBlank { "clickit" }
                                upiId = "$base$suffix"
                                errorText = null
                            },
                            label = { Text(suffix) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ClickitBrandTeal,
                                selectedLabelColor = Color.White,
                                containerColor = ClickitLightBg,
                                labelColor = ClickitTextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = upiId.endsWith(suffix),
                                borderColor = ClickitBorder
                            )
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Card details",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClickitTextSecondary
                    )
                    TextButton(
                        onClick = {
                            cardNumber = "6070980012345678"
                            cardExpiry = "08/29"
                            cardCvv = "123"
                            errorText = null
                        }
                    ) {
                        Text("Test card", color = ClickitBrandTeal)
                    }
                }

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { raw ->
                        cardNumber = raw.filter { it.isDigit() }.take(16)
                        errorText = null
                    },
                    label = { Text("Card number") },
                    placeholder = { Text("6070 •••• •••• 5678") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rupay_card_number_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cardExpiry,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(4)
                            cardExpiry = if (clean.length > 2) "${clean.take(2)}/${clean.drop(2)}" else clean
                            errorText = null
                        },
                        label = { Text("Expiry") },
                        placeholder = { Text("08/29") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rupay_card_expiry_input")
                    )
                    OutlinedTextField(
                        value = cardCvv,
                        onValueChange = { raw ->
                            cardCvv = raw.filter { it.isDigit() }.take(3)
                            errorText = null
                        },
                        label = { Text("CVV") },
                        placeholder = { Text("•••") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rupay_card_cvv_input")
                    )
                }
            }

            if (errorText != null) {
                Surface(
                    color = ClickitDangerContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ClickitCrimson.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = ClickitCrimson,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = ClickitSuccess,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Escrow protected • Auto-refund on cancel",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClickitTextSecondary
                )
            }

            Button(
                onClick = {
                    if (selectedMode == 0) {
                        val clean = upiId.trim()
                        val parts = clean.split("@")
                        if (clean.length < 5 || parts.size != 2 || parts[0].isBlank() || parts[1].length < 2) {
                            errorText = "Enter a valid UPI ID."
                            return@Button
                        }
                        onConfirmPayment("rupay_upi", "RuPay UPI ($clean)")
                    } else {
                        val digits = cardNumber.filter { it.isDigit() }
                        if (digits.length != 16) {
                            errorText = "Enter 16-digit card number."
                            return@Button
                        }
                        val expiryDigits = cardExpiry.filter { it.isDigit() }
                        val month = expiryDigits.take(2).toIntOrNull() ?: 0
                        if (expiryDigits.length != 4 || month !in 1..12) {
                            errorText = "Enter valid expiry (MM/YY)."
                            return@Button
                        }
                        if (cardCvv.length != 3) {
                            errorText = "Enter 3-digit CVV."
                            return@Button
                        }
                        onConfirmPayment("rupay_card", "RuPay Card •••• ${digits.takeLast(4)}")
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClickitBrandTeal,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_rupay_payment_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Processing...")
                } else {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Pay ${formatInr(booking.totalMinor)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RuPayModeOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) ClickitBrandTealTint else ClickitLightSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) ClickitBrandTeal else ClickitBorder),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ClickitBrandTeal else ClickitTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) ClickitBrandTeal else ClickitTextSecondary
            )
        }
    }
}
