package com.example.newvitalgest01.ui.agendamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newvitalgest01.core.Result
import com.example.newvitalgest01.domain.model.Hemocentro
import com.example.newvitalgest01.domain.repository.AgendamentoRepository
import com.example.newvitalgest01.domain.repository.HemocentroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgendarDoacaoUiState(
    val isLoadingHemocentros: Boolean = false,
    val hemocentros: List<Hemocentro> = emptyList(),
    val errorHemocentros: String? = null
)

/**
 * ViewModel focado em:
 *  - carregar lista de hemocentros via Repository (com cache/local se existir)
 *
 * A criação do agendamento ainda está sendo feita diretamente na Activity
 * (Agendamento.kt) usando Firestore, então aqui não chamamos mais criarAgendamento().
 */
class AgendarDoacaoViewModel(
    private val hemocentroRepository: HemocentroRepository,
    private val agendamentoRepository: AgendamentoRepository // mantido para uso futuro
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgendarDoacaoUiState())
    val uiState: StateFlow<AgendarDoacaoUiState> = _uiState

    fun carregarHemocentros(userLat: Double?, userLng: Double?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingHemocentros = true,
                    errorHemocentros = null
                )
            }

            when (
                val result = hemocentroRepository.listarHemocentros(
                    userLat = userLat,
                    userLng = userLng,
                    forceRemote = false
                )
            ) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingHemocentros = false,
                            hemocentros = result.data,
                            errorHemocentros = null
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingHemocentros = false,
                            errorHemocentros = result.message
                        )
                    }
                }
            }
        }
    }
}