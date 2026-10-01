package com.pastimes.app.data.model

import com.google.gson.annotations.SerializedName

data class ItemRequest(
    @SerializedName("category_id") val categoryId: Long,
    val title: String,
    val description: String? = null,
    val price: Double,
    val size: String? = null,
    val brand: String? = null,
    val colour: String? = null,
    @SerializedName("condition_tag") val conditionTag: String = "good",
    @SerializedName("image_url") val imageUrl: String? = null,
    val status: String? = null
)