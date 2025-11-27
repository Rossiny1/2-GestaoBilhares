$content = Get-Content -Path "data/src/main/java/com/example/gestaobilhares/data/repository/domain/SyncRepository.kt" -Raw
$content = $content -replace "return@forEach", "continue"
Set-Content -Path "data/src/main/java/com/example/gestaobilhares/data/repository/domain/SyncRepository.kt" -Value $content

