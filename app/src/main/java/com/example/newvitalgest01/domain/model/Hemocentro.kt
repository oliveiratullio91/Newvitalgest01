package com.example.newvitalgest01.domain.model

data class Hemocentro(
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
    val longitude: Double?,
    val distanciaKm: Double? // pode ser null se não tiver localização
)