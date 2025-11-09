# ✅ FASE 12.12: Documentação CI/CD Pipeline

## 📋 Visão Geral

Pipeline de CI/CD implementada para automatizar testes, análise de código e deploy do projeto Gestão Bilhares.

## 🔧 Configuração

### GitHub Actions

O projeto usa GitHub Actions para CI/CD. Os workflows estão em `.github/workflows/`:

1. **`ci-cd.yml`**: Pipeline principal
   - Testes unitários
   - Testes instrumentados (Android)
   - Análise de código (Lint)
   - Build de APK (Debug e Release)
   - Deploy automático (quando em main/master)

2. **`code-quality.yml`**: Análise de qualidade
   - Análise estática de código
   - Verificação de segurança
   - Estatísticas de código

### Scripts Locais

Scripts PowerShell para executar pipeline localmente:

- **`scripts/ci-run-tests.ps1`**: Executa testes e build localmente
- **`scripts/ci-analyze-code.ps1`**: Análise de qualidade de código

## 🚀 Como Usar

### Executar Pipeline Localmente

```powershell
# Executar todos os testes e build
.\scripts\ci-run-tests.ps1

# Análise de código
.\scripts\ci-analyze-code.ps1
```

### GitHub Actions

**✅ RECOMENDAÇÃO**: A pipeline executa automaticamente quando:
- **Push para `main`, `master` ou `develop`** (quando código é enviado ao repositório)
- **Pull Request para `main`, `master` ou `develop`** (antes de merge - essencial!)
- **Manualmente via GitHub Actions UI** (quando necessário)

**❌ NÃO executa em commits locais** (antes do push):
- Commits locais não disparam a pipeline (economiza recursos)
- Use scripts locais (`ci-run-tests.ps1`) para validar antes de fazer push
- Isso permite múltiplos commits locais sem consumir recursos do GitHub Actions

**💡 Boa Prática**:
1. Faça commits locais normalmente
2. Execute `.\scripts\ci-run-tests.ps1` antes de fazer push
3. Faça push - a pipeline executará automaticamente
4. A pipeline também executa em Pull Requests (antes de merge)

## 📊 Jobs da Pipeline

### 1. Testes Unitários
- Executa todos os testes unitários
- Publica resultados
- Upload de relatórios

### 2. Testes Instrumentados
- Executa testes Android em emulador
- API Level 29 (Android 10)
- Upload de relatórios

### 3. Análise de Código
- Android Lint
- Verificação de erros críticos
- Upload de relatórios HTML

### 4. Build APK
- Build APK Debug
- Build APK Release (requer keystore configurado)
- Análise de tamanho do APK
- Upload de APKs como artifacts

### 5. Deploy (Opcional)
- Cria release no GitHub quando há tag
- Anexa APK Release

## 🔐 Configuração de Secrets

Para build de Release, configure os seguintes secrets no GitHub:

- `KEYSTORE_PASSWORD`: Senha do keystore
- `KEY_PASSWORD`: Senha da chave

## ⚠️ Quando a Pipeline Executa?

### ✅ Executa Automaticamente:
- **Push para branches principais**: Quando você faz `git push` para `main`, `master` ou `develop`
- **Pull Requests**: Quando alguém abre um PR para essas branches (antes de merge)
- **Manual**: Via GitHub Actions UI quando você quiser

### ❌ NÃO Executa:
- **Commits locais**: Commits que ainda não foram enviados (`git commit` sem `git push`)
- **Commits em branches feature**: A menos que você faça push ou abra PR

### 💡 Por Que Não Executar em Cada Commit Local?

1. **Economia de Recursos**: GitHub Actions tem limites de minutos gratuitos
2. **Velocidade**: Permite múltiplos commits locais sem esperar pipeline
3. **Flexibilidade**: Você pode fazer vários commits antes de validar
4. **Boas Práticas**: Valide localmente antes de fazer push

### 🔄 Fluxo Recomendado:

```
1. Desenvolver código localmente
2. Fazer commits locais (git commit)
3. Executar testes locais: .\scripts\ci-run-tests.ps1
4. Se tudo OK, fazer push (git push)
5. Pipeline executa automaticamente no GitHub
6. Se PR, pipeline executa antes de merge
```

## 📈 Melhorias Futuras

- [ ] Integração com SonarQube
- [ ] Análise de cobertura de testes
- [ ] Deploy automático para Google Play (requer credenciais)
- [ ] Notificações via Slack/Email
- [ ] Cache de dependências Gradle

## 🐛 Troubleshooting

### Pipeline falha nos testes
- Verificar logs do job específico
- Executar testes localmente: `.\gradlew test`
- Verificar dependências e configurações

### Build de Release falha
- Verificar se secrets estão configurados
- Verificar se keystore existe e está configurado
- Verificar assinatura do APK

### Testes instrumentados falham
- Verificar se emulador está disponível
- Verificar configuração do Android SDK
- Verificar permissões do app

