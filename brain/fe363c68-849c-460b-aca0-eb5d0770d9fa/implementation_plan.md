# Finalização da Sincronização: Padronização de Timestamps para Long

Este plano visa finalizar a implementação da sincronização, garantindo que todos os handlers utilizem `Long` para timestamps, resolvendo erros de compilação (especialmente KSP) e implementando métodos utilitários faltantes.

## Propostas de Mudanças

### Core Utils

#### [MODIFY] [DateUtils.kt](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/core/src/main/java/com/example/gestaobilhares/utils/DateUtils.kt)
- Implementar o método `convertToLong(value: Any?): Long?` para lidar com conversões de `Timestamp` (Firestore), `Date`, `Long` e `String`.

### Sync Handlers

#### [MODIFY] [EquipamentoSyncHandler.kt](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/sync/src/main/java/com/example/gestaobilhares/sync/handlers/EquipamentoSyncHandler.kt)
- Garantir que todos os campos de data utilizem `DateUtils.convertToLong`.

#### [MODIFY] [FinancasSyncHandler.kt](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/sync/src/main/java/com/example/gestaobilhares/sync/handlers/FinancasSyncHandler.kt)
- Atualizar `pullCategorias` e `pullTipos` para converter `dataAtualizacao` usando `DateUtils.convertToLong`.

#### [MODIFY] [RotaSyncHandler.kt](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/sync/src/main/java/com/example/gestaobilhares/sync/handlers/RotaSyncHandler.kt)
- Verificar e atualizar campos de timestamp.

#### [MODIFY] [VeiculoSyncHandler.kt](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/sync/src/main/java/com/example/gestaobilhares/sync/handlers/VeiculoSyncHandler.kt)
- Padronizar uso de `Long` para timestamps.

#### [MODIFY] [MetaSyncHandler.kt](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/sync/src/main/java/com/example/gestaobilhares/sync/handlers/MetaSyncHandler.kt)
- Padronizar uso de `Long` para timestamps.

#### [MODIFY] [SyncRepository.kt](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/sync/src/main/java/com/example/gestaobilhares/sync/SyncRepository.kt)
- Revisar conversões de data para garantir consistência com os handlers.

## Plano de Verificação

### Testes Automatizados
- Executar build do módulo sync:
  ```powershell
  ./gradlew :sync:assembleDebug
  ```
- Verificar se não há erros de KSP no log de saída.

### Verificação Manual
- O usuário deve tentar rodar o aplicativo e verificar se a sincronização de equipamentos, finanças e metas funciona sem erros de parse de data.
