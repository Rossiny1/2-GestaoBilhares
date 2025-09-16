package com.example.gestaobilhares.utils

import android.content.Context
import com.example.gestaobilhares.data.entities.ProcuraçãoRepresentante
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gerador de procurações em PDF
 * Conforme modelo jurídico brasileiro
 */
class ProcuraçãoPdfGenerator(private val context: Context) {
    
    companion object {
        private const val TAG = "ProcuraçãoPdfGenerator"
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    /**
     * Gera procuração em PDF
     */
    fun generateProcuraçãoPdf(procuracao: ProcuraçãoRepresentante): File {
        val fileName = "procuracao_${procuracao.numeroProcuração}_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        
        val pdfWriter = PdfWriter(file)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)
        
        try {
            val font = PdfFontFactory.createFont()
            val fontBold = PdfFontFactory.createFont()
            
            // Cabeçalho
            addHeader(document, font, fontBold, procuracao)
            
            // Corpo da procuração
            addPowerOfAttorneyBody(document, font, fontBold, procuracao)
            
            // Poderes específicos
            addSpecificPowers(document, font, fontBold, procuracao)
            
            // Cláusulas finais
            addFinalClauses(document, font, fontBold, procuracao)
            
            // Assinatura
            addSignatureSection(document, font, fontBold, procuracao)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Erro ao gerar procuração", e)
            throw e
        } finally {
            document.close()
        }
        
        return file
    }
    
    private fun addHeader(
        document: Document,
        font: com.itextpdf.kernel.font.PdfFont,
        fontBold: com.itextpdf.kernel.font.PdfFont,
        procuracao: ProcuraçãoRepresentante
    ) {
        val title = Paragraph("PROCURAÇÃO")
            .setFont(fontBold)
            .setFontSize(16f)
        document.add(title)
        
        val subtitle = Paragraph("INSTRUMENTO PARTICULAR DE PROCURAÇÃO")
            .setFont(fontBold)
            .setFontSize(12f)
        document.add(subtitle)
        
        val number = Paragraph("Procuração nº ${procuracao.numeroProcuração}")
            .setFont(font)
            .setFontSize(10f)
        document.add(number)
        
        document.add(Paragraph("\n"))
    }
    
    private fun addPowerOfAttorneyBody(
        document: Document,
        font: com.itextpdf.kernel.font.PdfFont,
        fontBold: com.itextpdf.kernel.font.PdfFont,
        procuracao: ProcuraçãoRepresentante
    ) {
        val outorgante = Paragraph("OUTORGANTE:")
            .setFont(fontBold)
            .setFontSize(12f)
        document.add(outorgante)
        
        val outorganteInfo = """
        ${procuracao.empresaNome}, pessoa jurídica de direito privado, inscrita no CNPJ sob o 
        nº ${procuracao.empresaCnpj}, com sede em ${procuracao.empresaEndereco}, 
        neste ato representada por ${procuracao.representanteLegalNome}, CPF ${procuracao.representanteLegalCpf},
        ${procuracao.representanteLegalCargo}.
        """.trimIndent()
        
        document.add(Paragraph(outorganteInfo).setFont(font).setFontSize(10f))
        document.add(Paragraph("\n"))
        
        val outorgado = Paragraph("OUTORGADO:")
            .setFont(fontBold)
            .setFontSize(12f)
        document.add(outorgado)
        
        val outorgadoInfo = """
        ${procuracao.representanteOutorgadoNome}, ${procuracao.representanteOutorgadoCargo}, portador do CPF nº ${procuracao.representanteOutorgadoCpf}, 
        brasileiro, maior e capaz, usuário do sistema: ${procuracao.representanteOutorgadoUsuario}.
        """.trimIndent()
        
        document.add(Paragraph(outorgadoInfo).setFont(font).setFontSize(10f))
        document.add(Paragraph("\n"))
    }
    
    private fun addSpecificPowers(
        document: Document,
        font: com.itextpdf.kernel.font.PdfFont,
        fontBold: com.itextpdf.kernel.font.PdfFont,
        procuracao: ProcuraçãoRepresentante
    ) {
        val powersTitle = Paragraph("PODERES OUTORGADOS:")
            .setFont(fontBold)
            .setFontSize(12f)
        document.add(powersTitle)
        
        val powers = """
        Pelo presente instrumento particular de procuração, a OUTORGANTE nomeia e constitui 
        como seu bastante procurador o OUTORGADO acima qualificado, a quem confere os 
        seguintes poderes específicos:
        
        1. CONTRATOS DE LOCAÇÃO DE MESAS DE BILHAR:
           • Celebrar, assinar, alterar e rescindir contratos de locação de mesas de bilhar;
           • Negociar valores, prazos e condições contratuais;
           • Autorizar aditivos contratuais para inclusão ou retirada de mesas;
           • Assinar distratos e termos de encerramento de contratos.
        
        2. OPERAÇÕES FINANCEIRAS:
           • Receber valores decorrentes dos contratos de locação;
           • Emitir recibos e quitações;
           • Negociar formas de pagamento;
           • Autorizar descontos e acertos financeiros.
        
        3. GESTÃO OPERACIONAL:
           • Autorizar a instalação e retirada de mesas de bilhar;
           • Coordenar equipes de manutenção e transporte;
           • Resolver questões operacionais relacionadas aos contratos;
           • Representar a empresa perante clientes e terceiros.
        
        4. ASSINATURA ELETRÔNICA:
           • Utilizar assinatura eletrônica simples em nome da OUTORGANTE;
           • Assinar documentos digitalmente conforme Lei nº 14.063/2020;
           • Autorizar o uso da assinatura digital pré-cadastrada da empresa;
           • Validar documentos eletrônicos em nome da OUTORGANTE.
        
        5. PODERES GERAIS:
           • Praticar todos os atos necessários ao fiel cumprimento desta procuração;
           • Substabelecer os presentes poderes, no todo ou em parte;
           • Revogar substabelecimentos anteriormente outorgados.
        """.trimIndent()
        
        document.add(Paragraph(powers).setFont(font).setFontSize(9f))
        document.add(Paragraph("\n"))
    }
    
    private fun addFinalClauses(
        document: Document,
        font: com.itextpdf.kernel.font.PdfFont,
        fontBold: com.itextpdf.kernel.font.PdfFont,
        procuracao: ProcuraçãoRepresentante
    ) {
        val clausesTitle = Paragraph("CLÁUSULAS FINAIS:")
            .setFont(fontBold)
            .setFontSize(12f)
        document.add(clausesTitle)
        
        val finalClauses = """
        CLÁUSULA PRIMEIRA - VALIDADE
        Esta procuração tem validade ${if (procuracao.dataValidade != null) "até ${dateFormat.format(procuracao.dataValidade)}" else "por prazo indeterminado"}, 
        podendo ser revogada a qualquer tempo pela OUTORGANTE.
        
        CLÁUSULA SEGUNDA - RESPONSABILIDADE
        O OUTORGADO assume total responsabilidade pelos atos praticados no exercício 
        desta procuração, comprometendo-se a agir sempre no melhor interesse da OUTORGANTE.
        
        CLÁUSULA TERCEIRA - CONFORMIDADE LEGAL
        Esta procuração está em conformidade com o Código Civil Brasileiro e demais 
        legislações aplicáveis, incluindo a Lei nº 14.063/2020 sobre assinaturas eletrônicas.
        
        CLÁUSULA QUARTA - FORO
        Fica eleito o foro da comarca onde se situa a sede da OUTORGANTE para dirimir 
        quaisquer questões oriundas desta procuração.
        """.trimIndent()
        
        document.add(Paragraph(finalClauses).setFont(font).setFontSize(9f))
        document.add(Paragraph("\n"))
    }
    
    private fun addSignatureSection(
        document: Document,
        font: com.itextpdf.kernel.font.PdfFont,
        fontBold: com.itextpdf.kernel.font.PdfFont,
        procuracao: ProcuraçãoRepresentante
    ) {
        val location = Paragraph("Local e Data: ________________, ${dateFormat.format(procuracao.dataProcuração)}")
            .setFont(font)
            .setFontSize(10f)
        document.add(location)
        
        document.add(Paragraph("\n\n\n"))
        
        val signatures = """
        ___________________________________________
        ${procuracao.empresaNome}
        OUTORGANTE
        
        
        ___________________________________________
        ${procuracao.representanteOutorgadoNome}
        OUTORGADO
        CPF: ${procuracao.representanteOutorgadoCpf}
        """.trimIndent()
        
        document.add(Paragraph(signatures).setFont(font).setFontSize(10f))
        
        document.add(Paragraph("\n"))
        
        val footer = """
        ___________________________________________
        
        Documento gerado automaticamente pelo Sistema de Gestão de Bilhares
        ${procuracao.empresaNome}
        Data de Geração: ${dateFormat.format(Date())}
        
        Este documento possui validade jurídica conforme legislação brasileira vigente.
        """.trimIndent()
        
        document.add(Paragraph(footer).setFont(font).setFontSize(8f))
    }
}
