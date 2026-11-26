package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BookingStatus
import com.example.data.ServiceCategory
import com.example.data.UserRole
import com.example.data.formatInr
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

@Composable
fun ClickitBrandLogoMark(
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(ClickitPeach.copy(alpha = 0.85f), ClickitCrimson.copy(alpha = 0.6f), ClickitInfo.copy(alpha = 0.65f))
                ),
                shape = shape
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_clickit_logo),
            contentDescription = "Clickit logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ClickitWordmark(
    fontSizeSp: Float = 20f,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = ClickitIvory, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic)) {
                append("CLICK")
            }
            withStyle(SpanStyle(color = ClickitPeach, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic)) {
                append("IT")
            }
        },
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = fontSizeSp.sp,
            letterSpacing = 1.sp
        ),
        modifier = modifier
    )
}

@Composable
fun NeonIconBadge(
    icon: ImageVector,
    tint: Color = ClickitPeach,
    glowColor: Color = ClickitPeach,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = CircleShape,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(glowColor.copy(alpha = 0.14f))
            .border(
                width = 1.dp,
                color = glowColor.copy(alpha = 0.28f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun ClickitTopHeader(
    activeRole: UserRole,
    localityLabel: String,
    unreadNotifications: Int,
    onNotificationsClick: () -> Unit,
    onRoleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val subtitleText = when (activeRole) {
        UserRole.CUSTOMER -> localityLabel.ifBlank { "Nearby services" }
        UserRole.WORKER -> "Shopper • ${localityLabel.ifBlank { "My Shop" }}"
        UserRole.PARTNER -> "Delivery • ${localityLabel.ifBlank { "Active Zone" }}"
    }
    val roleChipLabel = when (activeRole) {
        UserRole.CUSTOMER -> "Customer"
        UserRole.WORKER -> "Shopper"
        UserRole.PARTNER -> "Delivery"
    }
    Surface(
        color = ClickitMidnight,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ClickitBrandLogoMark(size = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ClickitWordmark(fontSizeSp = 18f)
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = ClickitMutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onRoleClick != null) {
                        Surface(
                            color = ClickitSlateDeep,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, ClickitSubtleLine),
                            modifier = Modifier
                                .testTag("header_role_pill")
                                .minimumInteractiveComponentSize()
                                .clickable(onClick = onRoleClick)
                        ) {
                            Text(
                                text = roleChipLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = ClickitPeachSoft,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier
                            .testTag("header_notifications_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge(containerColor = ClickitCrimson, contentColor = Color.White) {
                                        Text(unreadNotifications.coerceAtMost(99).toString())
                                    }
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ClickitSlateDeep),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Notifications",
                                    tint = ClickitIvory,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = ClickitSubtleLine.copy(alpha = 0.55f))
        }
    }
}

@Composable
fun BookingStatusBadge(statusRaw: String, modifier: Modifier = Modifier) {
    val status = BookingStatus.fromRaw(statusRaw)
    val (bg, fg) = when (status) {
        BookingStatus.COMPLETED, BookingStatus.DELIVERED -> ClickitSuccessContainer to ClickitSuccess
        BookingStatus.CANCELLED, BookingStatus.DISPUTED -> ClickitBurgundy to ClickitPeachSoft
        BookingStatus.QUOTE_PENDING -> ClickitWarningContainer to ClickitWarning
        BookingStatus.PICKUP_SCHEDULED, BookingStatus.PICKED_UP,
        BookingStatus.RETURN_SCHEDULED, BookingStatus.OUT_FOR_DELIVERY -> ClickitInfoContainer to ClickitInfo
        else -> ClickitSlate to ClickitPeachSoft
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CategoryBadge(categoryRaw: String, modifier: Modifier = Modifier) {
    val cat = ServiceCategory.fromRaw(categoryRaw)
    Surface(
        color = ClickitSlate,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = cat.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ClickitPeachSoft,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun PriceBreakdownCard(
    serviceSubtotalMinor: Long,
    deliveryFeeMinor: Long,
    platformFeeMinor: Long,
    totalMinor: Long,
    quoteStatus: String,
    isFreeDeliveryByWeight: Boolean = false,
    savedDeliveryMinor: Long = 0L,
    emergencySurchargeMinor: Long = 0L,
    isUrgentService: Boolean = false,
    modifier: Modifier = Modifier
) {
    val statusLabel = when (quoteStatus) {
        "confirmed" -> "Final"
        "quote_revision_sent" -> "Updated Quote"
        "awaiting_worker_confirmation" -> "Upfront Estimate"
        else -> "Upfront Estimate"
    }
    val statusColor = when (quoteStatus) {
        "confirmed" -> ClickitSuccess
        "quote_revision_sent" -> ClickitWarning
        else -> ClickitPeachSoft
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
        border = BorderStroke(1.dp, ClickitSubtleLine),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Upfront Price Summary", style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
            if (emergencySurchargeMinor > 0L) {
                val baseService = (serviceSubtotalMinor - emergencySurchargeMinor).coerceAtLeast(0L)
                PriceRow("Service (Base)", formatInr(baseService))
                PriceRow(
                    label = "Emergency Service (+25%)",
                    value = "+${formatInr(emergencySurchargeMinor)}",
                    valueColor = ClickitWarning
                )
            } else {
                PriceRow(
                    label = if (isUrgentService) "Service (incl. +25% urgent)" else "Service",
                    value = formatInr(serviceSubtotalMinor)
                )
            }
            val deliveryLabel = when {
                isFreeDeliveryByWeight && savedDeliveryMinor > 0L ->
                    "FREE (5+ kg • Saved ${formatInr(savedDeliveryMinor)})"
                isFreeDeliveryByWeight -> "FREE (5+ kg)"
                deliveryFeeMinor == 0L -> "₹0"
                else -> formatInr(deliveryFeeMinor)
            }
            PriceRow(
                label = "Delivery",
                value = deliveryLabel,
                valueColor = if (deliveryFeeMinor == 0L) ClickitSuccess else ClickitIvory
            )
            PriceRow("Platform fee", formatInr(platformFeeMinor))
            HorizontalDivider(color = ClickitSubtleLine)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", style = MaterialTheme.typography.titleMedium, color = ClickitIvory)
                Text(formatInr(totalMinor), style = MaterialTheme.typography.titleMedium, color = ClickitPeach)
            }
            Text(
                text = "We always ask before charging anything extra.",
                style = MaterialTheme.typography.labelSmall,
                color = ClickitMutedText
            )
        }
    }
}

@Composable
private fun PriceRow(
    label: String,
    value: String,
    valueColor: Color = ClickitIvory
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
    }
}

@Composable
fun TrustProtectionStrip(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        color = ClickitSuccessContainer.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ClickitSuccess.copy(alpha = 0.28f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = "Clickit Reliability Guarantee",
                tint = ClickitSuccess,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Clickit Reliability Guarantee",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ClickitIvory
                )
                Text(
                    text = "QR garment tags • Condition logged • Up to ₹2,000 cover • Free re-service",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClickitMutedText
                )
            }
        }
    }
}

@Composable
fun GarmentScanTagCard(
    parcelCode: String,
    itemCount: Long,
    weightKg: Double,
    slotWindowLabel: String,
    isUrgent: Boolean = false,
    isDelayed: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
        border = BorderStroke(1.dp, if (isDelayed) ClickitWarning.copy(alpha = 0.65f) else ClickitSubtleLine),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = ClickitIvory,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                ) {
                    val grid = 9
                    val cell = size.width / grid.toFloat()
                    val seed = parcelCode.hashCode()
                    for (r in 0 until grid) {
                        for (c in 0 until grid) {
                            val inTopLeft = r < 3 && c < 3
                            val inTopRight = r < 3 && c >= grid - 3
                            val inBottomLeft = r >= grid - 3 && c < 3
                            val filled = when {
                                inTopLeft || inTopRight || inBottomLeft -> {
                                    val lr = if (r < 3) r else r - (grid - 3)
                                    val lc = if (c < 3) c else c - (grid - 3)
                                    lr == 0 || lr == 2 || lc == 0 || lc == 2 || (lr == 1 && lc == 1)
                                }
                                else -> (((seed ushr ((r * 3 + c) % 24)) + r * 7 + c * 11) and 1) == 0
                            }
                            if (filled) {
                                drawRect(
                                    color = Color(0xFF0E131F),
                                    topLeft = Offset(c * cell, r * cell),
                                    size = Size(cell * 0.92f, cell * 0.92f)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = "QR garment tag",
                        tint = ClickitInfo,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "QR Tag • $parcelCode",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ClickitIvory
                    )
                }
                Text(
                    text = "$itemCount items (${weightKg} kg) • Scanned at every handoff",
                    style = MaterialTheme.typography.bodySmall,
                    color = ClickitMutedText
                )
                Text(
                    text = "Committed window: ${slotWindowLabel.ifBlank { "Scheduled" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDelayed) ClickitWarning else ClickitInfo
                )
                if (isUrgent || isDelayed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isUrgent) {
                            Surface(color = ClickitWarningContainer, shape = RoundedCornerShape(6.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = ClickitWarning, modifier = Modifier.size(12.dp))
                                    Text("URGENT +25%", style = MaterialTheme.typography.labelSmall, color = ClickitWarning)
                                }
                            }
                        }
                        if (isDelayed) {
                            Surface(color = ClickitBurgundy, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "DELAY FLAGGED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ClickitPeachSoft,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartBatchCapacityBar(
    filledKg: Double,
    capacityKg: Double,
    stopsCount: Int,
    modifier: Modifier = Modifier
) {
    val ratio = (filledKg / capacityKg.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Smart Batch Route • $stopsCount stops",
                style = MaterialTheme.typography.labelMedium,
                color = ClickitInfo
            )
            Text(
                text = "${filledKg.toInt()} / ${capacityKg.toInt()} kg",
                style = MaterialTheme.typography.labelMedium,
                color = ClickitIvory
            )
        }
        LinearProgressIndicator(
            progress = { ratio },
            color = ClickitPeach,
            trackColor = ClickitSlate,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}

@Composable
fun OtpCustodyCard(
    parcelCode: String,
    otpLabel: String,
    otpCode: String,
    helperNote: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
        border = BorderStroke(1.dp, ClickitPeach.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(ClickitBurgundy.copy(alpha = 0.45f), ClickitSlateDeep)
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = ClickitPeach, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Parcel $parcelCode", style = MaterialTheme.typography.titleSmall, color = ClickitPeach)
                }
                Spacer(Modifier.height(4.dp))
                Text(otpLabel, style = MaterialTheme.typography.bodyMedium, color = ClickitIvory)
                Text(helperNote, style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                color = ClickitMidnight,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ClickitPeach)
            ) {
                Text(
                    text = otpCode,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = ClickitPeach,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingStateView(message: String = "Loading Clickit data...", modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = ClickitPeach)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = ClickitMutedText)
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitSlateDeep),
        border = BorderStroke(1.dp, ClickitSlate),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ClickitSlate),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ClickitPeach, modifier = Modifier.size(26.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = ClickitIvory)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onAction,
                    border = BorderStroke(1.dp, ClickitPeach),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(actionLabel, color = ClickitPeach)
                }
            }
        }
    }
}

@Composable
fun ErrorRetryCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitBurgundy.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, ClickitCrimson),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ClickitPeachSoft)
                Spacer(Modifier.width(8.dp))
                Text("Could not sync data", style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
            }
            Text(message, style = MaterialTheme.typography.bodySmall, color = ClickitPeachSoft)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ClickitPeach, contentColor = ClickitMidnight),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
fun DestructiveConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ClickitSlateDeep,
        titleContentColor = ClickitIvory,
        textContentColor = ClickitMutedText,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ClickitCrimson, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_destructive_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep", color = ClickitIvory)
            }
        }
    )
}

@Composable
fun RoleSwitcherSheetDialog(
    currentRole: UserRole,
    onSelectRole: (UserRole) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ClickitSlateDeep,
        title = {
            Text("Switch Mode", style = MaterialTheme.typography.titleLarge, color = ClickitIvory)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                UserRole.entries.forEach { role ->
                    val selected = role == currentRole
                    val roleIcon = when (role) {
                        UserRole.CUSTOMER -> Icons.Filled.ShoppingCart
                        UserRole.WORKER -> Icons.Filled.Storefront
                        UserRole.PARTNER -> Icons.AutoMirrored.Filled.DirectionsBike
                    }
                    Surface(
                        color = if (selected) ClickitBurgundy else ClickitMidnight,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (selected) ClickitPeach else ClickitSlate),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_role_${role.key}")
                            .minimumInteractiveComponentSize()
                            .clickable {
                                onSelectRole(role)
                                onDismiss()
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
                                size = 40.dp,
                                iconSize = 20.dp,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(role.label, style = MaterialTheme.typography.titleSmall, color = ClickitIvory)
                                Text(role.subtitle, style = MaterialTheme.typography.bodySmall, color = ClickitMutedText)
                            }
                            if (selected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Active", tint = ClickitPeach)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ClickitPeach)
            }
        }
    )
}
