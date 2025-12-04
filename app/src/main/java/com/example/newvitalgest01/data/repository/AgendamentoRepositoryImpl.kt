package com.example.newvitalgest01.data.repository

import com.example.newvitalgest01.core.Result
import com.example.newvitalgest01.data.local.AgendamentoLocalDataSource
import com.example.newvitalgest01.data.remote.AgendamentoRemoteDataSource
import com.example.newvitalgest01.domain.model.Agendamento
import com.example.newvitalgest01.domain.repository.AgendamentoRepository

class AgendamentoRepositoryImpl(
    private val remote: AgendamentoRemoteDataSource,
    private val local: AgendamentoLocalDataSource
) : AgendamentoRepository {

    override suspend fun listarAgendamentosUsuarioAtual(
        forceRemote: Boolean
    ): Result<List<Agendamento>> {
        return try {
            if (forceRemote) {
                // força buscar no remoto e atualizar cache
                val remotos = remote.listarAgendamentosUsuarioAtual()
                local.salvarAgendamentos(remotos)
                Result.Success(remotos)
            } else {
                // tenta primeiro o cache local
                val locais = local.listarAgendamentos()
                if (locais.isNotEmpty()) {
                    Result.Success(locais)
                } else {
                    // se não tiver cache, vai no remoto
                    val remotos = remote.listarAgendamentosUsuarioAtual()
                    local.salvarAgendamentos(remotos)
                    Result.Success(remotos)
                }
            }
        } catch (e: Exception) {
            // em caso de erro de rede, tenta devolver o cache
            val cache = local.listarAgendamentos()
            if (cache.isNotEmpty()) {
                Result.Success(cache)
            } else {
                Result.Error(e.message ?: "Erro ao carregar agendamentos", e)
            }
        }
    }
}