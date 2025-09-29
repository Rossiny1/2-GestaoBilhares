package com.example.gestaobilhares.ui.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para cadastro de metas
 */
@HiltViewModel
class MetaCadastroViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _rotas = MutableStateFlow<List<Rota>>(emptyList())
    val rotas: StateFlow<List<Rota>> = _rotas.asStateFlow()

    private val _ciclos = MutableStateFlow<List<CicloAcertoEntity>>(emptyList())
    val ciclos: StateFlow<List<CicloAcertoEntity>> = _ciclos.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _metaSalva = MutableStateFlow(false)
    val metaSalva: StateFlow<Boolean> = _metaSalva.asStateFlow()

    /**
     * Carrega todas as rotas ativas
     */
    fun carregarRotas() {
        viewModelScope.launch {
            try {
                val rotasAtivas = appRepository.obterTodasRotas().first().filter { rota -> rota.ativa }
                _rotas.value = rotasAtivas
                android.util.Log.d("MetaCadastroViewModel", "Rotas carregadas: ${rotasAtivas.size}")
            } catch (e: Exception) {
                android.util.Log.e("MetaCadastroViewModel", "Erro ao carregar rotas: ${e.message}", e)
                _message.value = "Erro ao carregar rotas: ${e.message}"
            }
        }
    }

    /**
     * Carrega ciclos para uma rota específica
     */
    fun carregarCiclosPorRota(rotaId: Long) {
        viewModelScope.launch {
            try {
                android.util.Log.d("MetaCadastroViewModel", "Carregando ciclos para rota $rotaId")
                
                // Buscar ciclo atual (em andamento)
                val cicloAtual = appRepository.buscarCicloAtualPorRota(rotaId)
                
                // Buscar ciclos futuros (planejados)
                val ciclosFuturos = appRepository.buscarCiclosFuturosPorRota(rotaId)
                
                val todosCiclos = mutableListOf<CicloAcertoEntity>()
                
                // Adicionar ciclo atual se existir
                cicloAtual?.let { ciclo -> todosCiclos.add(ciclo) }
                
                // Adicionar ciclos futuros
                todosCiclos.addAll(ciclosFuturos)
                
                _ciclos.value = todosCiclos
                android.util.Log.d("MetaCadastroViewModel", "Ciclos carregados: ${todosCiclos.size}")
                
                if (todosCiclos.isEmpty()) {
                    _message.value = "Nenhum ciclo encontrado para esta rota"
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MetaCadastroViewModel", "Erro ao carregar ciclos: ${e.message}", e)
                _message.value = "Erro ao carregar ciclos: ${e.message}"
            }
        }
    }

    /**
     * Salva uma nova meta
     */
    fun salvarMeta(meta: MetaColaborador) {
        viewModelScope.launch {
            try {
                android.util.Log.d("MetaCadastroViewModel", "Salvando meta: ${meta.tipoMeta} para rota ${meta.rotaId ?: "todas"}, ciclo ${meta.cicloId}")
                
                // Buscar colaborador responsável pela rota
                val colaboradorResponsavel = appRepository.buscarColaboradorResponsavelPrincipal(meta.rotaId ?: 0L)
                
                if (colaboradorResponsavel == null) {
                    _message.value = "Nenhum colaborador responsável encontrado para esta rota. Configure um responsável primeiro."
                    return@launch
                }
                
                // Definir o colaborador na meta
                val metaComColaborador = meta.copy(colaboradorId = colaboradorResponsavel.id)
                
                // Salvar a meta
                val metaId = appRepository.inserirMeta(metaComColaborador)
                
                android.util.Log.d("MetaCadastroViewModel", "Meta salva com ID: $metaId")
                _metaSalva.value = true
                
            } catch (e: Exception) {
                android.util.Log.e("MetaCadastroViewModel", "Erro ao salvar meta: ${e.message}", e)
                _message.value = "Erro ao salvar meta: ${e.message}"
            }
        }
    }

    /**
     * Limpa a mensagem
     */
    fun limparMensagem() {
        _message.value = null
    }

    /**
     * Reseta o estado de meta salva
     */
    fun resetarMetaSalva() {
        _metaSalva.value = false
    }
}
