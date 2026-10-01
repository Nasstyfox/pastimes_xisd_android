package com.pastimes.app.data.model

import com.google.gson.annotations.SerializedName

data class SettingsRequest(
    val theme: String? = null,
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean? = null,
    val language: String? = null
)