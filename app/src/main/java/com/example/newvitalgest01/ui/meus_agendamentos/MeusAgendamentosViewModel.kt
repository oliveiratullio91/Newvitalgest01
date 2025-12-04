package com.example.newvitalgest01.ui.meus_agendamentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newvitalgest01.core.Result
import com.example.newvitalgest01.domain.model.Agendamento
import com.example.newvitalgest01.domain.repository.AgendamentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeusAgendamentosUiState(
    val isLoading: Boolean = false,
    val agendamentos: List<Agendamento> = emptyList(),
    val error: String? = null,
    val vazio: Boolean = false
)

class MeusAgendamentosViewModel(
    private val repository: AgendamentoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeusAgendamentosUiState())
    val uiState: StateFlow<MeusAgendamentosUiState> = _uiState

    init {
        // Carrega na criação
        carregarAgendamentos(forceRemote = false)
    }

    fun recarregar(forceRemote: Boolean = true) {
        carregarAgendamentos(forceRemote)
    }

    private fun carregarAgendamentos(forceRemote: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.listarAgendamentosUsuarioAtual(forceRemote)) {
                is Result.Success -> {
                    val lista = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            agendamentos = lista,
                            vazio = lista.isEmpty(),
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