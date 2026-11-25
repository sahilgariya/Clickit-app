package com.example

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AuthDestination
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.ClickitRepository
import com.example.data.DashboardRoute
import com.example.data.DefaultPickupSlots
import com.example.data.RoleBasedAuthHandler
import com.example.data.Shop
import com.example.data.UserProfile
import com.example.data.UserRole
import com.example.data.attemptAutoSignIn
import com.example.data.signOutUser
import com.example.notifications.SmartBatchNotificationHelper
import com.example.ui.ClickitViewModel
import com.example.ui.UiState
import com.example.ui.components.ClickitTopHeader
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.RoleSwitcherSheetDialog
import com.example.ui.components.RuPayPaymentSheet
import com.example.ui.screens.AccountSetupScreen
import com.example.ui.screens.ActiveOrdersScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CustomerActivityScreen
import com.example.ui.screens.CustomerBookingDetailSheet
import com.example.ui.screens.CustomerBookingsScreen
import com.example.ui.screens.CustomerExploreScreen
import com.example.ui.screens.CustomerHomeScreen
import com.example.ui.screens.OrderHistoryScreen
import com.example.ui.screens.PartnerEarningsScreen
import com.example.ui.screens.PartnerOffersScreen
import com.example.ui.screens.PartnerRouteScreen
import com.example.ui.screens.ProfileAndSupportScreen
import com.example.ui.screens.ShopBookingBottomSheet
import com.example.ui.screens.WorkerDashboardScreen
import com.example.ui.screens.WorkerDukaanScreen
import com.example.ui.screens.WorkerEarningsScreen
import com.example.ui.screens.WorkerJobsScreen
import com.example.ui.theme.ClickitCrimson
import com.example.ui.theme.ClickitIvory
import com.example.ui.theme.ClickitMidnight
import com.example.ui.theme.ClickitMutedText
import com.example.ui.theme.ClickitPeach
import com.example.ui.theme.ClickitPeachSoft
import com.example.ui.theme.ClickitSlateDeep
import com.example.ui.theme.ClickitTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClickitTheme(darkTheme = true) {
                ClickitAppRoot()
            }
        }
    }
}

private data class NavTab(
    val label: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun ClickitAppRoot() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val prefs = remember(appContext) {
        appContext.getSharedPreferences("clickit_prefs", Context.MODE_PRIVATE)
    }
    var hasCompletedOnboarding by remember {
        mutableStateOf(prefs.getBoolean("has_completed_onboarding", false))
    }

    val repository = remember(appContext) {
        val databaseId = appContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        ClickitRepository(db, appContext = appContext)
    }
    val authHandler = remember(repository) { RoleBasedAuthHandler(repository) }
    val pendingSignupRoleFlow = remember { MutableStateFlow<UserRole?>(null) }
    val authRetryTrigger = remember { MutableStateFlow(0) }

    val initialDestination = remember {
        val existing = Firebase.auth.currentUser
        if (existing != null) AuthDestination.EvaluatingRole(existing.uid) else AuthDestination.Unauthenticated
    }
    val authDestination by remember(authHandler) {
        authHandler.observeAuthDestination(
            preselectedRoleFlow = pendingSignupRoleFlow,
            retryTrigger = authRetryTrigger
        )
    }.collectAsStateWithLifecycle(initialValue = initialDestination)

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val performSignOut: () -> Unit = {
        signOutUser(credentialManager, scope) {
            pendingSignupRoleFlow.value = null
        }
    }

    when (val dest = authDestination) {
        is AuthDestination.Unauthenticated -> {
            AuthScreen(
                isReturningUser = hasCompletedOnboarding,
                onAuthSuccess = { chosenRole ->
                    pendingSignupRoleFlow.value = chosenRole
                }
            )
        }

        is AuthDestination.EvaluatingRole -> {
            Surface(color = ClickitMidnight, modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingStateView("Checking your Clickit role...")
                }
            }
        }

        is AuthDestination.Error -> {
            Surface(color = ClickitMidnight, modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ErrorRetryCard(
                            message = dest.message,
                            onRetry = { authRetryTrigger.value += 1 }
                        )
                        OutlinedButton(
                            onClick = performSignOut,
                            border = BorderStroke(1.dp, ClickitCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Sign Out", color = ClickitPeachSoft)
                        }
                    }
                }
            }
        }

        is AuthDestination.NeedsOnboarding -> {
            key(dest.uid) {
                val viewModel: ClickitViewModel = viewModel(
                    key = dest.uid,
                    factory = viewModelFactory {
                        initializer {
                            ClickitViewModel(
                                repository = repository,
                                currentUserId = dest.uid,
                                initialDisplayName = dest.initialDisplayName
                            )
                        }
                    }
                )
                val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
                val isActionBusy by viewModel.isActionBusy.collectAsStateWithLifecycle()

                AccountSetupScreen(
                    initialDisplayName = viewModel.initialDisplayName,
                    initialRole = dest.preselectedRole ?: UserRole.CUSTOMER,
                    rolePreselected = dest.preselectedRole != null,
                    isBusy = isActionBusy,
                    errorMessage = actionMessage,
                    onClearError = viewModel::clearMessage,
                    onSetupCustomer = viewModel::completeCustomerSetup,
                    onSetupWorker = viewModel::completeWorkerSetup,
                    onSetupPartner = viewModel::completePartnerSetup,
                    onSignOut = performSignOut
                )
            }
        }

        is AuthDestination.RedirectToDashboard -> {
            LaunchedEffect(dest.profile.userId) {
                if (!hasCompletedOnboarding) {
                    hasCompletedOnboarding = true
                    prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                }
                pendingSignupRoleFlow.value = null
            }
            key(dest.profile.userId) {
                val viewModel: ClickitViewModel = viewModel(
                    key = dest.profile.userId,
                    factory = viewModelFactory {
                        initializer {
                            ClickitViewModel(
                                repository = repository,
                                currentUserId = dest.profile.userId,
                                initialDisplayName = dest.profile.displayName
                            )
                        }
                    }
                )
                ClickitMainShell(
                    viewModel = viewModel,
                    resolvedProfile = dest.profile,
                    resolvedRole = dest.role,
                    resolvedRoute = dest.route,
                    onSignOut = performSignOut
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClickitMainShell(
    viewModel: ClickitViewModel,
    resolvedProfile: UserProfile,
    resolvedRole: UserRole,
    resolvedRoute: DashboardRoute,
    onSignOut: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val addressesState by viewModel.addresses.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notifications.collectAsStateWithLifecycle()
    val shopsState by viewModel.publishedShops.collectAsStateWithLifecycle()
    val ownedShopsState by viewModel.ownedShops.collectAsStateWithLifecycle()
    val selectedShopServicesState by viewModel.selectedShopServices.collectAsStateWithLifecycle()
    val ownedShopServicesState by viewModel.ownedShopServices.collectAsStateWithLifecycle()
    val customerBookingsState by viewModel.customerBookings.collectAsStateWithLifecycle()
    val activeOrdersState by viewModel.activeCustomerOrders.collectAsStateWithLifecycle()
    val orderHistoryState by viewModel.orderHistory.collectAsStateWithLifecycle()
    val workerBookingsState by viewModel.workerBookings.collectAsStateWithLifecycle()
    val bookingTimelineState by viewModel.bookingTimeline.collectAsStateWithLifecycle()
    val offeredTasksState by viewModel.offeredTasks.collectAsStateWithLifecycle()
    val partnerTasksState by viewModel.partnerTasks.collectAsStateWithLifecycle()
    val partnerBookingsState by viewModel.partnerBookings.collectAsStateWithLifecycle()
    val customerDeliveryTasksState by viewModel.customerDeliveryTasks.collectAsStateWithLifecycle()
    val supportTicketsState by viewModel.supportTickets.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val batchOnlyFilter by viewModel.batchOnlyFilter.collectAsStateWithLifecycle()
    val partnerOnline by viewModel.partnerOnline.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isActionBusy by viewModel.isActionBusy.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data ?: resolvedProfile
    val activeRole = resolvedRole
    val unreadCount = (notificationsState as? UiState.Success)?.data?.count { !it.readFlag } ?: 0
    val completedOrderCount = (orderHistoryState as? UiState.Success)?.data?.size ?: 0

    var selectedTab by remember(resolvedRoute) { mutableIntStateOf(0) }
    var showRoleSwitcher by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showActiveOrdersScreen by remember(resolvedRoute) { mutableStateOf(false) }
    var showOrderHistoryScreen by remember(resolvedRoute) { mutableStateOf(false) }
    var activeShopForBooking by remember { mutableStateOf<Shop?>(null) }
    var prefillBookingForReorder by remember { mutableStateOf<Booking?>(null) }
    var activeBookingForDetail by remember { mutableStateOf<Booking?>(null) }
    var ruPayTargetBooking by remember { mutableStateOf<Booking?>(null) }
    var pendingNotificationOrder by remember { mutableStateOf<Booking?>(null) }
    var attemptedStarterSeed by remember { mutableStateOf(false) }

    LaunchedEffect(activeRole, shopsState) {
        val loadedShops = (shopsState as? UiState.Success)?.data
        if (activeRole == UserRole.CUSTOMER && loadedShops != null && loadedShops.isEmpty() && !attemptedStarterSeed) {
            attemptedStarterSeed = true
            viewModel.seedStarterShopsIfEmpty(profile.activeLocality, silent = true)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingNotificationOrder
        pendingNotificationOrder = null
        if (granted && pending != null) {
            SmartBatchNotificationHelper.showSmartBatchPickupAlert(
                context = appContext,
                parcelCode = pending.parcelCode,
                shopName = pending.shopName,
                slotWindowLabel = pending.slotWindowLabel
            )
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Smart Batch pickup alert sent for ${pending.parcelCode}")
            }
        } else if (!granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Notification permission denied — status alerts shown in-app")
            }
        }
    }

    val triggerSmartBatchAlert: (Booking?) -> Unit = { booking ->
        val target = booking ?: Booking(
            id = "sim_batch",
            parcelCode = "CLK-BATCH-01",
            shopName = "FreshPress Laundry",
            slotWindowLabel = "Evening 6–8 PM",
            status = BookingStatus.READY.key
        )
        if (SmartBatchNotificationHelper.hasNotificationPermission(appContext)) {
            SmartBatchNotificationHelper.showSmartBatchPickupAlert(
                context = appContext,
                parcelCode = target.parcelCode,
                shopName = target.shopName,
                slotWindowLabel = target.slotWindowLabel
            )
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Smart Batch alert: ${target.parcelCode} is Ready for Pickup")
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingNotificationOrder = target
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val previousOrderStatuses = remember { mutableMapOf<String, String>() }
    var seededInitialStatuses by remember { mutableStateOf(false) }

    LaunchedEffect(activeOrdersState) {
        val currentOrders = (activeOrdersState as? UiState.Success)?.data ?: return@LaunchedEffect
        if (!seededInitialStatuses) {
            currentOrders.forEach { previousOrderStatuses[it.id] = it.status }
            seededInitialStatuses = true
        } else {
            currentOrders.forEach { order ->
                val prev = previousOrderStatuses[order.id]
                val nowReady = BookingStatus.fromRaw(order.status) == BookingStatus.READY
                if (nowReady && prev != null && prev != order.status) {
                    triggerSmartBatchAlert(order)
                }
            }
            previousOrderStatuses.clear()
            currentOrders.forEach { previousOrderStatuses[it.id] = it.status }
        }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Refresh selected booking from live stream when open
    val liveBookingForDetail = activeBookingForDetail?.let { open ->
        (customerBookingsState as? UiState.Success)?.data?.firstOrNull { it.id == open.id }
            ?: (activeOrdersState as? UiState.Success)?.data?.firstOrNull { it.id == open.id }
            ?: open
    }

    val onReorderBooking: (Booking) -> Unit = { booking ->
        val shops = (shopsState as? UiState.Success)?.data.orEmpty()
        val matchedShop = shops.firstOrNull { it.id == booking.shopId }
        when {
            matchedShop == null -> {
                showOrderHistoryScreen = false
                selectedTab = 1
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("${booking.shopName} is currently unavailable — choose another local provider.")
                }
            }
            !matchedShop.openStatus -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("${matchedShop.shopName} is currently paused for new orders.")
                }
            }
            else -> {
                prefillBookingForReorder = booking
                viewModel.selectShop(matchedShop.id)
                activeShopForBooking = matchedShop
            }
        }
    }

    BackHandler(
        enabled = ruPayTargetBooking != null ||
            activeShopForBooking != null ||
            liveBookingForDetail != null ||
            showNotificationsSheet ||
            showActiveOrdersScreen ||
            showOrderHistoryScreen ||
            selectedTab != 0
    ) {
        when {
            ruPayTargetBooking != null -> ruPayTargetBooking = null
            activeShopForBooking != null -> {
                activeShopForBooking = null
                prefillBookingForReorder = null
                viewModel.selectShop(null)
            }
            liveBookingForDetail != null -> {
                activeBookingForDetail = null
                viewModel.selectBookingForTimeline(null)
            }
            showNotificationsSheet -> showNotificationsSheet = false
            showActiveOrdersScreen -> showActiveOrdersScreen = false
            showOrderHistoryScreen -> showOrderHistoryScreen = false
            selectedTab != 0 -> selectedTab = 0
        }
    }

    if (showRoleSwitcher) {
        RoleSwitcherSheetDialog(
            currentRole = activeRole,
            onSelectRole = { role -> viewModel.switchRole(role) },
            onDismiss = { showRoleSwitcher = false }
        )
    }

    if (showNotificationsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            sheetState = sheetState,
            containerColor = ClickitMidnight,
            contentColor = ClickitIvory
        ) {
            Column(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    color = ClickitIvory,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                CustomerActivityScreen(
                    notificationsState = notificationsState,
                    onNotificationClick = { notif ->
                        viewModel.markNotificationRead(notif.id)
                        showNotificationsSheet = false
                        if (activeRole == UserRole.WORKER) selectedTab = 1
                    },
                    onRetry = viewModel::retryStreams
                )
            }
        }
    }

    if (activeShopForBooking != null) {
        val shop = activeShopForBooking!!
        val addrList = (addressesState as? UiState.Success)?.data ?: emptyList()
        ShopBookingBottomSheet(
            shop = shop,
            servicesState = selectedShopServicesState,
            addresses = addrList,
            customerName = profile.displayName,
            isBusy = isActionBusy,
            prefillBooking = prefillBookingForReorder,
            onDismiss = {
                activeShopForBooking = null
                prefillBookingForReorder = null
                viewModel.selectShop(null)
            },
            onSubmitBooking = { svc, qty, weight, fulfilment, slotIdx, batchOptIn, addr, notes ->
                viewModel.createBooking(
                    customerName = profile.displayName,
                    shop = shop,
                    service = svc,
                    quantity = qty,
                    weightKg = weight,
                    fulfilment = fulfilment,
                    slotOption = DefaultPickupSlots[slotIdx],
                    batchOptIn = batchOptIn,
                    address = addr,
                    itemNotes = notes,
                    onCreated = {
                        activeShopForBooking = null
                        prefillBookingForReorder = null
                        showOrderHistoryScreen = false
                        viewModel.selectShop(null)
                        selectedTab = 2 // Jump to Customer Bookings tab
                    }
                )
            },
            onRetry = viewModel::retryStreams
        )
    }

    if (liveBookingForDetail != null) {
        val matchingTask = (customerDeliveryTasksState as? UiState.Success)?.data
            ?.filter { it.bookingId == liveBookingForDetail.id }
            ?.let { tasks ->
                tasks.firstOrNull { it.status !in listOf("handed_over", "delivered", "exception") }
                    ?: tasks.firstOrNull()
            }
        CustomerBookingDetailSheet(
            booking = liveBookingForDetail,
            timelineState = bookingTimelineState,
            deliveryTask = matchingTask,
            isBusy = isActionBusy,
            onDismiss = {
                activeBookingForDetail = null
                viewModel.selectBookingForTimeline(null)
            },
            onRespondQuote = { accept -> viewModel.customerRespondToQuote(liveBookingForDetail, accept) },
            onPay = { ruPayTargetBooking = liveBookingForDetail },
            onCancel = { reason -> viewModel.customerCancelBooking(liveBookingForDetail, reason) },
            onConfirmDeliveryAndRate = { stars, review ->
                viewModel.customerConfirmDeliveryAndRate(liveBookingForDetail, stars, review)
            },
            onReportIssue = { desc ->
                viewModel.createSupportTicket(
                    bookingId = liveBookingForDetail.id,
                    category = "Order Issue",
                    subject = "Issue with ${liveBookingForDetail.parcelCode}",
                    description = desc
                )
            },
            onSimulatePickupAlert = { triggerSmartBatchAlert(liveBookingForDetail) },
            onRetry = viewModel::retryStreams
        )
    }

    if (ruPayTargetBooking != null) {
        val payTarget = ruPayTargetBooking!!
        RuPayPaymentSheet(
            booking = payTarget,
            isBusy = isActionBusy,
            onDismiss = { ruPayTargetBooking = null },
            onConfirmPayment = { method, ref ->
                viewModel.customerCompletePayment(
                    booking = payTarget,
                    paymentMethod = method,
                    paymentRef = ref,
                    onDone = { ruPayTargetBooking = null }
                )
            }
        )
    }

    val navTabs = when (activeRole) {
        UserRole.CUSTOMER -> listOf(
            NavTab("Home", Icons.Filled.Home, "nav_customer_home"),
            NavTab("Explore", Icons.Filled.Explore, "nav_customer_explore"),
            NavTab("Bookings", Icons.AutoMirrored.Filled.ReceiptLong, "nav_customer_bookings"),
            NavTab("Activity", Icons.Filled.Notifications, "nav_customer_activity"),
            NavTab("Profile", Icons.Filled.Person, "nav_customer_profile")
        )
        UserRole.WORKER -> listOf(
            NavTab("Dashboard", Icons.Filled.Dashboard, "nav_worker_dashboard"),
            NavTab("Jobs", Icons.Filled.Work, "nav_worker_jobs"),
            NavTab("Dukaan", Icons.Filled.Storefront, "nav_worker_dukaan"),
            NavTab("Earnings", Icons.Filled.AccountBalanceWallet, "nav_worker_earnings"),
            NavTab("Profile", Icons.Filled.Person, "nav_worker_profile")
        )
        UserRole.PARTNER -> listOf(
            NavTab("Offers", Icons.Filled.LocalShipping, "nav_partner_offers"),
            NavTab("My Route", Icons.Filled.Route, "nav_partner_route"),
            NavTab("Earnings", Icons.Filled.AccountBalanceWallet, "nav_partner_earnings"),
            NavTab("Profile", Icons.Filled.Person, "nav_partner_profile")
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 720.dp

        Scaffold(
        containerColor = ClickitMidnight,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ClickitTopHeader(
                activeRole = activeRole,
                localityLabel = profile.activeLocality,
                unreadNotifications = unreadCount,
                onNotificationsClick = {
                    if (activeRole == UserRole.CUSTOMER) {
                        showActiveOrdersScreen = false
                        showOrderHistoryScreen = false
                        selectedTab = 3
                    } else {
                        showNotificationsSheet = true
                    }
                },
                onRoleClick = { showRoleSwitcher = true }
            )
        },
        bottomBar = {
            if (!isExpandedWidth) {
                NavigationBar(containerColor = ClickitSlateDeep) {
                    navTabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index && !showActiveOrdersScreen && !showOrderHistoryScreen,
                            onClick = {
                                showActiveOrdersScreen = false
                                showOrderHistoryScreen = false
                                selectedTab = index
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ClickitMidnight,
                                selectedTextColor = ClickitPeach,
                                indicatorColor = ClickitPeach,
                                unselectedIconColor = ClickitMutedText,
                                unselectedTextColor = ClickitMutedText
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isExpandedWidth) {
                NavigationRail(
                    containerColor = ClickitSlateDeep,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    navTabs.forEachIndexed { index, tab ->
                        NavigationRailItem(
                            selected = selectedTab == index && !showActiveOrdersScreen && !showOrderHistoryScreen,
                            onClick = {
                                showActiveOrdersScreen = false
                                showOrderHistoryScreen = false
                                selectedTab = index
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ClickitMidnight,
                                selectedTextColor = ClickitPeach,
                                indicatorColor = ClickitPeach,
                                unselectedIconColor = ClickitMutedText,
                                unselectedTextColor = ClickitMutedText
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (resolvedRoute) {
                    DashboardRoute.CUSTOMER_HOME -> when {
                        showOrderHistoryScreen -> OrderHistoryScreen(
                            orderHistoryState = orderHistoryState,
                            onOrderClick = { bk ->
                                viewModel.selectBookingForTimeline(bk.id, asWorker = false)
                                activeBookingForDetail = bk
                            },
                            onReorderClick = onReorderBooking,
                            onExploreClick = {
                                showOrderHistoryScreen = false
                                selectedTab = 1
                            },
                            onBack = { showOrderHistoryScreen = false },
                            onRetry = viewModel::retryStreams
                        )
                        showActiveOrdersScreen -> ActiveOrdersScreen(
                            activeOrdersState = activeOrdersState,
                            customerTasksState = customerDeliveryTasksState,
                            onOrderClick = { bk ->
                                viewModel.selectBookingForTimeline(bk.id, asWorker = false)
                                activeBookingForDetail = bk
                            },
                            onExploreClick = {
                                showActiveOrdersScreen = false
                                selectedTab = 1
                            },
                            onBack = { showActiveOrdersScreen = false },
                            onSimulatePickupAlert = triggerSmartBatchAlert,
                            onRetry = viewModel::retryStreams
                        )
                        else -> when (selectedTab) {
                            0 -> CustomerHomeScreen(
                                addressesState = addressesState,
                                shopsState = shopsState,
                                activeOrdersState = activeOrdersState,
                                searchQuery = searchQuery,
                                selectedCategory = categoryFilter,
                                onSearchChange = viewModel::setSearchQuery,
                                onCategoryChange = viewModel::setCategoryFilter,
                                onExploreClick = { selectedTab = 1 },
                                onShopClick = { shop ->
                                    prefillBookingForReorder = null
                                    viewModel.selectShop(shop.id)
                                    activeShopForBooking = shop
                                },
                                onBookingClick = { bk ->
                                    viewModel.selectBookingForTimeline(bk.id, asWorker = false)
                                    activeBookingForDetail = bk
                                },
                                onViewActiveOrdersClick = { showActiveOrdersScreen = true },
                                onManageAddressesClick = { selectedTab = 4 },
                                onLoadStarterShops = { viewModel.seedStarterShopsIfEmpty(profile.activeLocality) },
                                onRetry = viewModel::retryStreams
                            )
                            1 -> CustomerExploreScreen(
                                shopsState = shopsState,
                                searchQuery = searchQuery,
                                selectedCategory = categoryFilter,
                                batchOnly = batchOnlyFilter,
                                onSearchChange = viewModel::setSearchQuery,
                                onCategoryChange = viewModel::setCategoryFilter,
                                onBatchOnlyChange = viewModel::setBatchOnlyFilter,
                                onShopClick = { shop ->
                                    prefillBookingForReorder = null
                                    viewModel.selectShop(shop.id)
                                    activeShopForBooking = shop
                                },
                                onLoadStarterShops = { viewModel.seedStarterShopsIfEmpty(profile.activeLocality) },
                                onRetry = viewModel::retryStreams
                            )
                            2 -> CustomerBookingsScreen(
                                bookingsState = customerBookingsState,
                                orderHistoryState = orderHistoryState,
                                onBookingClick = { bk ->
                                    viewModel.selectBookingForTimeline(bk.id, asWorker = false)
                                    activeBookingForDetail = bk
                                },
                                onReorderBooking = onReorderBooking,
                                onExploreClick = { selectedTab = 1 },
                                onOpenLiveTracker = { showActiveOrdersScreen = true },
                                onRetry = viewModel::retryStreams
                            )
                            3 -> CustomerActivityScreen(
                                notificationsState = notificationsState,
                                onNotificationClick = { notif ->
                                    viewModel.markNotificationRead(notif.id)
                                    if (notif.bookingId.isNotBlank()) {
                                        val bk = (customerBookingsState as? UiState.Success)?.data
                                            ?.firstOrNull { it.id == notif.bookingId }
                                        if (bk != null) {
                                            viewModel.selectBookingForTimeline(bk.id, asWorker = false)
                                            activeBookingForDetail = bk
                                        }
                                    }
                                },
                                onRetry = viewModel::retryStreams
                            )
                            else -> ProfileAndSupportScreen(
                                profileState = profileState,
                                addressesState = addressesState,
                                ticketsState = supportTicketsState,
                                activeRole = activeRole,
                                isBusy = isActionBusy,
                                completedOrderCount = completedOrderCount,
                                onOpenOrderHistory = { showOrderHistoryScreen = true },
                                onOpenRoleSwitcher = { showRoleSwitcher = true },
                                onUpdatePhoto = { uri ->
                                    viewModel.updateProfilePhoto(uri, appContext.contentResolver)
                                },
                                onSaveProfile = { name, locality, phone, uri, clear, onDone ->
                                    viewModel.updateProfile(
                                        displayName = name,
                                        locality = locality,
                                        phoneMasked = phone,
                                        newPhotoUri = uri,
                                        clearPhoto = clear,
                                        contentResolver = appContext.contentResolver,
                                        onDone = onDone
                                    )
                                },
                                onSaveAddress = viewModel::saveAddress,
                                onDeleteAddress = viewModel::deleteAddress,
                                onCreateTicket = { cat, sub, desc ->
                                    viewModel.createSupportTicket("", cat, sub, desc)
                                },
                                onRetry = viewModel::retryStreams,
                                onSignOut = onSignOut
                            )
                        }
                    }

                    DashboardRoute.SHOPPER_DASHBOARD -> when (selectedTab) {
                        0 -> WorkerDashboardScreen(
                            ownedShopsState = ownedShopsState,
                            workerBookingsState = workerBookingsState,
                            onToggleShopOpen = viewModel::toggleShopOpen,
                            onOpenJobsTab = { selectedTab = 1 },
                            onOpenDukaanTab = { selectedTab = 2 }
                        )
                        1 -> WorkerJobsScreen(
                            workerBookingsState = workerBookingsState,
                            isBusy = isActionBusy,
                            onAccept = viewModel::workerAcceptBooking,
                            onSendQuote = viewModel::workerSendQuoteRevision,
                            onReject = viewModel::workerRejectBooking,
                            onReceiveItems = viewModel::workerReceiveItems,
                            onUpdateMilestone = viewModel::workerUpdateMilestone,
                            onMarkReady = viewModel::workerMarkReady,
                            onRetry = viewModel::retryStreams
                        )
                        2 -> WorkerDukaanScreen(
                            ownedShopsState = ownedShopsState,
                            ownedServicesState = ownedShopServicesState,
                            onSaveShop = viewModel::saveShop,
                            onSaveService = viewModel::saveShopService,
                            onToggleServiceActive = viewModel::toggleServiceActive
                        )
                        3 -> WorkerEarningsScreen(workerBookingsState = workerBookingsState)
                        else -> ProfileAndSupportScreen(
                            profileState = profileState,
                            addressesState = addressesState,
                            ticketsState = supportTicketsState,
                            activeRole = activeRole,
                            isBusy = isActionBusy,
                            onOpenRoleSwitcher = { showRoleSwitcher = true },
                            onUpdatePhoto = { uri ->
                                viewModel.updateProfilePhoto(uri, appContext.contentResolver)
                            },
                            onSaveProfile = { name, locality, phone, uri, clear, onDone ->
                                viewModel.updateProfile(
                                    displayName = name,
                                    locality = locality,
                                    phoneMasked = phone,
                                    newPhotoUri = uri,
                                    clearPhoto = clear,
                                    contentResolver = appContext.contentResolver,
                                    onDone = onDone
                                )
                            },
                            onSaveAddress = viewModel::saveAddress,
                            onDeleteAddress = viewModel::deleteAddress,
                            onCreateTicket = { cat, sub, desc ->
                                viewModel.createSupportTicket("", cat, sub, desc)
                            },
                            onRetry = viewModel::retryStreams,
                            onSignOut = onSignOut
                        )
                    }

                    DashboardRoute.DELIVERY_OFFERS -> when (selectedTab) {
                        0 -> PartnerOffersScreen(
                            online = partnerOnline,
                            offeredTasksState = offeredTasksState,
                            partnerTasksState = partnerTasksState,
                            isBusy = isActionBusy,
                            onToggleOnline = viewModel::setPartnerOnline,
                            onAcceptTask = viewModel::partnerAcceptTask,
                            onRetry = viewModel::retryStreams
                        )
                        1 -> PartnerRouteScreen(
                            partnerTasksState = partnerTasksState,
                            partnerBookingsState = partnerBookingsState,
                            isBusy = isActionBusy,
                            onAdvanceTask = viewModel::partnerAdvanceTask,
                            onReportException = viewModel::partnerReportException,
                            onUpdateLiveLocation = viewModel::updatePartnerTaskLocation,
                            onRetry = viewModel::retryStreams
                        )
                        2 -> PartnerEarningsScreen(partnerTasksState = partnerTasksState)
                        else -> ProfileAndSupportScreen(
                            profileState = profileState,
                            addressesState = addressesState,
                            ticketsState = supportTicketsState,
                            activeRole = activeRole,
                            isBusy = isActionBusy,
                            onOpenRoleSwitcher = { showRoleSwitcher = true },
                            onUpdatePhoto = { uri ->
                                viewModel.updateProfilePhoto(uri, appContext.contentResolver)
                            },
                            onSaveProfile = { name, locality, phone, uri, clear, onDone ->
                                viewModel.updateProfile(
                                    displayName = name,
                                    locality = locality,
                                    phoneMasked = phone,
                                    newPhotoUri = uri,
                                    clearPhoto = clear,
                                    contentResolver = appContext.contentResolver,
                                    onDone = onDone
                                )
                            },
                            onSaveAddress = viewModel::saveAddress,
                            onDeleteAddress = viewModel::deleteAddress,
                            onCreateTicket = { cat, sub, desc ->
                                viewModel.createSupportTicket("", cat, sub, desc)
                            },
                            onRetry = viewModel::retryStreams,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
    }
}
