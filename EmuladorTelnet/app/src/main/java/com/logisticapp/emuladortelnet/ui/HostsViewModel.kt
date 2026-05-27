package com.logisticapp.emuladortelnet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.logisticapp.emuladortelnet.database.SavedConnection
import com.logisticapp.emuladortelnet.database.TelnetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HostsViewModel(private val repository: TelnetRepository) : ViewModel() {

    val hosts: Flow<List<SavedConnection>> = repository.getAllConnections()

    fun deleteHost(id: Int) {
        viewModelScope.launch {
            repository.deleteConnection(id)
        }
    }
}

class HostsViewModelFactory(private val repository: TelnetRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HostsViewModel(repository) as T
    }
}
