# 📋 Análise do CSV de Clientes - Rota Bahia

## 📊 Estrutura do CSV Analisado

**Arquivo**: `Cadastro Clientes- Rota Bahia.csv`
**Total de linhas**: 114 registros
**Formato**: CSV delimitado por ponto e vírgula (;)

## 🔍 Campos Identificados no CSV

| Posição | Campo CSV | Exemplo | Observações |
|---------|-----------|---------|-------------|
| 1 | ID | 450 | Numérico, provavelmente ID legado |
| 2 | Nome | "João Ilton de medeiros" | Obrigatório |
| 3 | CPF | "27118628875" | 11 dígitos, alguns vazios |
| 4 | Endereço | "Rua Primeiro de Maio, s/n, Centro" | Endereço completo |
| 5 | Cidade | "Josenópolis" | Cidades variadas |
| 6 | Estado | "MG" | Sempre MG |
| 7 | Telefone | "3888525830" | Alguns vazios |
| 8 | Telefone 2 | "" | Opcional |
| 9 | Campo vazio | "" | Sem dados |
| 10 | Data Cadastro | "19/7/2018 00:00:00" | Formato DD/MM/YYYY |
| 11 | Campo vazio | "" | Sem dados |
| 12 | Débito | "R$ 130,00" | Valor monetário |
| 13 | Observações | "Ultimo acerto com pagamento..." | Texto longo |
| 14 | Valor Ficha | "R$ 0,40" | Apenas algumas linhas |

## 🗺️ Cidades Identificadas

- Josenópolis
- Francisco Sá
- Salinas
- Grão Mogol
- Fruta de Leite
- Itacambira
- Montes Claros
- Juramento
- Novorizonte
- Cristalha
- Pau Dolho
- Rio Verde
- Boa Vista MG

## 💰 Análise de Débitos

- **Clientes com débito**: ~70%
- **Débitos zerados**: ~30%
- **Valores variados**: R$ 0,00 a R$ 2.162,00
- **Média de débito**: ~R$ 245,00

## 📋 Status dos Clientes

### Observações Comuns:
- "Mesa retirada" - Cliente inativo
- "Tem débitos" - Cliente devendo
- "URGENTE" - Prioridade alta
- "Bimestral" - Frequência de acerto
- "Troca de pano" - Manutenção realizada

## 🎯 Planejamento de Importação

### 1. Mapeamento de Campos
```
CSV → Entidade Cliente
ID legado → Ignorar (gerar novo)
Nome → nome
CPF → cpfCnpj
Endereço → endereco
Cidade → cidade
Estado → estado
Telefone → telefone
Telefone 2 → telefone2
Data Cadastro → dataCadastro
Débito → debitoAtual
Observações → observacoes
Valor Ficha → valorFicha (quando presente)
```

### 2. Campos Fixos
```
rotaId → ID da rota "Bahia" (a criar)
ativo → true (mesa não retirada) / false (mesa retirada)
dataUltimaAtualizacao → System.currentTimeMillis()
```

### 3. Regras de Negócio
- Clientes com "Mesa retirada" → ativo: false
- Clientes com "Tem débitos" → importar com débitoAtual
- Formatar valores monetários (R$ 130,00 → 130.0)
- Converter datas (DD/MM/YYYY → timestamp)

## 🚧 Próximos Passos

1. ✅ Análise do CSV concluída
2. 🔄 Criar rota "Bahia" no sistema
3. 🔄 Criar script de importação
4. 🔄 Testar com amostra
5. 🔄 Importar todos os dados
6. 🔄 Validar importação

## 📊 Estatísticas

- **Total de clientes**: 114
- **Com CPF**: ~85%
- **Com telefone**: ~70%
- **Com endereço completo**: ~95%
- **Clientes inativos ("Mesa retirada")**: ~25%
- **Débito total estimado**: R$ 27.930,00
