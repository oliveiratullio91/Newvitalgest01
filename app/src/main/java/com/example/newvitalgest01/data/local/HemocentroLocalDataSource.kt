package com.example.newvitalgest01.data.local

import com.example.newvitalgest01.domain.model.Hemocentro

class HemocentroLocalDataSource(
    private val dao: HemocentroDao
) {

    suspend fun listarHemocentros(): List<Hemocentro> {
        return dao.listarTodos().map { it.toDomain() }
    }

    suspend fun salvarHemocentros(lista: List<Hemocentro>) {
        val entidades = lista.map { it.toEntity() }
        dao.salvarTodos(entidades)
    }

    suspend fun limpar() {
        dao.limpar()
    }
}