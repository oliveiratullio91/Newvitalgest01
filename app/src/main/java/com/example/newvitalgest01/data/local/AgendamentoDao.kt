package com.example.newvitalgest01.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AgendamentoDao {

    @Query("SELECT * FROM agendamentos")
    suspend fun listarTodos(): List<AgendamentoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarTodos(lista: List<AgendamentoEntity>)

    @Query("DELETE FROM agendamentos")
    suspend fun limpar()
}