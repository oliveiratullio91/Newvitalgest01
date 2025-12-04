package com.example.newvitalgest01.domain.repository

import com.example.newvitalgest01.core.Result
import com.example.newvitalgest01.domain.model.Hemocentro

interface HemocentroRepository {

    suspend fun listarHemocentros(
        userLat: Double? = null,
        userLng: Double? = null,
        forceRemote: Boolean = false
    ): Result<List<Hemocentro>>
}