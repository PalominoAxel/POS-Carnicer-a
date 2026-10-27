<#
    Genera el instalador nativo de Windows (.exe) del POS con jpackage, que incluye su
    propio runtime de Java empaquetado: quien lo instale no necesita tener Java instalado,
    solo descargar el .exe generado y correrlo con doble clic.

    jpackage viene incluido en el JDK (no es una dependencia nueva del proyecto); si "java"
    en el PATH no trae jpackage, ajusta la variable $jdkBin con el JDK que si lo tenga.

    Requiere, antes de correr este script, que NO exista un application.properties propio
    en src\main\resources (o que no tenga datos de desarrollo), para no empaquetar
    credenciales de desarrollo dentro del instalador distribuido a un comprador.

    Uso: .\build-installer.ps1 [-Version 1.0.0]
#>

param(
    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"

# JDK con jpackage a usar. Ajustar si el que esta en PATH no lo trae.
$jdkBin = "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin"

# jpackage --type exe necesita WiX Toolset (candle.exe/light.exe). Si no esta instalado como
# programa, se puede usar la version portable (binarios sueltos, sin instalador ni permisos
# de administrador): https://github.com/wixtoolset/wix3/releases -> wix311-binaries.zip,
# descomprimida en una carpeta cualquiera. Ajustar esta ruta segun donde haya quedado.
$wixBin = "C:\Users\axel\.local-tools\wix311"
if (Test-Path $wixBin) {
    $env:PATH = "$wixBin;$env:PATH"
}

$carpetaScript = Split-Path -Parent $MyInvocation.MyCommand.Path
$carpetaProyecto = Resolve-Path (Join-Path $carpetaScript "..")
$carpetaSalida = Join-Path $carpetaProyecto "target\installer"

$propPath = Join-Path $carpetaProyecto "src\main\resources\application.properties"
if (Test-Path $propPath) {
    Write-Warning "Existe $propPath. Se incluira dentro del instalador si tiene credenciales de desarrollo."
    Write-Warning "Si no es intencional, muevelo/borralo antes de generar el instalador para distribuir."
}

Write-Output "=== 1/2: Empaquetando el jar (mvn package) ==="
$env:PATH = "C:\Users\axel\.local-maven\apache-maven-3.9.9\bin;$env:PATH"
Push-Location $carpetaProyecto
try {
    mvn -q package -DskipTests
} finally {
    Pop-Location
}

$jar = Join-Path $carpetaProyecto "target\pos-dontoribio.jar"
if (-not (Test-Path $jar)) {
    throw "No se genero $jar. Revisa la salida de mvn package."
}

Write-Output "=== 2/2: Generando el instalador con jpackage ==="
if (Test-Path $carpetaSalida) {
    Remove-Item -Recurse -Force $carpetaSalida
}
New-Item -ItemType Directory -Force -Path $carpetaSalida | Out-Null

& "$jdkBin\jpackage.exe" `
    --type exe `
    --input (Split-Path -Parent $jar) `
    --main-jar (Split-Path -Leaf $jar) `
    --main-class com.dontoribio.pos.Launcher `
    --name "POS Carniceria" `
    --app-version $Version `
    --vendor "Viande" `
    --win-shortcut `
    --win-menu `
    --dest $carpetaSalida

if ($LASTEXITCODE -ne 0) {
    throw "jpackage termino con codigo $LASTEXITCODE"
}

Write-Output "Instalador generado en: $carpetaSalida"
Get-ChildItem $carpetaSalida -Filter "*.exe"
