package com.pastimes.app.data.model

import com.google.gson.annotations.SerializedName

data class CartResponse(
    @SerializedName("cart_id") val cartId: Long,
    @SerializedName("item_count") val itemCount: Int,
    val total: Double,
    val items: List<CartItem> = emptyList()
)

data class CartItem(
    @SerializedName("cart_item_id") val cartItemId: Long,
    @SerializedName("item_id") val itemId: Long,
    val title: String,
    val price: Double,
    @SerializedName("image_url") val imageUrl: String? = null,
    val size: String? = null,
    val brand: String? = null,
    val status: String = "available",
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("seller_name") val sellerName: String? = null
)