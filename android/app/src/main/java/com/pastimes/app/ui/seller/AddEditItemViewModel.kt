package com.pastimes.app.ui.seller

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.data.model.Category
import com.pastimes.app.data.model.Item
import com.pastimes.app.data.model.ItemRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
data class AddEditUiState(
    val editItemId: Long? = null,
    val categories: List<Category> = emptyList(),
    val categoryId: Long? = null,
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val size: String = "",
    val brand: String = "",
    val colour: String = "",
    val condition: String = "good",
    val imageUrl: String? = null,
    val uploading: Boolean = false,
    val saving: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class AddEditItemViewModel : ViewModel() {
    private val api = ApiClient.retrofit.create(ApiService::class.java)
    private val _state = MutableStateFlow(AddEditUiState())
    val state: StateFlow<AddEditUiState> = _state

    init { loadCategories() }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val cats = api.categories()
                _state.value = _state.value.copy(categories = cats)
            } catch (_: Exception) {}
        }
    }

    /** Called when editing an existing item */
    fun loadForEdit(id: Long) {
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            try {
                val item = api.itemDetail(id)
                _state.value = _state.value.copy(
                    editItemId = id,
                    categoryId = item.categoryId,
                    title = item.title,
                    description = item.description ?: "",
                    price = item.price.toString(),
                    size = item.size ?: "",
                    brand = item.brand ?: "",
                    colour = item.colour ?: "",
                    condition = item.conditionTag,
                    imageUrl = item.imageUrl,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load item"
                )
            }
        }
    }

    fun setCategory(id: Long) { _state.value = _state.value.copy(categoryId = id) }
    fun setTitle(v: String) { _state.value = _state.value.copy(title = v) }
    fun setDescription(v: String) { _state.value = _state.value.copy(description = v) }
    fun setPrice(v: String) { _state.value = _state.value.copy(price = v) }
    fun setSize(v: String) { _state.value = _state.value.copy(size = v) }
    fun setBrand(v: String) { _state.value = _state.value.copy(brand = v) }
    fun setColour(v: String) { _state.value = _state.value.copy(colour = v) }
    fun setCondition(v: String) { _state.value = _state.value.copy(condition = v) }

    fun uploadImage(uri: Uri) {
        _state.value = _state.value.copy(uploading = true, error = null)

        MediaManager.get().upload(uri)
            .unsigned("pastimes_unsigned") // Your preset name
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url") as? String
                    _state.value = _state.value.copy(imageUrl = imageUrl, uploading = false)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    _state.value = _state.value.copy(
                        uploading = false,
                        error = error?.description ?: "Upload failed"
                    )
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    fun save() {
        val s = _state.value
        val catId = s.categoryId ?: run {
            _state.value = s.copy(error = "Please select a category"); return
        }
        val priceVal = s.price.toDoubleOrNull() ?: run {
            _state.value = s.copy(error = "Price must be a number"); return
        }
        if (s.title.isBlank()) {
            _state.value = s.copy(error = "Title is required"); return
        }
        if (priceVal <= 0) {
            _state.value = s.copy(error = "Price must be greater than zero"); return
        }

        _state.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val req = ItemRequest(
                    categoryId = catId,
                    title = s.title.trim(),
                    description = s.description.trim().ifBlank { null },
                    price = priceVal,
                    size = s.size.trim().ifBlank { null },
                    brand = s.brand.trim().ifBlank { null },
                    colour = s.colour.trim().ifBlank { null },
                    conditionTag = s.condition,
                    imageUrl = s.imageUrl
                )
                if (s.editItemId != null) {
                    api.updateItem(s.editItemId, req)
                } else {
                    api.createItem(req)
                }
                _state.value = _state.value.copy(saving = false, success = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    saving = false,
                    error = e.message ?: "Failed to save"
                )
            }
        }
    }
}