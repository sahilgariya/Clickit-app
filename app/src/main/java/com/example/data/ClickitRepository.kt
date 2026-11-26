package com.example.data

import android.content.Context
import android.location.Geocoder
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.Locale

class ClickitRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val appContext: Context? = null
) {
    private val secureRandom = SecureRandom()

    companion object {
        fun getConfiguredFirestore(context: Context): FirebaseFirestore {
            val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
            val instance = FirebaseFirestore.getInstance(databaseId)
            runCatching {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(100L * 1024L * 1024L)
                            .build()
                    )
                    .build()
                instance.firestoreSettings = settings
            }
            return instance
        }
    }

    constructor(context: Context) : this(
        db = getConfiguredFirestore(context),
        appContext = context.applicationContext
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.logFirestoreErrors(op: OperationType, path: String, uid: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- REALTIME STREAMS ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    suspend fun fetchUserProfile(userId: String): Result<UserProfile?> = try {
        val uid = requireUserId()
        check(uid == userId)
        val snap = db.collection("users").document(uid).get().await()
        val profile = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        Result.success(profile)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, "users/$userId")
        Result.failure(e)
    }

    fun observeAddresses(userId: String): Flow<List<SavedAddress>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/addresses"
        emitAll(
            db.collection("users").document(uid).collection("addresses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.defaultAddress }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observePublishedShops(): Flow<List<Shop>> = flow {
        val uid = requireUserId()
        val path = "shops"
        emitAll(
            db.collection("shops")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.ratingAvg }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeOwnedShops(userId: String): Flow<List<Shop>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "shops"
        emitAll(
            db.collection("shops")
                .whereEqualTo("ownerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeActiveShopServices(shopId: String): Flow<List<ShopService>> = flow {
        val uid = requireUserId()
        val path = "shops/$shopId/services"
        emitAll(
            db.collection("shops").document(shopId).collection("services")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ShopService::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.priceMinor }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeOwnedShopServices(shopId: String, userId: String): Flow<List<ShopService>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "shops/$shopId/services"
        emitAll(
            db.collection("shops").document(shopId).collection("services")
                .whereEqualTo("ownerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ShopService::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeCustomerBookings(userId: String): Flow<List<Booking>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "bookings"
        emitAll(
            db.collection("bookings")
                .whereEqualTo("customerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeActiveCustomerBookings(userId: String): Flow<List<Booking>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "bookings"
        emitAll(
            db.collection("bookings")
                .whereEqualTo("customerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { BookingStatus.fromRaw(it.status).isActive() }
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeCompletedOrderHistory(userId: String): Flow<List<Booking>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "bookings"
        emitAll(
            db.collection("bookings")
                .whereEqualTo("customerId", uid)
                .whereEqualTo("status", BookingStatus.COMPLETED.key)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeWorkerBookings(userId: String): Flow<List<Booking>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "bookings"
        emitAll(
            db.collection("bookings")
                .whereEqualTo("workerOwnerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeBookingTimeline(bookingId: String, userId: String, asWorker: Boolean): Flow<List<BookingTimelineEvent>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "bookings/$bookingId/timeline"
        val filterField = if (asWorker) "workerOwnerId" else "customerId"
        emitAll(
            db.collection("bookings").document(bookingId).collection("timeline")
                .whereEqualTo(filterField, uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BookingTimelineEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeOfferedDeliveryTasks(): Flow<List<DeliveryTask>> = flow {
        val uid = requireUserId()
        val path = "delivery_tasks"
        emitAll(
            db.collection("delivery_tasks")
                .whereEqualTo("status", "offered")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DeliveryTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.batchStopIndex }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observePartnerDeliveryTasks(userId: String): Flow<List<DeliveryTask>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "delivery_tasks"
        emitAll(
            db.collection("delivery_tasks")
                .whereEqualTo("partnerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DeliveryTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeCustomerDeliveryTasks(userId: String): Flow<List<DeliveryTask>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "delivery_tasks"
        emitAll(
            db.collection("delivery_tasks")
                .whereEqualTo("customerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DeliveryTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observePartnerBookings(userId: String): Flow<List<Booking>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "bookings"
        emitAll(
            db.collection("bookings")
                .whereEqualTo("assignedPartnerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeSupportTickets(userId: String): Flow<List<SupportTicket>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "support_tickets"
        emitAll(
            db.collection("support_tickets")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    // --- ROLE ONBOARDING & PROFILE MUTATIONS ---

    suspend fun completeCustomerOnboarding(
        displayName: String,
        locality: String,
        phone: String,
        initialAddress: SavedAddress?
    ): Result<Unit> {
        var currentPath = "users/${auth.currentUser?.uid}"
        return try {
            val uid = requireUserId()
            currentPath = "users/$uid"
            val nameClean = displayName.trim()
            val locClean = locality.trim()
            require(nameClean.length in 1..60) { "Name must be 1–60 characters." }
            require(locClean.length in 1..80) { "Area / City must be 1–80 characters." }

            if (initialAddress != null) {
                require(initialAddress.houseNo.trim().length in 1..60) { "House / Flat no. is required." }
                require(initialAddress.street.trim().length in 1..100) { "Street is required." }
                require(initialAddress.locality.trim().length in 1..60) { "Locality is required." }
                require(initialAddress.city.trim().length in 1..50) { "City is required." }
                require(initialAddress.postalCode.trim().length in 3..12) { "Valid PIN code is required." }
            }

            val profilePayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to nameClean,
                "activeRole" to UserRole.CUSTOMER.key,
                "activeLocality" to locClean,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val phoneClean = phone.trim().take(24)
            if (phoneClean.isNotEmpty()) profilePayload["phoneMasked"] = phoneClean
            val authPhoto = auth.currentUser?.photoUrl?.toString()?.trim()?.take(1000).orEmpty()
            if (authPhoto.isNotEmpty()) profilePayload["photoUrl"] = authPhoto

            val userRef = db.collection("users").document(uid)
            userRef.set(profilePayload).await()

            if (initialAddress != null) {
                currentPath = "users/$uid/addresses"
                val addrPayload = mapOf(
                    "userId" to uid,
                    "label" to initialAddress.label.trim().ifBlank { "Home" }.take(30),
                    "houseNo" to initialAddress.houseNo.trim(),
                    "street" to initialAddress.street.trim(),
                    "landmark" to initialAddress.landmark.trim().takeIf { it.isNotEmpty() },
                    "locality" to initialAddress.locality.trim(),
                    "city" to initialAddress.city.trim(),
                    "postalCode" to initialAddress.postalCode.trim(),
                    "instructions" to initialAddress.instructions.trim().takeIf { it.isNotEmpty() },
                    "defaultAddress" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                userRef.collection("addresses").document().set(addrPayload).await()
            }
            ensureStarterMarketplaceIfEmpty(locClean)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun completeWorkerOnboarding(
        displayName: String,
        locality: String,
        phone: String,
        shop: Shop,
        initialServiceName: String,
        initialServicePriceMinor: Long,
        initialServiceUnit: String
    ): Result<Unit> {
        var currentPath = "users/${auth.currentUser?.uid}"
        return try {
            val uid = requireUserId()
            currentPath = "users/$uid"
            val nameClean = displayName.trim()
            val locClean = locality.trim()
            val shopNameClean = shop.shopName.trim()
            val addressClean = shop.addressSummary.trim()
            val svcNameClean = initialServiceName.trim()
            val svcUnitClean = initialServiceUnit.trim().ifBlank { "item" }.take(24)

            require(nameClean.length in 1..60) { "Name must be 1–60 characters." }
            require(locClean.length in 1..80) { "Area / City is required." }
            require(shopNameClean.length in 2..80) { "Shop name must be 2–80 characters." }
            require(addressClean.length in 1..160) { "Shop address is required." }
            require(svcNameClean.length in 2..80) { "Service name must be 2–80 characters." }
            require(initialServicePriceMinor in 100L..10000000L) { "Price must be at least ₹1." }

            val profilePayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to nameClean,
                "activeRole" to UserRole.WORKER.key,
                "activeLocality" to locClean,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val phoneClean = phone.trim().take(24)
            if (phoneClean.isNotEmpty()) profilePayload["phoneMasked"] = phoneClean
            val authPhoto = auth.currentUser?.photoUrl?.toString()?.trim()?.take(1000).orEmpty()
            if (authPhoto.isNotEmpty()) profilePayload["photoUrl"] = authPhoto

            db.collection("users").document(uid).set(profilePayload).await()

            currentPath = "shops"
            val shopRef = db.collection("shops").document()
            val shopId = shopRef.id
            shopRef.set(
                mapOf(
                    "ownerId" to uid,
                    "shopName" to shopNameClean,
                    "category" to shop.category,
                    "tagline" to shop.tagline.trim().take(140),
                    "locality" to shop.locality.trim().ifBlank { locClean }.take(80),
                    "addressSummary" to addressClean,
                    "hoursSummary" to shop.hoursSummary.trim().ifBlank { "9:00 AM – 8:30 PM" }.take(80),
                    "turnaroundLabel" to shop.turnaroundLabel.trim().ifBlank { "24 hrs" }.take(40),
                    "openStatus" to true,
                    "published" to true,
                    "verified" to true,
                    "supportsShopDrop" to shop.supportsShopDrop,
                    "supportsWorkerDelivery" to shop.supportsWorkerDelivery,
                    "supportsClickitBatch" to shop.supportsClickitBatch,
                    "dailyCapacity" to shop.dailyCapacity.coerceIn(1L, 500L),
                    "activeLoad" to 0L,
                    "ratingAvg" to 5.0,
                    "ratingCount" to 0L,
                    "startingPriceMinor" to initialServicePriceMinor,
                    "startingPriceUnit" to svcUnitClean,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            currentPath = "shops/$shopId/services"
            shopRef.collection("services").document().set(
                mapOf(
                    "shopId" to shopId,
                    "ownerId" to uid,
                    "name" to svcNameClean,
                    "description" to "",
                    "category" to shop.category,
                    "unit" to svcUnitClean,
                    "priceMinor" to initialServicePriceMinor,
                    "minQty" to 1L,
                    "turnaroundHours" to 24L,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun completePartnerOnboarding(
        displayName: String,
        locality: String,
        phone: String
    ): Result<Unit> {
        var currentPath = "users/${auth.currentUser?.uid}"
        return try {
            val uid = requireUserId()
            currentPath = "users/$uid"
            val nameClean = displayName.trim()
            val locClean = locality.trim()
            require(nameClean.length in 1..60) { "Name must be 1–60 characters." }
            require(locClean.length in 1..80) { "Operating zone is required." }

            val profilePayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to nameClean,
                "activeRole" to UserRole.PARTNER.key,
                "activeLocality" to locClean,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val phoneClean = phone.trim().take(24)
            if (phoneClean.isNotEmpty()) profilePayload["phoneMasked"] = phoneClean
            val authPhoto = auth.currentUser?.photoUrl?.toString()?.trim()?.take(1000).orEmpty()
            if (authPhoto.isNotEmpty()) profilePayload["photoUrl"] = authPhoto

            db.collection("users").document(uid).set(profilePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun updateActiveRole(role: UserRole): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).update(
            mapOf(
                "activeRole" to role.key,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
        Result.failure(e)
    }

    suspend fun updateUserProfile(
        displayName: String,
        locality: String,
        phoneMasked: String,
        photoUrl: String? = null
    ): Result<Unit> = try {
        val uid = requireUserId()
        val nameClean = displayName.trim()
        val locClean = locality.trim()
        require(nameClean.length in 1..60) { "Name must be 1–60 characters." }
        require(locClean.length in 1..80) { "Locality must be 1–80 characters." }
        if (photoUrl != null) {
            require(photoUrl.length <= 200_000) { "Profile photo exceeds maximum allowed size." }
        }
        val updates = mutableMapOf<String, Any>(
            "displayName" to nameClean,
            "activeLocality" to locClean,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val phoneClean = phoneMasked.trim().take(24)
        updates["phoneMasked"] = phoneClean
        if (photoUrl != null) updates["photoUrl"] = photoUrl
        db.collection("users").document(uid).update(updates).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
        Result.failure(e)
    }

    suspend fun updateProfilePhoto(photoUrl: String): Result<Unit> = try {
        val uid = requireUserId()
        require(photoUrl.length <= 200_000) { "Profile photo exceeds maximum allowed size." }
        db.collection("users").document(uid).update(
            mapOf(
                "photoUrl" to photoUrl,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
        Result.failure(e)
    }

    suspend fun saveAddress(address: SavedAddress): Result<Unit> = try {
        val uid = requireUserId()
        require(address.label.trim().length in 1..30) { "Label is required (max 30 chars)." }
        require(address.houseNo.trim().length in 1..60) { "House / Flat no. is required." }
        require(address.street.trim().length in 1..100) { "Street is required." }
        require(address.locality.trim().length in 1..60) { "Locality is required." }
        require(address.city.trim().length in 1..50) { "City is required." }
        require(address.postalCode.trim().length in 3..12) { "Valid postal code is required." }

        val colRef = db.collection("users").document(uid).collection("addresses")
        val docRef = if (address.id.isNotBlank()) colRef.document(address.id) else colRef.document()

        if (address.id.isBlank()) {
            val payload = mapOf(
                "userId" to uid,
                "label" to address.label.trim(),
                "houseNo" to address.houseNo.trim(),
                "street" to address.street.trim(),
                "landmark" to address.landmark.trim().takeIf { it.isNotEmpty() },
                "locality" to address.locality.trim(),
                "city" to address.city.trim(),
                "postalCode" to address.postalCode.trim(),
                "instructions" to address.instructions.trim().takeIf { it.isNotEmpty() },
                "defaultAddress" to address.defaultAddress,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
        } else {
            val updates = mapOf(
                "label" to address.label.trim(),
                "houseNo" to address.houseNo.trim(),
                "street" to address.street.trim(),
                "landmark" to address.landmark.trim().take(80),
                "locality" to address.locality.trim(),
                "city" to address.city.trim(),
                "postalCode" to address.postalCode.trim(),
                "instructions" to address.instructions.trim().take(160),
                "defaultAddress" to address.defaultAddress,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/addresses")
        Result.failure(e)
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> = try {
        val uid = requireUserId()
        require(addressId.isNotBlank())
        db.collection("users").document(uid).collection("addresses").document(addressId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/addresses/$addressId")
        Result.failure(e)
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("notifications").document(notificationId)
            .update(mapOf("readFlag" to true, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/notifications/$notificationId")
        Result.failure(e)
    }

    // --- WORKER DUKAAN & CATALOGUE ---

    suspend fun saveShop(shop: Shop): Result<Unit> = try {
        val uid = requireUserId()
        require(shop.shopName.trim().length in 2..80) { "Shop name must be 2–80 characters." }
        require(shop.locality.trim().length in 1..80) { "Locality is required." }
        require(shop.addressSummary.trim().length in 1..160) { "Address is required." }
        require(shop.dailyCapacity in 1..500) { "Daily capacity must be 1–500." }

        val colRef = db.collection("shops")
        if (shop.id.isBlank()) {
            val docRef = colRef.document()
            docRef.set(
                mapOf(
                    "ownerId" to uid,
                    "shopName" to shop.shopName.trim(),
                    "category" to shop.category,
                    "tagline" to shop.tagline.trim().take(140),
                    "locality" to shop.locality.trim(),
                    "addressSummary" to shop.addressSummary.trim(),
                    "hoursSummary" to shop.hoursSummary.trim().ifBlank { "9:00 AM – 8:00 PM" },
                    "turnaroundLabel" to shop.turnaroundLabel.trim().ifBlank { "24 hrs" },
                    "openStatus" to shop.openStatus,
                    "published" to shop.published,
                    "verified" to true,
                    "supportsShopDrop" to shop.supportsShopDrop,
                    "supportsWorkerDelivery" to shop.supportsWorkerDelivery,
                    "supportsClickitBatch" to shop.supportsClickitBatch,
                    "dailyCapacity" to shop.dailyCapacity,
                    "activeLoad" to 0L,
                    "ratingAvg" to 5.0,
                    "ratingCount" to 0L,
                    "startingPriceMinor" to shop.startingPriceMinor.coerceAtLeast(100L),
                    "startingPriceUnit" to shop.startingPriceUnit.trim().ifBlank { "item" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            colRef.document(shop.id).update(
                mapOf(
                    "shopName" to shop.shopName.trim(),
                    "category" to shop.category,
                    "tagline" to shop.tagline.trim().take(140),
                    "locality" to shop.locality.trim(),
                    "addressSummary" to shop.addressSummary.trim(),
                    "hoursSummary" to shop.hoursSummary.trim().ifBlank { "9:00 AM – 8:00 PM" },
                    "turnaroundLabel" to shop.turnaroundLabel.trim().ifBlank { "24 hrs" },
                    "openStatus" to shop.openStatus,
                    "published" to shop.published,
                    "supportsShopDrop" to shop.supportsShopDrop,
                    "supportsWorkerDelivery" to shop.supportsWorkerDelivery,
                    "supportsClickitBatch" to shop.supportsClickitBatch,
                    "dailyCapacity" to shop.dailyCapacity,
                    "startingPriceMinor" to shop.startingPriceMinor.coerceAtLeast(100L),
                    "startingPriceUnit" to shop.startingPriceUnit.trim().ifBlank { "item" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "shops/${shop.id}")
        Result.failure(e)
    }

    suspend fun toggleShopOpenStatus(shopId: String, open: Boolean): Result<Unit> = try {
        requireUserId()
        db.collection("shops").document(shopId)
            .update(mapOf("openStatus" to open, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "shops/$shopId")
        Result.failure(e)
    }

    suspend fun saveShopService(shopId: String, service: ShopService): Result<Unit> = try {
        val uid = requireUserId()
        require(service.name.trim().length in 2..80) { "Service name must be 2–80 characters." }
        require(service.priceMinor in 100L..10000000L) { "Price must be at least ₹1." }
        require(service.minQty in 1L..100L) { "Minimum quantity must be 1–100." }
        require(service.turnaroundHours in 1L..720L) { "Turnaround must be 1–720 hours." }

        val colRef = db.collection("shops").document(shopId).collection("services")
        if (service.id.isBlank()) {
            colRef.document().set(
                mapOf(
                    "shopId" to shopId,
                    "ownerId" to uid,
                    "name" to service.name.trim(),
                    "description" to service.description.trim().take(200),
                    "inclusions" to service.inclusions.trim().takeIf { it.isNotEmpty() },
                    "category" to service.category,
                    "unit" to service.unit.trim().ifBlank { "item" },
                    "priceMinor" to service.priceMinor,
                    "minQty" to service.minQty,
                    "turnaroundHours" to service.turnaroundHours,
                    "active" to service.active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
            ).await()
        } else {
            colRef.document(service.id).update(
                mapOf(
                    "name" to service.name.trim(),
                    "description" to service.description.trim().take(200),
                    "inclusions" to service.inclusions.trim().take(160),
                    "category" to service.category,
                    "unit" to service.unit.trim().ifBlank { "item" },
                    "priceMinor" to service.priceMinor,
                    "minQty" to service.minQty,
                    "turnaroundHours" to service.turnaroundHours,
                    "active" to service.active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "shops/$shopId/services/${service.id}")
        Result.failure(e)
    }

    suspend fun toggleServiceActive(shopId: String, serviceId: String, active: Boolean): Result<Unit> = try {
        requireUserId()
        db.collection("shops").document(shopId).collection("services").document(serviceId)
            .update(mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "shops/$shopId/services/$serviceId")
        Result.failure(e)
    }

    /**
     * Seeds verified local Laundry, Tailoring, and Footwear Repair providers with active services
     * if the Firestore `shops` collection currently has no published providers.
     */
    suspend fun ensureStarterMarketplaceIfEmpty(localityHint: String = "Indiranagar, Bengaluru"): Result<Boolean> = try {
        val uid = requireUserId()
        val existingSnap = db.collection("shops")
            .whereEqualTo("published", true)
            .limit(1)
            .get()
            .await()
        if (!existingSnap.isEmpty) {
            Result.success(false)
        } else {
            val cleanLocality = localityHint.trim().ifBlank { "Indiranagar, Bengaluru" }.take(80)
            data class StarterServiceSpec(
                val name: String,
                val description: String,
                val unit: String,
                val priceMinor: Long,
                val minQty: Long,
                val turnaroundHours: Long
            )
            data class StarterShopSpec(
                val shopName: String,
                val category: String,
                val tagline: String,
                val addressSummary: String,
                val turnaroundLabel: String,
                val ratingAvg: Double,
                val ratingCount: Long,
                val startingPriceMinor: Long,
                val startingPriceUnit: String,
                val services: List<StarterServiceSpec>
            )

            val starterCatalog = listOf(
                StarterShopSpec(
                    shopName = "FreshPress Fabric Studio",
                    category = ServiceCategory.LAUNDRY.key,
                    tagline = "Steam press, crisp starching & delicate wash care",
                    addressSummary = "100 Feet Road, $cleanLocality",
                    turnaroundLabel = "Same day • 8 hrs",
                    ratingAvg = 4.9,
                    ratingCount = 128L,
                    startingPriceMinor = 1500L,
                    startingPriceUnit = "cloth",
                    services = listOf(
                        StarterServiceSpec(
                            name = "Steam Press & Fold",
                            description = "Wrinkle-free steam iron with breathable garment sleeve",
                            unit = "cloth",
                            priceMinor = 1500L,
                            minQty = 2L,
                            turnaroundHours = 8L
                        ),
                        StarterServiceSpec(
                            name = "Wash, Dry & Fold",
                            description = "Hygienic separate-lot wash with gentle fabric softener",
                            unit = "kg",
                            priceMinor = 7900L,
                            minQty = 2L,
                            turnaroundHours = 24L
                        )
                    )
                ),
                StarterShopSpec(
                    shopName = "Savile Stitch Craft",
                    category = ServiceCategory.TAILORING.key,
                    tagline = "Precision trouser hemming, blouse fit & zip replacements",
                    addressSummary = "12th Main Market, $cleanLocality",
                    turnaroundLabel = "24 hrs",
                    ratingAvg = 4.8,
                    ratingCount = 94L,
                    startingPriceMinor = 9900L,
                    startingPriceUnit = "alteration",
                    services = listOf(
                        StarterServiceSpec(
                            name = "Trouser Hemming",
                            description = "Blind-stitch or original denim hem matching thread",
                            unit = "alteration",
                            priceMinor = 9900L,
                            minQty = 1L,
                            turnaroundHours = 24L
                        ),
                        StarterServiceSpec(
                            name = "Shirt Taper & Sleeve Fit",
                            description = "Custom side-seam and cuff tailoring for crisp silhouette",
                            unit = "garment",
                            priceMinor = 18000L,
                            minQty = 1L,
                            turnaroundHours = 36L
                        )
                    )
                ),
                StarterShopSpec(
                    shopName = "SoleCraft Footwear Spa",
                    category = ServiceCategory.FOOTWEAR.key,
                    tagline = "Sneaker deep clean, Vibram resole & leather polish",
                    addressSummary = "80 Feet Corner, $cleanLocality",
                    turnaroundLabel = "36 hrs",
                    ratingAvg = 4.9,
                    ratingCount = 76L,
                    startingPriceMinor = 24900L,
                    startingPriceUnit = "pair",
                    services = listOf(
                        StarterServiceSpec(
                            name = "Sneaker Deep Clean",
                            description = "Midsole whitening, suede brush & deodorizing treatment",
                            unit = "pair",
                            priceMinor = 24900L,
                            minQty = 1L,
                            turnaroundHours = 36L
                        ),
                        StarterServiceSpec(
                            name = "Sole Stitch & Heel Tip",
                            description = "Reinforced sole bonding and anti-slip heel replacement",
                            unit = "pair",
                            priceMinor = 29900L,
                            minQty = 1L,
                            turnaroundHours = 48L
                        )
                    )
                )
            )

            for (spec in starterCatalog) {
                val shopRef = db.collection("shops").document()
                val shopId = shopRef.id
                shopRef.set(
                    mapOf(
                        "ownerId" to uid,
                        "shopName" to spec.shopName,
                        "category" to spec.category,
                        "tagline" to spec.tagline,
                        "locality" to cleanLocality,
                        "addressSummary" to spec.addressSummary.take(160),
                        "hoursSummary" to "8:30 AM – 9:00 PM",
                        "turnaroundLabel" to spec.turnaroundLabel,
                        "openStatus" to true,
                        "published" to true,
                        "verified" to true,
                        "supportsShopDrop" to true,
                        "supportsWorkerDelivery" to true,
                        "supportsClickitBatch" to true,
                        "dailyCapacity" to 40L,
                        "activeLoad" to 4L,
                        "ratingAvg" to spec.ratingAvg,
                        "ratingCount" to spec.ratingCount,
                        "startingPriceMinor" to spec.startingPriceMinor,
                        "startingPriceUnit" to spec.startingPriceUnit,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                for (svc in spec.services) {
                    shopRef.collection("services").document().set(
                        mapOf(
                            "shopId" to shopId,
                            "ownerId" to uid,
                            "name" to svc.name,
                            "description" to svc.description,
                            "category" to spec.category,
                            "unit" to svc.unit,
                            "priceMinor" to svc.priceMinor,
                            "minQty" to svc.minQty,
                            "turnaroundHours" to svc.turnaroundHours,
                            "active" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(true)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "shops")
        Result.failure(e)
    }

    // --- BOOKINGS, QUOTES, WORKER WORKFLOW & SMART BATCH ---

    suspend fun createBooking(
        customerName: String,
        shop: Shop,
        service: ShopService,
        quantity: Int,
        weightKg: Double,
        fulfilment: FulfilmentMethod,
        slotOption: PickupSlotOption,
        batchOptIn: Boolean,
        address: SavedAddress?,
        itemNotes: String,
        urgentService: Boolean = false
    ): Result<String> = try {
        val uid = requireUserId()
        require(quantity in 1..500) { "Quantity must be 1–500." }
        val feeQuote = calculateBookingFees(
            unitPriceMinor = service.priceMinor,
            quantity = quantity,
            weightKg = weightKg,
            fulfilment = fulfilment,
            batchOptIn = batchOptIn,
            batchEligible = slotOption.batchEligible,
            urgentService = urgentService
        )
        val serviceSubtotal = feeQuote.serviceSubtotalMinor
        val deliveryFee = feeQuote.deliveryFeeMinor
        val platformFee = feeQuote.platformFeeMinor
        val total = feeQuote.totalMinor

        val parcelCode = "CLK-${1000 + secureRandom.nextInt(9000)}"
        val handoverOtp = (1000 + secureRandom.nextInt(9000)).toString()
        val deliveryOtp = (1000 + secureRandom.nextInt(9000)).toString()

        val bookingRef = db.collection("bookings").document()
        val bookingId = bookingRef.id
        val addrLabel = address?.label ?: "Self Drop"
        val addrSummary = address?.formattedSummary()?.ifBlank { shop.addressSummary } ?: shop.addressSummary
        val urgentPrefix = if (urgentService) "[URGENT +25%] " else ""
        val itemsSummary = "$urgentPrefix${quantity} × ${service.name} (${formatInr(service.priceMinor)}/${service.unit})" +
            if (itemNotes.isNotBlank()) " • ${itemNotes.trim()}" else ""

        val bookingPayload = mapOf(
            "customerId" to uid,
            "customerName" to customerName.trim().ifBlank { "Customer" }.take(60),
            "workerOwnerId" to shop.ownerId,
            "shopId" to shop.id,
            "shopName" to shop.shopName,
            "category" to shop.category,
            "status" to BookingStatus.REQUESTED.key,
            "quoteStatus" to "awaiting_worker_confirmation",
            "fulfilmentMethod" to fulfilment.key,
            "slotWindowLabel" to slotOption.windowLabel,
            "batchEligible" to slotOption.batchEligible,
            "batchOptIn" to (fulfilment == FulfilmentMethod.CLICKIT_PICKUP_RETURN && batchOptIn && slotOption.batchEligible),
            "addressLabel" to addrLabel.take(40),
            "addressSummary" to addrSummary.take(200),
            "parcelCode" to parcelCode,
            "handoverOtp" to handoverOtp,
            "deliveryOtp" to deliveryOtp,
            "itemsSummary" to itemsSummary.take(240),
            "itemCount" to quantity.toLong(),
            "weightKg" to weightKg.coerceIn(0.0, 500.0),
            "serviceSubtotalMinor" to serviceSubtotal,
            "deliveryFeeMinor" to deliveryFee,
            "platformFeeMinor" to platformFee,
            "totalMinor" to total,
            "paymentStatus" to "pending",
            "milestone" to if (urgentService) "Urgent pickup • Awaiting worker" else "Awaiting worker confirmation",
            "ratingStars" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        bookingRef.set(bookingPayload).await()

        val freeDeliveryNote = if (feeQuote.isFreeDeliveryByWeight) " • Free Delivery (>5 kg)" else ""
        val urgentNote = if (urgentService) " • Emergency (+25%)" else ""
        addTimelineAndNotify(
            bookingId = bookingId,
            customerId = uid,
            workerOwnerId = shop.ownerId,
            actorId = uid,
            actorRole = "customer",
            status = BookingStatus.REQUESTED.key,
            title = if (urgentService) "Urgent Booking Submitted" else "Booking Submitted",
            detail = "$itemsSummary • ${fulfilment.label}$freeDeliveryNote$urgentNote",
            notifyUserId = shop.ownerId,
            notifyTitle = if (urgentService) "Urgent Order ($parcelCode)" else "New Order ($parcelCode)",
            notifyCategory = "booking"
        )

        Result.success(bookingId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "bookings")
        Result.failure(e)
    }

    suspend fun workerAcceptBooking(booking: Booking): Result<Unit> = try {
        val uid = requireUserId()
        val isClickitPickup = booking.fulfilmentMethod == FulfilmentMethod.CLICKIT_PICKUP_RETURN.key
        val nextStatus = if (isClickitPickup) BookingStatus.PICKUP_SCHEDULED.key else BookingStatus.CONFIRMED.key
        val milestone = if (isClickitPickup) "Pickup scheduled (${booking.slotWindowLabel})" else "Confirmed for drop-off"

        db.collection("bookings").document(booking.id).update(
            mapOf(
                "status" to nextStatus,
                "quoteStatus" to "confirmed",
                "milestone" to milestone,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        if (isClickitPickup) {
            createDeliveryTaskForBooking(booking, taskType = "pickup", uid = uid)
        }

        addTimelineAndNotify(
            bookingId = booking.id,
            customerId = booking.customerId,
            workerOwnerId = booking.workerOwnerId,
            actorId = uid,
            actorRole = "worker",
            status = nextStatus,
            title = "Worker Accepted & Quote Confirmed",
            detail = "${booking.shopName} accepted your order (${formatInr(booking.totalMinor)} final).",
            notifyUserId = booking.customerId,
            notifyTitle = "${booking.shopName} accepted",
            notifyCategory = "booking"
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "bookings/${booking.id}")
        Result.failure(e)
    }

    suspend fun workerSendQuoteRevision(booking: Booking, newSubtotalMinor: Long, reason: String): Result<Unit> = try {
        val uid = requireUserId()
        require(newSubtotalMinor >= 100L) { "Revised subtotal must be at least ₹1." }
        require(reason.trim().length in 3..200) { "Please provide a clear reason for the quote revision." }
        val newTotal = newSubtotalMinor + booking.deliveryFeeMinor + booking.platformFeeMinor

        db.collection("bookings").document(booking.id).update(
            mapOf(
                "status" to BookingStatus.QUOTE_PENDING.key,
                "quoteStatus" to "quote_revision_sent",
                "quoteNote" to reason.trim(),
                "serviceSubtotalMinor" to newSubtotalMinor,
                "totalMinor" to newTotal,
                "milestone" to "Quote revision awaiting customer approval",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        addTimelineAndNotify(
            bookingId = booking.id,
            customerId = booking.customerId,
            workerOwnerId = booking.workerOwnerId,
            actorId = uid,
            actorRole = "worker",
            status = BookingStatus.QUOTE_PENDING.key,
            title = "Revised Quote Sent (${formatInr(newTotal)})",
            detail = reason.trim(),
            notifyUserId = booking.customerId,
            notifyTitle = "Quote updated for ${booking.parcelCode}",
            notifyCategory = "quote"
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "bookings/${booking.id}")
        Result.failure(e)
    }

    suspend fun customerRespondToQuote(booking: Booking, accept: Boolean): Result<Unit> = try {
        val uid = requireUserId()
        if (accept) {
            val isClickitPickup = booking.fulfilmentMethod == FulfilmentMethod.CLICKIT_PICKUP_RETURN.key
            val nextStatus = if (isClickitPickup) BookingStatus.PICKUP_SCHEDULED.key else BookingStatus.CONFIRMED.key
            db.collection("bookings").document(booking.id).update(
                mapOf(
                    "status" to nextStatus,
                    "quoteStatus" to "confirmed",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (isClickitPickup) {
                createDeliveryTaskForBooking(booking, taskType = "pickup", uid = uid)
            }
            addTimelineAndNotify(
                bookingId = booking.id,
                customerId = booking.customerId,
                workerOwnerId = booking.workerOwnerId,
                actorId = uid,
                actorRole = "customer",
                status = nextStatus,
                title = "Customer Approved Quote",
                detail = "Approved total ${formatInr(booking.totalMinor)}",
                notifyUserId = booking.workerOwnerId,
                notifyTitle = "Quote approved (${booking.parcelCode})",
                notifyCategory = "quote"
            )
        } else {
            db.collection("bookings").document(booking.id).update(
                mapOf(
                    "status" to BookingStatus.CANCELLED.key,
                    "cancellationReason" to "Declined revised quote",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "bookings/${booking.id}")
        Result.failure(e)
    }

    suspend fun customerCompletePayment(
        booking: Booking,
        paymentMethod: String = "rupay_upi",
        paymentRef: String = "RuPay UPI"
    ): Result<Unit> = try {
        val uid = requireUserId()
        val methodClean = paymentMethod.trim().ifBlank { "rupay_upi" }.take(32)
        val refClean = paymentRef.trim().ifBlank { "RuPay" }.take(64)
        db.collection("bookings").document(booking.id).update(
            mapOf(
                "paymentStatus" to "paid",
                "paymentMethod" to methodClean,
                "paymentRef" to refClean,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        addTimelineAndNotify(
            bookingId = booking.id,
            customerId = booking.customerId,
            workerOwnerId = booking.workerOwnerId,
            actorId = uid,
            actorRole = "customer",
            status = booking.status,
            title = "Paid via $refClean (${formatInr(booking.totalMinor)})",
            detail = "RuPay payment verified • Clickit Escrow",
            notifyUserId = booking.customerId,
            notifyTitle = "RuPay Receipt • ${formatInr(booking.totalMinor)}",
            notifyCategory = "payment"
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "bookings/${booking.id}")
        Result.failure(e)
    }

    suspend fun customerCancelBooking(booking: Booking, reason: String): Result<Unit> = try {
        val uid = requireUserId()
        val cleanReason = reason.trim().ifBlank { "Cancelled by customer" }.take(160)
        val refundState = if (booking.paymentStatus == "paid") "refunded" else booking.paymentStatus
        db.collection("bookings").document(booking.id).update(
            mapOf(
                "status" to BookingStatus.CANCELLED.key,
                "paymentStatus" to refundState,
                "cancellationReason" to cleanReason,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        addTimelineAndNotify(
            bookingId = booking.id,
            customerId = booking.customerId,
            workerOwnerId = booking.workerOwnerId,
            actorId = uid,
            actorRole = "customer",
            status = BookingStatus.CANCELLED.key,
            title = "Booking Cancelled",
            detail = cleanReason,
            notifyUserId = booking.customerId,
            notifyTitle = "Booking ${booking.parcelCode} Cancelled",
            notifyCategory = "booking"
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "bookings/${booking.id}")
        Result.failure(e)
    }

    suspend fun workerRejectBooking(booking: Booking, reason: String): Result<Unit> = try {
        val uid = requireUserId()
        val cleanReason = reason.trim().ifBlank { "Shop at capacity" }.take(160)
        db.collection("bookings").document(booking.id).update(
            mapOf(
                "status" to BookingStatus.CANCELLED.key,
                "cancellationReason" to cleanReason,
                "milestone" to "Rejected by worker",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        addTimelineAndNotify(
            bookingId = booking.id,
            customerId = booking.customerId,
            workerOwnerId = booking.workerOwnerId,
            actorId = uid,
            actorRole = "worker",
            status = BookingStatus.CANCELLED.key,
            title = "Request Declined",
            detail = cleanReason,
            notifyUserId = booking.customerId,
            notifyTitle = "${booking.shopName} could not accept",
            notifyCategory = "booking"
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "bookings/${booking.id}")
        Result.failure(e)
    }

    suspend fun workerReceiveItems(booking: Booking): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("bookings").document(booking.id).update(
            mapOf(
                "status" to BookingStatus.RECEIVED_BY_WORKER.key,
                "milestone" to "Items verified & received (${booking.itemCount} pcs)",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        addTimelineAndNotify(
            bookingId = booking.id,
            customerId = booking.customerId,
            workerOwnerId = booking.workerOwnerId,
            actorId = uid,
            actorRole = "worker",
            status = BookingStatus.RECEIVED_BY_WORKER.key,
            title = "Worker Received Items",
            detail = "Parcel ${booking.parcelCode} checked in (${booking.itemCount} items)",
            notifyUserId = booking.customerId,
            notifyTitle = "${booking.shopName} received your parcel",
            notifyCategory = "booking"
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "bookings/${booking.id}")
        Result.failure(e)
    }

    suspend fun workerUpdateMilestone(booking: Booking, milestone: String): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("bookings").document(booking.id).update(
            mapOf(
                "status" to BookingStatus.IN_PROGRESS.key,
                "milestone" to milestone.trim().take(60),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        addTimelineAndNotify(
            bookingId = booking.id,
            customerId = booking.customerId,
            workerOwnerId = booking.workerOwnerId,
            actorId = uid,
            actorRole = "worker",
            status = BookingStatus.IN_PROGRESS.key,
            title = "Service Update: $milestone",
            detail = "${booking.shopName} updated job progress.",
            notifyUserId = booking.customerId,
            notifyTitle = "Progress: $milestone",
            notifyCategory = "booking"
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "bookings/${booking.id}")
        Result.failure(e)
    }

    suspend fun workerMarkReady(booking: Booking): Result<Unit> = try {
        val uid = requireUserId()
        val isClickitReturn = booking.fulfilmentMethod == FulfilmentMethod.CLICKIT_PICKUP_RETURN.key
        val nextStatus = if (isClickitReturn) BookingStatus.RETURN_SCHEDULED.key else BookingStatus.READY.key
        val milestone = if (isClickitReturn) "Packed & ready for return batch" else "Ready for shop collection"

        db.collection("bookings").document(booking.id).update(
            mapOf(
                "status" to nextStatus,
                "milestone" to milestone,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        if (isClickitReturn) {
            createDeliveryTaskForBooking(booking, taskType = "return", uid = uid)
        }

        addTimelineAndNotify(
            bookingId = booking.id,
            customerId = booking.customerId,
            workerOwnerId = booking.workerOwnerId,
            actorId = uid,
            actorRole = "worker",
            status = nextStatus,
            title = "Work Ready • Parcel Sealed",
            detail = milestone,
            notifyUserId = booking.customerId,
            notifyTitle = "Your order ${booking.parcelCode} is ready!",
            notifyCategory = "pickup"
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "bookings/${booking.id}")
        Result.failure(e)
    }

    suspend fun customerConfirmDeliveryAndRate(booking: Booking, stars: Int, reviewText: String): Result<Unit> = try {
        val uid = requireUserId()
        if (booking.status != BookingStatus.COMPLETED.key) {
            require(booking.paymentStatus == "paid") { "Please complete RuPay payment before marking the order complete." }
            db.collection("bookings").document(booking.id).update(
                mapOf(
                    "status" to BookingStatus.COMPLETED.key,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        if (stars in 1..5 && booking.ratingStars == 0L) {
            val ratingUpdates = mutableMapOf<String, Any>(
                "ratingStars" to stars.toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val cleanReview = reviewText.trim().take(300)
            if (cleanReview.isNotEmpty()) ratingUpdates["reviewText"] = cleanReview
            db.collection("bookings").document(booking.id).update(ratingUpdates).await()
        }
        addTimelineAndNotify(
            bookingId = booking.id,
            customerId = booking.customerId,
            workerOwnerId = booking.workerOwnerId,
            actorId = uid,
            actorRole = "customer",
            status = BookingStatus.COMPLETED.key,
            title = "Order Completed",
            detail = if (stars > 0) "Rated $stars★ by customer" else "Customer confirmed receipt",
            notifyUserId = booking.customerId,
            notifyTitle = "Thank you! Order ${booking.parcelCode} completed",
            notifyCategory = "booking"
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "bookings/${booking.id}")
        Result.failure(e)
    }

    // --- DELIVERY PARTNER & SMART BATCH ---

    @Suppress("DEPRECATION")
    private suspend fun geocodeOrFallback(addressOrZone: String, seedHint: String): GeoPointPair {
        val ctx = appContext
        if (ctx != null && addressOrZone.isNotBlank() && Geocoder.isPresent()) {
            val geocoded = withContext(Dispatchers.IO) {
                runCatching {
                    val geocoder = Geocoder(ctx, Locale.getDefault())
                    geocoder.getFromLocationName(addressOrZone, 1)
                        ?.firstOrNull()
                        ?.takeIf { isValidCoordinate(it.latitude, it.longitude) }
                        ?.let { GeoPointPair(it.latitude, it.longitude) }
                }.getOrNull()
            }
            if (geocoded != null) return geocoded
        }
        return resolveZoneCoordinates(addressOrZone, seedHint)
    }

    private suspend fun createDeliveryTaskForBooking(booking: Booking, taskType: String, uid: String) {
        val batchSuffix = booking.slotWindowLabel.take(5).filter { it.isLetterOrDigit() }.ifBlank { "AM" }
        val batchId = if (booking.batchOptIn) "BATCH-$batchSuffix-IND" else "SOLO-${booking.parcelCode}"
        val customerZone = maskAddressForOffer(booking.addressSummary)
        val pickupZone = if (taskType == "pickup") customerZone else booking.shopName
        val dropZone = if (taskType == "pickup") booking.shopName else customerZone
        val payout = if (booking.deliveryFeeMinor >= 1500L) booking.deliveryFeeMinor else 2000L

        val customerCoords = geocodeOrFallback(booking.addressSummary.ifBlank { customerZone }, booking.parcelCode)
        val shopCoords = geocodeOrFallback("${booking.shopName}, $customerZone", booking.shopId)
        val pickupCoords = if (taskType == "pickup") customerCoords else shopCoords
        val dropCoords = if (taskType == "pickup") shopCoords else customerCoords

        val payload = mapOf(
            "bookingId" to booking.id,
            "customerId" to booking.customerId,
            "workerOwnerId" to booking.workerOwnerId,
            "batchId" to batchId,
            "taskType" to taskType,
            "status" to "offered",
            "pickupZone" to pickupZone.take(80),
            "dropZone" to dropZone.take(80),
            "shopName" to booking.shopName.take(80),
            "timeWindowLabel" to booking.slotWindowLabel.take(60),
            "parcelCode" to booking.parcelCode,
            "itemCount" to booking.itemCount,
            "estimatedWeightKg" to booking.weightKg.coerceAtLeast(1.0),
            "payoutMinor" to payout,
            "batchStopIndex" to 1L,
            "batchTotalStops" to if (booking.batchOptIn) 3L else 1L,
            "pickupLat" to pickupCoords.lat,
            "pickupLng" to pickupCoords.lng,
            "dropLat" to dropCoords.lat,
            "dropLng" to dropCoords.lng,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("delivery_tasks").document().set(payload).await()
    }

    suspend fun partnerAcceptTask(task: DeliveryTask): Result<Unit> = try {
        val uid = requireUserId()
        val batch = db.batch()
        batch.update(
            db.collection("delivery_tasks").document(task.id),
            mapOf(
                "partnerId" to uid,
                "status" to "accepted",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("bookings").document(task.bookingId),
            mapOf(
                "assignedPartnerId" to uid,
                "milestone" to "Delivery partner assigned (${task.parcelCode})",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "delivery_tasks/${task.id}")
        Result.failure(e)
    }

    suspend fun partnerAdvanceTask(
        task: DeliveryTask,
        nextTaskStatus: String,
        nextBookingStatus: String?,
        milestone: String
    ): Result<Unit> = try {
        requireUserId()
        val batch = db.batch()
        batch.update(
            db.collection("delivery_tasks").document(task.id),
            mapOf(
                "status" to nextTaskStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (nextBookingStatus != null) {
            batch.update(
                db.collection("bookings").document(task.bookingId),
                mapOf(
                    "status" to nextBookingStatus,
                    "milestone" to milestone.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "delivery_tasks/${task.id}")
        Result.failure(e)
    }

    suspend fun partnerReportException(task: DeliveryTask, reason: String): Result<Unit> = try {
        requireUserId()
        val clean = reason.trim().ifBlank { "Customer unavailable" }.take(160)
        db.collection("delivery_tasks").document(task.id).update(
            mapOf(
                "status" to "exception",
                "exceptionReason" to clean,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "delivery_tasks/${task.id}")
        Result.failure(e)
    }

    suspend fun partnerUpdateLiveLocation(
        taskId: String,
        lat: Double,
        lng: Double,
        bearing: Double?
    ): Result<Unit> = try {
        requireUserId()
        require(taskId.isNotBlank())
        require(isValidCoordinate(lat, lng)) { "Invalid GPS coordinate." }
        val updates = mutableMapOf<String, Any>(
            "agentLat" to lat,
            "agentLng" to lng,
            "locationUpdatedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (bearing != null && !bearing.isNaN() && bearing in 0.0..360.0) {
            updates["agentBearing"] = bearing
        }
        db.collection("delivery_tasks").document(taskId).update(updates).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "delivery_tasks/$taskId")
        Result.failure(e)
    }

    // --- SUPPORT TICKETS ---

    suspend fun createSupportTicket(
        bookingId: String,
        category: String,
        subject: String,
        description: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        require(subject.trim().length in 3..100) { "Subject must be 3–100 characters." }
        require(description.trim().length in 5..400) { "Please describe the issue (5–400 chars)." }

        val payload = mapOf(
            "userId" to uid,
            "bookingId" to bookingId.trim().takeIf { it.isNotEmpty() },
            "category" to category.trim().ifBlank { "Booking Issue" }.take(40),
            "subject" to subject.trim(),
            "description" to description.trim(),
            "status" to "open",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        db.collection("support_tickets").document().set(payload).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "support_tickets")
        Result.failure(e)
    }

    private suspend fun addTimelineAndNotify(
        bookingId: String,
        customerId: String,
        workerOwnerId: String,
        actorId: String,
        actorRole: String,
        status: String,
        title: String,
        detail: String,
        notifyUserId: String,
        notifyTitle: String,
        notifyCategory: String
    ) {
        db.collection("bookings").document(bookingId).collection("timeline").document().set(
            mapOf(
                "customerId" to customerId,
                "workerOwnerId" to workerOwnerId,
                "actorId" to actorId,
                "actorRole" to actorRole,
                "status" to status,
                "title" to title.take(80),
                "detail" to detail.take(200),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        db.collection("users").document(notifyUserId).collection("notifications").document().set(
            mapOf(
                "userId" to notifyUserId,
                "title" to notifyTitle.take(80),
                "message" to detail.take(240),
                "category" to notifyCategory,
                "bookingId" to bookingId,
                "readFlag" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
