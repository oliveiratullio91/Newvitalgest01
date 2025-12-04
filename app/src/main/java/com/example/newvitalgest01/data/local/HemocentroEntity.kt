package com.example.newvitalgest01.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.newvitalgest01.domain.model.Hemocentro

@Entity(tableName = "hemocentros")
data class HemocentroEntity(
    @PrimaryKey
    val id: String,
    val nome: String,
    val cidade: String,
    val uf: String,
    val regiao: String,
    val telefone: String?,
    val logradouro: String?,
    val status: String?,
    val cep: String?,
    val numero: String?,
    val horarioFuncionamento: String?,
    val latitude: Double?,
    val longitude: Double?
)

fun HemocentroEntity.toDomain(): Hemocentro =
    Hemocentro(
        id = id,
        nome = nome,
        cidade = cidade,
        uf = uf,
        regiao = regiao,
        telefone = telefone,
        logradouro = logradouro,
        status = status,
        cep = cep,
        numero = numero,
        horarioFuncionamento = horarioFuncionamento,
        latitude = latitude,
        longitude = longitude,
        distanciaKm = null // calculamos depois se precisar
    )

fun Hemocentro.toEntity(): HemocentroEntity =
    HemocentroEntity(
        id = id,
        nome = nome,
        cidade = cidade,
        uf = uf,
        regiao = regiao,
        telefone = telefone,
        logradouro = logradouro,
        status = status,
        cep = cep,
        numero = numero,
        horarioFuncionamento = horarioFuncionamento,
        latitude = latitude,
        longitude = longitude
    )