package com.solupos.contenedor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solupos.contenedor.data.db.StoreEntity
import com.solupos.contenedor.data.preferences.UserPreferences
import com.solupos.contenedor.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StoreListViewModel(
    private val repository: StoreRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val stores: StateFlow<List<StoreEntity>> = repository.stores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showTutorial = MutableStateFlow(false)
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    init {
        viewModelScope.launch {
            if (!userPreferences.hasSeenTutorial.first()) {
                _showTutorial.value = true
            }
        }
    }

    fun dismissTutorial() {
        _showTutorial.value = false
        viewModelScope.launch { userPreferences.markTutorialSeen() }
    }

    fun replayTutorial() {
        _showTutorial.value = true
    }

    fun deleteStore(store: StoreEntity) {
        viewModelScope.launch { repository.delete(store) }
    }

    fun saveLastStore(id: String) {
        viewModelScope.launch { repository.saveLastStoreId(id) }
    }
}

class StoreListViewModelFactory(
    private val repository: StoreRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        StoreListViewModel(repository, userPreferences) as T
}
