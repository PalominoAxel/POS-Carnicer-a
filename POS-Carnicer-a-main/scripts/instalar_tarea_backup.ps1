<#
    Registra una tarea programada de Windows (el equivalente a un cron job) que corre
    backup_bd.ps1 todos los dias a las 23:30. Correr este script UNA SOLA VEZ, como
    administrador, para instalar la tarea.

    Para revisar la tarea despues: Get-ScheduledTask -TaskName "POS Don Toribio - Backup diario"
    Para quitarla: Unregister-ScheduledTask -TaskName "POS Don Toribio - Backup diario"
#>

$ErrorActionPreference = "Stop"

$carpetaScript = Split-Path -Parent $MyInvocation.MyCommand.Path
$rutaBackupScript = Join-Path $carpetaScript "backup_bd.ps1"
$nombreTarea = "POS Don Toribio - Backup diario"

$accion = New-ScheduledTaskAction -Execute "powershell.exe" `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$rutaBackupScript`""
$disparador = New-ScheduledTaskTrigger -Daily -At "23:30"
$configuracion = New-ScheduledTaskSettingsSet -StartWhenAvailable -RunOnlyIfNetworkAvailable:$false

Register-ScheduledTask -TaskName $nombreTarea -Action $accion -Trigger $disparador `
    -Settings $configuracion -Description "Respaldo diario de la base de datos dontoribio_pos (pg_dump)." `
    -Force

Write-Output "Tarea '$nombreTarea' instalada. Correra todos los dias a las 23:30."
