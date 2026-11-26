package com.example

import com.example.data.BookingStatus
import com.example.data.FulfilmentMethod
import com.example.data.ServiceCategory
import com.example.data.UserRole
import com.example.data.formatInr
import com.example.data.maskAddressForOffer
import com.example.ui.components.smartBatchStageIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `formatInr formats whole rupees and paise cleanly`() {
        assertEquals("₹12", formatInr(1200L).replace("\u00A0", ""))
        assertEquals("₹199", formatInr(19900L).replace("\u00A0", ""))
        assertEquals("₹12.50", formatInr(1250L).replace("\u00A0", ""))
    }

    @Test
    fun `smart batch address masking preserves neighborhood locality only`() {
        val full = "Flat 402, 12th Main Road, Indiranagar, Bengaluru, 560038"
        assertEquals("Bengaluru, 560038", maskAddressForOffer(full))
    }

    @Test
    fun `enum parsing falls back gracefully on unknown raw keys`() {
        assertEquals(UserRole.CUSTOMER, UserRole.fromRaw("unknown"))
        assertEquals(UserRole.PARTNER, UserRole.fromRaw("partner"))
        assertEquals(ServiceCategory.TAILORING, ServiceCategory.fromRaw("tailoring"))
        assertEquals(FulfilmentMethod.CLICKIT_PICKUP_RETURN, FulfilmentMethod.fromRaw("clickit_pickup_return"))
        assertEquals(BookingStatus.PICKUP_SCHEDULED, BookingStatus.fromRaw("pickup_scheduled"))
    }

    @Test
    fun `booking status active filter and tracker stages map accurately`() {
        assertTrue(BookingStatus.REQUESTED.isActive())
        assertTrue(BookingStatus.QUOTE_PENDING.isActive())
        assertTrue(BookingStatus.IN_PROGRESS.isActive())
        assertTrue(BookingStatus.OUT_FOR_DELIVERY.isActive())
        assertTrue(BookingStatus.DELIVERED.isActive())
        assertTrue(BookingStatus.DISPUTED.isActive())
        assertFalse(BookingStatus.COMPLETED.isActive())
        assertFalse(BookingStatus.CANCELLED.isActive())

        assertEquals(0, BookingStatus.REQUESTED.trackerStageIndex())
        assertEquals(1, BookingStatus.PICKUP_SCHEDULED.trackerStageIndex())
        assertEquals(2, BookingStatus.IN_PROGRESS.trackerStageIndex())
        assertEquals(3, BookingStatus.OUT_FOR_DELIVERY.trackerStageIndex())
        assertEquals(4, BookingStatus.DELIVERED.trackerStageIndex())
        assertEquals(-1, BookingStatus.DISPUTED.trackerStageIndex())
        assertEquals("Ready for Pickup", BookingStatus.READY.label)
    }

    @Test
    fun `booking model stores rupay payment method and reference`() {
        val booking = com.example.data.Booking(
            id = "b1",
            paymentStatus = "paid",
            paymentMethod = "rupay_card",
            paymentRef = "RuPay Card •••• 5678"
        )
        assertEquals("rupay_card", booking.paymentMethod)
        assertEquals("RuPay Card •••• 5678", booking.paymentRef)
    }

    @Test
    fun `smart batch visual tracker maps statuses across 4 progress stages`() {
        assertEquals(0, com.example.ui.components.SmartBatchStage.ORDER_RECEIVED.stepNumber - 1)
        assertEquals(0, BookingStatus.REQUESTED.smartBatchStageIndex())
        assertEquals(1, BookingStatus.PICKUP_SCHEDULED.smartBatchStageIndex())
        assertEquals(1, BookingStatus.IN_PROGRESS.smartBatchStageIndex())
        assertEquals(2, BookingStatus.READY.smartBatchStageIndex())
        assertEquals(2, BookingStatus.OUT_FOR_DELIVERY.smartBatchStageIndex())
        assertEquals(3, BookingStatus.DELIVERED.smartBatchStageIndex())
        assertEquals(3, BookingStatus.COMPLETED.smartBatchStageIndex())
        assertEquals(-1, BookingStatus.CANCELLED.smartBatchStageIndex())
    }

    @Test
    fun `filterShops filters local providers by category and search query`() {
        val shops = listOf(
            com.example.data.Shop(id = "s1", shopName = "Sparkle Wash", category = "laundry", locality = "Indiranagar"),
            com.example.data.Shop(id = "s2", shopName = "Master Stitch", category = "tailoring", locality = "Koramangala"),
            com.example.data.Shop(id = "s3", shopName = "Sole Care", category = "footwear", locality = "HSR Layout")
        )

        val tailoringOnly = com.example.ui.screens.filterShops(shops, "", ServiceCategory.TAILORING)
        assertEquals(listOf("s2"), tailoringOnly.map { it.id })

        val footwearSearch = com.example.ui.screens.filterShops(shops, "footwear repair", null)
        assertEquals(listOf("s3"), footwearSearch.map { it.id })

        val laundrySearch = com.example.ui.screens.filterShops(shops, "Sparkle", ServiceCategory.LAUNDRY)
        assertEquals(listOf("s1"), laundrySearch.map { it.id })
    }

    @Test
    fun `order history filtering and stats calculate completed orders and reorder matching`() {
        val orders = listOf(
            com.example.data.Booking(
                id = "b1",
                shopId = "s1",
                category = "laundry",
                status = BookingStatus.COMPLETED.key,
                totalMinor = 15000L,
                batchOptIn = true,
                itemsSummary = "4 × Steam Press & Fold (₹15/cloth) • Starch collars"
            ),
            com.example.data.Booking(
                id = "b2",
                shopId = "s2",
                category = "tailoring",
                status = BookingStatus.COMPLETED.key,
                totalMinor = 9900L,
                batchOptIn = false,
                itemsSummary = "1 × Trouser Hemming (₹99/alteration)"
            ),
            com.example.data.Booking(
                id = "b3",
                shopId = "s3",
                category = "footwear",
                status = BookingStatus.IN_PROGRESS.key,
                totalMinor = 24900L,
                batchOptIn = true
            )
        )

        val allHistory = com.example.ui.screens.filterOrderHistory(orders, null)
        assertEquals(listOf("b1", "b2"), allHistory.map { it.id })

        val laundryHistory = com.example.ui.screens.filterOrderHistory(orders, ServiceCategory.LAUNDRY)
        assertEquals(listOf("b1"), laundryHistory.map { it.id })

        val stats = com.example.ui.screens.summarizeOrderHistory(orders)
        assertEquals(2, stats.completedCount)
        assertEquals(24900L, stats.totalSpentMinor)
        assertEquals(1, stats.smartBatchCount)

        val services = listOf(
            com.example.data.ShopService(id = "svc1", name = "Wash, Dry & Fold"),
            com.example.data.ShopService(id = "svc2", name = "Steam Press & Fold")
        )
        val matched = com.example.ui.screens.matchServiceForReorder(services, orders[0].itemsSummary)
        assertEquals("svc2", matched?.id)
        assertEquals("Starch collars", com.example.ui.screens.extractNotesFromSummary(orders[0].itemsSummary))
    }

    @Test
    fun `role based auth handler gates unauthenticated users and routes roles to dashboards`() {
        val unauth = com.example.data.resolveAuthDestination(
            uid = null,
            profile = com.example.data.UserProfile(userId = "u1", activeRole = "worker"),
            preselectedRole = UserRole.PARTNER
        )
        assertEquals(com.example.data.AuthDestination.Unauthenticated, unauth)

        val blankUid = com.example.data.resolveAuthDestination(uid = "  ", profile = null)
        assertEquals(com.example.data.AuthDestination.Unauthenticated, blankUid)

        val onboarding = com.example.data.resolveAuthDestination(
            uid = "u1",
            initialDisplayName = "Asha",
            profile = null,
            preselectedRole = UserRole.WORKER
        )
        assertTrue(onboarding is com.example.data.AuthDestination.NeedsOnboarding)
        val onboardingDest = onboarding as com.example.data.AuthDestination.NeedsOnboarding
        assertEquals(UserRole.WORKER, onboardingDest.preselectedRole)

        val customerDest = com.example.data.resolveAuthDestination(
            uid = "u1",
            profile = com.example.data.UserProfile(userId = "u1", activeRole = "customer")
        ) as com.example.data.AuthDestination.RedirectToDashboard
        assertEquals(UserRole.CUSTOMER, customerDest.role)
        assertEquals(com.example.data.DashboardRoute.CUSTOMER_HOME, customerDest.route)

        val shopperDest = com.example.data.resolveAuthDestination(
            uid = "u2",
            profile = com.example.data.UserProfile(userId = "u2", activeRole = "worker")
        ) as com.example.data.AuthDestination.RedirectToDashboard
        assertEquals(UserRole.WORKER, shopperDest.role)
        assertEquals(com.example.data.DashboardRoute.SHOPPER_DASHBOARD, shopperDest.route)

        val deliveryDest = com.example.data.resolveAuthDestination(
            uid = "u3",
            profile = com.example.data.UserProfile(userId = "u3", activeRole = "partner")
        ) as com.example.data.AuthDestination.RedirectToDashboard
        assertEquals(UserRole.PARTNER, deliveryDest.role)
        assertEquals(com.example.data.DashboardRoute.DELIVERY_OFFERS, deliveryDest.route)
    }

    @Test
    fun `smart batch live map resolves coordinates, projection, distance, ETA, and live agent GPS cleanly`() {
        assertTrue(com.example.data.isValidCoordinate(12.9716, 77.6412))
        assertFalse(com.example.data.isValidCoordinate(95.0, 77.6412))
        assertFalse(com.example.data.isValidCoordinate(12.9716, Double.NaN))
        assertFalse(com.example.data.isValidCoordinate(null, 77.6))

        val booking = com.example.data.Booking(
            id = "bk_map_1",
            shopId = "shop_ind",
            shopName = "FreshPress Fabric Studio",
            addressSummary = "Flat 402, 12th Main, Indiranagar, Bengaluru",
            parcelCode = "CLK-9012"
        )
        val taskWithAgent = com.example.data.DeliveryTask(
            id = "task_map_1",
            bookingId = "bk_map_1",
            parcelCode = "CLK-9012",
            shopName = "FreshPress Fabric Studio",
            pickupZone = "Indiranagar, Bengaluru",
            dropZone = "FreshPress Fabric Studio",
            pickupLat = 12.9716,
            pickupLng = 77.6412,
            dropLat = 12.9816,
            dropLng = 77.6512,
            agentLat = 12.9760,
            agentLng = 77.6450
        )

        val liveTelemetry = com.example.ui.components.resolveSmartBatchRouteTelemetry(booking, taskWithAgent)
        assertTrue(liveTelemetry.hasLiveAgentGps)
        assertEquals(12.9760, liveTelemetry.agentPoint?.lat ?: 0.0, 0.0001)
        assertTrue(liveTelemetry.distanceKm > 0.2)
        assertTrue(liveTelemetry.etaMinutes >= 2)

        val projected = com.example.ui.components.projectRoutePoints(
            pickupPoint = liveTelemetry.pickupPoint,
            dropPoint = liveTelemetry.dropPoint,
            agentPoint = liveTelemetry.agentPoint
        )
        assertTrue(projected.pickup.xFraction in 0.1f..0.9f)
        assertTrue(projected.drop.yFraction in 0.1f..0.9f)
        assertTrue((projected.agent?.xFraction ?: 0f) in 0.1f..0.9f)

        val pendingTelemetry = com.example.ui.components.resolveSmartBatchRouteTelemetry(
            booking,
            taskWithAgent.copy(agentLat = null, agentLng = null)
        )
        assertFalse(pendingTelemetry.hasLiveAgentGps)
        assertEquals(null, pendingTelemetry.agentPoint)
        assertEquals(12.9716, pendingTelemetry.pickupPoint.lat, 0.0001)
    }

    @Test
    fun `calculateBookingFees unlocks free delivery strictly above 5kg for delivery fulfilments`() {
        val standardBatch = com.example.data.calculateBookingFees(
            unitPriceMinor = 1500L,
            quantity = 6,
            weightKg = 3.0,
            fulfilment = FulfilmentMethod.CLICKIT_PICKUP_RETURN,
            batchOptIn = true,
            batchEligible = true
        )
        assertFalse(standardBatch.isFreeDeliveryByWeight)
        assertEquals(1200L, standardBatch.deliveryFeeMinor)
        assertEquals(1200L, standardBatch.originalDeliveryFeeMinor)
        assertEquals(9000L + 1200L + 100L, standardBatch.totalMinor)

        val exactBoundary = com.example.data.calculateBookingFees(
            unitPriceMinor = 1500L,
            quantity = 10,
            weightKg = 5.0,
            fulfilment = FulfilmentMethod.WORKER_DELIVERY,
            batchOptIn = false,
            batchEligible = false
        )
        assertFalse(exactBoundary.isFreeDeliveryByWeight)
        assertEquals(2500L, exactBoundary.deliveryFeeMinor)

        val justOverThreshold = com.example.data.calculateBookingFees(
            unitPriceMinor = 1500L,
            quantity = 10,
            weightKg = 5.1,
            fulfilment = FulfilmentMethod.CLICKIT_PICKUP_RETURN,
            batchOptIn = true,
            batchEligible = true
        )
        assertTrue(justOverThreshold.isFreeDeliveryByWeight)
        assertEquals(0L, justOverThreshold.deliveryFeeMinor)
        assertEquals(2000L, justOverThreshold.originalDeliveryFeeMinor)
        assertEquals(15000L + 100L, justOverThreshold.totalMinor)

        val heavyWorkerDelivery = com.example.data.calculateBookingFees(
            unitPriceMinor = 2000L,
            quantity = 8,
            weightKg = 7.5,
            fulfilment = FulfilmentMethod.WORKER_DELIVERY,
            batchOptIn = false,
            batchEligible = true
        )
        assertTrue(heavyWorkerDelivery.isFreeDeliveryByWeight)
        assertEquals(0L, heavyWorkerDelivery.deliveryFeeMinor)
        assertEquals(2500L, heavyWorkerDelivery.originalDeliveryFeeMinor)
        assertEquals(16000L + 100L, heavyWorkerDelivery.totalMinor)

        val shopDropHeavy = com.example.data.calculateBookingFees(
            unitPriceMinor = 1500L,
            quantity = 12,
            weightKg = 7.5,
            fulfilment = FulfilmentMethod.SHOP_DROP,
            batchOptIn = false,
            batchEligible = false
        )
        assertFalse(shopDropHeavy.isFreeDeliveryByWeight)
        assertEquals(0L, shopDropHeavy.deliveryFeeMinor)
        assertEquals(0L, shopDropHeavy.originalDeliveryFeeMinor)

        val urgentHeavyBundle = com.example.data.calculateBookingFees(
            unitPriceMinor = 2000L,
            quantity = 10,
            weightKg = 6.0,
            fulfilment = FulfilmentMethod.CLICKIT_PICKUP_RETURN,
            batchOptIn = true,
            batchEligible = true,
            urgentService = true
        )
        assertTrue(urgentHeavyBundle.isUrgentService)
        assertTrue(urgentHeavyBundle.isFreeDeliveryByWeight)
        assertEquals(20000L, urgentHeavyBundle.baseServiceSubtotalMinor)
        assertEquals(5000L, urgentHeavyBundle.emergencySurchargeMinor)
        assertEquals(25000L, urgentHeavyBundle.serviceSubtotalMinor)
        assertEquals(0L, urgentHeavyBundle.deliveryFeeMinor)
        assertEquals(25000L + 100L, urgentHeavyBundle.totalMinor)
    }

    @Test
    fun `maskEmail redacts local part while preserving domain and handles edge inputs`() {
        assertEquals("sa***@gmail.com", com.example.data.maskEmail("sahilgariya@gmail.com"))
        assertEquals("a***@example.org", com.example.data.maskEmail("a@example.org"))
        assertEquals("***", com.example.data.maskEmail("invalid-email"))
        assertEquals("", com.example.data.maskEmail(""))
        assertEquals(null, com.example.data.maskEmail(null))
    }
}



