# Script pour récupérer les logs du serveur Minecraft
Write-Host "Récupération des logs du serveur PrivateMines..." -ForegroundColor Green

# Dossier de destination pour les logs
$localLogsDir = "$PWD\server_logs"

# Création du dossier s'il n'existe pas
if (-not (Test-Path $localLogsDir)) {
    New-Item -ItemType Directory -Path $localLogsDir | Out-Null
    Write-Host "Dossier de logs créé: $localLogsDir" -ForegroundColor Yellow
}

# Nom du fichier de log avec timestamp
$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$localLogFile = "$localLogsDir\logs_$timestamp.log"

# Récupération des logs via WinSCP
Write-Host "Connexion au serveur et récupération des logs..." -ForegroundColor Yellow
& "C:\Program Files (x86)\WinSCP\WinSCP.com" /command `
    "option batch abort" `
    "option confirm off" `
    "open -hostkey=* sftp://root:7pVXtp!b=s!S@185.142.53.62" `
    "get /root/test/logs/latest.log $localLogFile" `
    "exit"

# Vérification du transfert
if ($LASTEXITCODE -ne 0) {
    Write-Host "Erreur lors de la récupération des logs." -ForegroundColor Red
    exit 1
}

Write-Host "Logs récupérés avec succès: $localLogFile" -ForegroundColor Green

# Création d'un fichier temporaire pour le script bash qui récupérera les 100 dernières lignes des logs
$getLatestLogsContent = @"
#!/bin/bash
echo "Récupération des dernières entrées du log..."
cd /root/test
tail -n 100 logs/latest.log
"@

# Écriture du script dans un fichier temporaire
$tempScriptPath = "$env:TEMP\get_latest_logs.sh"
$getLatestLogsContent | Out-File -FilePath $tempScriptPath -Encoding ASCII -NoNewline

# Convertir les retours à la ligne Windows (CRLF) en retours à la ligne Unix (LF)
$content = [System.IO.File]::ReadAllText($tempScriptPath).Replace("`r`n", "`n")
[System.IO.File]::WriteAllText($tempScriptPath, $content)

# Transfert et exécution du script bash pour afficher les logs récents
Write-Host "Affichage des 100 dernières lignes de log:" -ForegroundColor Yellow
& "C:\Program Files (x86)\WinSCP\WinSCP.com" /command `
    "option batch abort" `
    "option confirm off" `
    "open -hostkey=* sftp://root:7pVXtp!b=s!S@185.142.53.62" `
    "put -transfer=binary -permissions=755 $tempScriptPath /root/get_latest_logs.sh" `
    "call bash /root/get_latest_logs.sh" `
    "call rm /root/get_latest_logs.sh" `
    "exit"

# Suppression du script temporaire local
Remove-Item -Path $tempScriptPath -Force

Write-Host ""
Write-Host "Chercher des erreurs liées à PrivateMines dans les logs:" -ForegroundColor Cyan
Select-String -Path $localLogFile -Pattern "PrivateMines.*error|error.*PrivateMines|Exception|PrivateMines.*Exception" -CaseSensitive:$false

Write-Host ""
Write-Host "Opération terminée!" -ForegroundColor Green 