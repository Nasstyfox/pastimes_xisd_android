package com.pastimes.app.data.model

import com.google.gson.annotations.SerializedName

data class Category(
    val id: Long,
    val name: String,
    val description: String? = null
)

data class Item(
    val id: Long,
    val title: String,
    val description: String? = null,
    val price: Double,
    val size: String? = null,
    val brand: String? = null,
    val colour: String? = null,
    @SerializedName("condition_tag") val conditionTag: String = "good",
    @SerializedName("image_url") val imageUrl: String? = null,
    val status: String = "available",
    @SerializedName("category_id") val categoryId: Long,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("seller_id") val sellerId: Long,
    @SerializedName("seller_name") val sellerName: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AddToCartRequest(
    @SerializedName("item_id") val itemId: Long
)

data class OkResponse(val ok: Boolean? = null, val error: String? = null)