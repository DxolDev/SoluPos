package com.solupos.contenedor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solupos.contenedor.data.db.StoreEntity
import com.solupos.contenedor.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class StoreFormState(
    val name: String = "",
    val url: String = "",
    val nameError: Boolean = false,
    val urlError: Boolean = false,
    val isSaved: Boolean = false
)

class StoreFormViewModel(
    private val repository: StoreRepository,
    private val editStoreId: String?
) : ViewModel() {

    val isEditing: Boolean = editStoreId != null

    private val _state = MutableStateFlow(StoreFormState())
    val state: StateFlow<StoreFormState> = _state.asStateFlow()

    init {
        editStoreId?.let { id ->
            viewModelScope.launch {
                repository.getById(id)?.let { store ->
                    _state.value = StoreFormState(name = store.name, url = store.url)
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value, nameError = false)
    }

    fun onUrlChange(value: String) {
        _state.value = _state.value.copy(url = value, urlError = false)
    }

    fun save() {
        val name = _state.value.name.trim()
        val url = _state.value.url.trim()
        if (name.isEmpty() || url.isEmpty()) {
            _state.value = _state.value.copy(nameError = name.isEmpty(), urlError = url.isEmpty())
            return
        }
        viewModelScope.launch {
            if (editStoreId != null) {
                repository.update(StoreEntity(id = editStoreId, name = name, url = url))
            } else {
                repository.insert(StoreEntity(id = UUID.randomUUID().toString(), name = name, url = url))
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}

class StoreFormViewModelFactory(
    private val repository: StoreRepository,
    private val editStoreId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        StoreFormViewModel(repository, editStoreId) as T
}
