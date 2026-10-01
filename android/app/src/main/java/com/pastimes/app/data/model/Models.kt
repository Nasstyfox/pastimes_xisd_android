package com.pastimes.app.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Long,
    @SerializedName("firebase_uid") val firebaseUid: String,
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val phone: String?,
    val role: String,
    @SerializedName("auth_provider") val authProvider: String,
    @SerializedName("is_active") val isActive: Int = 1,
    @SerializedName("created_at") val createdAt: String? = null,
    val settings: UserSettings? = null
)

data class UserSettings(
    val theme: String = "system",
    @SerializedName("notifications_enabled") val notificationsEnabled: Int = 1,
    val language: String = "en"
)

data class RegisterRequest(
    @SerializedName("full_name") val fullName: String,
    val phone: String?,
    val role: String,
    @SerializedName("auth_provider") val authProvider: String = "password"
)

data class ErrorResponse(val error: String? = null)