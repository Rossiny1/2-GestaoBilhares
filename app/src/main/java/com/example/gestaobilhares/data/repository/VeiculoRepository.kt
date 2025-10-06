package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.VeiculoDao
import com.example.gestaobilhares.data.entities.Veiculo
import kotlinx.coroutines.flow.Flow
class VeiculoRepository constructor(
    private val dao: VeiculoDao
) {
    fun listar(): Flow<List<Veiculo>> = dao.listar()
    suspend fun inserir(veiculo: Veiculo): Long = dao.inserir(veiculo)
    suspend fun atualizar(veiculo: Veiculo) = dao.atualizar(veiculo)
    suspend fun deletar(veiculo: Veiculo) = dao.deletar(veiculo)
}



