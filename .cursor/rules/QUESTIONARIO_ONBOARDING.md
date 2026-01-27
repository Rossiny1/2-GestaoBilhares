# 🔍 QUESTIONÁRIO DE ONBOARDING - Gestão Bilhares

> **Instruções:** Execute TODOS os comandos abaixo e cole os resultados COMPLETOS no arquivo de resposta `PROJECT_CONTEXT_FULL.md`.  
> **Não resuma, não use "...", não omita nada.**  
> **Se um comando falhar, documente o erro e tente alternativa.**

---

## 📂 SEÇÃO 1: ESTRUTURA DE PASTAS

### 1.1 Árvore de Diretórios
**Execute:**
```bash
tree -L 4 -I 'build|.gradle|.idea' --dirsfirst
```

**Se `tree` não disponível (Windows), use:**
```powershell
Get-ChildItem -Recurse -Depth 3 -Directory | Select-Object FullName | Out-String
```

**Cole o resultado completo abaixo no arquivo de resposta.**

---

## 📄 SEÇÃO 2: VIEWMODELS

### 2.1 Listar Todos os ViewModels
**Execute:**
```bash
rg "class.*ViewModel" --type kt -l
```

**Cole a lista completa.**

---

### 2.2 Para CADA ViewModel da lista acima, responda:

**Template para cada ViewModel:**
```markdown
#### ViewModel: [Nome]
- **Path:** [path completo]
- **Responsabilidades:** [leia o arquivo e resuma em 2-3 linhas]
- **StateFlows principais:** [liste os StateFlows declarados]
- **Use Cases injetados:** [liste os use cases no construtor]
- **Funções principais:** [liste 5-10 funções públicas mais importantes]
```

**Comando para ajudar (execute para CADA ViewModel):**
```bash
# Exemplo para SettlementViewModel
rg "val.*StateFlow|fun [a-z].*\(" --type kt ui/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementViewModel.kt
```

---

## 🗄️ SEÇÃO 3: ENTIDADES (ROOM DATABASE)

### 3.1 Listar Todas as Entidades
**Execute:**
```bash
rg "@Entity" --type kt -l
```

**Cole a lista completa.**

---

### 3.2 Para CADA Entidade, responda:

**Template:**
```markdown
#### Entity: [Nome]
- **Path:** [path completo]
- **Nome da tabela:** [buscar em @Entity(tableName = "...")]
- **Campos:** [liste TODOS os campos com nome + tipo]
- **Primary Key:** [campo(s) marcado com @PrimaryKey]
- **Foreign Keys:** [campo(s) com @ForeignKey ou relacionamentos]
- **Índices:** [se houver @Index]
```

**Comando para ajudar:**
```bash
# Exemplo para HistoricoManutencaoMesa
rg "@Entity|@PrimaryKey|@ColumnInfo|val " --type kt data/src/main/java/com/example/gestaobilhares/data/entities/HistoricoManutencaoMesa.kt -C 2
```

---

## 🔌 SEÇÃO 4: DAOs (DATA ACCESS OBJECTS)

### 4.1 Listar Todos os DAOs
**Execute:**
```bash
rg "@Dao" --type kt -l
```

**Cole a lista completa.**

---

### 4.2 Para CADA DAO, responda:

**Template:**
```markdown
#### DAO: [Nome]
- **Path:** [path completo]
- **Entidade relacionada:** [qual entidade ele gerencia]
- **Queries principais:**
  - [Nome da função]: [descrição breve - INSERT, UPDATE, DELETE, SELECT]
  - [Nome da função]: [descrição]
  - ...
```

**Comando para ajudar:**
```bash
# Exemplo
rg "@Query|@Insert|@Update|@Delete|fun " --type kt data/src/main/java/com/example/gestaobilhares/data/dao/HistoricoManutencaoMesaDao.kt -C 1
```

---

## ⚙️ SEÇÃO 5: USE CASES

### 5.1 Listar Todos os Use Cases
**Execute:**
```bash
rg "UseCase" --type kt -l | grep -v "Test"
```

**Se grep não funcionar (Windows):**
```powershell
rg "UseCase" --type kt -l | Select-String -NotMatch "Test"
```

**Cole a lista completa.**

---

### 5.2 Para CADA Use Case, responda:

**Template:**
```markdown
#### UseCase: [Nome]
- **Path:** [path completo]
- **Propósito:** [1 linha - o que faz]
- **Parâmetros de entrada:** [data class ou tipo]
- **Retorno:** [tipo de retorno]
- **Repository usado:** [qual repository injeta]
```

**Comando para ajudar:**
```bash
# Exemplo
rg "class.*UseCase|invoke|data class.*Params" --type kt ui/src/main/java/com/example/gestaobilhares/ui/mesas/usecases/RegistrarTrocaPanoUseCase.kt -C 5
```

---

## 📦 SEÇÃO 6: REPOSITORIES

### 6.1 Listar Todos os Repositories
**Execute:**
```bash
rg "class.*Repository" --type kt -l
```

**Cole a lista completa.**

---

### 6.2 Para CADA Repository, responda:

**Template:**
```markdown
#### Repository: [Nome]
- **Path:** [path completo]
- **DAOs utilizados:** [liste todos os DAOs injetados]
- **Funções principais:** [liste 10-15 funções públicas com assinatura]
```

**Comando para ajudar:**
```bash
# Exemplo
rg "private val.*Dao|suspend fun|fun " --type kt data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt | head -50
```

---

## 🖼️ SEÇÃO 7: FRAGMENTS

### 7.1 Listar Todos os Fragments
**Execute:**
```bash
rg "class.*Fragment" --type kt -l
```

**Cole a lista completa.**

---

### 7.2 Para CADA Fragment, responda:

**Template:**
```markdown
#### Fragment: [Nome]
- **Path:** [path completo]
- **ViewModel associado:** [qual ViewModel usa]
- **Propósito:** [1 linha - qual tela/funcionalidade]
- **Navegação:**
  - **Vem de:** [qual(is) tela(s) navegam para este Fragment]
  - **Vai para:** [qual(is) tela(s) este Fragment navega]
```

---

## 🔄 SEÇÃO 8: ADAPTERS

### 8.1 Listar Todos os Adapters
**Execute:**
```bash
rg "class.*Adapter" --type kt -l
```

**Cole a lista completa.**

---

### 8.2 Para CADA Adapter, responda:

**Template:**
```markdown
#### Adapter: [Nome]
- **Path:** [path completo]
- **Data class exibida:** [qual data class o adapter recebe]
- **Layout XML usado:** [qual arquivo de layout infla]
- **ViewHolder:** [nome da classe ViewHolder]
```

**Comando para ajudar:**
```bash
# Exemplo
rg "class.*Adapter|inflate|bind.*:" --type kt ui/src/main/java/com/example/gestaobilhares/ui/mesas/MesasReformadasAdapter.kt -C 3
```

---

## 🎯 SEÇÃO 9: ENUMS E TIPOS

### 9.1 Listar Todos os Enums
**Execute:**
```bash
rg "enum class" --type kt -l
```

**Cole a lista completa.**

---

### 9.2 Para CADA Enum, responda:

**Template:**
```markdown
#### Enum: [Nome]
- **Path:** [path completo]
- **Valores:** [liste TODOS os valores do enum]
```

**Comando para ajudar:**
```bash
# Exemplo
rg "enum class|^    [A-Z_]+" --type kt data/src/main/java/com/example/gestaobilhares/data/entities/TipoManutencao.kt
```

---

## 🔍 SEÇÃO 10: LOGS DE DEBUG

### 10.1 Listar Todas as Tags de Log
**Execute:**
```bash
rg 'Log\.[dwe]\("([^"]+)"' --type kt -o -r '$1' | sort | uniq
```

**Se não funcionar, use:**
```bash
rg 'Log\.d\(|Log\.e\(|Log\.w\(' --type kt -C 0 | grep -o '"[A-Z_]*"' | sort | uniq
```

**Cole a lista completa e para cada tag, descreva brevemente o propósito.**

---

## 📜 SEÇÃO 11: SCRIPTS

### 11.1 Listar Scripts Disponíveis
**Execute:**
```bash
ls scripts/*.ps1
```

**Windows alternativo:**
```powershell
Get-ChildItem scripts/*.ps1 | Select-Object Name
```

**Cole a lista completa.**

---

### 11.2 Para CADA Script, responda:

**Template:**
```markdown
#### Script: [Nome.ps1]
- **Propósito:** [leia o cabeçalho/comentários e resuma]
- **Parâmetros:** [se houver]
- **Exemplo de uso:** [comando exemplo]
```

---

## 🏗️ SEÇÃO 12: COMANDOS GRADLE

### 12.1 Módulos do Projeto
**Execute:**
```bash
rg "include\(" settings.gradle.kts
```

**Cole o resultado.**

---

### 12.2 Dependências Principais (app module)
**Execute:**
```bash
rg 'implementation\(' app/build.gradle.kts | grep -v "^//"
```

**Cole TODAS as dependências.**

---

## 🐛 SEÇÃO 13: PROBLEMAS CONHECIDOS

### 13.1 Listar Relatórios de Correção
**Execute:**
```bash
ls *.md | grep -i "relatorio\|correcao\|diagnostico"
```

**Windows:**
```powershell
Get-ChildItem *.md | Where-Object {$_.Name -match "RELATORIO|CORRECAO|DIAGNOSTICO"} | Select-Object Name
```

**Cole a lista completa.**

---

### 13.2 Para os 5 relatórios mais recentes, resuma:

**Template:**
```markdown
#### Relatório: [Nome do arquivo]
- **Data:** [data do arquivo]
- **Problema:** [1-2 linhas]
- **Solução:** [1-2 linhas]
- **Arquivos modificados:** [lista de arquivos]
```

---

## 🧪 SEÇÃO 14: DADOS DE TESTE

### 14.1 Procurar Dados de Teste no Código
**Execute:**
```bash
rg "846967|ADELSON|Mesa 100|Mesa 333" --type kt -C 2
```

**Cole exemplos de dados de teste encontrados.**

---

## 🎨 SEÇÃO 15: LAYOUTS E RECURSOS

### 15.1 Listar Layouts XML Principais
**Execute:**
```bash
ls app/src/main/res/layout/*.xml | grep -E "fragment|activity|item_"
```

**Windows:**
```powershell
Get-ChildItem app/src/main/res/layout/*.xml | Where-Object {$_.Name -match "fragment|activity|item_"} | Select-Object Name
```

**Cole a lista.**

---

## 📊 SEÇÃO 16: FLUXOS DE DADOS

### 16.1 Mapear Fluxo: Criar Acerto com Troca de Pano

**Responda seguindo o código:**
```markdown
#### Fluxo: Criar Acerto com Troca de Pano

1. **Fragment inicial:** [qual Fragment]
2. **Ação do usuário:** [o que usuário faz]
3. **ViewModel chamado:** [qual função]
4. **Repository/UseCase:** [qual função]
5. **DAO executado:** [qual query]
6. **Entidade afetada:** [qual tabela]
7. **Navegação final:** [para onde vai]
```

**Comando para ajudar:**
```bash
rg "trocarPano|registrarTrocaPano" --type kt -C 5
```

---

### 16.2 Mapear Fluxo: Visualizar Cards de Reforma

**Mesmo formato acima.**

**Comando para ajudar:**
```bash
rg "carregarMesasReformadas|ReformaCard" --type kt -C 5
```

---

## ✅ SEÇÃO 17: CHECKLIST FINAL

### 17.1 Verificações de Arquitetura
**Confirme no código:**
- [ ] Projeto usa MVVM? (ViewModels + LiveData/StateFlow)
- [ ] Usa Hilt para DI? (busque @HiltViewModel, @Inject)
- [ ] Usa Room como banco? (busque @Database)
- [ ] Usa Coroutines? (busque suspend fun)
- [ ] Usa Navigation Component? (busque navigation.xml)

**Execute para confirmar:**
```bash
rg "@HiltViewModel|@Inject|@Database|suspend fun|navigation.xml" --type kt -l | wc -l
```

---

## 📦 ENTREGA FINAL

Organize TODAS as respostas acima no formato:

```markdown
# 📘 PROJECT_CONTEXT_FULL - Gestão Bilhares

> **Gerado em:** [data/hora atual]  
> **Versão:** 1.0  
> **Tamanho:** [número de linhas] linhas

---

[Cole TODAS as seções com respostas organizadas]

---

**FIM DO DOCUMENTO**
```

**Arquivo de saída:** `PROJECT_CONTEXT_FULL.md`

**Critérios de qualidade:**
- ✅ Mínimo 500 linhas
- ✅ Zero placeholders ("...", "etc")
- ✅ Todos os paths exatos (copiados dos comandos)
- ✅ Todas as listas completas (não resumidas)
- ✅ Comandos que falharam documentados com erro

**Tempo estimado:** 30-45 minutos

---

**IMPORTANTE:** Se algum comando falhar, documente assim:
```markdown
❌ **Comando falhou:**
```bash
[comando que falhou]
```

**Erro:**
```
[mensagem de erro]
```

**Alternativa usada:**
```bash
[comando alternativo]
```
```
