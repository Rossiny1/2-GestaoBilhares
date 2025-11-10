package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.CategoriaDespesaDao
import com.example.gestaobilhares.data.dao.TipoDespesaDao
import com.example.gestaobilhares.data.entities.CategoriaDespesa
import com.example.gestaobilhares.data.entities.TipoDespesa
import com.example.gestaobilhares.data.entities.NovaCategoriaDespesa
import com.example.gestaobilhares.data.entities.NovoTipoDespesa
import android.util.Log

/**
 * ✅ FASE 12.14 Etapa 7: Repository interno para operações de CategoriaDespesa e TipoDespesa
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Inclui: CategoriaDespesa, TipoDespesa e todos os métodos relacionados
 */
internal class CategoriaTipoDespesaRepositoryInternal(
    private val categoriaDespesaDao: CategoriaDespesaDao,
    private val tipoDespesaDao: TipoDespesaDao
) {
    
    // ==================== CATEGORIA DESPESA ====================
    
    fun buscarCategoriasAtivas() = categoriaDespesaDao.buscarAtivas()
    
    suspend fun buscarCategoriaPorNome(nome: String) = categoriaDespesaDao.buscarPorNome(nome)
    
    suspend fun categoriaExiste(nome: String): Boolean = categoriaDespesaDao.contarPorNome(nome) > 0
    
    suspend fun criarCategoria(
        nova: NovaCategoriaDespesa,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        val entity = CategoriaDespesa(
            nome = nova.nome,
            descricao = nova.descricao,
            criadoPor = nova.criadoPor
        )
        val id = categoriaDespesaDao.inserir(entity)
        
        try {
            val payload = """
                {
                    "id": $id,
                    "nome": "${entity.nome}",
                    "descricao": "${entity.descricao}",
                    "criadoPor": "${entity.criadoPor}",
                    "ativo": ${entity.ativa},
                    "dataCriacao": ${entity.dataCriacao.time}
                }
            """.trimIndent()
            adicionarOperacaoSync("CategoriaDespesa", id, "CREATE", payload, 1)
            logarOperacaoSync("CategoriaDespesa", id, "CREATE", "PENDING", null, payload)
        } catch (syncError: Exception) {
            Log.w("CategoriaTipoDespesaRepositoryInternal", "Erro ao adicionar categoria à fila de sync: ${syncError.message}")
        }
        
        return id
    }
    
    fun obterTodasCategoriasDespesa() = categoriaDespesaDao.buscarTodas()
    
    suspend fun obterCategoriaDespesaPorId(id: Long) = categoriaDespesaDao.buscarPorId(id)
    
    suspend fun inserirCategoriaDespesaSync(
        categoria: CategoriaDespesa,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("CATEGORIA_DESPESA", "Nome=${categoria.nome}")
        return try {
            val id = categoriaDespesaDao.inserir(categoria)
            logDbInsertSuccess("CATEGORIA_DESPESA", "Nome=${categoria.nome}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "nome": "${categoria.nome}",
                        "descricao": "${categoria.descricao}",
                        "ativa": ${categoria.ativa},
                        "dataCriacao": ${categoria.dataCriacao.time},
                        "dataAtualizacao": ${categoria.dataAtualizacao.time},
                        "criadoPor": "${categoria.criadoPor}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("CategoriaDespesa", id, "CREATE", payload, 1)
                logarOperacaoSync("CategoriaDespesa", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("CategoriaTipoDespesaRepositoryInternal", "Erro ao adicionar criação de categoria despesa à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("CATEGORIA_DESPESA", "Nome=${categoria.nome}", e)
            throw e
        }
    }
    
    suspend fun atualizarCategoriaDespesaSync(
        categoria: CategoriaDespesa,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("CATEGORIA_DESPESA", "ID=${categoria.id}, Nome=${categoria.nome}")
        try {
            categoriaDespesaDao.atualizar(categoria)
            logDbUpdateSuccess("CATEGORIA_DESPESA", "ID=${categoria.id}")
            
            try {
                val payload = """
                    {
                        "id": ${categoria.id},
                        "nome": "${categoria.nome}",
                        "descricao": "${categoria.descricao}",
                        "ativa": ${categoria.ativa},
                        "dataCriacao": ${categoria.dataCriacao.time},
                        "dataAtualizacao": ${categoria.dataAtualizacao.time},
                        "criadoPor": "${categoria.criadoPor}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("CategoriaDespesa", categoria.id, "UPDATE", payload, 1)
                logarOperacaoSync("CategoriaDespesa", categoria.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("CategoriaTipoDespesaRepositoryInternal", "Erro ao adicionar atualização de categoria despesa à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("CATEGORIA_DESPESA", "ID=${categoria.id}", e)
            throw e
        }
    }
    
    suspend fun deletarCategoriaDespesa(categoria: CategoriaDespesa) = categoriaDespesaDao.deletar(categoria)
    
    // ==================== TIPO DESPESA ====================
    
    fun buscarTiposPorCategoria(categoriaId: Long) = tipoDespesaDao.buscarPorCategoria(categoriaId)
    
    suspend fun buscarTipoPorNome(nome: String) = tipoDespesaDao.buscarPorNome(nome)
    
    suspend fun tipoExiste(nome: String, categoriaId: Long): Boolean {
        val tipo = tipoDespesaDao.buscarPorNome(nome)
        return tipo != null && tipo.categoriaId == categoriaId
    }
    
    suspend fun criarTipo(
        novo: NovoTipoDespesa,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        val entity = TipoDespesa(
            categoriaId = novo.categoriaId,
            nome = novo.nome,
            descricao = novo.descricao,
            criadoPor = novo.criadoPor
        )
        val id = tipoDespesaDao.inserir(entity)
        
        try {
            val payload = """
                {
                    "id": $id,
                    "categoriaId": ${entity.categoriaId},
                    "nome": "${entity.nome}",
                    "descricao": "${entity.descricao}",
                    "criadoPor": "${entity.criadoPor}",
                    "ativo": ${entity.ativo},
                    "dataCriacao": ${entity.dataCriacao.time}
                }
            """.trimIndent()
            adicionarOperacaoSync("TipoDespesa", id, "CREATE", payload, 1)
            logarOperacaoSync("TipoDespesa", id, "CREATE", "PENDING", null, payload)
        } catch (syncError: Exception) {
            Log.w("CategoriaTipoDespesaRepositoryInternal", "Erro ao adicionar tipo à fila de sync: ${syncError.message}")
        }
        
        return id
    }
    
    fun obterTodosTiposDespesa() = tipoDespesaDao.buscarTodos()
    
    suspend fun obterTipoDespesaPorId(id: Long) = tipoDespesaDao.buscarPorId(id)
    
    suspend fun inserirTipoDespesaSync(
        tipo: TipoDespesa,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("TIPO_DESPESA", "Nome=${tipo.nome}, Categoria=${tipo.categoriaId}")
        return try {
            val id = tipoDespesaDao.inserir(tipo)
            logDbInsertSuccess("TIPO_DESPESA", "Nome=${tipo.nome}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "categoriaId": ${tipo.categoriaId},
                        "nome": "${tipo.nome}",
                        "descricao": "${tipo.descricao}",
                        "ativo": ${tipo.ativo},
                        "dataCriacao": ${tipo.dataCriacao.time},
                        "dataAtualizacao": ${tipo.dataAtualizacao.time},
                        "criadoPor": "${tipo.criadoPor}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("TipoDespesa", id, "CREATE", payload, 1)
                logarOperacaoSync("TipoDespesa", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("CategoriaTipoDespesaRepositoryInternal", "Erro ao adicionar criação de tipo despesa à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("TIPO_DESPESA", "Nome=${tipo.nome}", e)
            throw e
        }
    }
    
    suspend fun atualizarTipoDespesaSync(
        tipo: TipoDespesa,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("TIPO_DESPESA", "ID=${tipo.id}, Nome=${tipo.nome}")
        try {
            tipoDespesaDao.atualizar(tipo)
            logDbUpdateSuccess("TIPO_DESPESA", "ID=${tipo.id}")
            
            try {
                val payload = """
                    {
                        "id": ${tipo.id},
                        "categoriaId": ${tipo.categoriaId},
                        "nome": "${tipo.nome}",
                        "descricao": "${tipo.descricao}",
                        "ativo": ${tipo.ativo},
                        "dataCriacao": ${tipo.dataCriacao.time},
                        "dataAtualizacao": ${tipo.dataAtualizacao.time},
                        "criadoPor": "${tipo.criadoPor}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("TipoDespesa", tipo.id, "UPDATE", payload, 1)
                logarOperacaoSync("TipoDespesa", tipo.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("CategoriaTipoDespesaRepositoryInternal", "Erro ao adicionar atualização de tipo despesa à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("TIPO_DESPESA", "ID=${tipo.id}", e)
            throw e
        }
    }
    
    suspend fun deletarTipoDespesa(tipo: TipoDespesa) = tipoDespesaDao.deletar(tipo)
}

