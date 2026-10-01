package com.pastimes.app.ui.buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.data.model.Category
import com.pastimes.app.data.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val items: List<Item> = emptyList(),
    val categories: List<Category> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val minPrice: Double? = null,
    val maxPrice: Double? = null
)

class BuyerHomeViewModel : ViewModel() {
    private val api = ApiClient.retrofit.create(ApiService::class.java)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init { loadCategories(); loadItems() }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val cats = api.categories()
                _state.value = _state.value.copy(categories = cats)
            } catch (_: Exception) {}
        }
    }

    fun loadItems() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val s = _state.value
                val list = api.items(
                    categoryId = s.selectedCategoryId,
                    minPrice = s.minPrice,
                    maxPrice = s.maxPrice,
                    query = s.searchQuery.ifBlank { null }
                )
                _state.value = _state.value.copy(items = list, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load items"
                )
            }
        }
    }

    fun selectCategory(id: Long?) {
        _state.value = _state.value.copy(selectedCategoryId = id)
        loadItems()
    }

    fun setSearch(q: String) {
        _state.value = _state.value.copy(searchQuery = q)
    }

    fun applySearch() {
        loadItems()
    }

    fun setPriceRange(min: Double?, max: Double?) {
        _state.value = _state.value.copy(minPrice = min, maxPrice = max)
        loadItems()
    }
}