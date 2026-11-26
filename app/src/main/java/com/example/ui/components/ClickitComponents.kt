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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import com.example.ui.theme.ClickitBorder
import com.example.ui.theme.ClickitBrandTeal
import com.example.ui.theme.ClickitBrandTealDark
import com.example.ui.theme.ClickitBrandTealTint
import com.example.ui.theme.ClickitCrimson
import com.example.ui.theme.ClickitDangerContainer
import com.example.ui.theme.ClickitLightBg
import com.example.ui.theme.ClickitLightSurface
import com.example.ui.theme.ClickitLightSurfaceVar
import com.example.ui.theme.ClickitSuccess
import com.example.ui.theme.ClickitSuccessContainer
import com.example.ui.theme.ClickitTextPrimary
import com.example.ui.theme.ClickitTextSecondary
import com.example.ui.theme.ClickitWarning
import com.example.ui.theme.ClickitWarningContainer

@Composable
fun ClickitBrandLogoMark(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(
                width = 1.dp,
                color = ClickitBorder,
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
    fontSizeSp: Float = 19f,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = ClickitTextPrimary, fontWeight = FontWeight.Bold)) {
                append("Click")
            }
            withStyle(SpanStyle(color = ClickitBrandTeal, fontWeight = FontWeight.Bold)) {
                append("it")
            }
        },
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = fontSizeSp.sp,
            letterSpacing = (-0.3).sp
        ),
        modifier = modifier
    )
}

@Composable
fun NeonIconBadge(
    icon: ImageVector,
    tint: Color = ClickitBrandTeal,
    glowColor: Color = ClickitBrandTeal,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(ClickitLightSurfaceVar),
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
        UserRole.CUSTOMER -> localityLabel.ifBlank { "Nearby" }
        UserRole.WORKER -> localityLabel.ifBlank { "My Shop" }
        UserRole.PARTNER -> localityLabel.ifBlank { "Active Zone" }
    }
    val roleChipLabel = when (activeRole) {
        UserRole.CUSTOMER -> "Customer"
        UserRole.WORKER -> "Shopper"
        UserRole.PARTNER -> "Delivery"
    }
    Surface(
        color = ClickitLightSurface,
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
                    ClickitBrandLogoMark(size = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ClickitWordmark(fontSizeSp = 18f)
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = ClickitTextSecondary,
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
                            color = ClickitLightSurfaceVar,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ClickitBorder),
                            modifier = Modifier
                                .testTag("header_role_pill")
                                .minimumInteractiveComponentSize()
                                .clickable(onClick = onRoleClick)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = roleChipLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = ClickitTextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Filled.SwapHoriz,
                                    contentDescription = null,
                                    tint = ClickitTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
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
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = ClickitTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = ClickitBorder)
        }
    }
}

@Composable
fun BookingStatusBadge(statusRaw: String, modifier: Modifier = Modifier) {
    val status = BookingStatus.fromRaw(statusRaw)
    val (bg, fg) = when (status) {
        BookingStatus.COMPLETED, BookingStatus.DELIVERED -> ClickitSuccessContainer to ClickitSuccess
        BookingStatus.CANCELLED, BookingStatus.DISPUTED -> ClickitDangerContainer to ClickitCrimson
        BookingStatus.QUOTE_PENDING -> ClickitWarningContainer to ClickitWarning
        BookingStatus.PICKUP_SCHEDULED, BookingStatus.PICKED_UP,
        BookingStatus.RETURN_SCHEDULED, BookingStatus.OUT_FOR_DELIVERY -> ClickitBrandTealTint to ClickitBrandTeal
        else -> ClickitLightSurfaceVar to ClickitTextPrimary
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CategoryBadge(categoryRaw: String, modifier: Modifier = Modifier) {
    val cat = ServiceCategory.fromRaw(categoryRaw)
    Surface(
        color = ClickitLightSurfaceVar,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = cat.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = ClickitTextSecondary,
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
        "quote_revision_sent" -> "Updated"
        "awaiting_worker_confirmation" -> "Estimate"
        else -> "Estimate"
    }
    val statusColor = when (quoteStatus) {
        "confirmed" -> ClickitSuccess
        "quote_revision_sent" -> ClickitWarning
        else -> ClickitTextSecondary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
        border = BorderStroke(1.dp, ClickitBorder),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Price Summary", style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
            if (emergencySurchargeMinor > 0L) {
                val baseService = (serviceSubtotalMinor - emergencySurchargeMinor).coerceAtLeast(0L)
                PriceRow("Service", formatInr(baseService))
                PriceRow(
                    label = "Urgent (+25%)",
                    value = "+${formatInr(emergencySurchargeMinor)}",
                    valueColor = ClickitWarning
                )
            } else {
                PriceRow(
                    label = if (isUrgentService) "Service (+25% urgent)" else "Service",
                    value = formatInr(serviceSubtotalMinor)
                )
            }
            val deliveryLabel = when {
                isFreeDeliveryByWeight && savedDeliveryMinor > 0L ->
                    "Free (5+ kg)"
                isFreeDeliveryByWeight -> "Free (5+ kg)"
                deliveryFeeMinor == 0L -> "₹0"
                else -> formatInr(deliveryFeeMinor)
            }
            PriceRow(
                label = "Delivery",
                value = deliveryLabel,
                valueColor = if (deliveryFeeMinor == 0L) ClickitSuccess else ClickitTextPrimary
            )
            PriceRow("Platform fee", formatInr(platformFeeMinor))
            HorizontalDivider(color = ClickitBorder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", style = MaterialTheme.typography.titleMedium, color = ClickitTextPrimary)
                Text(
                    text = formatInr(totalMinor),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ClickitBrandTeal
                )
            }
        }
    }
}

@Composable
private fun PriceRow(
    label: String,
    value: String,
    valueColor: Color = ClickitTextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
    }
}

@Composable
fun TrustProtectionStrip(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        color = ClickitSuccessContainer,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ClickitSuccess.copy(alpha = 0.22f)),
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
                    text = "Order Protection",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ClickitTextPrimary
                )
                Text(
                    text = "QR tagged • ₹2,000 cover • Free re-service",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClickitTextSecondary
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
        colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
        border = BorderStroke(1.dp, if (isDelayed) ClickitWarning else ClickitBorder),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                color = ClickitLightBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ClickitBorder),
                modifier = Modifier.size(60.dp)
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
                                    color = Color(0xFF111827),
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
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = "QR garment tag",
                        tint = ClickitBrandTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = parcelCode,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = ClickitTextPrimary
                    )
                }
                Text(
                    text = "$itemCount items • $weightKg kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = ClickitTextSecondary
                )
                Text(
                    text = slotWindowLabel.ifBlank { "Scheduled" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDelayed) ClickitWarning else ClickitBrandTeal
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
                            Surface(color = ClickitDangerContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "DELAY FLAGGED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ClickitCrimson,
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
                color = ClickitBrandTeal
            )
            Text(
                text = "${filledKg.toInt()} / ${capacityKg.toInt()} kg",
                style = MaterialTheme.typography.labelMedium,
                color = ClickitTextPrimary
            )
        }
        LinearProgressIndicator(
            progress = { ratio },
            color = ClickitBrandTeal,
            trackColor = ClickitLightSurfaceVar,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
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
        colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
        border = BorderStroke(1.dp, ClickitBorder),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = ClickitBrandTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Parcel $parcelCode", style = MaterialTheme.typography.titleSmall, color = ClickitBrandTeal)
                }
                Spacer(Modifier.height(4.dp))
                Text(otpLabel, style = MaterialTheme.typography.bodyMedium, color = ClickitTextPrimary)
                Text(helperNote, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                color = ClickitBrandTealTint,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ClickitBrandTeal.copy(alpha = 0.35f))
            ) {
                Text(
                    text = otpCode,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = ClickitBrandTealDark,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingStateView(message: String = "Loading...", modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(color = ClickitBrandTeal, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = ClickitTextSecondary)
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
        colors = CardDefaults.cardColors(containerColor = ClickitLightSurface),
        border = BorderStroke(1.dp, ClickitBorder),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ClickitLightSurfaceVar),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ClickitTextSecondary, modifier = Modifier.size(22.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = ClickitTextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onAction,
                    border = BorderStroke(1.dp, ClickitBrandTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(actionLabel, color = ClickitBrandTeal)
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
        colors = CardDefaults.cardColors(containerColor = ClickitDangerContainer),
        border = BorderStroke(1.dp, ClickitCrimson.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ClickitCrimson)
                Spacer(Modifier.width(8.dp))
                Text("Sync failed", style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
            }
            Text(message, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ClickitBrandTeal, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
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
        containerColor = ClickitLightSurface,
        titleContentColor = ClickitTextPrimary,
        textContentColor = ClickitTextSecondary,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ClickitCrimson, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_destructive_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep", color = ClickitTextPrimary)
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
        containerColor = ClickitLightSurface,
        title = {
            Text("Switch Role", style = MaterialTheme.typography.titleLarge, color = ClickitTextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UserRole.entries.forEach { role ->
                    val selected = role == currentRole
                    val roleIcon = when (role) {
                        UserRole.CUSTOMER -> Icons.Filled.ShoppingCart
                        UserRole.WORKER -> Icons.Filled.Storefront
                        UserRole.PARTNER -> Icons.AutoMirrored.Filled.DirectionsBike
                    }
                    Surface(
                        color = if (selected) ClickitBrandTealTint else ClickitLightSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (selected) ClickitBrandTeal else ClickitBorder),
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
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NeonIconBadge(
                                icon = roleIcon,
                                tint = if (selected) ClickitBrandTeal else ClickitTextSecondary,
                                size = 38.dp,
                                iconSize = 19.dp,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(role.label, style = MaterialTheme.typography.titleSmall, color = ClickitTextPrimary)
                                Text(role.subtitle, style = MaterialTheme.typography.bodySmall, color = ClickitTextSecondary)
                            }
                            if (selected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Active", tint = ClickitBrandTeal)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ClickitBrandTeal)
            }
        }
    )
}
