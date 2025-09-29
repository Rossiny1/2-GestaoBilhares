package com.example.gestaobilhares.utils

import android.content.Context
import android.util.Log
import android.content.SharedPreferences
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.dao.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.util.*

/**
 * Classe utilitária para popular o banco de dados com dados de teste
 * ✅ NOVO: Sistema de população de dados para testes
 */
class DatabasePopulator(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("db_populator", Context.MODE_PRIVATE)
    private val SEED_FLAG_V1 = "seed_v1_completed"

    /**
     * Popula o banco de dados com dados de teste completos
     */
    suspend fun popularBancoCompleto() = withContext(Dispatchers.IO) {
        try {
            Log.d("DatabasePopulator", "🚀 Iniciando população do banco de dados...")

            // Impedir duplicações: se já populado uma vez, ou já existem rotas, abortar
            val jaExecutado = prefs.getBoolean(SEED_FLAG_V1, false)
            val rotasExistentes = try { database.rotaDao().contarRotasAtivas() } catch (_: Exception) { 0 }
            if (jaExecutado || rotasExistentes > 0) {
                Log.d("DatabasePopulator", "População ignorada: já executada anteriormente ou dados existentes detectados (rotas=$rotasExistentes)")
                return@withContext
            }

            // 1. Limpar dados existentes (opcional - comentado para preservar dados)
            // limparDadosExistentes()

            // 2. Inserir colaboradores
            Log.d("DatabasePopulator", "📝 Inserindo colaboradores...")
            val colaboradores = inserirColaboradores()
            Log.d("DatabasePopulator", "✅ Colaboradores inseridos: ${colaboradores.size}")

            // 3. Inserir rotas
            Log.d("DatabasePopulator", "📝 Inserindo rotas...")
            val rotas = inserirRotas(colaboradores)
            Log.d("DatabasePopulator", "✅ Rotas inseridas: ${rotas.size}")

            // 4. Inserir clientes
            Log.d("DatabasePopulator", "📝 Inserindo clientes...")
            val clientes = inserirClientes(rotas)
            Log.d("DatabasePopulator", "✅ Clientes inseridos: ${clientes.size}")

            // 5. Inserir mesas
            Log.d("DatabasePopulator", "📝 Inserindo mesas...")
            val mesas = inserirMesas(clientes)
            Log.d("DatabasePopulator", "✅ Mesas inseridas: ${mesas.size}")

            // 6. Inserir mesas no depósito
            Log.d("DatabasePopulator", "📝 Inserindo mesas no depósito...")
            val mesasDeposito = inserirMesasDeposito()
            Log.d("DatabasePopulator", "✅ Mesas no depósito: ${mesasDeposito.size}")

            // 7. Inserir categorias e tipos de despesas
            Log.d("DatabasePopulator", "📝 Inserindo categorias e tipos de despesas...")
            inserirCategoriasETiposDespesas()
            Log.d("DatabasePopulator", "✅ Categorias e tipos de despesas inseridos")

            // 8. Inserir ciclos de acerto
            Log.d("DatabasePopulator", "📝 Inserindo ciclos de acerto...")
            val ciclos = inserirCiclosAcerto(rotas)
            Log.d("DatabasePopulator", "✅ Ciclos de acerto inseridos: ${ciclos.size}")

            // 9. Inserir acertos
            Log.d("DatabasePopulator", "📝 Inserindo acertos...")
            inserirAcertos(clientes, mesas, ciclos)
            Log.d("DatabasePopulator", "✅ Acertos inseridos")

            // 10. Inserir despesas
            Log.d("DatabasePopulator", "📝 Inserindo despesas...")
            inserirDespesas(ciclos)
            Log.d("DatabasePopulator", "✅ Despesas inseridas")

            Log.d("DatabasePopulator", "🎉 População do banco concluída com sucesso!")
            prefs.edit().putBoolean(SEED_FLAG_V1, true).apply()
            
        } catch (e: Exception) {
            Log.e("DatabasePopulator", "❌ Erro ao popular banco: ${e.message}", e)
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Insere colaboradores de teste
     */
    private suspend fun inserirColaboradores(): List<Colaborador> {
        Log.d("DatabasePopulator", "📝 Criando lista de colaboradores...")
        val colaboradores = listOf(
            Colaborador(
                nome = "João Silva",
                email = "joao.silva@bilhares.com",
                telefone = "(11) 99999-1111",
                cpf = "123.456.789-00",
                endereco = "Rua das Flores, 123",
                bairro = "Centro",
                cidade = "São Paulo",
                estado = "SP",
                cep = "01234-567",
                nivelAcesso = NivelAcesso.ADMIN,
                ativo = true,
                aprovado = true,
                dataAprovacao = Date(),
                aprovadoPor = "Sistema",
                observacoes = "Administrador principal do sistema"
            ),
            Colaborador(
                nome = "Maria Santos",
                email = "maria.santos@bilhares.com",
                telefone = "(11) 99999-2222",
                cpf = "987.654.321-00",
                endereco = "Av. Paulista, 456",
                bairro = "Bela Vista",
                cidade = "São Paulo",
                estado = "SP",
                cep = "01310-100",
                nivelAcesso = NivelAcesso.USER,
                ativo = true,
                aprovado = true,
                dataAprovacao = Date(),
                aprovadoPor = "João Silva",
                observacoes = "Colaboradora responsável pela rota Centro"
            )
        )

        Log.d("DatabasePopulator", "📝 Inserindo ${colaboradores.size} colaboradores no banco...")
        colaboradores.forEachIndexed { index, colaborador ->
            try {
                val id = database.colaboradorDao().inserir(colaborador)
                Log.d("DatabasePopulator", "✅ Colaborador ${index + 1} inserido com ID: $id")
            } catch (e: Exception) {
                Log.e("DatabasePopulator", "❌ Erro ao inserir colaborador ${index + 1}: ${e.message}", e)
            }
        }

        return colaboradores
    }

    /**
     * Insere rotas de teste
     */
    private suspend fun inserirRotas(colaboradores: List<Colaborador>): List<Rota> {
        val rotasBase = listOf(
            Rota(
                nome = "Centro",
                descricao = "Rota do centro da cidade - bares e restaurantes",
                colaboradorResponsavel = colaboradores[1].nome,
                cidades = "São Paulo - Centro",
                ativa = true,
                cor = "#FF5722",
                statusAtual = StatusRota.EM_ANDAMENTO,
                cicloAcertoAtual = 3,
                dataInicioCiclo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 7 dias atrás
            ),
            Rota(
                nome = "Zona Sul",
                descricao = "Rota da zona sul - estabelecimentos residenciais",
                colaboradorResponsavel = colaboradores[0].nome,
                cidades = "São Paulo - Zona Sul",
                ativa = true,
                cor = "#4CAF50",
                statusAtual = StatusRota.FINALIZADA,
                cicloAcertoAtual = 2,
                dataInicioCiclo = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000L), // 14 dias atrás
                dataFimCiclo = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000L) // 1 dia atrás
            ),
            Rota(
                nome = "Zona Norte",
                descricao = "Rota da zona norte - bares e lanchonetes",
                colaboradorResponsavel = colaboradores[1].nome,
                cidades = "São Paulo - Zona Norte",
                ativa = true,
                cor = "#2196F3",
                statusAtual = StatusRota.PAUSADA,
                cicloAcertoAtual = 1
            )
        )

        Log.d("DatabasePopulator", "📝 Inserindo ${rotasBase.size} rotas no banco...")
        val rotasComId = mutableListOf<Rota>()
        rotasBase.forEachIndexed { index, rota ->
            try {
                // Evitar duplicação por nome (idempotente)
                val existente = database.rotaDao().getRotaByNome(rota.nome)
                if (existente != null) {
                    rotasComId.add(existente)
                    Log.d("DatabasePopulator", "↩︎ Rota já existia: ${rota.nome} (ID: ${existente.id})")
                } else {
                    val id = database.rotaDao().insertRota(rota)
                    val rotaComId = rota.copy(id = id)
                    rotasComId.add(rotaComId)
                    Log.d("DatabasePopulator", "✅ Rota ${index + 1} inserida com ID: $id")
                }
            } catch (e: Exception) {
                Log.e("DatabasePopulator", "❌ Erro ao inserir rota ${index + 1}: ${e.message}", e)
            }
        }

        return rotasComId
    }

    /**
     * Insere clientes de teste
     */
    private suspend fun inserirClientes(rotas: List<Rota>): List<Cliente> {
        val clientesBase = listOf(
            // Clientes da Rota Centro
            Cliente(
                nome = "Bar do João",
                nomeFantasia = "Bar do João",
                cpfCnpj = "12.345.678/0001-90",
                telefone = "(11) 3333-1111",
                telefone2 = "(11) 99999-1111",
                email = "contato@bardojoao.com",
                endereco = "Rua Augusta, 100",
                bairro = "Consolação",
                cidade = "São Paulo",
                estado = "SP",
                cep = "01305-000",
                rotaId = rotas[0].id,
                ativo = true,
                observacoes = "Cliente antigo, sempre pontual no pagamento"
            ),
            Cliente(
                nome = "Restaurante Sabor & Arte",
                nomeFantasia = "Sabor & Arte",
                cpfCnpj = "98.765.432/0001-10",
                telefone = "(11) 3333-2222",
                email = "gerencia@saborearte.com",
                endereco = "Av. Paulista, 200",
                bairro = "Bela Vista",
                cidade = "São Paulo",
                estado = "SP",
                cep = "01310-100",
                rotaId = rotas[0].id,
                ativo = true,
                observacoes = "Restaurante de alto padrão, mesas premium"
            ),
            Cliente(
                nome = "Lanchonete do Bairro",
                nomeFantasia = "Lanchonete do Bairro",
                cpfCnpj = "11.222.333/0001-44",
                telefone = "(11) 3333-3333",
                endereco = "Rua da Consolação, 300",
                bairro = "Consolação",
                cidade = "São Paulo",
                estado = "SP",
                cep = "01302-000",
                rotaId = rotas[0].id,
                ativo = true,
                observacoes = "Cliente novo, ainda em período de teste"
            ),

            // Clientes da Rota Zona Sul
            Cliente(
                nome = "Bar da Esquina",
                nomeFantasia = "Bar da Esquina",
                cpfCnpj = "55.666.777/0001-88",
                telefone = "(11) 4444-1111",
                endereco = "Rua das Palmeiras, 400",
                bairro = "Vila Madalena",
                cidade = "São Paulo",
                estado = "SP",
                cep = "05433-000",
                rotaId = rotas[1].id,
                ativo = true,
                observacoes = "Bar tradicional, clientela fiel"
            ),
            Cliente(
                nome = "Café Central",
                nomeFantasia = "Café Central",
                cpfCnpj = "99.888.777/0001-66",
                telefone = "(11) 4444-2222",
                endereco = "Av. Faria Lima, 500",
                bairro = "Itaim Bibi",
                cidade = "São Paulo",
                estado = "SP",
                cep = "04538-132",
                rotaId = rotas[1].id,
                ativo = true,
                observacoes = "Café moderno, ambiente corporativo"
            ),

            // Clientes da Rota Zona Norte
            Cliente(
                nome = "Boteco do Zé",
                nomeFantasia = "Boteco do Zé",
                cpfCnpj = "33.444.555/0001-77",
                telefone = "(11) 5555-1111",
                endereco = "Rua do Comércio, 600",
                bairro = "Santana",
                cidade = "São Paulo",
                estado = "SP",
                cep = "02013-000",
                rotaId = rotas[2].id,
                ativo = true,
                observacoes = "Boteco tradicional, preços populares"
            )
        )

        Log.d("DatabasePopulator", "📝 Inserindo ${clientesBase.size} clientes no banco (idempotente)...")
        val clientesComId = mutableListOf<Cliente>()
        clientesBase.forEachIndexed { index, cliente ->
            try {
                // idempotência por nome+rota
                val lista = database.clienteDao().obterTodos().first()
                val existente = lista.find { it.nome.equals(cliente.nome, ignoreCase = true) && it.rotaId == cliente.rotaId }
                if (existente != null) {
                    clientesComId.add(existente)
                    Log.d("DatabasePopulator", "↩︎ Cliente já existia: ${cliente.nome} (ID: ${existente.id})")
                } else {
                    val id = database.clienteDao().inserir(cliente)
                    val clienteComId = cliente.copy(id = id)
                    clientesComId.add(clienteComId)
                    Log.d("DatabasePopulator", "✅ Cliente ${index + 1} inserido com ID: $id")
                }
            } catch (e: Exception) {
                Log.e("DatabasePopulator", "❌ Erro ao inserir cliente ${index + 1}: ${e.message}", e)
            }
        }

        return clientesComId
    }

    /**
     * Insere mesas de teste
     */
    private suspend fun inserirMesas(clientes: List<Cliente>): List<Mesa> {
        val mesasBase = mutableListOf<Mesa>()

        // Mesas para Bar do João (2 mesas)
        mesasBase.addAll(listOf(
            Mesa(
                numero = "101",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = clientes[0].id,
                fichasInicial = 0,
                fichasFinal = 1250,
                valorFixo = 2.50,
                ativa = true,
                observacoes = "Mesa principal do bar"
            ),
            Mesa(
                numero = "102",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = clientes[0].id,
                fichasInicial = 0,
                fichasFinal = 980,
                valorFixo = 2.50,
                ativa = true,
                observacoes = "Mesa secundária"
            )
        ))

        // Mesas para Restaurante Sabor & Arte (3 mesas)
        mesasBase.addAll(listOf(
            Mesa(
                numero = "201",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = clientes[1].id,
                fichasInicial = 0,
                fichasFinal = 2100,
                valorFixo = 3.00,
                ativa = true,
                observacoes = "Mesa premium"
            ),
            Mesa(
                numero = "202",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = clientes[1].id,
                fichasInicial = 0,
                fichasFinal = 1750,
                valorFixo = 3.00,
                ativa = true,
                observacoes = "Mesa padrão"
            ),
            Mesa(
                numero = "203",
                tipoMesa = TipoMesa.PEMBOLIM,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = clientes[1].id,
                fichasInicial = 0,
                fichasFinal = 890,
                valorFixo = 1.50,
                ativa = true,
                observacoes = "Pembolim para diversão"
            )
        ))

        // Mesas para Lanchonete do Bairro (1 mesa)
        mesasBase.add(
            Mesa(
                numero = "301",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = clientes[2].id,
                fichasInicial = 0,
                fichasFinal = 450,
                valorFixo = 2.00,
                ativa = true,
                observacoes = "Mesa única da lanchonete"
            )
        )

        // Mesas para Bar da Esquina (2 mesas)
        mesasBase.addAll(listOf(
            Mesa(
                numero = "401",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = clientes[3].id,
                fichasInicial = 0,
                fichasFinal = 1680,
                valorFixo = 2.25,
                ativa = true,
                observacoes = "Mesa principal"
            ),
            Mesa(
                numero = "402",
                tipoMesa = TipoMesa.JUKEBOX,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = clientes[3].id,
                fichasInicial = 0,
                fichasFinal = 320,
                valorFixo = 1.00,
                ativa = true,
                observacoes = "Jukebox para música"
            )
        ))

        // Mesas para Café Central (1 mesa)
        mesasBase.add(
            Mesa(
                numero = "501",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = clientes[4].id,
                fichasInicial = 0,
                fichasFinal = 750,
                valorFixo = 2.75,
                ativa = true,
                observacoes = "Mesa do café"
            )
        )

        // Mesas para Boteco do Zé (2 mesas)
        mesasBase.addAll(listOf(
            Mesa(
                numero = "601",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = clientes[5].id,
                fichasInicial = 0,
                fichasFinal = 1100,
                valorFixo = 1.75,
                ativa = true,
                observacoes = "Mesa popular"
            ),
            Mesa(
                numero = "602",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = clientes[5].id,
                fichasInicial = 0,
                fichasFinal = 920,
                valorFixo = 1.75,
                ativa = true,
                observacoes = "Mesa secundária"
            )
        ))

        Log.d("DatabasePopulator", "📝 Inserindo ${mesasBase.size} mesas no banco (idempotente)...")
        val mesasComId = mutableListOf<Mesa>()
        mesasBase.forEachIndexed { index, mesa ->
            try {
                // idempotência por numero+clienteId
                val existentesCliente = if (mesa.clienteId != null) database.mesaDao().obterMesasPorClienteDireto(mesa.clienteId) else emptyList()
                val existente = existentesCliente.firstOrNull { it.numero == mesa.numero }
                if (existente != null) {
                    mesasComId.add(existente)
                    Log.d("DatabasePopulator", "↩︎ Mesa já existia: ${mesa.numero} (ID: ${existente.id})")
                } else {
                    val id = database.mesaDao().inserir(mesa)
                    val mesaComId = mesa.copy(id = id)
                    mesasComId.add(mesaComId)
                    Log.d("DatabasePopulator", "✅ Mesa ${index + 1} inserida com ID: $id")
                }
            } catch (e: Exception) {
                Log.e("DatabasePopulator", "❌ Erro ao inserir mesa ${index + 1}: ${e.message}", e)
            }
        }

        return mesasComId
    }

    /**
     * Insere mesas no depósito
     */
    private suspend fun inserirMesasDeposito(): List<Mesa> {
        val mesasDepositoBase = listOf(
            Mesa(
                numero = "701",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = null, // Mesas no depósito não têm cliente
                fichasInicial = 0,
                fichasFinal = 0,
                valorFixo = 2.50,
                ativa = true,
                observacoes = "Mesa em manutenção"
            ),
            Mesa(
                numero = "702",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = null,
                fichasInicial = 0,
                fichasFinal = 0,
                valorFixo = 3.00,
                ativa = true,
                observacoes = "Mesa nova, aguardando instalação"
            ),
            Mesa(
                numero = "703",
                tipoMesa = TipoMesa.PEMBOLIM,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = null,
                fichasInicial = 0,
                fichasFinal = 0,
                valorFixo = 1.50,
                ativa = true,
                observacoes = "Pembolim reserva"
            ),
            Mesa(
                numero = "704",
                tipoMesa = TipoMesa.JUKEBOX,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = null,
                fichasInicial = 0,
                fichasFinal = 0,
                valorFixo = 1.00,
                ativa = true,
                observacoes = "Jukebox reserva"
            ),
            Mesa(
                numero = "705",
                tipoMesa = TipoMesa.SINUCA,
                tamanho = TamanhoMesa.GRANDE,
                clienteId = null,
                fichasInicial = 0,
                fichasFinal = 0,
                valorFixo = 2.25,
                ativa = true,
                observacoes = "Mesa para venda"
            )
        )

        val mesasDepositoComId = mutableListOf<Mesa>()
        mesasDepositoBase.forEach { mesa ->
            val id = database.mesaDao().inserir(mesa)
            mesasDepositoComId.add(mesa.copy(id = id))
        }

        return mesasDepositoComId
    }

    /**
     * Insere categorias e tipos de despesas
     */
    private suspend fun inserirCategoriasETiposDespesas() {
        // Categorias de despesas
        val categorias = listOf(
            CategoriaDespesa(nome = "Combustível", descricao = "Gastos com combustível para veículos"),
            CategoriaDespesa(nome = "Manutenção", descricao = "Manutenção de equipamentos e veículos"),
            CategoriaDespesa(nome = "Alimentação", descricao = "Gastos com alimentação durante o trabalho"),
            CategoriaDespesa(nome = "Comunicação", descricao = "Gastos com telefone e internet"),
            CategoriaDespesa(nome = "Outros", descricao = "Outras despesas diversas")
        )

        categorias.forEach { categoria ->
            // tenta evitar duplicação por nome
            val existente = try { database.categoriaDespesaDao().buscarPorNome(categoria.nome) } catch (_: Exception) { null }
            if (existente == null) database.categoriaDespesaDao().inserir(categoria)
        }

        // Tipos de despesas
        val tiposDespesa = listOf(
            TipoDespesa(nome = "Gasolina", categoriaId = 1, descricao = "Combustível para veículos"),
            TipoDespesa(nome = "Óleo do Motor", categoriaId = 2, descricao = "Troca de óleo"),
            TipoDespesa(nome = "Pneus", categoriaId = 2, descricao = "Troca de pneus"),
            TipoDespesa(nome = "Almoço", categoriaId = 3, descricao = "Refeições durante o trabalho"),
            TipoDespesa(nome = "Lanche", categoriaId = 3, descricao = "Lanches e bebidas"),
            TipoDespesa(nome = "Telefone", categoriaId = 4, descricao = "Recarga de celular"),
            TipoDespesa(nome = "Internet", categoriaId = 4, descricao = "Pacote de dados móveis"),
            TipoDespesa(nome = "Diversos", categoriaId = 5, descricao = "Outras despesas")
        )

        tiposDespesa.forEach { tipo ->
            val existente = try { database.tipoDespesaDao().buscarPorNome(tipo.nome) } catch (_: Exception) { null }
            if (existente == null) database.tipoDespesaDao().inserir(tipo)
        }
    }

    /**
     * Insere ciclos de acerto
     */
    private suspend fun inserirCiclosAcerto(rotas: List<Rota>): List<CicloAcertoEntity> {
        val ciclosBase = mutableListOf<CicloAcertoEntity>()

        // Ciclos para Rota Centro (3 ciclos)
        for (i in 1..3) {
            val dataInicio = System.currentTimeMillis() - ((30 * i) * 24 * 60 * 60 * 1000L)
            val dataFim = if (i < 3) dataInicio + (15 * 24 * 60 * 60 * 1000L) else dataInicio + (15 * 24 * 60 * 60 * 1000L)
            
            ciclosBase.add(
                CicloAcertoEntity(
                    rotaId = rotas[0].id,
                    numeroCiclo = i,
                    ano = 2025,
                    dataInicio = Date(dataInicio),
                    dataFim = Date(dataFim),
                    valorTotalAcertado = when (i) {
                        1 -> 2500.0
                        2 -> 3200.0
                        else -> 0.0 // Ciclo atual ainda não finalizado
                    },
                    status = if (i < 3) StatusCicloAcerto.FINALIZADO else StatusCicloAcerto.EM_ANDAMENTO
                )
            )
        }

        // Ciclos para Rota Zona Sul (2 ciclos finalizados)
        for (i in 1..2) {
            val dataInicio = System.currentTimeMillis() - ((45 * i) * 24 * 60 * 60 * 1000L)
            val dataFim = dataInicio + (15 * 24 * 60 * 60 * 1000L)
            
            ciclosBase.add(
                CicloAcertoEntity(
                    rotaId = rotas[1].id,
                    numeroCiclo = i,
                    ano = 2025,
                    dataInicio = Date(dataInicio),
                    dataFim = Date(dataFim),
                    valorTotalAcertado = when (i) {
                        1 -> 1800.0
                        2 -> 2200.0
                        else -> 0.0
                    },
                    status = StatusCicloAcerto.FINALIZADO
                )
            )
        }

        // Ciclo para Rota Zona Norte (1 ciclo pausado)
        ciclosBase.add(
            CicloAcertoEntity(
                rotaId = rotas[2].id,
                numeroCiclo = 1,
                ano = 2025,
                dataInicio = Date(System.currentTimeMillis() - (60 * 24 * 60 * 60 * 1000L)),
                dataFim = Date(System.currentTimeMillis() - (45 * 24 * 60 * 60 * 1000L)),
                valorTotalAcertado = 0.0,
                status = StatusCicloAcerto.CANCELADO
            )
        )

        val ciclosComId = mutableListOf<CicloAcertoEntity>()
        ciclosBase.forEachIndexed { index, ciclo ->
            val id = database.cicloAcertoDao().inserir(ciclo)
            ciclosComId.add(ciclo.copy(id = id))
            Log.d("DatabasePopulator", "✅ Ciclo ${index + 1} inserido com ID: $id (Rota=${ciclo.rotaId}, Nº=${ciclo.numeroCiclo})")
        }

        return ciclosComId
    }

    /**
     * Insere acertos de teste
     */
    private suspend fun inserirAcertos(clientes: List<Cliente>, mesas: List<Mesa>, ciclos: List<CicloAcertoEntity>) {
        // Descobrir ids reais das rotas
        val rotaCentroId = ciclos.firstOrNull { it.numeroCiclo == 1 }?.rotaId
        val rotaZonaSulId = ciclos.firstOrNull { it.rotaId != rotaCentroId }?.rotaId

        // Acertos para o 1º ciclo da Rota Centro (finalizado)
        val ciclo1Centro = ciclos.find { it.rotaId == rotaCentroId && it.numeroCiclo == 1 }
        ciclo1Centro?.let { ciclo ->
            // Acerto do Bar do João
            val acerto1 = Acerto(
                clienteId = clientes[0].id,
                cicloId = ciclo.id,
                rotaId = ciclo.rotaId,
                periodoInicio = Date(ciclo.dataInicio.time),
                periodoFim = Date(ciclo.dataFim.time),
                valorTotal = 125.0, // 50 fichas * R$ 2,50
                desconto = 5.0,
                valorComDesconto = 120.0,
                valorRecebido = 105.0,
                dataAcerto = Date(ciclo.dataInicio.time + (5 * 24 * 60 * 60 * 1000L)),
                observacoes = "Acerto realizado com desconto por pontualidade"
            )
            database.acertoDao().inserir(acerto1)

            // Acerto do Restaurante Sabor & Arte
            val acerto2 = Acerto(
                clienteId = clientes[1].id,
                cicloId = ciclo.id,
                rotaId = ciclo.rotaId,
                periodoInicio = Date(ciclo.dataInicio.time),
                periodoFim = Date(ciclo.dataFim.time),
                valorTotal = 315.0, // 105 fichas * R$ 3,00
                desconto = 0.0,
                valorComDesconto = 315.0,
                valorRecebido = 295.0,
                dataAcerto = Date(ciclo.dataInicio.time + (7 * 24 * 60 * 60 * 1000L)),
                observacoes = "Cliente premium, pagamento à vista"
            )
            database.acertoDao().inserir(acerto2)
        }

        // Acertos para o 2º ciclo da Rota Centro (finalizado)
        val ciclo2Centro = ciclos.find { it.rotaId == rotaCentroId && it.numeroCiclo == 2 }
        ciclo2Centro?.let { ciclo ->
            // Acerto do Bar do João
            val acerto3 = Acerto(
                clienteId = clientes[0].id,
                cicloId = ciclo.id,
                rotaId = ciclo.rotaId,
                periodoInicio = Date(ciclo.dataInicio.time),
                periodoFim = Date(ciclo.dataFim.time),
                valorTotal = 150.0,
                desconto = 10.0,
                valorComDesconto = 140.0,
                valorRecebido = 122.0,
                dataAcerto = Date(ciclo.dataInicio.time + (6 * 24 * 60 * 60 * 1000L)),
                observacoes = "Desconto por volume de fichas"
            )
            database.acertoDao().inserir(acerto3)

            // Acerto do Restaurante Sabor & Arte
            val acerto4 = Acerto(
                clienteId = clientes[1].id,
                cicloId = ciclo.id,
                rotaId = ciclo.rotaId,
                periodoInicio = Date(ciclo.dataInicio.time),
                periodoFim = Date(ciclo.dataFim.time),
                valorTotal = 360.0,
                desconto = 0.0,
                valorComDesconto = 360.0,
                valorRecebido = 335.0,
                dataAcerto = Date(ciclo.dataInicio.time + (8 * 24 * 60 * 60 * 1000L)),
                observacoes = "Acerto sem desconto"
            )
            database.acertoDao().inserir(acerto4)

            // Acerto da Lanchonete do Bairro
            val acerto5 = Acerto(
                clienteId = clientes[2].id,
                cicloId = ciclo.id,
                rotaId = ciclo.rotaId,
                periodoInicio = Date(ciclo.dataInicio.time),
                periodoFim = Date(ciclo.dataFim.time),
                valorTotal = 90.0,
                desconto = 5.0,
                valorComDesconto = 85.0,
                valorRecebido = 73.0,
                dataAcerto = Date(ciclo.dataInicio.time + (10 * 24 * 60 * 60 * 1000L)),
                observacoes = "Cliente novo, desconto de boas-vindas"
            )
            database.acertoDao().inserir(acerto5)
        }

        // Acertos para ciclos da Rota Zona Sul
        val ciclosZonaSul = ciclos.filter { it.rotaId == rotaZonaSulId }
        ciclosZonaSul.forEach { ciclo ->
            // Acerto do Bar da Esquina
            val acerto = Acerto(
                clienteId = clientes[3].id,
                cicloId = ciclo.id,
                rotaId = ciclo.rotaId,
                periodoInicio = Date(ciclo.dataInicio.time),
                periodoFim = Date(ciclo.dataFim.time),
                valorTotal = 200.0,
                desconto = 8.0,
                valorComDesconto = 192.0,
                valorRecebido = 172.0,
                dataAcerto = Date(ciclo.dataInicio.time + (7 * 24 * 60 * 60 * 1000L)),
                observacoes = "Acerto regular"
            )
            database.acertoDao().inserir(acerto)

            // Acerto do Café Central
            val acerto2 = Acerto(
                clienteId = clientes[4].id,
                cicloId = ciclo.id,
                rotaId = ciclo.rotaId,
                periodoInicio = Date(ciclo.dataInicio.time),
                periodoFim = Date(ciclo.dataFim.time),
                valorTotal = 165.0,
                desconto = 0.0,
                valorComDesconto = 165.0,
                valorRecebido = 150.0,
                dataAcerto = Date(ciclo.dataInicio.time + (9 * 24 * 60 * 60 * 1000L)),
                observacoes = "Cliente corporativo"
            )
            database.acertoDao().inserir(acerto2)
        }
    }

    /**
     * Insere despesas de teste
     */
    private suspend fun inserirDespesas(ciclos: List<CicloAcertoEntity>) {
        // Despesas para ciclos ativos
        val ciclosAtivos = ciclos.filter { it.status == StatusCicloAcerto.EM_ANDAMENTO }
        
        ciclosAtivos.forEach { ciclo ->
            // Despesas de combustível
            val despesa1 = Despesa(
                rotaId = ciclo.rotaId,
                cicloId = ciclo.id,
                descricao = "Abastecimento do veículo da rota",
                valor = 80.0,
                categoria = "Combustível",
                tipoDespesa = "Gasolina"
            )
            database.despesaDao().inserir(despesa1)

            // Despesas de alimentação
            val despesa2 = Despesa(
                rotaId = ciclo.rotaId,
                cicloId = ciclo.id,
                descricao = "Almoço durante visita aos clientes",
                valor = 25.0,
                categoria = "Alimentação",
                tipoDespesa = "Almoço"
            )
            database.despesaDao().inserir(despesa2)

            // Despesas de comunicação
            val despesa3 = Despesa(
                rotaId = ciclo.rotaId,
                cicloId = ciclo.id,
                descricao = "Pacote de dados móveis",
                valor = 15.0,
                categoria = "Comunicação",
                tipoDespesa = "Internet"
            )
            database.despesaDao().inserir(despesa3)
        }

        // Despesas para ciclos finalizados
        val ciclosFinalizados = ciclos.filter { it.status == StatusCicloAcerto.FINALIZADO }
        
        ciclosFinalizados.forEach { ciclo ->
            // Despesas de manutenção
            val despesa1 = Despesa(
                rotaId = ciclo.rotaId,
                cicloId = ciclo.id,
                descricao = "Troca de óleo do veículo",
                valor = 45.0,
                categoria = "Manutenção",
                tipoDespesa = "Óleo do Motor"
            )
            database.despesaDao().inserir(despesa1)

            // Despesas de combustível
            val despesa2 = Despesa(
                rotaId = ciclo.rotaId,
                cicloId = ciclo.id,
                descricao = "Combustível para o ciclo completo",
                valor = 120.0,
                categoria = "Combustível",
                tipoDespesa = "Gasolina"
            )
            database.despesaDao().inserir(despesa2)
        }
    }

    /**
     * Limpa dados existentes (opcional)
     */
    private suspend fun limparDadosExistentes() {
        Log.d("DatabasePopulator", "🧹 Limpando dados existentes...")
        
        // Nota: Os métodos de limpeza em lote não existem nos DAOs
        // Em um cenário real, seria necessário implementar queries DELETE específicas
        // ou usar uma abordagem diferente para limpeza
        
        Log.d("DatabasePopulator", "✅ Limpeza de dados desabilitada - use com cuidado")
    }
}
