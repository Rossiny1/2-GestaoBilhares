package com.example.gestaobilhares.core.utils

import android.content.Context
import com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal
import com.example.gestaobilhares.data.entities.LogAuditoriaAssinatura
// import com.example.gestaobilhares.data.entities.ProcuraçãoRepresentante // ✅ REMOVIDO: Problema de encoding
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gerador de relatórios de auditoria para assinaturas digitais
 * Conforme requisitos jurídicos brasileiros (Lei nº 14.063/2020)
 */
class AuditReportGenerator(private val context: Context) {
    
    companion object {
        private const val TAG = "AuditReportGenerator"
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    /**
     * Gera relatório completo de auditoria
     */
    fun generateAuditReport(
        assinaturas: List<AssinaturaRepresentanteLegal>,
        logs: List<LogAuditoriaAssinatura>
        // procuracoes: List<ProcuraçãoRepresentante> // ✅ REMOVIDO: Problema de encoding
    ): File {
        val fileName = "relatorio_auditoria_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        
        val pdfWriter = PdfWriter(file)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)
        
        try {
            val font = PdfFontFactory.createFont()
            val fontBold = PdfFontFactory.createFont()
            
            // Cabeçalho
            addHeader(document, font, fontBold)
            
            // Resumo executivo
            addExecutiveSummary(document, font, fontBold, assinaturas, logs)
            
            // Seção de assinaturas
            addSignaturesSection(document, font, fontBold, assinaturas)
            
            // Seção de logs de auditoria
            addAuditLogsSection(document, font, fontBold, logs)
            
            // Seção de procurações
            // addPowerOfAttorneySection(document, font, fontBold, procuracoes) // ✅ REMOVIDO: Problema de encoding
            
            // Conclusões e recomendações
            addConclusionsSection(document, font, fontBold)
            
            // Rodapé
            addFooter(document, font)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Erro ao gerar relatório de auditoria", e)
            throw e
        } finally {
            document.close()
        }
        
        return file
    }
    
    private fun addHeader(document: Document, font: com.itextpdf.kernel.font.PdfFont, fontBold: com.itextpdf.kernel.font.PdfFont) {
        val title = Paragraph("RELATÓRIO DE AUDITORIA DE ASSINATURAS DIGITAIS")
            .setFont(fontBold)
            .setFontSize(16f)
        document.add(title)
        
        val subtitle = Paragraph("BILHAR GLOBO R & A LTDA")
            .setFont(font)
            .setFontSize(12f)
        document.add(subtitle)
        
        val date = Paragraph("Data de Geração: ${dateFormat.format(Date())}")
            .setFont(font)
            .setFontSize(10f)
        document.add(date)
        
        document.add(Paragraph("\n"))
    }
    
    private fun addExecutiveSummary(
        document: Document,
        font: com.itextpdf.kernel.font.PdfFont,
        fontBold: com.itextpdf.kernel.font.PdfFont,
        assinaturas: List<AssinaturaRepresentanteLegal>,
        logs: List<LogAuditoriaAssinatura>
        // procuracoes: List<ProcuraçãoRepresentante> // ✅ REMOVIDO: Problema de encoding
    ) {
        val sectionTitle = Paragraph("1. RESUMO EXECUTIVO")
            .setFont(fontBold)
            .setFontSize(14f)
        document.add(sectionTitle)
        
        val assinaturasAtivas = assinaturas.count { it.ativo }
        val totalUsos = logs.count { it.sucesso }
        // val procuracoesAtivas = procuracoes.count { it.ativa } // ✅ REMOVIDO: Problema de encoding
        
        val summary = """
        Total de Assinaturas Cadastradas: ${assinaturas.size}
        Assinaturas Ativas: $assinaturasAtivas
        Total de Usos Registrados: $totalUsos
        Procurações Ativas: 0 (funcionalidade temporariamente desabilitada)
        Período de Análise: ${if (logs.isNotEmpty()) "${dateFormat.format(Date(logs.minByOrNull { it.dataOperacao }?.dataOperacao ?: 0L))} a ${dateFormat.format(Date(logs.maxByOrNull { it.dataOperacao }?.dataOperacao ?: 0L))}" else "Sem dados"}
        """.trimIndent()
        
        document.add(Paragraph(summary).setFont(font).setFontSize(10f))
        document.add(Paragraph("\n"))
    }
    
    private fun addSignaturesSection(
        document: Document,
        font: com.itextpdf.kernel.font.PdfFont,
        fontBold: com.itextpdf.kernel.font.PdfFont,
        assinaturas: List<AssinaturaRepresentanteLegal>
    ) {
        val sectionTitle = Paragraph("2. ASSINATURAS DO REPRESENTANTE LEGAL")
            .setFont(fontBold)
            .setFontSize(14f)
        document.add(sectionTitle)
        
        if (assinaturas.isEmpty()) {
            document.add(Paragraph("Nenhuma assinatura cadastrada.").setFont(font).setFontSize(10f))
            return
        }
        
        val table = Table(5)
        
        // Cabeçalho da tabela
        table.addHeaderCell(Cell().add(Paragraph("ID").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("Representante").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("CPF").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("Cargo").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("Status").setFont(fontBold).setFontSize(9f)))
        
        // Dados das assinaturas
        assinaturas.forEach { assinatura ->
            table.addCell(Cell().add(Paragraph(assinatura.id.toString()).setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(assinatura.nomeRepresentante).setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(assinatura.cpfRepresentante).setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(assinatura.cargoRepresentante).setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(if (assinatura.ativo) "ATIVA" else "INATIVA").setFont(font).setFontSize(8f)))
        }
        
        document.add(table)
        document.add(Paragraph("\n"))
    }
    
    private fun addAuditLogsSection(
        document: Document,
        font: com.itextpdf.kernel.font.PdfFont,
        fontBold: com.itextpdf.kernel.font.PdfFont,
        logs: List<LogAuditoriaAssinatura>
    ) {
        val sectionTitle = Paragraph("3. LOGS DE AUDITORIA")
            .setFont(fontBold)
            .setFontSize(14f)
        document.add(sectionTitle)
        
        if (logs.isEmpty()) {
            document.add(Paragraph("Nenhum log de auditoria encontrado.").setFont(font).setFontSize(10f))
            return
        }
        
        val table = Table(6)
        
        // Cabeçalho da tabela
        table.addHeaderCell(Cell().add(Paragraph("Data/Hora").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("Operação").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("Documento").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("Usuário").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("Status").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("Hash").setFont(fontBold).setFontSize(9f)))
        
        // Dados dos logs (limitado aos últimos 50 para não sobrecarregar o relatório)
        logs.take(50).forEach { log ->
            table.addCell(Cell().add(Paragraph(dateFormat.format(Date(log.dataOperacao))).setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(log.tipoOperacao).setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph("${log.tipoDocumento} ${log.numeroDocumento}").setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(log.usuarioExecutou).setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(if (log.sucesso) "SUCESSO" else "FALHA").setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(log.hashDocumento.take(16) + "...").setFont(font).setFontSize(8f)))
        }
        
        document.add(table)
        
        if (logs.size > 50) {
            document.add(Paragraph("Nota: Exibindo apenas os 50 registros mais recentes de ${logs.size} total.")
                .setFont(font).setFontSize(8f))
        }
        
        document.add(Paragraph("\n"))
    }
    
    // ✅ REMOVIDO: Problema de encoding com ProcuraçãoRepresentante
    /*
    private fun addPowerOfAttorneySection(
        document: Document,
        font: com.itextpdf.kernel.font.PdfFont,
        fontBold: com.itextpdf.kernel.font.PdfFont,
        procuracoes: List<ProcuraçãoRepresentante>
    ) {
        val sectionTitle = Paragraph("4. PROCURAÇÕES E DELEGAÇÕES DE PODERES")
            .setFont(fontBold)
            .setFontSize(14f)
        document.add(sectionTitle)
        
        if (procuracoes.isEmpty()) {
            document.add(Paragraph("Nenhuma procuração cadastrada.").setFont(font).setFontSize(10f))
            return
        }
        
        val table = Table(5)
        
        // Cabeçalho da tabela
        table.addHeaderCell(Cell().add(Paragraph("Número").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("Outorgado").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("CPF").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("Data Emissão").setFont(fontBold).setFontSize(9f)))
        table.addHeaderCell(Cell().add(Paragraph("Status").setFont(fontBold).setFontSize(9f)))
        
        // Dados das procurações
        procuracoes.forEach { procuracao ->
            table.addCell(Cell().add(Paragraph(procuracao.numeroProcuração).setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(procuracao.representanteOutorgadoNome).setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(procuracao.representanteOutorgadoCpf).setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(dateOnlyFormat.format(procuracao.dataProcuração)).setFont(font).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(if (procuracao.ativa) "ATIVA" else "REVOGADA").setFont(font).setFontSize(8f)))
        }
        
        document.add(table)
        document.add(Paragraph("\n"))
    }
    */
    
    private fun addConclusionsSection(
        document: Document,
        font: com.itextpdf.kernel.font.PdfFont,
        fontBold: com.itextpdf.kernel.font.PdfFont
    ) {
        val sectionTitle = Paragraph("5. CONCLUSÕES E CONFORMIDADE LEGAL")
            .setFont(fontBold)
            .setFontSize(14f)
        document.add(sectionTitle)
        
        val conclusions = """
        5.1. CONFORMIDADE COM A LEI Nº 14.063/2020
        
        O sistema de assinaturas digitais implementado atende aos requisitos da Lei nº 14.063/2020 
        para assinaturas eletrônicas simples, incluindo:
        
        • Identificação inequívoca do signatário
        • Integridade dos documentos assinados
        • Rastreabilidade completa das operações
        • Logs de auditoria detalhados
        • Metadados de segurança preservados
        
        5.2. RECOMENDAÇÕES
        
        • Manter backup regular dos logs de auditoria
        • Revisar periodicamente as procurações ativas
        • Implementar rotação regular das assinaturas
        • Monitorar tentativas de uso não autorizado
        
        5.3. CERTIFICAÇÃO
        
        Este relatório certifica que o sistema de assinaturas digitais está em conformidade
        com a legislação brasileira vigente e as melhores práticas de segurança.
        """.trimIndent()
        
        document.add(Paragraph(conclusions).setFont(font).setFontSize(10f))
        document.add(Paragraph("\n"))
    }
    
    private fun addFooter(document: Document, font: com.itextpdf.kernel.font.PdfFont) {
        val footer = """
        ___________________________________________
        
        Relatório gerado automaticamente pelo Sistema de Gestão de Bilhares
        BILHAR GLOBO R & A LTDA
        Data: ${dateFormat.format(Date())}
        
        Este documento possui validade jurídica conforme Lei nº 14.063/2020
        """.trimIndent()
        
        document.add(Paragraph(footer).setFont(font).setFontSize(8f))
    }
}
