package com.pastimes.app.ui.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.data.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SellerStats(
    val total: Int = 0,
    val available: Int = 0,
    val sold: Int = 0,
    val removed: Int = 0,
    val earnings: Double = 0.0
)

data class SellerListingsUiState(
    val items: List<Item> = emptyList(),
    val stats: SellerStats = SellerStats(),
    val filter: String = "all",
    val loading: Boolean = false,
    val error: String? = null
)

class SellerListingsViewModel : ViewModel() {
    private val api = ApiClient.retrofit.create(ApiService::class.java)
    private val _state = MutableStateFlow(SellerListingsUiState())
    val state: StateFlow<SellerListingsUiState> = _state

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                // Fetch all items for stats
                val all = api.myItems(null)
                val stats = SellerStats(
                    total = all.size,
                    available = all.count { it.status == "available" },
                    sold = all.count { it.status == "sold" },
                    removed = all.count { it.status == "removed" },
                    earnings = all.filter { it.status == "sold" }.sumOf { it.price }
                )

                // Filter for display
                val filtered = when (_state.value.filter) {
                    "available" -> all.filter { it.status == "available" }
                    "sold" -> all.filter { it.status == "sold" }
                    "removed" -> all.filter { it.status == "removed" }
                    else -> all
                }

                _state.value = _state.value.copy(
                    items = filtered,
                    stats = stats,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load items"
                )
            }
        }
    }

    fun setFilter(filter: String) {
        _state.value = _state.value.copy(filter = filter)
        load()
    }

    fun delete(itemId: Long) {
        viewModelScope.launch {
            try {
                api.deleteItem(itemId)
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }
}