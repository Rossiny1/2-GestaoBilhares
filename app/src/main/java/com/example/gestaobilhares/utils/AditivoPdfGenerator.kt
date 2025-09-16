package com.example.gestaobilhares.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.gestaobilhares.data.entities.AditivoContrato
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

class AditivoPdfGenerator(private val context: Context) {
    
    fun generateAditivoPdf(aditivo: AditivoContrato, contrato: ContratoLocacao, mesas: List<Mesa>, assinaturaRepresentante: String? = null): File {
        // Criar diretório dedicado para cada aditivo (evita ENOENT ao salvar)
        val aditivoDirName = "aditivo_${aditivo.numeroAditivo}"
        val aditivoDir = File(context.getExternalFilesDir(null), aditivoDirName)
        if (!aditivoDir.exists()) {
            aditivoDir.mkdirs()
        }

        val fileName = "${System.currentTimeMillis()}.pdf"
        val file = File(aditivoDir, fileName)
        
        val writer = PdfWriter(FileOutputStream(file))
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)
        
        try {
            // Configurar fonte
            val font = PdfFontFactory.createFont()
            val fontBold = PdfFontFactory.createFont("Helvetica-Bold")
            
            // Título
            val tituloNumero = if (aditivo.tipo.equals("RETIRADA", ignoreCase = true))
                aditivo.numeroAditivo + " (RETIRADA)" else aditivo.numeroAditivo
            addTitle(document, tituloNumero, contrato.numeroContrato, fontBold)
            
            // Identificação das Partes
            addPartiesSection(document, contrato, font, fontBold)
            
            // Cláusulas do Aditivo
            addAditivoClauses(document, aditivo, contrato, mesas, font, fontBold)
            
            // ✅ OTIMIZADO: Assinaturas lado a lado para aditivo
            addDistratoSignatures(document, contrato, font, fontBold, assinaturaRepresentante)
            
        } finally {
            document.close()
        }
        
        return file
    }
    
    private fun addTitle(document: Document, numeroAditivo: String, numeroContrato: String, fontBold: com.itextpdf.kernel.font.PdfFont) {
        val title = Paragraph("ADITIVO CONTRATUAL")
            .setFont(fontBold)
            .setFontSize(16f)
            .setMarginBottom(10f)
        
        val aditivoNumero = Paragraph("Aditivo nº: $numeroAditivo")
            .setFont(fontBold)
            .setFontSize(12f)
            .setMarginBottom(5f)
        
        val contratoNumero = Paragraph("Contrato nº: $numeroContrato")
            .setFont(fontBold)
            .setFontSize(12f)
            .setMarginBottom(30f)
        
        document.add(title)
        document.add(aditivoNumero)
        document.add(contratoNumero)
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
        
        val intro = Paragraph("As partes acima qualificadas, tendo em vista o Contrato de Locação nº ${contrato.numeroContrato}, celebrado em ${SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(contrato.dataContrato)}, resolvem aditar o referido contrato nos termos das cláusulas abaixo:")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(20f)
        
        document.add(sectionTitle)
        document.add(locadora)
        document.add(locatario)
        document.add(intro)
    }
    
    private fun addAditivoClauses(document: Document, aditivo: AditivoContrato, contrato: ContratoLocacao, mesas: List<Mesa>, font: com.itextpdf.kernel.font.PdfFont, fontBold: com.itextpdf.kernel.font.PdfFont) {
        val isRetirada = aditivo.tipo.equals("RETIRADA", ignoreCase = true)
        addClause(document, "CLÁUSULA 1ª – DO OBJETO DO ADITIVO", fontBold, font)

        val equipamentosPorTipo = mesas.groupBy { it.tipoMesa }
        val descricaoEquipamentos = equipamentosPorTipo.map { (tipo, lista) ->
            val nomeTipo = getTipoEquipamentoNome(tipo)
            val quantidade = lista.size
            "$quantidade $nomeTipo${if (quantidade > 1) "s" else ""}"
        }.joinToString(", ")
        val numerosSerie = mesas.joinToString("; ") { it.numero.toString() }

        if (!isRetirada) {
            val objeto1 = Paragraph("1.1. O presente aditivo tem por objeto a inclusão de novos equipamentos ao contrato de locação original.")
                .setFont(font).setFontSize(10f).setMarginBottom(5f)
            val objeto2 = Paragraph("1.2. Novos equipamentos a serem incluídos: $descricaoEquipamentos")
                .setFont(font).setFontSize(10f).setMarginBottom(5f)
            val objeto3 = Paragraph("1.3. Número de Série/Identificação dos novos equipamentos: $numerosSerie")
                .setFont(font).setFontSize(10f).setMarginBottom(5f)
            val objeto4 = Paragraph("1.4. Os novos equipamentos serão entregues em perfeito estado de funcionamento e conservação, conforme verificado pelo(a) LOCATÁRIO(A) no ato da instalação.")
                .setFont(font).setFontSize(10f).setMarginBottom(15f)
            document.add(objeto1); document.add(objeto2); document.add(objeto3); document.add(objeto4)
        } else {
            val objeto1 = Paragraph("1.1. O presente aditivo tem por objeto a RETIRADA de equipamentos do contrato de locação original, com ciência do(a) LOCATÁRIO(A).")
                .setFont(font).setFontSize(10f).setMarginBottom(5f)
            val objeto2 = Paragraph("1.2. Equipamentos a serem retirados: $descricaoEquipamentos")
                .setFont(font).setFontSize(10f).setMarginBottom(5f)
            val objeto3 = Paragraph("1.3. Número de Série/Identificação dos equipamentos retirados: $numerosSerie")
                .setFont(font).setFontSize(10f).setMarginBottom(5f)
            val objeto4 = Paragraph("1.4. As demais cláusulas do contrato original permanecem válidas para os equipamentos remanescentes.")
                .setFont(font).setFontSize(10f).setMarginBottom(15f)
            document.add(objeto1); document.add(objeto2); document.add(objeto3); document.add(objeto4)
        }

        addClause(document, "CLÁUSULA 2ª – DAS CONDIÇÕES FINANCEIRAS", fontBold, font)
        if (!isRetirada) {
            val condicoes1 = Paragraph("2.1. Os novos equipamentos incluídos seguirão as mesmas condições financeiras estabelecidas no contrato original.")
                .setFont(font).setFontSize(10f).setMarginBottom(5f)
            val condicoes2 = if (contrato.tipoPagamento == "PERCENTUAL") {
                Paragraph("2.2. O aluguel dos novos equipamentos corresponderá a ${contrato.percentualReceita}% (${contrato.percentualReceita} por cento) da receita bruta apurada nos novos equipamentos, considerada antes da incidência de quaisquer tributos ou deduções.")
            } else {
                Paragraph("2.2. O aluguel dos novos equipamentos será calculado proporcionalmente ao valor mensal estabelecido no contrato original (R$ ${String.format("%.2f", contrato.valorMensal)}), considerando a quantidade e tipo dos equipamentos adicionados.")
            }.setFont(font).setFontSize(10f).setMarginBottom(5f)
            val condicoes3 = Paragraph("2.3. A apuração da receita e o pagamento do aluguel dos novos equipamentos seguirão o mesmo cronograma estabelecido no contrato original.")
                .setFont(font).setFontSize(10f).setMarginBottom(15f)
            document.add(condicoes1); document.add(condicoes2); document.add(condicoes3)

            val condicoes4 = Paragraph("2.4. O preço das Fichas será definido pelo representante da LOCADORA no ato da locação/instalação, informado no Recibo de Acerto ou placa informativa, conforme cláusula 2.4 do Contrato de Locação original.")
                .setFont(font).setFontSize(10f).setMarginBottom(15f)
            document.add(condicoes4)
        } else {
            val c1 = Paragraph("2.1. A retirada implica ajuste proporcional da base de cálculo do aluguel, mantendo-se o mesmo modelo financeiro (percentual ou valor fixo) do contrato original.")
                .setFont(font).setFontSize(10f).setMarginBottom(5f)
            val c2 = Paragraph("2.2. A apuração e o pagamento considerarão apenas os equipamentos remanescentes a partir desta data.")
                .setFont(font).setFontSize(10f).setMarginBottom(15f)
            document.add(c1); document.add(c2)
        }

        addClause(document, "CLÁUSULA 3ª – DAS OBRIGAÇÕES", fontBold, font)
        val obrig = Paragraph("3.1. Permanecem inalteradas e aplicáveis as obrigações do contrato original, no que couber.")
            .setFont(font).setFontSize(10f).setMarginBottom(15f)
        document.add(obrig)

        addClause(document, "CLÁUSULA 4ª – DA VIGÊNCIA", fontBold, font)
        val v1 = Paragraph("4.1. O presente aditivo entra em vigor na data de sua assinatura pelas partes.")
            .setFont(font).setFontSize(10f).setMarginBottom(5f)
        val v2 = Paragraph("4.2. Este aditivo integra o contrato original, complementando-o no que aqui se estabelece.")
            .setFont(font).setFontSize(10f).setMarginBottom(15f)
        document.add(v1); document.add(v2)

        aditivo.observacoes?.let { observacoes ->
            if (observacoes.isNotBlank()) {
                addClause(document, "CLÁUSULA 5ª – DAS OBSERVAÇÕES", fontBold, font)
                val obs = Paragraph("5.1. $observacoes").setFont(font).setFontSize(10f).setMarginBottom(15f)
                document.add(obs)
            }
        }
    }
    
    private fun addClause(document: Document, title: String, fontBold: com.itextpdf.kernel.font.PdfFont, font: com.itextpdf.kernel.font.PdfFont) {
        val clause = Paragraph(title)
            .setFont(fontBold)
            .setFontSize(12f)
            .setMarginBottom(10f)
        
        document.add(clause)
    }
    
    private fun addDistratoSignatures(document: Document, contrato: ContratoLocacao, font: com.itextpdf.kernel.font.PdfFont, fontBold: com.itextpdf.kernel.font.PdfFont, assinaturaRepresentante: String? = null) {
        val dataAtual = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR")).format(Date())
        val data = Paragraph("Montes Claros, $dataAtual.")
            .setFont(font)
            .setFontSize(9f) // ✅ OTIMIZADO: menor para caber em 1 página
            .setMarginTop(10f) // ✅ OTIMIZADO: reduzido
            .setMarginBottom(15f) // ✅ OTIMIZADO: reduzido
        
        document.add(data)
        
        // ✅ NOVO: Layout lado a lado com Table
        val table = Table(2).useAllAvailableWidth()
        
        // Coluna 1: Locadora
        val cellLocadora = Cell()
        cellLocadora.setPadding(5f)
        
        // Assinatura do locador (representante legal)
        val assinaturaLocador = assinaturaRepresentante ?: contrato.assinaturaLocador
        assinaturaLocador?.let { assinaturaBase64 ->
            try {
                val assinaturaBytes = Base64.decode(assinaturaBase64, Base64.DEFAULT)
                val assinaturaImage = ImageDataFactory.create(assinaturaBytes)
                val image = Image(assinaturaImage)
                    .scaleToFit(150f, 80f) // ✅ OTIMIZADO: menor para lado a lado
                    .setMarginBottom(5f)
                
                cellLocadora.add(image)
            } catch (e: Exception) {
                // Se houver erro, adicionar linha para assinatura
                val linha = Paragraph("_________________________")
                    .setFont(font)
                    .setFontSize(9f)
                    .setMarginBottom(5f)
                cellLocadora.add(linha)
            }
        } ?: run {
            // Linha para assinatura se não houver
            val linha = Paragraph("_________________________")
                .setFont(font)
                .setFontSize(9f)
                .setMarginBottom(5f)
            cellLocadora.add(linha)
        }
        
        // Nome da empresa
        val locadoraNome = Paragraph("BILHAR GLOBO R & A LTDA")
            .setFont(fontBold)
            .setFontSize(9f)
            .setMarginBottom(2f)
        cellLocadora.add(locadoraNome)
        
        // CNPJ embaixo do nome
        val locadoraCnpj = Paragraph("CNPJ: ${contrato.locadorCnpj}")
            .setFont(font)
            .setFontSize(8f)
            .setMarginBottom(5f)
        cellLocadora.add(locadoraCnpj)
        
        // Coluna 2: Locatário
        val cellLocatario = Cell()
        cellLocatario.setPadding(5f)
        
        // Assinatura do locatário
        contrato.assinaturaLocatario?.let { assinaturaBase64 ->
            try {
                val assinaturaBytes = Base64.decode(assinaturaBase64, Base64.DEFAULT)
                val assinaturaImage = ImageDataFactory.create(assinaturaBytes)
                val image = Image(assinaturaImage)
                    .scaleToFit(150f, 80f) // ✅ OTIMIZADO: menor para lado a lado
                    .setMarginBottom(5f)
                
                cellLocatario.add(image)
            } catch (e: Exception) {
                // Se houver erro, adicionar linha para assinatura
                val linha = Paragraph("_________________________")
                    .setFont(font)
                    .setFontSize(9f)
                    .setMarginBottom(5f)
                cellLocatario.add(linha)
            }
        } ?: run {
            // Linha para assinatura se não houver
            val linha = Paragraph("_________________________")
                .setFont(font)
                .setFontSize(9f)
                .setMarginBottom(5f)
            cellLocatario.add(linha)
        }
        
        // Nome do locatário
        val locatarioNome = Paragraph(contrato.locatarioNome)
            .setFont(fontBold)
            .setFontSize(9f)
            .setMarginBottom(2f)
        cellLocatario.add(locatarioNome)
        
        // CPF embaixo do nome
        val locatarioCpf = Paragraph("CPF/CNPJ: ${contrato.locatarioCpf}")
            .setFont(font)
            .setFontSize(8f)
            .setMarginBottom(5f)
        cellLocatario.add(locatarioCpf)
        
        table.addCell(cellLocadora)
        table.addCell(cellLocatario)
        
        document.add(table)
    }
    
    private fun addSignatures(document: Document, aditivo: AditivoContrato, contrato: ContratoLocacao, font: com.itextpdf.kernel.font.PdfFont, fontBold: com.itextpdf.kernel.font.PdfFont, assinaturaRepresentante: String? = null) {
        val dataAtual = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR")).format(Date())
        val data = Paragraph("Montes Claros, $dataAtual.")
            .setFont(font)
            .setFontSize(10f)
            .setMarginTop(30f)
            .setMarginBottom(40f)
        
        document.add(data)
        
        // Assinatura do Locador
        val locadorTitle = Paragraph("BILHAR GLOBO R & A LTDA (Locadora) - CNPJ: ${contrato.locadorCnpj}")
            .setFont(fontBold)
            .setFontSize(10f)
            .setMarginBottom(10f)
        
        document.add(locadorTitle)
        
        // Linha para assinatura da locadora
        val linhaLocadora = LineSeparator(null)
            .setMarginBottom(20f)
        document.add(linhaLocadora)
        
        // Adicionar assinatura do locador (priorizar assinatura pré-fabricada do representante)
        val assinaturaLocador = assinaturaRepresentante ?: aditivo.assinaturaLocador
        assinaturaLocador?.let { assinaturaBase64 ->
            try {
                val assinaturaBytes = Base64.decode(assinaturaBase64, Base64.DEFAULT)
                val assinaturaImage = ImageDataFactory.create(assinaturaBytes)
                val image = Image(assinaturaImage)
                    .scaleToFit(200f, 100f)
                    .setMarginBottom(20f)
                
                document.add(image)
            } catch (e: Exception) {
                // Se houver erro ao processar a assinatura, manter apenas a linha
            }
        }
        
        // Assinatura do Locatário
        val locatarioTitle = Paragraph("${contrato.locatarioNome} (Locatário(a)) - CPF/CNPJ: ${contrato.locatarioCpf}")
            .setFont(fontBold)
            .setFontSize(10f)
            .setMarginTop(40f)
            .setMarginBottom(10f)
        
        document.add(locatarioTitle)
        
        // Adicionar assinatura do locatário se existir (ACIMA da linha)
        aditivo.assinaturaLocatario?.let { assinaturaBase64 ->
            try {
                val assinaturaBytes = Base64.decode(assinaturaBase64, Base64.DEFAULT)
                val assinaturaImage = ImageDataFactory.create(assinaturaBytes)
                val image = Image(assinaturaImage)
                    .scaleToFit(200f, 100f)
                    .setMarginBottom(10f)
                
                document.add(image)
            } catch (e: Exception) {
                // Se houver erro ao processar a assinatura, manter apenas a linha
            }
        }
        
        // Linha para assinatura do locatário (DEPOIS da assinatura)
        val linhaLocatario = LineSeparator(null)
            .setMarginBottom(20f)
        document.add(linhaLocatario)

        // Testemunhas
        addClause(document, "TESTEMUNHAS", fontBold, font)

        val testemunha1 = Paragraph("Testemunha 1: _______________________________________   CPF: ______________________________")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(10f)
        val testemunha2 = Paragraph("Testemunha 2: _______________________________________   CPF: ______________________________")
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(10f)

        document.add(testemunha1)
        document.add(testemunha2)
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
