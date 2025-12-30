package com.example.gestaobilhares.ui.contracts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.AditivoContrato
import com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.launch
import java.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AditivoSignatureViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
    
    // ✅ CORREÇÃO: Repository já injetado via construtor
    // private lateinit var repository: AppRepository
    
    // Assinatura pendente para evitar segundo clique caso o aditivo ainda não exista
    private var pendingSignatureBase64: String? = null
    
    /**
     * ✅ CORREÇÃO: Inicializar repository (Mantido para compatibilidade, mas vazio)
     */
    fun initializeRepository(repository: AppRepository) {
        // this.repository = repository // Já injetado
    }
    
    fun isRepositoryInitialized(): Boolean = true

    // Tipo do aditivo: INCLUSAO (default) ou RETIRADA
    private var aditivoTipo: String = "INCLUSAO"

    fun setAditivoTipo(tipo: String) {
        aditivoTipo = tipo
    }

    private val _aditivo = MutableLiveData<AditivoContrato?>()
    val aditivo: LiveData<AditivoContrato?> = _aditivo
    
    private val _contrato = MutableLiveData<ContratoLocacao?>()
    val contrato: LiveData<ContratoLocacao?> = _contrato
    
    private val _mesas = MutableLiveData<List<Mesa>>()
    val mesas: LiveData<List<Mesa>> = _mesas
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun carregarDados(contratoId: Long, mesasIds: LongArray) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Carregar contrato
                val contrato = repository.buscarContratoPorId(contratoId)
                _contrato.value = contrato
                
                // Carregar mesas
                val mesas = mutableListOf<Mesa>()
                for (mesaId in mesasIds) {
                    val mesa = repository.obterMesaPorId(mesaId)
                    if (mesa != null) {
                        mesas.add(mesa)
                    }
                }
                _mesas.value = mesas
                
            } catch (e: Exception) {
                _error.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun gerarAditivo(observacoes: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val contrato = _contrato.value ?: throw Exception("Contrato não encontrado")
                val mesas = _mesas.value ?: throw Exception("Mesas não encontradas")
                
                if (mesas.isEmpty()) {
                    throw Exception("Nenhuma mesa selecionada")
                }
                
                // Gerar número do aditivo
                val numeroAditivo = gerarNumeroAditivo()
                
                // Criar aditivo
                val aditivo = AditivoContrato(
                    numeroAditivo = numeroAditivo,
                    contratoId = contrato.id,
                    dataAditivo = System.currentTimeMillis(),
                    observacoes = observacoes,
                    tipo = aditivoTipo
                )
                
                // Salvar aditivo
                val aditivoId = repository.inserirAditivo(aditivo)
                
                // Vincular mesas ao aditivo
                val aditivoMesas = mesas.map { mesa ->
                    com.example.gestaobilhares.data.entities.AditivoMesa(
                        aditivoId = aditivoId,
                        mesaId = mesa.id,
                        tipoEquipamento = mesa.tipoMesa.name,
                        numeroSerie = mesa.numero.toString(),
                        valorFicha = 0.0, // Mesa não tem valorFicha, usar 0.0
                        valorFixo = mesa.valorFixo
                    )
                }
                
                repository.inserirAditivoMesas(aditivoMesas)
                
                // Atualizar mesas conforme o tipo do aditivo
                if (aditivoTipo.equals("RETIRADA", ignoreCase = true)) {
                    // Retirada: remover vínculo do cliente e manter mesa disponível
                    mesas.forEach { mesa ->
                        repository.retirarMesa(mesa.id)
                    }
                } else {
                    // Inclusão: vincular mesas ao cliente do contrato e atualizar timestamps
                    mesas.forEach { mesa ->
                        val agora = System.currentTimeMillis()
                        val mesaAtualizada = mesa.copy(
                            clienteId = contrato.clienteId,
                            dataInstalacao = agora,
                            dataUltimaLeitura = agora
                        )
                        repository.atualizarMesa(mesaAtualizada)
                    }
                }
                
                val aditivoCriado = aditivo.copy(id = aditivoId)
                _aditivo.value = aditivoCriado

                // Se houver assinatura pendente (usuário confirmou antes do aditivo existir), aplicar agora
                pendingSignatureBase64?.let { assinatura ->
                    val aditivoAssinado = aditivoCriado.copy(
                        assinaturaLocatario = assinatura,
                        dataAtualizacao = System.currentTimeMillis()
                    )
                    repository.atualizarAditivo(aditivoAssinado)
                    _aditivo.value = aditivoAssinado
                    pendingSignatureBase64 = null
                }
                
            } catch (e: Exception) {
                _error.value = "Erro ao gerar aditivo: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun adicionarAssinaturaLocatario(assinaturaBase64: String) {
        viewModelScope.launch {
            try {
                val aditivo = _aditivo.value
                if (aditivo == null) {
                    // Guardar assinatura para aplicar assim que o aditivo for criado
                    pendingSignatureBase64 = assinaturaBase64
                    return@launch
                }
                val aditivoAtualizado = aditivo.copy(
                    assinaturaLocatario = assinaturaBase64,
                    dataAtualizacao = System.currentTimeMillis()
                )
                
                repository.atualizarAditivo(aditivoAtualizado)
                _aditivo.value = aditivoAtualizado
                
            } catch (e: Exception) {
                _error.value = "Erro ao salvar assinatura: ${e.message}"
            }
        }
    }
    
    private suspend fun gerarNumeroAditivo(): String {
        val ano = Calendar.getInstance().get(Calendar.YEAR)
        val count = repository.contarAditivosPorAno(ano.toString())
        val numero = count + 1
        return "ADT-${String.format("%03d", numero)}/$ano"
    }
    
    /**
     * ✅ NOVO: Obter assinatura do representante legal ativa
     * Retorna a assinatura Base64 do representante legal para uso automático em aditivos
     */
    suspend fun obterAssinaturaRepresentanteLegalAtiva(): AssinaturaRepresentanteLegal? {
        return try {
            repository.obterAssinaturaRepresentanteLegalAtiva()
        } catch (e: Exception) {
            android.util.Log.e("AditivoSignatureViewModel", "Erro ao obter assinatura do representante: ${e.message}")
            null
        }
    }
}

