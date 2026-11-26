package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingStatus
import com.example.ui.theme.ClickitBorder
import com.example.ui.theme.ClickitBrandTeal
import com.example.ui.theme.ClickitBrandTealTint
import com.example.ui.theme.ClickitLightBg
import com.example.ui.theme.ClickitLightSurface
import com.example.ui.theme.ClickitLightSurfaceVar
import com.example.ui.theme.ClickitSuccess
import com.example.ui.theme.ClickitTextPrimary
import com.example.ui.theme.ClickitTextSecondary

enum class SmartBatchStage(
    val stepNumber: Int,
    val label: String,
    val icon: ImageVector
) {
    ORDER_RECEIVED(1, "Order\nReceived", Icons.AutoMirrored.Filled.ReceiptLong),
    IN_TRANSIT(2, "In\nTransit", Icons.Filled.LocalShipping),
    READY_FOR_PICKUP(3, "Ready for\nPickup", Icons.Filled.Inventory2),
    DELIVERED(4, "Delivered", Icons.Filled.DoneAll)
}

fun BookingStatus.smartBatchStageIndex(): Int = when (this) {
    BookingStatus.REQUESTED,
    BookingStatus.ACCEPTED,
    BookingStatus.QUOTE_PENDING,
    BookingStatus.CONFIRMED -> 0

    BookingStatus.PICKUP_SCHEDULED,
    BookingStatus.PICKED_UP,
    BookingStatus.RECEIVED_BY_WORKER,
    BookingStatus.IN_PROGRESS -> 1

    BookingStatus.READY,
    BookingStatus.RETURN_SCHEDULED,
    BookingStatus.OUT_FOR_DELIVERY -> 2

    BookingStatus.DELIVERED,
    BookingStatus.COMPLETED -> 3

    BookingStatus.CANCELLED,
    BookingStatus.DISPUTED -> -1
}

@Composable
fun SmartBatchVisualTracker(
    status: BookingStatus,
    isSmartBatch: Boolean = true,
    slotWindowLabel: String = "",
    modifier: Modifier = Modifier
) {
    val stages = SmartBatchStage.entries
    val rawStage = status.smartBatchStageIndex()
    val currentStage = rawStage.coerceIn(0, stages.lastIndex)

    val animatedProgress by animateFloatAsState(
        targetValue = currentStage / stages.lastIndex.toFloat(),
        animationSpec = spring(stiffness = 320f),
        label = "smart_batch_progress"
    )

    Surface(
        color = ClickitLightBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ClickitBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_batch_visual_tracker")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = ClickitBrandTealTint,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isSmartBatch) "Smart Batch" else "Pickup & Return",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ClickitBrandTeal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (slotWindowLabel.isNotBlank()) {
                        Text(
                            text = slotWindowLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = ClickitTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "Step ${currentStage + 1}/${stages.size}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ClickitTextSecondary
                )
            }

            // Progress rail + clean stage circles
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ClickitBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ClickitBrandTeal)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stages.forEachIndexed { idx, stage ->
                        val isCompleted = idx < currentStage
                        val isCurrent = idx == currentStage
                        val accent = when {
                            isCompleted -> ClickitSuccess
                            isCurrent -> ClickitBrandTeal
                            else -> ClickitBorder
                        }
                        val fill = when {
                            isCompleted -> ClickitSuccess
                            isCurrent -> ClickitLightSurface
                            else -> ClickitLightSurfaceVar
                        }
                        val iconTint = when {
                            isCompleted -> Color.White
                            isCurrent -> ClickitBrandTeal
                            else -> ClickitTextSecondary
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("tracker_step_${stage.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 28.dp else 24.dp)
                                    .clip(CircleShape)
                                    .background(fill)
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = accent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCompleted) Icons.Filled.Check else stage.icon,
                                    contentDescription = stage.label.replace("\n", " "),
                                    tint = iconTint,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Stage labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                stages.forEachIndexed { idx, stage ->
                    val labelColor = when {
                        idx < currentStage -> ClickitSuccess
                        idx == currentStage -> ClickitTextPrimary
                        else -> ClickitTextSecondary
                    }
                    Text(
                        text = stage.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            fontWeight = if (idx == currentStage) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = labelColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
