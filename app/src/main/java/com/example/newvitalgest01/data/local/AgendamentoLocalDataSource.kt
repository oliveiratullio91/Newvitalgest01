package com.example.newvitalgest01.data.local

import com.example.newvitalgest01.domain.model.Agendamento

class AgendamentoLocalDataSource(
    private val dao: AgendamentoDao
) {

    suspend fun listarAgendamentos(): List<Agendamento> {
        return dao.listarTodos().map { it.toDomain() }
    }

    suspend fun salvarAgendamentos(lista: List<Agendamento>) {
        val entidades = lista.map { it.toEntity() }
        dao.salvarTodos(entidades)
    }

    suspend fun limpar() {
        dao.limpar()
    }
}