package com.example.newvitalgest01.ui.hemocentros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newvitalgest01.core.Result
import com.example.newvitalgest01.domain.model.Hemocentro
import com.example.newvitalgest01.domain.repository.HemocentroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HemocentrosUiState(
    val isLoading: Boolean = false,
    val hemocentros: List<Hemocentro> = emptyList(),
    val error: String? = null
)

class HemocentrosProximosViewModel(
    private val repository: HemocentroRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HemocentrosUiState())
    val uiState: StateFlow<HemocentrosUiState> = _uiState

    fun carregarHemocentros(
        userLat: Double? = null,
        userLng: Double? = null,
        forceRemote: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result =
                repository.listarHemocentros(userLat, userLng, forceRemote)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hemocentros = result.data,
                            error = null
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }
}