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
    
    fun generateContractPdf(contrato: ContratoLocacao, mesas: List<Mesa>): File {
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
            addSignatures(document, contrato, font, fontBold)
            
        } finally {
            document.close()
        }
        
        return file
    }
    
    private fun addTitle(document: Document, numeroContrato: String, fontBold: com.itextpdf.kernel.font.PdfFont) {
        val title = Paragraph("CONTRATO DE LOCAÇÃO DE EQUIPAMENTO DE DIVERSÃO")
            .setFont(fontBold)
            .setFontSize(16f)
            .setMarginBottom(20f)
        
        val numero = Paragraph("Nº contrato: $numeroContrato")
            .setFont(fontBold)
            .setFontSize(12f)
            .setMarginBottom(30f)
        
        document.add(title)
        document.add(numero)
    }
    
    private fun addPartiesSection(document: Document, contrato: ContratoLocacao, font: com.itextpdf.kernel.font.PdfFont, fontBold: com.itextpdf.kernel.font.PdfFont) {
        val sectionTitle = Paragraph("IDENTIFICAÇÃO DAS PARTES")
            .setFont(fontBold)
            .setFontSize(14f)
            .setMarginBottom(15f)
        
        val locadora = Paragraph("LOCADORA: ${contrato.locadorNome}, pessoa jurídica de direito privado, inscrita no CNPJ/MF sob o nº ${contrato.locadorCnpj}, com sede na ${contrato.locadorEndereco}, CEP ${contrato.locadorCep}, neste ato representada por seu representante legal.")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(15f)
        
        val locatario = Paragraph("LOCATÁRIO(A): NOME/RAZÃO SOCIAL: ${contrato.locatarioNome} CPF/CNPJ: ${contrato.locatarioCpf} ENDEREÇO COMERCIAL: ${contrato.locatarioEndereco} TELEFONE CELULAR (WhatsApp): ${contrato.locatarioTelefone} E-MAIL: ${contrato.locatarioEmail}")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(15f)
        
        val intro = Paragraph("As partes acima qualificadas celebram o presente Contrato de Locação, que se regerá pelas seguintes cláusulas e condições:")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(20f)
        
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
        
        val equipamentos = mesas.joinToString(", ") { "Mesa ${it.numero} (${it.tipoMesa.name})" }
        val objeto2 = Paragraph("1.2. Tipo de Equipamento: $equipamentos")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(5f)
        
        val numerosSerie = mesas.joinToString(", ") { it.numero.toString() }
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
            "9.1. As partes reconhecem que este contrato será celebrado por meio eletrônico, sendo as assinaturas apostas manualmente em tela de dispositivo móvel (assinatura eletrônica).",
            "9.2. Nos termos da Medida Provisória nº 2.200-2/2001 e do Código Civil brasileiro, e em conformidade com a Lei nº 14.063/2020, as partes declaram que a assinatura eletrônica utilizada possui a mesma validade jurídica de uma assinatura de próprio punho.",
            "9.3. Uma via deste contrato, devidamente assinada, será enviada para o e-mail ou número de telefone celular informado pelo(a) LOCATÁRIO(A).",
            
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
    
    private fun addSignatures(document: Document, contrato: ContratoLocacao, font: com.itextpdf.kernel.font.PdfFont, fontBold: com.itextpdf.kernel.font.PdfFont) {
        val dataAtual = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR")).format(Date())
        val data = Paragraph("Montes Claros, $dataAtual.")
            .setFont(font)
            .setFontSize(10f)
            .setMarginTop(30f)
            .setMarginBottom(40f)
        
        document.add(data)
        
        // Assinatura do Locador
        val locadorTitle = Paragraph("BILHAR GLOBO R & A LTDA (Locadora)")
            .setFont(fontBold)
            .setFontSize(10f)
            .setMarginBottom(20f)
        
        document.add(locadorTitle)
        
        // Adicionar assinatura do locador se existir
        contrato.assinaturaLocador?.let { assinaturaBase64 ->
            try {
                val assinaturaBytes = Base64.decode(assinaturaBase64, Base64.DEFAULT)
                val assinaturaBitmap = BitmapFactory.decodeByteArray(assinaturaBytes, 0, assinaturaBytes.size)
                val assinaturaImage = ImageDataFactory.create(assinaturaBytes)
                val image = Image(assinaturaImage)
                    .scaleToFit(200f, 100f)
                    .setMarginBottom(20f)
                
                document.add(image)
            } catch (e: Exception) {
                // Se houver erro ao processar a assinatura, adicionar linha para assinatura
                val linhaAssinatura = LineSeparator(null)
                    .setMarginBottom(20f)
                document.add(linhaAssinatura)
            }
        } ?: run {
            val linhaAssinatura = LineSeparator(null)
                .setMarginBottom(20f)
            document.add(linhaAssinatura)
        }
        
        // Assinatura do Locatário
        val locatarioTitle = Paragraph("${contrato.locatarioNome} (Locatário(a))")
            .setFont(fontBold)
            .setFontSize(10f)
            .setMarginTop(40f)
            .setMarginBottom(20f)
        
        document.add(locatarioTitle)
        
        // Adicionar assinatura do locatário se existir
        contrato.assinaturaLocatario?.let { assinaturaBase64 ->
            try {
                val assinaturaBytes = Base64.decode(assinaturaBase64, Base64.DEFAULT)
                val assinaturaBitmap = BitmapFactory.decodeByteArray(assinaturaBytes, 0, assinaturaBytes.size)
                val assinaturaImage = ImageDataFactory.create(assinaturaBytes)
                val image = Image(assinaturaImage)
                    .scaleToFit(200f, 100f)
                    .setMarginBottom(20f)
                
                document.add(image)
            } catch (e: Exception) {
                // Se houver erro ao processar a assinatura, adicionar linha para assinatura
                val linhaAssinatura = LineSeparator(null)
                    .setMarginBottom(20f)
                document.add(linhaAssinatura)
            }
        } ?: run {
            val linhaAssinatura = LineSeparator(null)
                .setMarginBottom(20f)
            document.add(linhaAssinatura)
        }
    }
}
