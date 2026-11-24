package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.util.Locale

enum class UserRole(val key: String, val label: String, val subtitle: String) {
    CUSTOMER("customer", "Customer", "Book & track"),
    WORKER("worker", "Shop Owner", "Orders & menu"),
    PARTNER("partner", "Delivery", "Pickups & drops");

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: CUSTOMER
    }
}

enum class ServiceCategory(val key: String, val label: String, val tagline: String) {
    LAUNDRY("laundry", "Laundry", "Wash & press"),
    TAILORING("tailoring", "Tailoring", "Hems & fitting"),
    FOOTWEAR("footwear", "Footwear", "Repair & polish");

    companion object {
        fun fromRaw(raw: String?): ServiceCategory =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: LAUNDRY
    }
}

enum class FulfilmentMethod(
    val key: String,
    val label: String,
    val subtitle: String,
    val defaultDeliveryFeeMinor: Long
) {
    SHOP_DROP("shop_drop", "Shop Drop-Off", "Visit shop • ₹0", 0L),
    WORKER_DELIVERY("worker_delivery", "Shop Delivery", "Shop pickup & drop", 2500L),
    CLICKIT_PICKUP_RETURN(
        "clickit_pickup_return",
        "Smart Batch",
        "₹2/cloth • min ₹10",
        1000L
    );

    companion object {
        fun fromRaw(raw: String?): FulfilmentMethod =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: CLICKIT_PICKUP_RETURN
    }
}

enum class BookingStatus(val key: String, val label: String, val stepIndex: Int) {
    REQUESTED("requested", "Requested", 0),
    ACCEPTED("accepted", "Worker Accepted", 1),
    QUOTE_PENDING("quote_pending", "Quote Pending", 1),
    CONFIRMED("confirmed", "Confirmed", 2),
    PICKUP_SCHEDULED("pickup_scheduled", "Pickup Scheduled", 3),
    PICKED_UP("picked_up", "Picked Up", 4),
    RECEIVED_BY_WORKER("received_by_worker", "With Worker", 5),
    IN_PROGRESS("in_progress", "In Service", 6),
    READY("ready", "Work Ready", 7),
    RETURN_SCHEDULED("return_scheduled", "Return Scheduled", 8),
    OUT_FOR_DELIVERY("out_for_delivery", "Out for Delivery", 9),
    DELIVERED("delivered", "Delivered", 10),
    COMPLETED("completed", "Completed", 11),
    CANCELLED("cancelled", "Cancelled", -1),
    DISPUTED("disputed", "Disputed", -1);

    fun isActive(): Boolean = this != COMPLETED && this != CANCELLED

    fun trackerStageIndex(): Int = when (this) {
        REQUESTED, ACCEPTED, QUOTE_PENDING, CONFIRMED -> 0
        PICKUP_SCHEDULED, PICKED_UP -> 1
        RECEIVED_BY_WORKER, IN_PROGRESS -> 2
        READY, RETURN_SCHEDULED, OUT_FOR_DELIVERY -> 3
        DELIVERED, COMPLETED -> 4
        CANCELLED, DISPUTED -> -1
    }

    companion object {
        fun fromRaw(raw: String?): BookingStatus =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: REQUESTED
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val activeRole: String = "customer",
    val activeLocality: String = "",
    val phoneMasked: String = "",
    val photoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val houseNo: String = "",
    val street: String = "",
    val landmark: String = "",
    val locality: String = "",
    val city: String = "",
    val postalCode: String = "",
    val instructions: String = "",
    val defaultAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun formattedSummary(): String =
        listOf(houseNo, street, locality, city, postalCode)
            .filter { it.isNotBlank() }
            .joinToString(", ")
}

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "booking",
    val bookingId: String = "",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Shop(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val shopName: String = "",
    val category: String = "laundry",
    val tagline: String = "",
    val locality: String = "",
    val addressSummary: String = "",
    val hoursSummary: String = "8:30 AM – 8:30 PM",
    val turnaroundLabel: String = "24 hrs",
    val openStatus: Boolean = true,
    val published: Boolean = true,
    val verified: Boolean = true,
    val supportsShopDrop: Boolean = true,
    val supportsWorkerDelivery: Boolean = true,
    val supportsClickitBatch: Boolean = true,
    val dailyCapacity: Long = 35L,
    val activeLoad: Long = 0L,
    val ratingAvg: Double = 4.8,
    val ratingCount: Long = 0L,
    val startingPriceMinor: Long = 1200L,
    val startingPriceUnit: String = "cloth",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShopService(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val inclusions: String = "",
    val category: String = "laundry",
    val unit: String = "cloth",
    val priceMinor: Long = 1200L,
    val minQty: Long = 1L,
    val turnaroundHours: Long = 24L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Booking(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val workerOwnerId: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val category: String = "laundry",
    val assignedPartnerId: String = "",
    val status: String = "requested",
    val quoteStatus: String = "awaiting_worker_confirmation",
    val quoteNote: String = "",
    val fulfilmentMethod: String = "clickit_pickup_return",
    val slotWindowLabel: String = "",
    val batchEligible: Boolean = true,
    val batchOptIn: Boolean = true,
    val addressLabel: String = "",
    val addressSummary: String = "",
    val parcelCode: String = "",
    val handoverOtp: String = "",
    val deliveryOtp: String = "",
    val itemsSummary: String = "",
    val itemCount: Long = 1L,
    val weightKg: Double = 0.0,
    val serviceSubtotalMinor: Long = 0L,
    val deliveryFeeMinor: Long = 0L,
    val platformFeeMinor: Long = 100L,
    val totalMinor: Long = 0L,
    val paymentStatus: String = "pending",
    val milestone: String = "",
    val ratingStars: Long = 0L,
    val reviewText: String = "",
    val cancellationReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BookingTimelineEvent(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val workerOwnerId: String = "",
    val actorId: String = "",
    val actorRole: String = "system",
    val status: String = "",
    val title: String = "",
    val detail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryTask(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val workerOwnerId: String = "",
    val partnerId: String = "",
    val batchId: String = "",
    val taskType: String = "pickup",
    val status: String = "offered",
    val pickupZone: String = "",
    val dropZone: String = "",
    val shopName: String = "",
    val timeWindowLabel: String = "",
    val parcelCode: String = "",
    val itemCount: Long = 1L,
    val estimatedWeightKg: Double = 0.0,
    val payoutMinor: Long = 2000L,
    val batchStopIndex: Long = 1L,
    val batchTotalStops: Long = 1L,
    val exceptionReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookingId: String = "",
    val category: String = "Booking Issue",
    val subject: String = "",
    val description: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class PickupSlotOption(
    val id: String,
    val windowLabel: String,
    val batchEligible: Boolean,
    val batchFilledKg: Double,
    val batchCapacityKg: Double,
    val batchStopsCount: Int,
    val statusLabel: String
)

val DefaultPickupSlots = listOf(
    PickupSlotOption(
        id = "slot_morning",
        windowLabel = "09:00 AM – 10:00 AM",
        batchEligible = true,
        batchFilledKg = 14.0,
        batchCapacityKg = 25.0,
        batchStopsCount = 4,
        statusLabel = "Smart Batch Open • Shared Eco-Route"
    ),
    PickupSlotOption(
        id = "slot_midday",
        windowLabel = "12:00 PM – 01:30 PM",
        batchEligible = true,
        batchFilledKg = 6.5,
        batchCapacityKg = 25.0,
        batchStopsCount = 2,
        statusLabel = "Smart Batch Open • 2 nearby stops"
    ),
    PickupSlotOption(
        id = "slot_evening",
        windowLabel = "05:30 PM – 07:00 PM",
        batchEligible = false,
        batchFilledKg = 0.0,
        batchCapacityKg = 15.0,
        batchStopsCount = 1,
        statusLabel = "Standard Slot • Estimated window"
    )
)

fun formatInr(minorUnits: Long): String {
    val rupees = minorUnits / 100.0
    val indiaLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()
    val format = NumberFormat.getCurrencyInstance(indiaLocale)
    format.maximumFractionDigits = if (minorUnits % 100L == 0L) 0 else 2
    format.minimumFractionDigits = if (minorUnits % 100L == 0L) 0 else 2
    return format.format(rupees)
}

fun maskAddressForOffer(fullOrZone: String): String {
    val parts = fullOrZone.split(",").map { it.trim() }.filter { it.isNotBlank() }
    return if (parts.size >= 2) parts.takeLast(2).joinToString(", ") else fullOrZone
}
