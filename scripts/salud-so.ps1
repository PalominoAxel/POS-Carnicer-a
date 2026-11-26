param(
    [string]$JarName = "pos-dontoribio",
    [string]$HealthUrl = "http://localhost:8080/health"
)

Write-Host "=== SALUD DEL EQUIPO DE CAJA - DON TORIBIO POS ===" -ForegroundColor Cyan
Get-Date -Format "yyyy-MM-dd HH:mm:ss"

Write-Host "`n[1] CPU y Memoria RAM:" -ForegroundColor Yellow
Get-CimInstance Win32_Processor | Select-Object Name, LoadPercentage | Format-Table -AutoSize
$os = Get-CimInstance Win32_OperatingSystem
$ramUsadaGB = [math]::Round(($os.TotalVisibleMemorySize - $os.FreePhysicalMemory) / 1MB, 2)
$ramTotalGB = [math]::Round($os.TotalVisibleMemorySize / 1MB, 2)
Write-Host "RAM en uso: $ramUsadaGB GB / $ramTotalGB GB"

Write-Host "`n[2] Espacio en Disco (C:):" -ForegroundColor Yellow
Get-PSDrive C | Select-Object Name, @{Name="Libre(GB)";Expression={[math]::Round($_.Free/1GB,2)}}, @{Name="Usado(GB)";Expression={[math]::Round($_.Used/1GB,2)}} | Format-Table -AutoSize

Write-Host "`n[3] Procesos Java / POS:" -ForegroundColor Yellow
$procs = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like "*$JarName*" -or $_.Name -like "java*" }
if ($procs) {
    $procs | Select-Object ProcessId, Name, @{Name="Memoria(MB)";Expression={[math]::Round($_.WorkingSetSize/1MB,2)}} | Format-Table -AutoSize
} else {
    Write-Host "No hay un proceso Java/POS activo en este momento." -ForegroundColor Red
}

Write-Host "`n[4] Estado del Endpoint de Salud ($HealthUrl):" -ForegroundColor Yellow
try {
    $res = Invoke-WebRequest -Uri $HealthUrl -TimeoutSec 3 -UseBasicParsing
    Write-Host "HTTP $($res.StatusCode) -> $($res.Content)" -ForegroundColor Green
} catch {
    Write-Host "Sin respuesta en $HealthUrl" -ForegroundColor Red
}