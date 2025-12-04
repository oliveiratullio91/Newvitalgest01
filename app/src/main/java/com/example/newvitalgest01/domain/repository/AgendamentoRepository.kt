package com.example.newvitalgest01.domain.repository

import com.example.newvitalgest01.core.Result
import com.example.newvitalgest01.domain.model.Agendamento

interface AgendamentoRepository {

    suspend fun listarAgendamentosUsuarioAtual(
        forceRemote: Boolean = false
    ): Result<List<Agendamento>>

    // no futuro: criar / cancelar / sync etc.
}