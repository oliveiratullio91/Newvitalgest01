package com.example.newvitalgest01.domain.model

data class Agendamento(
    val id: String = "",
    val data: String = "",
    val hora: String = "",
    val hemocentro: String = "",
    val cidade: String = "",
    val estado: String = "",
    val endereco: String = "",
    val telefone: String = "",
    val status: String = ""
)