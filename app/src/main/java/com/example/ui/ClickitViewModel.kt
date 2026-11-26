package com.example.ui

import android.content.ContentResolver
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppNotification
import com.example.data.Booking
import com.example.data.BookingTimelineEvent
import com.example.data.ClickitRepository
import com.example.data.DeliveryTask
import com.example.data.FulfilmentMethod
import com.example.data.PickupSlotOption
import com.example.data.SavedAddress
import com.example.data.ServiceCategory
import com.example.data.Shop
import com.example.data.ShopService
import com.example.data.SupportTicket
import com.example.data.UserProfile
import com.example.data.UserRole
import com.example.data.encodeProfileImageToDataUri
import com.example.data.friendlyFirestoreMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class ClickitViewModel(
    private val repository: ClickitRepository,
    val currentUserId: String,
    val initialDisplayName: String,
    private val prefs: SharedPreferences? = null
) : ViewModel() {

    private companion object {
        const val TAG = "ClickitVM"
        const val STOP_TIMEOUT_MS = 5000L
    }

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Stream error ($label)", root)
                emit(UiState.Error(friendlyFirestoreMessage(root)))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    private val _retryTrigger = MutableStateFlow(0)

    private val _selectedShopId = MutableStateFlow<String?>(null)
    val selectedShopId: StateFlow<String?> = _selectedShopId.asStateFlow()

    private val _selectedBookingContext = MutableStateFlow<Pair<String, Boolean>?>(null)

    private val _partnerOnline = MutableStateFlow(
        prefs?.getBoolean("partner_online_$currentUserId", true) ?: true
    )
    val partnerOnline: StateFlow<Boolean> = _partnerOnline.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow(
        prefs?.getString("category_filter_$currentUserId", null)?.let { key ->
            ServiceCategory.entries.firstOrNull { it.key == key }
        }
    )
    val selectedCategoryFilter: StateFlow<ServiceCategory?> = _selectedCategoryFilter.asStateFlow()

    private val _batchOnlyFilter = MutableStateFlow(
        prefs?.getBoolean("batch_only_$currentUserId", false) ?: false
    )
    val batchOnlyFilter: StateFlow<Boolean> = _batchOnlyFilter.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isActionBusy = MutableStateFlow(false)
    val isActionBusy: StateFlow<Boolean> = _isActionBusy.asStateFlow()

    val userProfile: StateFlow<UiState<UserProfile?>> =
        _retryTrigger.flatMapLatest { repository.observeUserProfile(currentUserId) }
            .toUiStateFlow("userProfile")

    val addresses: StateFlow<UiState<List<SavedAddress>>> =
        _retryTrigger.flatMapLatest { repository.observeAddresses(currentUserId) }
            .toUiStateFlow("addresses")

    val notifications: StateFlow<UiState<List<AppNotification>>> =
        _retryTrigger.flatMapLatest { repository.observeNotifications(currentUserId) }
            .toUiStateFlow("notifications")

    val publishedShops: StateFlow<UiState<List<Shop>>> =
        _retryTrigger.flatMapLatest { repository.observePublishedShops() }
            .toUiStateFlow("publishedShops")

    val ownedShops: StateFlow<UiState<List<Shop>>> =
        _retryTrigger.flatMapLatest { repository.observeOwnedShops(currentUserId) }
            .toUiStateFlow("ownedShops")

    val selectedShopServices: StateFlow<UiState<List<ShopService>>> =
        _selectedShopId
            .flatMapLatest { shopId ->
                if (shopId.isNullOrBlank()) flowOf(emptyList())
                else _retryTrigger.flatMapLatest { repository.observeActiveShopServices(shopId) }
            }
            .toUiStateFlow("selectedShopServices")

    val ownedShopServices: StateFlow<UiState<List<ShopService>>> =
        ownedShops
            .flatMapLatest { state ->
                val firstShopId = (state as? UiState.Success)?.data?.firstOrNull()?.id
                if (firstShopId.isNullOrBlank()) flowOf(emptyList())
                else _retryTrigger.flatMapLatest { repository.observeOwnedShopServices(firstShopId, currentUserId) }
            }
            .toUiStateFlow("ownedShopServices")

    val customerBookings: StateFlow<UiState<List<Booking>>> =
        _retryTrigger.flatMapLatest { repository.observeCustomerBookings(currentUserId) }
            .toUiStateFlow("customerBookings")

    val activeCustomerOrders: StateFlow<UiState<List<Booking>>> =
        _retryTrigger.flatMapLatest { repository.observeActiveCustomerBookings(currentUserId) }
            .toUiStateFlow("activeCustomerOrders")

    val orderHistory: StateFlow<UiState<List<Booking>>> =
        _retryTrigger.flatMapLatest { repository.observeCompletedOrderHistory(currentUserId) }
            .toUiStateFlow("orderHistory")

    val workerBookings: StateFlow<UiState<List<Booking>>> =
        _retryTrigger.flatMapLatest { repository.observeWorkerBookings(currentUserId) }
            .toUiStateFlow("workerBookings")

    val bookingTimeline: StateFlow<UiState<List<BookingTimelineEvent>>> =
        _selectedBookingContext
            .flatMapLatest { ctx ->
                if (ctx == null) flowOf(emptyList())
                else _retryTrigger.flatMapLatest { repository.observeBookingTimeline(ctx.first, currentUserId, ctx.second) }
            }
            .toUiStateFlow("bookingTimeline")

    val offeredTasks: StateFlow<UiState<List<DeliveryTask>>> =
        _retryTrigger.flatMapLatest { repository.observeOfferedDeliveryTasks() }
            .toUiStateFlow("offeredTasks")

    val partnerTasks: StateFlow<UiState<List<DeliveryTask>>> =
        _retryTrigger.flatMapLatest { repository.observePartnerDeliveryTasks(currentUserId) }
            .toUiStateFlow("partnerTasks")

    val partnerBookings: StateFlow<UiState<List<Booking>>> =
        _retryTrigger.flatMapLatest { repository.observePartnerBookings(currentUserId) }
            .toUiStateFlow("partnerBookings")

    val customerDeliveryTasks: StateFlow<UiState<List<DeliveryTask>>> =
        _retryTrigger.flatMapLatest { repository.observeCustomerDeliveryTasks(currentUserId) }
            .toUiStateFlow("customerDeliveryTasks")

    val supportTickets: StateFlow<UiState<List<SupportTicket>>> =
        _retryTrigger.flatMapLatest { repository.observeSupportTickets(currentUserId) }
            .toUiStateFlow("supportTickets")

    fun selectShop(shopId: String?) {
        _selectedShopId.value = shopId
    }

    fun selectBookingForTimeline(bookingId: String?, asWorker: Boolean = false) {
        _selectedBookingContext.value = bookingId?.let { it to asWorker }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: ServiceCategory?) {
        _selectedCategoryFilter.value = category
        prefs?.edit()?.apply {
            if (category == null) remove("category_filter_$currentUserId")
            else putString("category_filter_$currentUserId", category.key)
            apply()
        }
    }

    fun setBatchOnlyFilter(enabled: Boolean) {
        _batchOnlyFilter.value = enabled
        prefs?.edit()?.putBoolean("batch_only_$currentUserId", enabled)?.apply()
    }

    fun setPartnerOnline(online: Boolean) {
        _partnerOnline.value = online
        prefs?.edit()?.putBoolean("partner_online_$currentUserId", online)?.apply()
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun retryStreams() {
        _retryTrigger.value += 1
    }

    fun seedStarterShopsIfEmpty(localityHint: String = "Indiranagar, Bengaluru", silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _isActionBusy.value = true
            val res = repository.ensureStarterMarketplaceIfEmpty(localityHint)
            if (!silent) _isActionBusy.value = false
            res.onSuccess { seeded ->
                if (seeded && !silent) {
                    _actionMessage.value = "Loaded nearby laundry, tailoring & footwear providers"
                }
            }.onFailure { err ->
                if (!silent) {
                    _actionMessage.value = friendlyFirestoreMessage(err)
                }
            }
        }
    }

    private fun runAction(successMessage: String? = null, block: suspend () -> Result<*>, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = block()
            _isActionBusy.value = false
            res.onSuccess {
                if (!successMessage.isNullOrBlank()) _actionMessage.value = successMessage
                onSuccess?.invoke()
            }.onFailure { err ->
                _actionMessage.value = friendlyFirestoreMessage(err)
            }
        }
    }

    fun completeCustomerSetup(
        displayName: String,
        locality: String,
        phone: String,
        initialAddress: SavedAddress?
    ) = runAction(
        successMessage = "Welcome to Clickit",
        block = { repository.completeCustomerOnboarding(displayName, locality, phone, initialAddress) }
    )

    fun completeWorkerSetup(
        displayName: String,
        locality: String,
        phone: String,
        shop: Shop,
        initialServiceName: String,
        initialServicePriceMinor: Long,
        initialServiceUnit: String
    ) = runAction(
        successMessage = "Shop created",
        block = {
            repository.completeWorkerOnboarding(
                displayName = displayName,
                locality = locality,
                phone = phone,
                shop = shop,
                initialServiceName = initialServiceName,
                initialServicePriceMinor = initialServicePriceMinor,
                initialServiceUnit = initialServiceUnit
            )
        }
    )

    fun completePartnerSetup(
        displayName: String,
        locality: String,
        phone: String
    ) = runAction(
        successMessage = "Ready for deliveries",
        block = { repository.completePartnerOnboarding(displayName, locality, phone) }
    )

    fun switchRole(role: UserRole) = runAction(
        successMessage = "Switched to ${role.label}",
        block = { repository.updateActiveRole(role) }
    )

    fun updateProfile(
        displayName: String,
        locality: String,
        phoneMasked: String,
        newPhotoUri: Uri? = null,
        clearPhoto: Boolean = false,
        contentResolver: ContentResolver? = null,
        onDone: () -> Unit = {}
    ) = runAction(
        successMessage = "Profile updated",
        block = {
            val encodedPhoto = when {
                clearPhoto -> ""
                newPhotoUri != null && contentResolver != null ->
                    encodeProfileImageToDataUri(contentResolver, newPhotoUri)
                else -> null
            }
            repository.updateUserProfile(displayName, locality, phoneMasked, encodedPhoto)
        },
        onSuccess = onDone
    )

    fun updateProfilePhoto(
        photoUri: Uri,
        contentResolver: ContentResolver
    ) = runAction(
        successMessage = "Profile picture updated",
        block = {
            val dataUri = encodeProfileImageToDataUri(contentResolver, photoUri)
            repository.updateProfilePhoto(dataUri)
        }
    )

    fun saveAddress(address: SavedAddress, onDone: () -> Unit = {}) = runAction(
        successMessage = "Address saved",
        block = { repository.saveAddress(address) },
        onSuccess = onDone
    )

    fun deleteAddress(addressId: String) = runAction(
        successMessage = "Address removed",
        block = { repository.deleteAddress(addressId) }
    )

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun saveShop(shop: Shop, onDone: () -> Unit = {}) = runAction(
        successMessage = "Digital Dukaan saved",
        block = { repository.saveShop(shop) },
        onSuccess = onDone
    )

    fun toggleShopOpen(shopId: String, open: Boolean) = runAction(
        successMessage = if (open) "Shop is now Open" else "Shop paused for new orders",
        block = { repository.toggleShopOpenStatus(shopId, open) }
    )

    fun saveShopService(shopId: String, service: ShopService, onDone: () -> Unit = {}) = runAction(
        successMessage = "Service saved to catalogue",
        block = { repository.saveShopService(shopId, service) },
        onSuccess = onDone
    )

    fun toggleServiceActive(shopId: String, serviceId: String, active: Boolean) = runAction(
        successMessage = if (active) "Service activated" else "Service paused",
        block = { repository.toggleServiceActive(shopId, serviceId, active) }
    )

    fun createBooking(
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
        urgentService: Boolean = false,
        saveNewAddress: Boolean = false,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.createBooking(
                customerName = customerName,
                shop = shop,
                service = service,
                quantity = quantity,
                weightKg = weightKg,
                fulfilment = fulfilment,
                slotOption = slotOption,
                batchOptIn = batchOptIn,
                address = address,
                itemNotes = itemNotes,
                urgentService = urgentService
            )
            if (res.isSuccess && saveNewAddress && address != null && address.id.isBlank() &&
                fulfilment != FulfilmentMethod.SHOP_DROP &&
                address.postalCode.trim().length in 3..12
            ) {
                repository.saveAddress(
                    address.copy(
                        label = address.label.trim().ifBlank { "Home" },
                        city = address.city.trim().ifBlank { address.locality.trim() },
                        defaultAddress = true
                    )
                )
            }
            _isActionBusy.value = false
            res.onSuccess { bookingId ->
                _actionMessage.value = "Booking submitted! Awaiting worker confirmation."
                onCreated(bookingId)
            }.onFailure { err ->
                _actionMessage.value = friendlyFirestoreMessage(err)
            }
        }
    }

    fun workerAcceptBooking(booking: Booking) = runAction(
        successMessage = "Booking accepted & quote confirmed",
        block = { repository.workerAcceptBooking(booking) }
    )

    fun workerSendQuoteRevision(
        booking: Booking,
        newSubtotalMinor: Long,
        reason: String,
        onDone: () -> Unit = {}
    ) = runAction(
        successMessage = "Revised quote sent to customer",
        block = { repository.workerSendQuoteRevision(booking, newSubtotalMinor, reason) },
        onSuccess = onDone
    )

    fun workerRejectBooking(booking: Booking, reason: String) = runAction(
        successMessage = "Booking declined",
        block = { repository.workerRejectBooking(booking, reason) }
    )

    fun customerRespondToQuote(booking: Booking, accept: Boolean) = runAction(
        successMessage = if (accept) "Revised quote approved" else "Booking cancelled",
        block = { repository.customerRespondToQuote(booking, accept) }
    )

    fun customerCompletePayment(
        booking: Booking,
        paymentMethod: String = "rupay_upi",
        paymentRef: String = "RuPay UPI",
        onDone: () -> Unit = {}
    ) = runAction(
        successMessage = "RuPay payment confirmed!",
        block = { repository.customerCompletePayment(booking, paymentMethod, paymentRef) },
        onSuccess = onDone
    )

    fun customerCancelBooking(booking: Booking, reason: String) = runAction(
        successMessage = "Booking cancelled",
        block = { repository.customerCancelBooking(booking, reason) }
    )

    fun workerReceiveItems(booking: Booking) = runAction(
        successMessage = "Parcel custody verified & received",
        block = { repository.workerReceiveItems(booking) }
    )

    fun workerUpdateMilestone(booking: Booking, milestone: String) = runAction(
        successMessage = "Updated status: $milestone",
        block = { repository.workerUpdateMilestone(booking, milestone) }
    )

    fun workerMarkReady(booking: Booking) = runAction(
        successMessage = "Order marked ready & sealed",
        block = { repository.workerMarkReady(booking) }
    )

    fun customerConfirmDeliveryAndRate(booking: Booking, stars: Int, reviewText: String) = runAction(
        successMessage = "Receipt confirmed. Thank you!",
        block = { repository.customerConfirmDeliveryAndRate(booking, stars, reviewText) }
    )

    fun partnerAcceptTask(task: DeliveryTask) = runAction(
        successMessage = "Smart Batch stop accepted (${task.parcelCode})",
        block = { repository.partnerAcceptTask(task) }
    )

    fun partnerAdvanceTask(
        task: DeliveryTask,
        nextTaskStatus: String,
        nextBookingStatus: String?,
        milestone: String
    ) = runAction(
        successMessage = milestone,
        block = { repository.partnerAdvanceTask(task, nextTaskStatus, nextBookingStatus, milestone) }
    )

    fun partnerReportException(task: DeliveryTask, reason: String) = runAction(
        successMessage = "Exception flagged; other batch stops preserved",
        block = { repository.partnerReportException(task, reason) }
    )

    fun updatePartnerTaskLocation(taskId: String, lat: Double, lng: Double, bearing: Double?) {
        viewModelScope.launch {
            repository.partnerUpdateLiveLocation(taskId, lat, lng, bearing)
        }
    }

    fun createSupportTicket(
        bookingId: String,
        category: String,
        subject: String,
        description: String,
        onDone: () -> Unit = {}
    ) = runAction(
        successMessage = "Support ticket submitted",
        block = { repository.createSupportTicket(bookingId, category, subject, description) },
        onSuccess = onDone
    )
}
