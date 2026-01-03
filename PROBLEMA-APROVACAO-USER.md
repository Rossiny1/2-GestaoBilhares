# ⚠️ PROBLEMA CONHECIDO: Aprovação de User não sincroniza com Firestore

## Status
**PENDENTE DE CONSERTO**

## Descrição
A aprovação de usuários (User) funciona corretamente no banco de dados local (Room), mas não está sincronizando com o Firestore (nuvem).

## Comportamento Esperado
1. Admin aprova colaborador no app
2. Status `aprovado` é atualizado localmente ✅
3. Status `aprovado` é sincronizado para Firestore ❌ (não está funcionando)

## Possíveis Causas
1. **Falta de Firebase UID**: Colaboradores aprovados sem credenciais podem não ter `firebaseUid`, impedindo sincronização no novo schema
2. **Problema de Permissões**: Regras do Firestore podem estar bloqueando a atualização
3. **Erro Silencioso**: Exceção pode estar sendo capturada e não reportada

## Logs Adicionados
Foram adicionados logs detalhados em:
- `ColaboradorManagementViewModel.sincronizarColaboradorParaFirestore()`
- `ColaboradorManagementViewModel.prepararDadosColaboradorParaFirestore()`

## Próximos Passos
1. Verificar logs do app ao aprovar usuário
2. Confirmar se colaborador tem `firebaseUid` quando aprovado
3. Verificar se erro de permissão está sendo lançado
4. Considerar usar "Aprovar com Credenciais" que cria Firebase UID automaticamente

## Data
2025-01-02
