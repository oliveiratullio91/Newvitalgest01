package com.example.newvitalgest01.view

data class AgendamentoItem(
    val id: String,          // ID do documento no Firestore
    val hemocentro: String,
    val data: String,
    val hora: String,
    val endereco: String,
    val telefone: String,
    val status: String
)