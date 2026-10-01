package com.pastimes.app.data.model

import com.google.gson.annotations.SerializedName

data class AdminUser(
    val id: Long,
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val phone: String?,
    val role: String,
    @SerializedName("auth_provider") val authProvider: String,
    @SerializedName("is_active") val isActive: Int,
    @SerializedName("created_at") val createdAt: String? = null,
    val settings: UserSettings? = null,
    val summary: UserSummary? = null
)

data class UserSummary(
    @SerializedName("order_count") val orderCount: Int? = null,
    @SerializedName("total_spent") val totalSpent: Double? = null,
    @SerializedName("total_items") val totalItems: Int? = null,
    @SerializedName("available_items") val availableItems: Int? = null,
    @SerializedName("sold_items") val soldItems: Int? = null,
    @SerializedName("total_earnings") val totalEarnings: Double? = null
)

data class AdminTransactionItem(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("item_id") val itemId: Long,
    @SerializedName("price_at_purchase") val priceAtPurchase: Double,
    val title: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("seller_name") val sellerName: String? = null
)

data class AdminTransaction(
    val id: Long,
    @SerializedName("buyer_id") val buyerId: Long,
    @SerializedName("total_amount") val totalAmount: Double,
    val status: String,
    @SerializedName("created_at") val createdAt: String? = null,
    val items: List<AdminTransactionItem> = emptyList()
)

data class AdminEarningsResponse(
    @SerializedName("total_earnings") val totalEarnings: Double,
    @SerializedName("sold_count") val soldCount: Int,
    val sales: List<AdminSale> = emptyList()
)

data class AdminSale(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("item_id") val itemId: Long,
    @SerializedName("price_at_purchase") val priceAtPurchase: Double,
    @SerializedName("created_at") val createdAt: String? = null,
    val title: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("buyer_name") val buyerName: String? = null
)

data class ResetPasswordResponse(
    val ok: Boolean? = null,
    val message: String? = null,
    val email: String? = null,
    val error: String? = null
)

data class ToggleActiveResponse(
    val ok: Boolean? = null,
    @SerializedName("is_active") val isActive: Int? = null,
    val error: String? = null
)