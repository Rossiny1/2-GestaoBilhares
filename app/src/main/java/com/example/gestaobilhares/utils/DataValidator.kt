package com.example.gestaobilhares.utils

import android.text.TextUtils
import java.util.regex.Pattern

/**
 * ✅ FASE 2: VALIDADOR DE DADOS CENTRALIZADO
 * 
 * Centraliza todas as validações de dados do sistema para:
 * - Eliminar duplicação de código (~200 linhas)
 * - Garantir consistência de validações
 * - Facilitar manutenção e testes
 * - Padronizar mensagens de erro
 */
object DataValidator {
    
    // ==================== VALIDAÇÕES BÁSICAS ====================
    
    /**
     * Valida se um campo obrigatório não está vazio
     */
    fun validarCampoObrigatorio(
        valor: String?,
        nomeCampo: String
    ): ResultadoValidacao {
        return when {
            valor.isNullOrBlank() -> ResultadoValidacao.Erro(listOf("$nomeCampo é obrigatório"))
            else -> ResultadoValidacao.Sucesso
        }
    }
    
    /**
     * Valida tamanho mínimo de texto
     */
    fun validarTamanhoMinimo(
        valor: String,
        tamanhoMinimo: Int,
        nomeCampo: String
    ): ResultadoValidacao {
        return when {
            valor.length < tamanhoMinimo -> ResultadoValidacao.Erro(listOf("$nomeCampo deve ter pelo menos $tamanhoMinimo caracteres"))
            else -> ResultadoValidacao.Sucesso
        }
    }
    
    /**
     * Valida tamanho máximo de texto
     */
    fun validarTamanhoMaximo(
        valor: String,
        tamanhoMaximo: Int,
        nomeCampo: String
    ): ResultadoValidacao {
        return when {
            valor.length > tamanhoMaximo -> ResultadoValidacao.Erro(listOf("$nomeCampo deve ter no máximo $tamanhoMaximo caracteres"))
            else -> ResultadoValidacao.Sucesso
        }
    }
    
    // ==================== VALIDAÇÕES DE EMAIL ====================
    
    /**
     * Valida formato de email
     */
    fun validarEmail(email: String?): ResultadoValidacao {
        if (email.isNullOrBlank()) {
            return ResultadoValidacao.Sucesso // Email é opcional
        }
        
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
        )
        
        return if (emailPattern.matcher(email).matches()) {
            ResultadoValidacao.Sucesso
        } else {
            ResultadoValidacao.Erro(listOf("E-mail inválido"))
        }
    }
    
    // ==================== VALIDAÇÕES DE TELEFONE ====================
    
    /**
     * Valida formato de telefone brasileiro
     */
    fun validarTelefone(telefone: String?): ResultadoValidacao {
        if (telefone.isNullOrBlank()) {
            return ResultadoValidacao.Sucesso // Telefone é opcional
        }
        
        // Remove caracteres não numéricos
        val telefoneLimpo = telefone.replace(Regex("[^0-9]"), "")
        
        return when {
            telefoneLimpo.length < 10 -> ResultadoValidacao.Erro(listOf("Telefone deve ter pelo menos 10 dígitos"))
            telefoneLimpo.length > 11 -> ResultadoValidacao.Erro(listOf("Telefone deve ter no máximo 11 dígitos"))
            else -> ResultadoValidacao.Sucesso
        }
    }
    
    // ==================== VALIDAÇÕES NUMÉRICAS ====================
    
    /**
     * Valida se um valor numérico é positivo
     */
    fun validarValorPositivo(
        valor: Double,
        nomeCampo: String
    ): ResultadoValidacao {
        return when {
            valor <= 0 -> ResultadoValidacao.Erro(listOf("$nomeCampo deve ser maior que zero"))
            else -> ResultadoValidacao.Sucesso
        }
    }
    
    /**
     * Valida se um valor numérico não é negativo
     */
    fun validarValorNaoNegativo(
        valor: Double,
        nomeCampo: String
    ): ResultadoValidacao {
        return when {
            valor < 0 -> ResultadoValidacao.Erro(listOf("$nomeCampo não pode ser negativo"))
            else -> ResultadoValidacao.Sucesso
        }
    }
    
    /**
     * Valida se um valor está dentro de um range
     */
    fun validarRange(
        valor: Double,
        valorMinimo: Double,
        valorMaximo: Double,
        nomeCampo: String
    ): ResultadoValidacao {
        return when {
            valor < valorMinimo -> ResultadoValidacao.Erro(listOf("$nomeCampo deve ser maior ou igual a $valorMinimo"))
            valor > valorMaximo -> ResultadoValidacao.Erro(listOf("$nomeCampo deve ser menor ou igual a $valorMaximo"))
            else -> ResultadoValidacao.Sucesso
        }
    }
    
    // ==================== VALIDAÇÕES DE CPF/CNPJ ====================
    
    /**
     * Valida CPF
     */
    fun validarCPF(cpf: String?): ResultadoValidacao {
        if (cpf.isNullOrBlank()) {
            return ResultadoValidacao.Sucesso // CPF é opcional
        }
        
        val cpfLimpo = cpf.replace(Regex("[^0-9]"), "")
        
        return when {
            cpfLimpo.length != 11 -> ResultadoValidacao.Erro(listOf("CPF deve ter 11 dígitos"))
            cpfLimpo.all { it == cpfLimpo[0] } -> ResultadoValidacao.Erro(listOf("CPF inválido"))
            !isValidCPF(cpfLimpo) -> ResultadoValidacao.Erro(listOf("CPF inválido"))
            else -> ResultadoValidacao.Sucesso
        }
    }
    
    /**
     * Valida CNPJ
     */
    fun validarCNPJ(cnpj: String?): ResultadoValidacao {
        if (cnpj.isNullOrBlank()) {
            return ResultadoValidacao.Sucesso // CNPJ é opcional
        }
        
        val cnpjLimpo = cnpj.replace(Regex("[^0-9]"), "")
        
        return when {
            cnpjLimpo.length != 14 -> ResultadoValidacao.Erro(listOf("CNPJ deve ter 14 dígitos"))
            cnpjLimpo.all { it == cnpjLimpo[0] } -> ResultadoValidacao.Erro(listOf("CNPJ inválido"))
            !isValidCNPJ(cnpjLimpo) -> ResultadoValidacao.Erro(listOf("CNPJ inválido"))
            else -> ResultadoValidacao.Sucesso
        }
    }
    
    // ==================== VALIDAÇÕES DE MESA ====================
    
    /**
     * Valida dados de mesa para acerto
     */
    fun validarDadosMesa(
        relogioInicial: Int,
        relogioFinal: Int,
        comDefeito: Boolean,
        relogioReiniciou: Boolean,
        fotoRelogioFinal: String?
    ): ResultadoValidacao {
        val erros = mutableListOf<String>()
        
        // Validação básica de relógio
        if (relogioFinal < relogioInicial && !comDefeito && !relogioReiniciou) {
            erros.add("Relógio final deve ser maior que o inicial")
        }
        
        // Validação de foto obrigatória
        if ((comDefeito || relogioReiniciou) && fotoRelogioFinal.isNullOrBlank()) {
            erros.add("Foto é obrigatória quando há defeito ou reinicialização")
        }
        
        return if (erros.isEmpty()) {
            ResultadoValidacao.Sucesso
        } else {
            ResultadoValidacao.Erro(erros)
        }
    }
    
    // ==================== VALIDAÇÕES DE DESPESA ====================
    
    /**
     * Valida dados de despesa
     */
    fun validarDespesa(
        valor: Double,
        quantidade: Int,
        categoria: String?,
        tipo: String?,
        veiculoId: Long?
    ): ResultadoValidacao {
        val erros = mutableListOf<String>()
        
        // Validações básicas
        if (valor <= 0) {
            erros.add("Valor deve ser maior que zero")
        }
        
        if (quantidade <= 0) {
            erros.add("Quantidade deve ser maior que zero")
        }
        
        // Validações específicas para viagem
        val isViagem = categoria.equals("Viagem", ignoreCase = true)
        if (isViagem && veiculoId == null) {
            erros.add("Selecione um veículo para despesa de viagem")
        }
        
        return if (erros.isEmpty()) {
            ResultadoValidacao.Sucesso
        } else {
            ResultadoValidacao.Erro(erros)
        }
    }
    
    // ==================== VALIDAÇÕES COMPOSTAS ====================
    
    /**
     * Valida dados completos de cliente
     */
    fun validarCliente(
        nome: String,
        endereco: String,
        comissaoFicha: Double,
        email: String? = null,
        telefone: String? = null,
        cpf: String? = null
    ): ResultadoValidacao {
        val erros = mutableListOf<String>()
        
        // Validações obrigatórias
        val validacaoNome = validarCampoObrigatorio(nome, "Nome")
        if (validacaoNome.isErro()) {
            erros.addAll((validacaoNome as ResultadoValidacao.Erro).mensagens)
        }
        
        val validacaoEndereco = validarCampoObrigatorio(endereco, "Endereço")
        if (validacaoEndereco.isErro()) {
            erros.addAll((validacaoEndereco as ResultadoValidacao.Erro).mensagens)
        }
        
        val validacaoTamanhoNome = validarTamanhoMinimo(nome, 3, "Nome")
        if (validacaoTamanhoNome.isErro()) {
            erros.addAll((validacaoTamanhoNome as ResultadoValidacao.Erro).mensagens)
        }
        
        val validacaoTamanhoEndereco = validarTamanhoMinimo(endereco, 10, "Endereço")
        if (validacaoTamanhoEndereco.isErro()) {
            erros.addAll((validacaoTamanhoEndereco as ResultadoValidacao.Erro).mensagens)
        }
        
        val validacaoComissao = validarValorPositivo(comissaoFicha, "Comissão por ficha")
        if (validacaoComissao.isErro()) {
            erros.addAll((validacaoComissao as ResultadoValidacao.Erro).mensagens)
        }
        
        // Validações opcionais
        val validacaoEmail = validarEmail(email)
        if (validacaoEmail.isErro()) {
            erros.addAll((validacaoEmail as ResultadoValidacao.Erro).mensagens)
        }
        
        val validacaoTelefone = validarTelefone(telefone)
        if (validacaoTelefone.isErro()) {
            erros.addAll((validacaoTelefone as ResultadoValidacao.Erro).mensagens)
        }
        
        val validacaoCPF = validarCPF(cpf)
        if (validacaoCPF.isErro()) {
            erros.addAll((validacaoCPF as ResultadoValidacao.Erro).mensagens)
        }
        
        return if (erros.isEmpty()) {
            ResultadoValidacao.Sucesso
        } else {
            ResultadoValidacao.Erro(erros)
        }
    }
    
    // ==================== FUNÇÕES AUXILIARES ====================
    
    /**
     * Valida CPF usando algoritmo oficial
     */
    private fun isValidCPF(cpf: String): Boolean {
        if (cpf.length != 11) return false
        
        // Verifica se todos os dígitos são iguais
        if (cpf.all { it == cpf[0] }) return false
        
        // Calcula primeiro dígito verificador
        var soma = 0
        for (i in 0..8) {
            soma += cpf[i].digitToInt() * (10 - i)
        }
        val primeiroDigito = if (soma % 11 < 2) 0 else 11 - (soma % 11)
        
        // Calcula segundo dígito verificador
        soma = 0
        for (i in 0..9) {
            soma += cpf[i].digitToInt() * (11 - i)
        }
        val segundoDigito = if (soma % 11 < 2) 0 else 11 - (soma % 11)
        
        return cpf[9].digitToInt() == primeiroDigito && cpf[10].digitToInt() == segundoDigito
    }
    
    /**
     * Valida CNPJ usando algoritmo oficial
     */
    private fun isValidCNPJ(cnpj: String): Boolean {
        if (cnpj.length != 14) return false
        
        // Verifica se todos os dígitos são iguais
        if (cnpj.all { it == cnpj[0] }) return false
        
        // Calcula primeiro dígito verificador
        var soma = 0
        val pesos1 = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        for (i in 0..11) {
            soma += cnpj[i].digitToInt() * pesos1[i]
        }
        val primeiroDigito = if (soma % 11 < 2) 0 else 11 - (soma % 11)
        
        // Calcula segundo dígito verificador
        soma = 0
        val pesos2 = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        for (i in 0..12) {
            soma += cnpj[i].digitToInt() * pesos2[i]
        }
        val segundoDigito = if (soma % 11 < 2) 0 else 11 - (soma % 11)
        
        return cnpj[12].digitToInt() == primeiroDigito && cnpj[13].digitToInt() == segundoDigito
    }
    
    // ==================== CLASSES DE DADOS ====================
    
    /**
     * Resultado de validação
     */
    sealed class ResultadoValidacao {
        object Sucesso : ResultadoValidacao()
        data class Erro(val mensagens: List<String>) : ResultadoValidacao()
        
        fun isSucesso(): Boolean = this is Sucesso
        fun isErro(): Boolean = this is Erro
    }
}
