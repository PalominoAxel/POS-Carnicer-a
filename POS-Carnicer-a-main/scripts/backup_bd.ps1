<#
    Respaldo local de la base de datos del POS (dontoribio_pos).
    Lee la conexion desde src\main\resources\application.properties (el archivo real,
    con las credenciales, no el .example) y genera un dump con pg_dump en la carpeta
    "backups" del proyecto. Borra respaldos de mas de 30 dias para no llenar el disco.

    Uso manual:   .\backup_bd.ps1
    Uso programado: ver instalar_tarea_backup.ps1 (crea la tarea diaria en Windows).
#>

$ErrorActionPreference = "Stop"

$carpetaScript = Split-Path -Parent $MyInvocation.MyCommand.Path
$rutaProperties = Join-Path $carpetaScript "..\src\main\resources\application.properties"
$carpetaBackups = Join-Path $carpetaScript "..\backups"
$diasRetencion = 30

if (-not (Test-Path $rutaProperties)) {
    Write-Error "No se encontro $rutaProperties. Este script debe correr desde una copia del proyecto con application.properties ya configurado."
    exit 1
}

# application.properties es un archivo de propiedades Java (clave=valor); lo parseamos a mano.
$propiedades = @{}
Get-Content $rutaProperties | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $propiedades[$Matches[1].Trim()] = $Matches[2].Trim()
    }
}

if ($propiedades["db.url"] -notmatch 'jdbc:postgresql://([^:/]+):(\d+)/(\w+)') {
    Write-Error "No se pudo interpretar db.url ('$($propiedades['db.url'])') en $rutaProperties"
    exit 1
}
$dbHost = $Matches[1]
$dbPort = $Matches[2]
$dbNombre = $Matches[3]
$dbUsuario = $propiedades["db.user"]

if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue)) {
    Write-Error "pg_dump no esta en el PATH. Agrega la carpeta 'bin' de la instalacion de PostgreSQL (ej. C:\Program Files\PostgreSQL\16\bin) al PATH del sistema."
    exit 1
}

New-Item -ItemType Directory -Force -Path $carpetaBackups | Out-Null

$marcaTiempo = Get-Date -Format "yyyy-MM-dd_HHmmss"
$archivoSalida = Join-Path $carpetaBackups "$dbNombre`_$marcaTiempo.backup"

$env:PGPASSWORD = $propiedades["db.password"]
try {
    & pg_dump --host=$dbHost --port=$dbPort --username=$dbUsuario --format=custom --file=$archivoSalida $dbNombre
    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump termino con codigo $LASTEXITCODE"
    }
} finally {
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Output "Respaldo creado: $archivoSalida"

# Limpieza de respaldos viejos
$limite = (Get-Date).AddDays(-$diasRetencion)
Get-ChildItem -Path $carpetaBackups -Filter "*.backup" | Where-Object { $_.LastWriteTime -lt $limite } | ForEach-Object {
    Write-Output "Borrando respaldo viejo: $($_.Name)"
    Remove-Item $_.FullName -Force
}
