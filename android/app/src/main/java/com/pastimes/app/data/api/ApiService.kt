package com.pastimes.app.data.api

import com.pastimes.app.data.model.*
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    data class HealthResponse(val status: String, val ts: String)

    @GET("health")
    suspend fun health(): HealthResponse

    // --- Auth ---
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): User

    @POST("api/auth/login")
    suspend fun login(): User

    @GET("api/auth/me")
    suspend fun me(): User

    // --- Categories / Items ---
    @GET("api/categories")
    suspend fun categories(): List<Category>

    @GET("api/items")
    suspend fun items(
        @Query("category_id") categoryId: Long? = null,
        @Query("size") size: String? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("q") query: String? = null
    ): List<Item>

    @GET("api/items/{id}")
    suspend fun itemDetail(@Path("id") id: Long): Item

    // --- Seller ---
    @GET("api/items/mine")
    suspend fun myItems(@Query("status") status: String? = null): List<Item>

    @POST("api/items")
    suspend fun createItem(@Body body: ItemRequest): Item

    @PUT("api/items/{id}")
    suspend fun updateItem(@Path("id") id: Long, @Body body: ItemRequest): Item

    @DELETE("api/items/{id}")
    suspend fun deleteItem(@Path("id") id: Long): OkResponse

    // --- Cart ---
    @GET("api/cart")
    suspend fun cart(): CartResponse

    @POST("api/cart/items")
    suspend fun addToCart(@Body body: AddToCartRequest): OkResponse

    @DELETE("api/cart/items/{itemId}")
    suspend fun removeFromCart(@Path("itemId") itemId: Long): OkResponse

    @DELETE("api/cart")
    suspend fun clearCart(): OkResponse

    // --- Addresses ---
    @GET("api/addresses")
    suspend fun addresses(): List<Address>

    @POST("api/addresses")
    suspend fun createAddress(@Body body: AddressRequest): Address

    @DELETE("api/addresses/{id}")
    suspend fun deleteAddress(@Path("id") id: Long): OkResponse

    // --- Orders ---
    @POST("api/orders")
    suspend fun createOrder(@Body body: CreateOrderRequest): Order

    @GET("api/orders")
    suspend fun orders(): List<Order>

    @GET("api/orders/{id}")
    suspend fun orderDetail(@Path("id") id: Long): Order

    // --- Admin ---
    @GET("api/admin/users")
    suspend fun adminUsers(@Query("role") role: String? = null): List<AdminUser>

    @GET("api/admin/users/{id}")
    suspend fun adminUserDetail(@Path("id") id: Long): AdminUser

    @GET("api/admin/users/{id}/transactions")
    suspend fun adminUserTransactions(@Path("id") id: Long): List<AdminTransaction>

    @GET("api/admin/users/{id}/earnings")
    suspend fun adminUserEarnings(@Path("id") id: Long): AdminEarningsResponse

    @POST("api/admin/users/{id}/reset-password")
    suspend fun adminResetPassword(@Path("id") id: Long): ResetPasswordResponse

    @POST("api/admin/users/{id}/toggle-active")
    suspend fun adminToggleActive(@Path("id") id: Long): ToggleActiveResponse

    // --- Settings ---
    @GET("api/settings")
    suspend fun getSettings(): UserSettings

    @PUT("api/settings")
    suspend fun updateSettings(@Body body: SettingsRequest): UserSettings
}