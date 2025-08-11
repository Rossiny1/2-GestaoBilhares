package com.example.gestaobilhares.ui.clients

/**
 * Tipos de pesquisa disponíveis para clientes
 */
enum class SearchType(val displayName: String) {
    NOME_CLIENTE("Nome do Cliente"),
    NUMERO_MESA("Número da Mesa"),
    CIDADE("Cidade"),
    CPF("CPF/CNPJ")
}
