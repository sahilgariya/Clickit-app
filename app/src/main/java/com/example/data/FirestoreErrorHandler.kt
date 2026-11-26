package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun maskEmail(email: String?): String? {
    val clean = email?.trim().orEmpty()
    if (clean.isEmpty()) return email
    val atIdx = clean.indexOf('@')
    if (atIdx <= 0) return "***"
    val local = clean.substring(0, atIdx)
    val domain = clean.substring(atIdx)
    val visiblePrefix = local.take(2)
    return "$visiblePrefix***$domain"
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = try {
        Firebase.auth.currentUser
    } catch (_: Exception) {
        null
    }

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", maskEmail(provider.email))
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", maskEmail(currentUser?.email))
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun friendlyFirestoreMessage(error: Throwable): String {
    val causes = generateSequence(error) { it.cause }
    val firestoreException = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    val rawMessage = firestoreException?.message ?: error.localizedMessage ?: "Something went wrong"
    return when (firestoreException?.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Permission denied for this operation. Please ensure you are signed in with the right account."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Network unavailable. Changes will sync once reconnected."
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
            "Daily database quota exceeded. Please try again tomorrow."
        else -> rawMessage
    }
}
