package com.example.newvitalgest01.data.repository

import com.example.newvitalgest01.core.Result
import com.example.newvitalgest01.data.local.HemocentroLocalDataSource
import com.example.newvitalgest01.data.remote.HemocentroRemoteDataSource
import com.example.newvitalgest01.domain.model.Hemocentro
import com.example.newvitalgest01.domain.repository.HemocentroRepository

class HemocentroRepositoryImpl(
    private val remote: HemocentroRemoteDataSource,
    private val local: HemocentroLocalDataSource
) : HemocentroRepository {

    override suspend fun listarHemocentros(
        userLat: Double?,
        userLng: Double?,
        forceRemote: Boolean
    ): Result<List<Hemocentro>> {
        return try {
            if (forceRemote) {
                val remotos = remote.listarHemocentros(userLat, userLng)
                local.salvarHemocentros(remotos)
                Result.Success(remotos)
            } else {
                val locais = local.listarHemocentros()
                if (locais.isNotEmpty()) {
                    Result.Success(locais)
                } else {
                    val remotos = remote.listarHemocentros(userLat, userLng)
                    local.salvarHemocentros(remotos)
                    Result.Success(remotos)
                }
            }
        } catch (e: Exception) {
            val cache = local.listarHemocentros()
            if (cache.isNotEmpty()) {
                Result.Success(cache)
            } else {
                Result.Error(e.message ?: "Erro ao carregar hemocentros", e)
            }
        }
    }
}