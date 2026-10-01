package com.pastimes.app.data.model

import com.google.gson.annotations.SerializedName

data class Order(
    val id: Long,
    @SerializedName("buyer_id") val buyerId: Long,
    @SerializedName("total_amount") val totalAmount: Double,
    val status: String,
    @SerializedName("ship_recipient") val shipRecipient: String,
    @SerializedName("ship_phone") val shipPhone: String,
    @SerializedName("ship_street") val shipStreet: String,
    @SerializedName("ship_suburb") val shipSuburb: String,
    @SerializedName("ship_city") val shipCity: String,
    @SerializedName("ship_province") val shipProvince: String,
    @SerializedName("ship_postal_code") val shipPostalCode: String,
    @SerializedName("ship_country") val shipCountry: String,
    @SerializedName("created_at") val createdAt: String? = null,
    val items: List<OrderItem> = emptyList()
)

data class OrderItem(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("item_id") val itemId: Long,
    @SerializedName("price_at_purchase") val priceAtPurchase: Double,
    val title: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class CreateOrderRequest(
    @SerializedName("address_id") val addressId: Long
)