package com.example.newvitalgest01.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HemocentroDao {

    @Query("SELECT * FROM hemocentros")
    suspend fun listarTodos(): List<HemocentroEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarTodos(lista: List<HemocentroEntity>)

    @Query("DELETE FROM hemocentros")
    suspend fun limpar()
}