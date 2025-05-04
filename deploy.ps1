# Script de déploiement automatique
Write-Host "Démarrage du processus de déploiement de PrivateMines..." -ForegroundColor Green

# Compilation du plugin
Write-Host "Compilation du plugin..." -ForegroundColor Yellow
mvn clean package

# Vérification de la compilation
if ($LASTEXITCODE -ne 0) {
    Write-Host "Erreur lors de la compilation. Arrêt du déploiement." -ForegroundColor Red
    exit 1
}

# Chemin du fichier .jar généré
$jarPath = "$PWD\target\PrivateMines-3.0-HOTFIX.jar"

# Vérification de l'existence du fichier
if (-not (Test-Path $jarPath)) {
    Write-Host "Le fichier .jar n'a pas été trouvé. Arrêt du déploiement." -ForegroundColor Red
    exit 1
}

# Transfert du fichier via WinSCP
Write-Host "Transfert du fichier vers le serveur..." -ForegroundColor Yellow
& "C:\Program Files (x86)\WinSCP\WinSCP.com" /command `
    "option batch abort" `
    "option confirm off" `
    "open -hostkey=* sftp://root:7pVXtp!b=s!S@185.142.53.62" `
    "put $jarPath /root/test/plugins/" `
    "exit"

# Vérification du transfert
if ($LASTEXITCODE -ne 0) {
    Write-Host "Erreur lors du transfert du fichier." -ForegroundColor Red
    exit 1
}

Write-Host "Déploiement terminé avec succès!" -ForegroundColor Green

# Création d'un fichier temporaire pour le script bash avec le bon format Unix (LF)
$restartScriptContent = @"
#!/bin/bash
echo "Arrêt du serveur actuel..."
screen -S minecraft -X quit
sleep 2
echo "Nettoyage des sessions screen mortes..."
screen -wipe > /dev/null 2>&1
echo "Redémarrage du serveur dans un nouveau screen..."
cd /root/test
screen -dmS minecraft java -Xms2048M -Xmx4096M --add-modules=jdk.incubator.vector -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:+UseStringDeduplication -jar paper-1.21.4-222.jar --nogui
echo "Serveur redémarré avec succès !"
"@

# Écriture du script dans un fichier temporaire
$tempScriptPath = "$env:TEMP\restart_minecraft.sh"
$restartScriptContent | Out-File -FilePath $tempScriptPath -Encoding ASCII -NoNewline

# Convertir les retours à la ligne Windows (CRLF) en retours à la ligne Unix (LF)
$content = [System.IO.File]::ReadAllText($tempScriptPath).Replace("`r`n", "`n")
[System.IO.File]::WriteAllText($tempScriptPath, $content)

# Transfert et exécution du script bash
Write-Host "Redémarrage du serveur..." -ForegroundColor Yellow
& "C:\Program Files (x86)\WinSCP\WinSCP.com" /command `
    "option batch abort" `
    "option confirm off" `
    "open -hostkey=* sftp://root:7pVXtp!b=s!S@185.142.53.62" `
    "put -transfer=binary -permissions=755 $tempScriptPath /root/restart_minecraft.sh" `
    "call bash /root/restart_minecraft.sh" `
    "call rm /root/restart_minecraft.sh" `
    "exit"

# Suppression du script temporaire local
Remove-Item -Path $tempScriptPath -Force

Write-Host "Serveur redémarré avec succès!" -ForegroundColor Green 