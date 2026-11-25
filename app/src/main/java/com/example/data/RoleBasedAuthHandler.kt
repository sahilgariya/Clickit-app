package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

enum class DashboardRoute {
    CUSTOMER_HOME,
    SHOPPER_DASHBOARD,
    DELIVERY_OFFERS
}

fun UserRole.toDashboardRoute(): DashboardRoute = when (this) {
    UserRole.CUSTOMER -> DashboardRoute.CUSTOMER_HOME
    UserRole.WORKER -> DashboardRoute.SHOPPER_DASHBOARD
    UserRole.PARTNER -> DashboardRoute.DELIVERY_OFFERS
}

sealed interface AuthDestination {
    data object Unauthenticated : AuthDestination
    data class EvaluatingRole(val uid: String) : AuthDestination
    data class NeedsOnboarding(
        val uid: String,
        val initialDisplayName: String,
        val preselectedRole: UserRole?
    ) : AuthDestination
    data class RedirectToDashboard(
        val profile: UserProfile,
        val role: UserRole,
        val route: DashboardRoute
    ) : AuthDestination
    data class Error(val message: String) : AuthDestination
}

/**
 * Pure role-based auth resolver mapping Firebase Auth state and Firestore UserProfile
 * to the strictly gated destination. Non-logged-in users always resolve to Unauthenticated.
 */
fun resolveAuthDestination(
    uid: String?,
    initialDisplayName: String = "Clickit Neighbor",
    profile: UserProfile?,
    preselectedRole: UserRole? = null
): AuthDestination {
    if (uid.isNullOrBlank()) {
        return AuthDestination.Unauthenticated
    }
    if (profile == null) {
        return AuthDestination.NeedsOnboarding(
            uid = uid,
            initialDisplayName = initialDisplayName.ifBlank { "Clickit Neighbor" },
            preselectedRole = preselectedRole
        )
    }
    val resolvedRole = UserRole.fromRaw(profile.activeRole)
    return AuthDestination.RedirectToDashboard(
        profile = profile,
        role = resolvedRole,
        route = resolvedRole.toDashboardRoute()
    )
}

/**
 * Evaluates Firebase Auth session state and fetches/observes the user's role
 * from Cloud Firestore (`users/{uid}`) upon login, redirecting authenticated users
 * to their Customer, Shopper, or Delivery dashboard while keeping unauthenticated
 * users gated strictly inside the login/onboarding screens.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoleBasedAuthHandler(
    private val repository: ClickitRepository,
    private val auth: FirebaseAuth = Firebase.auth
) {
    fun observeAuthDestination(
        preselectedRoleFlow: Flow<UserRole?>,
        retryTrigger: Flow<Int>
    ): Flow<AuthDestination> = combine(
        auth.authStateFlow(),
        retryTrigger
    ) { firebaseUser, _ -> firebaseUser }
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                flowOf<AuthDestination>(AuthDestination.Unauthenticated)
            } else {
                val uid = firebaseUser.uid
                val fallbackName = firebaseUser.displayName ?: "Clickit Neighbor"
                flow<AuthDestination> {
                    emit(AuthDestination.EvaluatingRole(uid))
                    val initialFetch = repository.fetchUserProfile(uid)
                    if (initialFetch.isFailure) {
                        val err = initialFetch.exceptionOrNull()
                        emit(AuthDestination.Error(friendlyFirestoreMessage(err ?: IllegalStateException("Unable to verify account role."))))
                        return@flow
                    }
                    emitAll(
                        combine(
                            repository.observeUserProfile(uid),
                            preselectedRoleFlow
                        ) { profile, chosenRole ->
                            resolveAuthDestination(
                                uid = uid,
                                initialDisplayName = fallbackName,
                                profile = profile,
                                preselectedRole = chosenRole
                            )
                        }.catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            emit(AuthDestination.Error(friendlyFirestoreMessage(firestoreError ?: error)))
                        }
                    )
                }
            }
        }
}
