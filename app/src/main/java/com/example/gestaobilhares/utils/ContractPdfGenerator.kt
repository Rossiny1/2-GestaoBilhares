package com.example.gestaobilhares.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.data.entities.Mesa
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ContractPdfGenerator(private val context: Context) {
    
    fun generateContractPdf(contrato: ContratoLocacao, mesas: List<Mesa>, assinaturaRepresentante: String? = null): File {
        val fileName = "contrato_${contrato.numeroContrato}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        val writer = PdfWriter(FileOutputStream(file))
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)
        
        try {
            // Configurar fonte
            val font = PdfFontFactory.createFont()
            val fontBold = PdfFontFactory.createFont("Helvetica-Bold")
            
            // Título
            addTitle(document, contrato.numeroContrato, fontBold)
            
            // Identificação das Partes
            addPartiesSection(document, contrato, font, fontBold)
            
            // Cláusulas
            addClauses(document, contrato, mesas, font, fontBold)
            
            // Assinaturas
            addSignatures(document, contrato, font, fontBold, assinaturaRepresentante)
            
        } finally {
            document.close()
        }
        
        return file
    }

    data class FechamentoResumo(
        val totalRecebido: Double,
        val despesasViagem: Double,
        val subtotal: Double,
        val comissaoMotorista: Double,
        val comissaoIltair: Double,
        val totalGeral: Double,
        val saldoApurado: Double
    )

    fun generateDistratoPdf(
        contrato: ContratoLocacao,
        mesas: List<Mesa>,
        fechamento: FechamentoResumo,
        confissaoDivida: Pair<Double, Date?>? = null,
        assinaturaRepresentante: String? = null
    ): File {
        val dir = File(context.getExternalFilesDir(null), "distratos_${contrato.numeroContrato}")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "distrato_${System.currentTimeMillis()}.pdf")

        val writer = PdfWriter(FileOutputStream(file))
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)

        try {
            val font = PdfFontFactory.createFont()
            val fontBold = PdfFontFactory.createFont("Helvetica-Bold")

            val title = Paragraph("TERMO DE DISTRATO DO CONTRATO DE LOCAÇÃO")
                .setFont(fontBold).setFontSize(16f).setMarginBottom(20f)
            val numero = Paragraph("Contrato nº: ${contrato.numeroContrato}")
                .setFont(fontBold).setFontSize(12f).setMarginBottom(20f)
            document.add(title); document.add(numero)

            addPartiesSection(document, contrato, font, fontBold)

            // Equipamentos devolvidos (todas as mesas do contrato)
            addClause(document, "CLÁUSULA 1ª – DA DEVOLUÇÃO DO OBJETO", fontBold, font)
            val devolucao1 = Paragraph("1.1. O LOCATÁRIO devolve à LOCADORA todos os equipamentos locados, em regular estado de conservação, conforme vistoria no ato da retirada.")
                .setFont(font).setFontSize(10f).setMarginBottom(5f)
            document.add(devolucao1)
            if (mesas.isNotEmpty()) {
                val lista = com.itextpdf.layout.element.List()
                mesas.forEach { m ->
                    val itemText = "${getTipoEquipamentoNome(m.tipoMesa)} nº ${m.numero}"
                    lista.add(ListItem(itemText))
                }
                document.add(lista)
            }

            // Resumo de fechamento
            addClause(document, "CLÁUSULA 2ª – DO ACERTO DE FECHAMENTO", fontBold, font)
            val resumo = Paragraph(
                "Total Recebido: R$ ${formatMoney(fechamento.totalRecebido)}\n" +
                "Despesas de viagem: R$ ${formatMoney(fechamento.despesasViagem)}\n" +
                "Subtotal: R$ ${formatMoney(fechamento.subtotal)}\n" +
                "Comissão motorista (3%): R$ ${formatMoney(fechamento.comissaoMotorista)}\n" +
                "Comissão Iltair (2%): R$ ${formatMoney(fechamento.comissaoIltair)}\n" +
                "Total Geral: R$ ${formatMoney(fechamento.totalGeral)}\n" +
                "Saldo Apurado: R$ ${formatMoney(fechamento.saldoApurado)}"
            ).setFont(font).setFontSize(10f).setMarginBottom(10f)
            document.add(resumo)

            // Quitação ou Confissão de Dívida
            if (confissaoDivida == null || confissaoDivida.first <= 0.0) {
                val quitacao = Paragraph("2.1. As partes dão-se plena, geral e irrevogável quitação, nada mais tendo a reclamar uma da outra a qualquer título, relativamente ao contrato ora distratado.")
                    .setFont(font).setFontSize(10f).setMarginBottom(15f)
                document.add(quitacao)
            } else {
                addClause(document, "CLÁUSULA 3ª – CONFISSÃO DE DÍVIDA", fontBold, font)
                val (valor, venc) = confissaoDivida
                val vencTxt = venc?.let { SimpleDateFormat("dd/MM/yyyy", Locale("pt","BR")).format(it) } ?: "imediato"
                val conf = Paragraph(
                    "3.1. O LOCATÁRIO confessa dever à LOCADORA a quantia de R$ ${formatMoney(valor)}, obrigando-se ao pagamento até ${vencTxt}. O não pagamento no prazo sujeitará o devedor às penalidades contratuais e legais."
                ).setFont(font).setFontSize(10f).setMarginBottom(15f)
                document.add(conf)
            }

            // ✅ CORRIGIDO: Removida duplicação - usar apenas o método addSignatures
            // ✅ NOVO: Usar método addSignatures para incluir assinatura do representante legal
            addSignatures(document, contrato, font, fontBold, assinaturaRepresentante)
        } finally {
            document.close()
        }

        return file
    }

    private fun formatMoney(v: Double): String = String.format(Locale("pt","BR"), "%.2f", v)
    
    private fun addTitle(document: Document, numeroContrato: String, fontBold: com.itextpdf.kernel.font.PdfFont) {
        val title = Paragraph("CONTRATO DE LOCAÇÃO DE EQUIPAMENTO DE DIVERSÃO")
            .setFont(fontBold)
            .setFontSize(16f) // ✅ REVERTIDO: voltou para 16f
            .setMarginBottom(10f) // ✅ OTIMIZADO: 20f → 10f
        
        val numero = Paragraph("Contrato nº: $numeroContrato")
            .setFont(fontBold)
            .setFontSize(12f) // ✅ REVERTIDO: voltou para 12f
            .setMarginBottom(15f) // ✅ OTIMIZADO: 30f → 15f
        
        document.add(title)
        document.add(numero)
    }
    
    private fun addPartiesSection(document: Document, contrato: ContratoLocacao, font: com.itextpdf.kernel.font.PdfFont, fontBold: com.itextpdf.kernel.font.PdfFont) {
        val sectionTitle = Paragraph("IDENTIFICAÇÃO DAS PARTES")
            .setFont(fontBold)
            .setFontSize(14f) // ✅ REVERTIDO: voltou para 14f
            .setMarginBottom(8f) // ✅ OTIMIZADO: 15f → 8f
        
        val locadora = Paragraph("LOCADORA: ${contrato.locadorNome}, pessoa jurídica de direito privado, inscrita no CNPJ/MF sob o nº ${contrato.locadorCnpj}, com sede na ${contrato.locadorEndereco}, CEP ${contrato.locadorCep}, neste ato representada por seu representante legal.")
            .setFont(font)
            .setFontSize(10f) // ✅ REVERTIDO: voltou para 10f
            .setMarginBottom(8f) // ✅ OTIMIZADO: 15f → 8f
        
        val locatario = Paragraph("LOCATÁRIO(A): NOME/RAZÃO SOCIAL: ${contrato.locatarioNome} CPF/CNPJ: ${contrato.locatarioCpf} ENDEREÇO COMERCIAL: ${contrato.locatarioEndereco} TELEFONE CELULAR (WhatsApp): ${contrato.locatarioTelefone} E-MAIL: ${contrato.locatarioEmail}")
            .setFont(font)
            .setFontSize(10f) // ✅ REVERTIDO: voltou para 10f
            .setMarginBottom(8f) // ✅ OTIMIZADO: 15f → 8f
        
        val intro = Paragraph("As partes acima qualificadas celebram o presente Contrato de Locação, que se regerá pelas seguintes cláusulas e condições:")
            .setFont(font)
            .setFontSize(10f) // ✅ REVERTIDO: voltou para 10f
            .setMarginBottom(10f) // ✅ OTIMIZADO: 20f → 10f
        
        document.add(sectionTitle)
        document.add(locadora)
        document.add(locatario)
        document.add(intro)
    }
    
    private fun addClauses(document: Document, contrato: ContratoLocacao, mesas: List<Mesa>, font: com.itextpdf.kernel.font.PdfFont, fontBold: com.itextpdf.kernel.font.PdfFont) {
        // Cláusula 1ª - DO OBJETO
        addClause(document, "CLÁUSULA 1ª – DO OBJETO", fontBold, font)
        
        val objeto1 = Paragraph("1.1. O objeto deste contrato é a locação de aparelho(s) de diversão, de propriedade da LOCADORA.")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(5f)
        
        // Contar quantidade de cada tipo de equipamento
        val equipamentosPorTipo = mesas.groupBy { it.tipoMesa }
        val descricaoEquipamentos = equipamentosPorTipo.map { (tipo, lista) ->
            val nomeTipo = getTipoEquipamentoNome(tipo)
            val quantidade = lista.size
            "$quantidade $nomeTipo${if (quantidade > 1) "s" else ""}"
        }.joinToString(", ")
        
        val objeto2 = Paragraph("1.2. Tipo de equipamento e quantidade: $descricaoEquipamentos")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(5f)
        
        val numerosSerie = mesas.joinToString("; ") { it.numero.toString() }
        val objeto3 = Paragraph("1.3. Número de Série/Identificação: $numerosSerie")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(5f)
        
        val objeto4 = Paragraph("1.4. O(s) equipamento(s) é(são) entregue(s) em perfeito estado de funcionamento e conservação, conforme verificado pelo(a) LOCATÁRIO(A) no ato da instalação.")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(15f)
        
        document.add(objeto1)
        document.add(objeto2)
        document.add(objeto3)
        document.add(objeto4)
        
        // Cláusula 2ª - DO VALOR E DA FORMA DE PAGAMENTO
        addClause(document, "CLÁUSULA 2ª – DO VALOR E DA FORMA DE PAGAMENTO", fontBold, font)
        
        val valor1 = if (contrato.tipoPagamento == "PERCENTUAL") {
            Paragraph("2.1. O aluguel corresponde a ${contrato.percentualReceita}% (${contrato.percentualReceita} por cento) da receita bruta apurada no(s) equipamento(s) locado(s), considerada antes da incidência de quaisquer tributos ou deduções.")
        } else {
            Paragraph("2.1. O Locatário(a) pagará ao locador a quantia de R$ ${String.format("%.2f", contrato.valorMensal)} reais mensal que deverá ser pago todo dia ${contrato.diaVencimento} de cada mês.")
        }
        valor1.setFont(font)
            .setFontSize(10f)
            .setMarginBottom(5f)
        
        val valor2 = Paragraph("2.2. A apuração da receita e o pagamento do aluguel ocorrerão durante as visitas do representante da LOCADORA, realizadas no mínimo a cada 30 (trinta) dias, em datas a serem flexivelmente combinadas. A apuração também, a critério do locador, poderá ser feita de maneira remota. O pagamento deverá ser efetuado em até 24 (vinte e quatro) horas após a apuração da receita.")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(15f)
        
        document.add(valor1)
        document.add(valor2)
        
        // Cláusula específica sobre preços das fichas
        addClause(document, "CLÁUSULA 2.3 – DO PREÇO DAS FICHAS E REAJUSTES", fontBold, font)
        
        val precoFichas = Paragraph("2.3. O preço das fichas dos equipamentos de sinuca será estabelecido EXCLUSIVAMENTE pela LOCADORA, sendo vedado ao LOCATÁRIO(A) qualquer alteração sem prévia anuência expressa da LOCADORA.")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(5f)
        
        val reajustes = Paragraph("2.4. O preço das Fichas será definido pelo representante da LOCADORA no ato da locação. O valor estabelecido será informado ao LOCATÁRIO(A) através do Recibo de Acerto ou por meio de uma placa informativa de preços entregue no momento da locação.")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(5f)
        
        val multa = Paragraph("2.5. O descumprimento desta cláusula, especialmente a alteração não autorizada dos preços das fichas, constitui violação contratual passível de multa e rescisão do presente contrato, sem prejuízo das demais medidas legais cabíveis.")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(15f)
        
        document.add(precoFichas)
        document.add(reajustes)
        document.add(multa)
        
        // Adicionar outras cláusulas padrão
        addStandardClauses(document, font, fontBold)
    }
    
    private fun addClause(document: Document, title: String, fontBold: com.itextpdf.kernel.font.PdfFont, font: com.itextpdf.kernel.font.PdfFont) {
        val clause = Paragraph(title)
            .setFont(fontBold)
            .setFontSize(12f)
            .setMarginBottom(10f)
        
        document.add(clause)
    }
    
    private fun addStandardClauses(document: Document, font: com.itextpdf.kernel.font.PdfFont, fontBold: com.itextpdf.kernel.font.PdfFont) {
        val clauses = listOf(
            "CLÁUSULA 3ª – DOS MÉTODOS DE APURAÇÃO E COMPROVAÇÃO",
            "3.1. Apuração Presencial: O representante da LOCADORA poderá realizar a contagem dos valores no local, na presença do(a) LOCATÁRIO(A) ou seu preposto.",
            "3.2. Apuração Remota via WhatsApp: O(a) LOCATÁRIO(A) se obriga a enviar imediatamente fotografia nítida e legível do relógio ou marcador do equipamento.",
            "3.3. Emissão do Recibo: Após a apuração e pagamento, será emitido Recibo de Acerto, especificando o valor total apurado, pago e saldo devedor.",
            "3.4. Envio do Recibo: O recibo será enviado digitalmente para o WhatsApp informado pelo(a) LOCATÁRIO(A) ou impresso no local.",
            
            "CLÁUSULA 4ª – DA CONFISSÃO DE DÍVIDA E MEDIDAS DE COBRANÇA",
            "4.1. O(A) LOCATÁRIO(A) reconhece que o saldo devedor no Recibo de Acerto constitui confissão de dívida líquida, certa e exigível.",
            "4.2. Qualquer contestação sobre os valores deverá ser formalizada via e-mail bilharesrea@gmail.com no prazo de 5 (cinco) dias úteis.",
            "4.3. Na falta de pagamento, a LOCADORA poderá tomar medidas legais, incluindo protesto e inclusão do nome/CPF/CNPJ do(a) LOCATÁRIO(A) em órgãos de proteção ao crédito.",
            "4.4. O saldo devedor não quitado será acrescido de multa de 10%, juros de mora de 1% ao mês e correção monetária pelo IGPM/FGV.",
            
            "CLÁUSULA 5ª – DAS OBRIGAÇÕES DO(A) LOCATÁRIO(A)",
            "5.1. Zelar e manter o(s) equipamento(s) em boa ordem, responsabilizando-se por quaisquer danos, furtos ou roubos.",
            "5.2. Obter e manter todas as licenças e alvarás necessários para o funcionamento do(s) equipamento(s).",
            "5.3. É vedado ao(à) LOCATÁRIO(A), sob pena de rescisão imediata: a) Remover o(s) equipamento(s) do local de instalação sem autorização prévia; b) Sublocar, ceder ou transferir este contrato a terceiros; c) Instalar equipamentos do mesmo gênero pertencentes a outras empresas; d) Abrir, violar, parar o relógio/marcador ou alterar qualquer mecanismo interno.",
            
            "CLÁUSULA 6ª – DAS OBRIGAÇÕES DA LOCADORA",
            "6.1. Realizar a manutenção técnica do(s) equipamento(s) sempre que necessário.",
            "6.2. Fornecer os acessórios indispensáveis ao regular funcionamento do(s) equipamento(s).",
            
            "CLÁUSULA 7ª – DA RESCISÃO",
            "7.1. O presente contrato é por prazo indeterminado e poderá ser rescindido por qualquer das partes, a qualquer tempo, mediante aviso prévio de 30 (trinta) dias.",
            "7.2. A violação de qualquer cláusula deste contrato pelo(a) LOCATÁRIO(A), especialmente o não pagamento do aluguel, ensejará a rescisão imediata.",
            
            "CLÁUSULA 8ª – DAS PENALIDADES",
            "8.1. A violação de qualquer obrigação estipulada neste contrato sujeitará o(a) LOCATÁRIO(A) ao pagamento de multa compensatória no valor equivalente a 2 (duas) vezes a média do aluguel apurado nos últimos 3 (três) meses.",
            "8.2. Caso o(a) LOCATÁRIO(A) impeça ou dificulte a retirada do(s) equipamento(s) pela LOCADORA, incidirá multa diária de R$ 200,00 (duzentos reais).",
            
            "CLÁUSULA 9ª – DA VALIDADE JURÍDICA E ASSINATURA ELETRÔNICA",
            "9.1. As partes reconhecem que este contrato será celebrado por meio eletrônico, sendo as assinaturas apostas manualmente em tela de dispositivo móvel (assinatura eletrônica simples), conforme classificação da Lei nº 14.063/2020.",
            "9.2. Nos termos da Medida Provisória nº 2.200-2/2001 e do Código Civil brasileiro, e em conformidade com a Lei nº 14.063/2020, as partes declaram que a assinatura eletrônica simples utilizada possui validade jurídica.",
            "9.3. Para garantir a validade jurídica da assinatura eletrônica simples, o sistema implementa: (a) captura de metadados detalhados (timestamp, device ID, IP, pressão, velocidade); (b) geração de hash SHA-256 para integridade do documento e assinatura; (c) logs jurídicos completos para auditoria; (d) validação de características biométricas da assinatura; (e) confirmação de presença física do LOCATÁRIO durante a assinatura.",
            "9.4. Uma via deste contrato, devidamente assinada, será enviada para o e-mail ou número de telefone celular informado pelo(a) LOCATÁRIO(A).",
            
            "CLÁUSULA 10ª – DO FORO",
            "10.1. Fica eleito o foro da Comarca de Montes Claros, MG, para dirimir quaisquer controvérsias oriundas deste contrato, com renúncia expressa a qualquer outro, por mais privilegiado que seja."
        )
        
        clauses.forEach { clauseText ->
            val paragraph = Paragraph(clauseText)
                .setFont(if (clauseText.startsWith("CLÁUSULA")) fontBold else font)
                .setFontSize(if (clauseText.startsWith("CLÁUSULA")) 12f else 10f)
                .setMarginBottom(if (clauseText.startsWith("CLÁUSULA")) 10f else 5f)
            
            document.add(paragraph)
        }
    }
    
    private fun addSignatures(document: Document, contrato: ContratoLocacao, font: com.itextpdf.kernel.font.PdfFont, fontBold: com.itextpdf.kernel.font.PdfFont, assinaturaRepresentante: String? = null) {
        val dataAtual = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR")).format(Date())
        val data = Paragraph("Montes Claros, $dataAtual.")
            .setFont(font)
            .setFontSize(10f) // ✅ REVERTIDO: voltou para 10f
            .setMarginTop(15f) // ✅ OTIMIZADO: 30f → 15f
            .setMarginBottom(20f) // ✅ OTIMIZADO: 40f → 20f
        
        document.add(data)
        
        // ✅ CORRIGIDO: Assinatura do Locador (ACIMA do nome)
        // Adicionar assinatura do locador primeiro (priorizar assinatura pré-fabricada do representante)
        val assinaturaLocador = assinaturaRepresentante ?: contrato.assinaturaLocador
        assinaturaLocador?.let { assinaturaBase64 ->
            try {
                val assinaturaBytes = Base64.decode(assinaturaBase64, Base64.DEFAULT)
                val assinaturaImage = ImageDataFactory.create(assinaturaBytes)
                val image = Image(assinaturaImage)
                    .scaleToFit(200f, 100f)
                    .setMarginBottom(5f) // ✅ OTIMIZADO: 10f → 5f
                
                document.add(image)
            } catch (e: Exception) {
                // Se houver erro ao processar a assinatura, manter apenas a linha
            }
        }
        
        val locadorTitle = Paragraph("BILHAR GLOBO R & A LTDA (Locadora) - CNPJ: ${contrato.locadorCnpj}")
            .setFont(fontBold)
            .setFontSize(10f) // ✅ REVERTIDO: voltou para 10f
            .setMarginBottom(10f) // ✅ OTIMIZADO: 20f → 10f
        
        document.add(locadorTitle)
        
        // ✅ CORRIGIDO: Assinatura do Locatário (ACIMA do nome)
        // Adicionar assinatura do locatário primeiro
        contrato.assinaturaLocatario?.let { assinaturaBase64 ->
            try {
                val assinaturaBytes = Base64.decode(assinaturaBase64, Base64.DEFAULT)
                val assinaturaImage = ImageDataFactory.create(assinaturaBytes)
                val image = Image(assinaturaImage)
                    .scaleToFit(200f, 100f)
                    .setMarginTop(20f) // ✅ OTIMIZADO: 40f → 20f
                    .setMarginBottom(5f) // ✅ OTIMIZADO: 10f → 5f
                
                document.add(image)
            } catch (e: Exception) {
                // Se houver erro ao processar a assinatura, manter apenas a linha
            }
        }
        
        val locatarioTitle = Paragraph("${contrato.locatarioNome} (Locatário(a)) - CPF/CNPJ: ${contrato.locatarioCpf}")
            .setFont(fontBold)
            .setFontSize(10f) // ✅ REVERTIDO: voltou para 10f
            .setMarginBottom(10f) // ✅ OTIMIZADO: 20f → 10f
        
        document.add(locatarioTitle)
    }
    
    private fun getTipoEquipamentoNome(tipoMesa: com.example.gestaobilhares.data.entities.TipoMesa): String {
        return when (tipoMesa) {
            com.example.gestaobilhares.data.entities.TipoMesa.SINUCA -> "Sinuca"
            com.example.gestaobilhares.data.entities.TipoMesa.JUKEBOX -> "Jukebox"
            com.example.gestaobilhares.data.entities.TipoMesa.PEMBOLIM -> "Pembolim"
            com.example.gestaobilhares.data.entities.TipoMesa.OUTROS -> "Outros"
        }
    }
}
