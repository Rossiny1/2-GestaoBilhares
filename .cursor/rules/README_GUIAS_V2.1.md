# 📚 DOCUMENTAÇÃO V2.1 - DATA LINEAGE & RASTREAMENTO

## ✅ Arquivos Gerados

### 1. AI_GUIDE_V2.1.md [code_file:38]
**Para:** IAs de desenvolvimento (Cursor, Windsurf, etc.)  
**Substitui:** AI_GUIDE.md  
**Principal melhoria:** Gate 0 com Data Lineage Obrigatório (Origem -> Transformação -> Destino)

### 2. GUIA_DIAGNOSTICO_SENIOR_V2.md [code_file:39]
**Para:** Desenvolvedores humanos  
**Novo:** Conceito de "Ponto de Perda" e Taxonomia de Bugs  

---

## 🚀 COMO USAR (MIGRAÇÃO)

### Passo 1: Substituir os arquivos
```bash
# Substituir AI_GUIDE
cp AI_GUIDE_V2.1.md AI_GUIDE.md

# Adicionar Guia Sênior
cp GUIA_DIAGNOSTICO_SENIOR_V2.md docs/
```

### Passo 2: Instruir a IA
Na próxima conversa, use este prompt inicial:

```markdown
Anexos: PROJECT.md + AI_GUIDE.md

LEITURA OBRIGATÓRIA:
O arquivo AI_GUIDE.md foi atualizado para V2.1.
Agora o Gate 0 exige "Data Lineage" para qualquer bug de dados.
Você deve identificar Origem -> Transformação -> Destino e encontrar o Ponto de Perda.
```

### Passo 3: Code Review Humano
Ao revisar PRs ou soluções da IA, pergunte:
- "Cadê o Data Lineage?"
- "Onde está o Ponto de Perda identificado?"
- "Qual foi a evidência do Estado Anterior?"

---

## 🔥 O QUE MUDOU DA V2 PARA V2.1

| Recurso | V2.0 | V2.1 (Atual) |
|---|---|---|
| **Diagnóstico** | Obrigatório | Obrigatório + Data Lineage |
| **Foco** | Sintoma | Rastreamento do Dado |
| **Logs** | Estruturados | Estado Anterior + Posterior |
| **Taxonomia** | N/A | Guia Rápido por Tipo de Bug |
| **Anti-Loop** | Voltar ao Gate 0 | Identificar Ponto de Perda |

---

**Versão:** 2.1  
**Data:** 24/01/2026  
**Status:** ✅ Definitivo para Android Sênior
