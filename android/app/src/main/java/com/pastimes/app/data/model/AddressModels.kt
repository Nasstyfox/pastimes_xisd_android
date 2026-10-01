package com.pastimes.app.data.model

import com.google.gson.annotations.SerializedName

data class Address(
    val id: Long,
    @SerializedName("user_id") val userId: Long,
    val label: String? = null,
    val recipient: String,
    val phone: String,
    val street: String,
    val suburb: String,
    val city: String,
    val province: String,
    @SerializedName("postal_code") val postalCode: String,
    val country: String = "South Africa",
    @SerializedName("is_default") val isDefault: Int = 0
)

data class AddressRequest(
    val label: String? = null,
    val recipient: String,
    val phone: String,
    val street: String,
    val suburb: String,
    val city: String,
    val province: String,
    @SerializedName("postal_code") val postalCode: String,
    val country: String = "South Africa",
    @SerializedName("is_default") val isDefault: Boolean = false
)