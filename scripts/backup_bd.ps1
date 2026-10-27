<#
    Respaldo local de la base de datos embebida del POS (H2).
    A diferencia de la version anterior (PostgreSQL + pg_dump), ya no hace falta ningun
    servidor de base de datos ni credenciales: el respaldo se genera con el comando SQL
    "SCRIPT" de H2 a traves de una conexion JDBC corta, lo que produce una foto consistente
    aunque la app este abierta en ese momento. Usa las clases de H2 que ya estan empaquetadas
    dentro del propio jar de la app, asi que no requiere instalar nada adicional.

    Uso manual:     .\backup_bd.ps1
    Uso programado: ver instalar_tarea_backup.ps1 (crea la tarea diaria en Windows).
#>

$ErrorActionPreference = "Stop"

# Ruta del jar de la app instalada (contiene las clases de H2 necesarias para el respaldo).
# Ajustar esta ruta si se instalo en otra ubicacion (revisar la carpeta de instalacion real).
$jarApp = "C:\Program Files\POS Carniceria\app\pos-dontoribio.jar"

$carpetaScript = Split-Path -Parent $MyInvocation.MyCommand.Path
$carpetaBackups = Join-Path $carpetaScript "..\backups"
$diasRetencion = 30

# Misma ubicacion que usa DatabaseConnection.java para la base de datos de esta instalacion.
$carpetaDatos = Join-Path $env:APPDATA "DonToribioPOS\data"
$archivoDb = Join-Path $carpetaDatos "dontoribio"

if (-not (Test-Path "$archivoDb.mv.db")) {
    Write-Error "No se encontro la base de datos en $archivoDb.mv.db. Corre este script en la misma PC donde esta instalado el POS."
    exit 1
}
if (-not (Test-Path $jarApp)) {
    Write-Error "No se encontro el jar de la app en '$jarApp'. Edita la variable `$jarApp en este script con la ruta real de instalacion."
    exit 1
}

New-Item -ItemType Directory -Force -Path $carpetaBackups | Out-Null

$marcaTiempo = Get-Date -Format "yyyy-MM-dd_HHmmss"
$archivoSalida = Join-Path $carpetaBackups "dontoribio_$marcaTiempo.sql.zip"
$rutaJdbc = "jdbc:h2:file:$($archivoDb.Replace('\', '/'));MODE=PostgreSQL;AUTO_SERVER=TRUE"
$archivoSalidaUrl = $archivoSalida.Replace('\', '/')

& java -cp $jarApp org.h2.tools.Shell -url $rutaJdbc -user sa -sql "SCRIPT TO '$archivoSalidaUrl' COMPRESSION ZIP" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "El respaldo con H2 Shell termino con codigo $LASTEXITCODE"
}

Write-Output "Respaldo creado: $archivoSalida"

# Limpieza de respaldos viejos
$limite = (Get-Date).AddDays(-$diasRetencion)
Get-ChildItem -Path $carpetaBackups -Filter "*.sql.zip" | Where-Object { $_.LastWriteTime -lt $limite } | ForEach-Object {
    Write-Output "Borrando respaldo viejo: $($_.Name)"
    Remove-Item $_.FullName -Force
}
