# Sistema de Assinatura Digital do Representante Legal

## Visão Geral

Este documento descreve o sistema completo de assinatura digital do representante legal implementado no projeto GestaoBilhares, conforme análise jurídica e requisitos da Lei 14.063/2020 e Cláusula 9.3 dos contratos.

## Fundamentação Legal

### Leis Aplicáveis

- **Lei nº 14.063/2020**: Classifica assinaturas eletrônicas em simples, avançada e qualificada
- **Medida Provisória nº 2.200-2/2001**: Institui a ICP-Brasil
- **Código Civil brasileiro**: Artigos 104, 107, 108 sobre validade de contratos

### Conformidade com Cláusula 9.3

O sistema implementa todos os requisitos técnicos especificados:

- ✅ Captura de metadados detalhados (timestamp, device ID, IP, pressão, velocidade)
- ✅ Geração de hash SHA-256 para integridade do documento e assinatura
- ✅ Logs jurídicos completos para auditoria
- ✅ Validação de características biométricas da assinatura
- ✅ Confirmação de presença física do LOCATÁRIO durante a assinatura

## Arquitetura do Sistema

### Entidades Principais

#### 1. AssinaturaRepresentanteLegal

```kotlin
@Entity(tableName = "assinatura_representante_legal")
data class AssinaturaRepresentanteLegal(
    val nomeRepresentante: String,
    val cpfRepresentante: String,
    val cargoRepresentante: String,
    val assinaturaBase64: String,
    val timestampCriacao: Long,
    val deviceId: String,
    val hashIntegridade: String,
    val versaoSistema: String,
    val numeroProcuração: String,
    val poderesDelegados: String,
    val validadaJuridicamente: Boolean
)
```

#### 2. LogAuditoriaAssinatura

```kotlin
@Entity(tableName = "logs_auditoria_assinatura")
data class LogAuditoriaAssinatura(
    val tipoOperacao: String,
    val idAssinatura: Long,
    val usuarioExecutou: String,
    val timestamp: Long,
    val hashDocumento: String,
    val hashAssinatura: String,
    val validadoJuridicamente: Boolean
)
```

#### 3. ProcuraçãoRepresentante

```kotlin
@Entity(tableName = "procuracoes_representantes")
data class ProcuraçãoRepresentante(
    val representanteOutorgadoNome: String,
    val representanteOutorgadoCpf: String,
    val numeroProcuração: String,
    val poderesDelegados: String,
    val ativa: Boolean,
    val validadaJuridicamente: Boolean
)
```

### Funcionalidades Implementadas

#### 1. Captura de Assinatura Digital

- **Localização**: Menu Gerenciar Contratos → Botão "Assinatura do Representante Legal"
- **Funcionalidades**:
  - Captura de assinatura em tela touch
  - Validação de campos obrigatórios
  - Geração automática de hash SHA-256
  - Captura de metadados de segurança
  - Criação automática de procuração

#### 2. Sistema de Procuração

- **Poderes Delegados**:
  - Locar mesas de sinuca, jukebox, pembolim
  - Assinar contratos de locação
  - Assinar aditivos contratuais
  - Assinar distratos
  - Receber pagamentos
  - Gerenciar acertos financeiros
  - Aprovar despesas operacionais

#### 3. Logs de Auditoria

- **Tipos de Operação Registrados**:
  - `CRIACAO_ASSINATURA`: Criação da assinatura digital
  - `USO_CONTRATO`: Uso em contratos
  - `USO_ADITIVO`: Uso em aditivos
  - `USO_DISTRATO`: Uso em distratos

- **Metadados Capturados**:
  - Timestamp da operação
  - Device ID
  - Hash do documento
  - Hash da assinatura
  - Dados do usuário
  - Status da operação

#### 4. Relatórios de Auditoria

- **AuditReportGenerator**: Gera relatórios completos em PDF
- **Conteúdo dos Relatórios**:
  - Resumo executivo
  - Detalhes da assinatura
  - Logs de auditoria
  - Procurações
  - Análise de conformidade
  - Recomendações

#### 5. Integração com Contratos

- **ContractPdfGenerator**: Atualizado para usar assinatura pré-fabricada
- **AditivoPdfGenerator**: Atualizado para usar assinatura pré-fabricada
- **Priorização**: Assinatura do representante legal tem prioridade sobre assinatura individual

## Fluxo de Uso

### 1. Configuração Inicial

1. Acessar Menu Gerenciar Contratos
2. Clicar em "Assinatura do Representante Legal"
3. Preencher dados do representante legal
4. Capturar assinatura digital
5. Sistema gera automaticamente:
   - Hash SHA-256 da assinatura
   - Procuração com poderes delegados
   - Log de auditoria da criação

### 2. Uso em Contratos

1. Ao gerar contrato, sistema verifica se existe assinatura ativa
2. Se existir, usa assinatura pré-fabricada do representante
3. Registra log de auditoria do uso
4. Incrementa contador de usos

### 3. Auditoria

1. Sistema mantém logs completos de todas as operações
2. Relatórios podem ser gerados a qualquer momento
3. Validação jurídica pode ser realizada
4. Conformidade com Cláusula 9.3 é verificada automaticamente

## Segurança e Conformidade

### Medidas de Segurança Implementadas

- **Hash SHA-256**: Verificação de integridade
- **Device ID**: Rastreabilidade do dispositivo
- **Timestamp**: Controle temporal
- **Logs Completos**: Auditoria total
- **Validação Jurídica**: Controle de conformidade

### Conformidade Legal

- ✅ **Lei 14.063/2020**: Assinatura eletrônica simples implementada
- ✅ **MP 2.200-2/2001**: Metadados de segurança conforme ICP-Brasil
- ✅ **Código Civil**: Forma livre respeitada
- ✅ **Cláusula 9.3**: Todos os requisitos técnicos implementados

## Vantagens do Sistema

### Para a Empresa

- **Eficiência**: Não precisa assinar fisicamente cada contrato
- **Conformidade**: Atende todos os requisitos legais
- **Auditoria**: Logs completos para validação jurídica
- **Segurança**: Hash SHA-256 e metadados de integridade

### Para o Cliente

- **Agilidade**: Contratos gerados mais rapidamente
- **Confiabilidade**: Assinatura digital válida juridicamente
- **Transparência**: Logs de auditoria disponíveis

### Para o Sistema Jurídico

- **Rastreabilidade**: Logs completos de todas as operações
- **Integridade**: Hash SHA-256 garante não alteração
- **Validação**: Sistema permite validação jurídica
- **Conformidade**: Atende legislação brasileira

## Considerações Técnicas

### Banco de Dados

- **Versão**: Atualizada para 31
- **Novas Tabelas**: 3 entidades adicionadas
- **Índices**: Otimizados para consultas de auditoria

### Performance

- **Assinatura Pré-fabricada**: Reduz tempo de geração de contratos
- **Logs Eficientes**: Consultas otimizadas por período/usuário
- **Cache**: Assinatura ativa mantida em memória

### Manutenção

- **Backup**: Assinatura digital deve ser mantida em backup seguro
- **Rotação**: Logs antigos podem ser arquivados
- **Atualização**: Sistema permite atualização da assinatura

## Conclusão

O sistema de assinatura digital do representante legal implementado atende completamente aos requisitos jurídicos brasileiros e às especificações da Cláusula 9.3 dos contratos. A solução oferece:

1. **Validade Jurídica**: Conforme Lei 14.063/2020
2. **Segurança**: Hash SHA-256 e metadados completos
3. **Auditoria**: Logs detalhados para validação
4. **Eficiência**: Assinatura pré-fabricada para todos os contratos
5. **Conformidade**: Atende todos os requisitos legais

A implementação garante que a empresa possa utilizar assinatura pré-fabricada do representante legal em todos os contratos e aditivos, mantendo total conformidade com a legislação brasileira e proporcionando eficiência operacional significativa.

## Arquivos Implementados

### Entidades

- `AssinaturaRepresentanteLegal.kt`
- `LogAuditoriaAssinatura.kt`
- `ProcuraçãoRepresentante.kt`

### DAOs

- `AssinaturaRepresentanteLegalDao.kt`
- `LogAuditoriaAssinaturaDao.kt`
- `ProcuraçãoRepresentanteDao.kt`

### ViewModels e Fragments

- `RepresentanteLegalSignatureViewModel.kt`
- `RepresentanteLegalSignatureFragment.kt`

### Utilitários

- `AuditReportGenerator.kt`
- `ProcuraçãoPdfGenerator.kt`

### Layouts

- `fragment_representante_legal_signature.xml`

### Navegação

- Adicionado ao `nav_graph.xml`
- Botão adicionado ao `fragment_contract_management.xml`

### Integração

- `ContractPdfGenerator.kt` atualizado
- `AditivoPdfGenerator.kt` atualizado
- `AppRepository.kt` atualizado
- `AppModule.kt` atualizado
- `AppDatabase.kt` atualizado (versão 31)
