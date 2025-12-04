package com.example.newvitalgest01.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.newvitalgest01.domain.model.Agendamento

@Entity(tableName = "agendamentos")
data class AgendamentoEntity(
    @PrimaryKey
    val id: String,
    val data: String,
    val hora: String,
    val hemocentro: String,
    val cidade: String,
    val estado: String,
    val endereco: String,
    val telefone: String,
    val status: String
)

fun AgendamentoEntity.toDomain(): Agendamento =
    Agendamento(
        id = id,
        data = data,
        hora = hora,
        hemocentro = hemocentro,
        cidade = cidade,
        estado = estado,
        endereco = endereco,
        telefone = telefone,
        status = status
    )

fun Agendamento.toEntity(): AgendamentoEntity =
    AgendamentoEntity(
        id = id,
        data = data,
        hora = hora,
        hemocentro = hemocentro,
        cidade = cidade,
        estado = estado,
        endereco = endereco,
        telefone = telefone,
        status = status
    )