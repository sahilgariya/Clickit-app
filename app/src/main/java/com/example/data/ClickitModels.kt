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
    READY("ready", "Ready for Pickup", 7),
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
    val paymentMethod: String = "",
    val paymentRef: String = "",
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
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val dropLat: Double? = null,
    val dropLng: Double? = null,
    val agentLat: Double? = null,
    val agentLng: Double? = null,
    val agentBearing: Double? = null,
    val locationUpdatedAt: Timestamp? = null,
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

const val FREE_DELIVERY_WEIGHT_THRESHOLD_KG = 5.0
const val PLATFORM_FEE_MINOR = 100L
const val URGENT_SERVICE_SURCHARGE_RATE = 0.25

data class BookingFeeQuote(
    val baseServiceSubtotalMinor: Long,
    val emergencySurchargeMinor: Long,
    val serviceSubtotalMinor: Long,
    val deliveryFeeMinor: Long,
    val originalDeliveryFeeMinor: Long,
    val platformFeeMinor: Long,
    val totalMinor: Long,
    val isFreeDeliveryByWeight: Boolean,
    val isUrgentService: Boolean
)

fun calculateBookingFees(
    unitPriceMinor: Long,
    quantity: Int,
    weightKg: Double,
    fulfilment: FulfilmentMethod,
    batchOptIn: Boolean,
    batchEligible: Boolean,
    urgentService: Boolean = false
): BookingFeeQuote {
    val safeQty = quantity.coerceAtLeast(1)
    val baseSubtotal = unitPriceMinor * safeQty.toLong()
    val emergencySurcharge = if (urgentService) {
        kotlin.math.round(baseSubtotal * URGENT_SERVICE_SURCHARGE_RATE).toLong()
    } else {
        0L
    }
    val serviceSubtotal = baseSubtotal + emergencySurcharge
    val baseDeliveryFee = when (fulfilment) {
        FulfilmentMethod.SHOP_DROP -> 0L
        FulfilmentMethod.WORKER_DELIVERY -> 2500L
        FulfilmentMethod.CLICKIT_PICKUP_RETURN -> {
            val perCloth = safeQty.toLong() * 200L
            if (batchOptIn && batchEligible) perCloth.coerceAtLeast(1000L) else 2500L
        }
    }
    val freeByWeight = fulfilment != FulfilmentMethod.SHOP_DROP && weightKg > FREE_DELIVERY_WEIGHT_THRESHOLD_KG
    val effectiveDeliveryFee = if (freeByWeight) 0L else baseDeliveryFee
    return BookingFeeQuote(
        baseServiceSubtotalMinor = baseSubtotal,
        emergencySurchargeMinor = emergencySurcharge,
        serviceSubtotalMinor = serviceSubtotal,
        deliveryFeeMinor = effectiveDeliveryFee,
        originalDeliveryFeeMinor = baseDeliveryFee,
        platformFeeMinor = PLATFORM_FEE_MINOR,
        totalMinor = serviceSubtotal + effectiveDeliveryFee + PLATFORM_FEE_MINOR,
        isFreeDeliveryByWeight = freeByWeight,
        isUrgentService = urgentService
    )
}

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

data class GeoPointPair(val lat: Double, val lng: Double)

fun isValidCoordinate(lat: Double?, lng: Double?): Boolean =
    lat != null && lng != null && !lat.isNaN() && !lng.isNaN() &&
        lat in -90.0..90.0 && lng in -180.0..180.0

fun resolveZoneCoordinates(zoneText: String, seedHint: String = ""): GeoPointPair {
    val text = zoneText.lowercase(Locale.getDefault())
    val anchor = when {
        "indiranagar" in text -> GeoPointPair(12.9784, 77.6408)
        "koramangala" in text -> GeoPointPair(12.9352, 77.6245)
        "hsr" in text -> GeoPointPair(12.9121, 77.6446)
        "whitefield" in text -> GeoPointPair(12.9698, 77.7500)
        "jayanagar" in text -> GeoPointPair(12.9308, 77.5838)
        "malleshwaram" in text -> GeoPointPair(13.0031, 77.5643)
        "bandra" in text -> GeoPointPair(19.0596, 72.8295)
        "andheri" in text -> GeoPointPair(19.1136, 72.8697)
        "mumbai" in text -> GeoPointPair(19.0760, 72.8777)
        "connaught" in text || "delhi" in text -> GeoPointPair(28.6315, 77.2167)
        "gurugram" in text || "gurgaon" in text -> GeoPointPair(28.4595, 77.0266)
        "hyderabad" in text || "jubilee" in text -> GeoPointPair(17.4326, 78.4071)
        "pune" in text || "koregaon" in text -> GeoPointPair(18.5362, 73.8940)
        "chennai" in text || "adyar" in text -> GeoPointPair(13.0012, 80.2565)
        else -> GeoPointPair(12.9716, 77.5946)
    }
    val hash = (zoneText + "|" + seedHint).hashCode()
    val latOffset = (((hash and 0xFF) / 255.0) - 0.5) * 0.012
    val lngOffset = ((((hash shr 8) and 0xFF) / 255.0) - 0.5) * 0.012
    return GeoPointPair(
        lat = (anchor.lat + latOffset).coerceIn(-89.9, 89.9),
        lng = (anchor.lng + lngOffset).coerceIn(-179.9, 179.9)
    )
}

fun computeDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val sinLat = kotlin.math.sin(dLat / 2.0)
    val sinLng = kotlin.math.sin(dLng / 2.0)
    val a = sinLat * sinLat +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        sinLng * sinLng
    val c = 2.0 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt((1.0 - a).coerceAtLeast(0.0)))
    return earthRadiusKm * c
}

fun estimateEtaMinutes(distanceKm: Double, averageSpeedKmh: Double = 22.0): Int {
    if (distanceKm <= 0.05) return 1
    return kotlin.math.ceil((distanceKm / averageSpeedKmh) * 60.0).toInt().coerceIn(2, 120)
}
