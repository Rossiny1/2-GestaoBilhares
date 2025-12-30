package com.example.gestaobilhares.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gestaobilhares.data.entities.MetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(meta: MetaEntity): Long

    @Update
    suspend fun atualizar(meta: MetaEntity)

    @Query("SELECT * FROM metas")
    fun obterTodas(): Flow<List<MetaEntity>>

    @Query("SELECT * FROM metas WHERE id = :metaId")
    suspend fun obterPorId(metaId: Long): MetaEntity?

    @Query("DELETE FROM metas WHERE id = :metaId")
    suspend fun deletarPorId(metaId: Long)
}
