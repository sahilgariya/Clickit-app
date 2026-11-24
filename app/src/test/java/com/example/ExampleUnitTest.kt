package com.example

import com.example.data.BookingStatus
import com.example.data.FulfilmentMethod
import com.example.data.ServiceCategory
import com.example.data.UserRole
import com.example.data.formatInr
import com.example.data.maskAddressForOffer
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
    }
}
