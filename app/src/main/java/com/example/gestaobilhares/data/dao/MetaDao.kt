package com.example.gestaobilhares.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gestaobilhares.data.entities.Meta
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meta: Meta): Long

    @Update
    suspend fun update(meta: Meta)

    @Query("SELECT * FROM metas")
    fun getAllMetas(): Flow<List<Meta>>

    @Query("SELECT * FROM metas WHERE id = :metaId")
    suspend fun getMetaById(metaId: Long): Meta?

    @Query("DELETE FROM metas WHERE id = :metaId")
    suspend fun deleteById(metaId: Long)
}
