<#
.SYNOPSIS
  Saca una copia completa de la base de datos de la demo (Aiven) a un archivo comprimido en tu equipo.

.DESCRIPTION
  Usa mysqldump (viene con MySQL Server / Workbench). Pide la contraseña en pantalla: no se guarda en ningún archivo.
  La copia queda en la carpeta "respaldos" (ignorada por git: contiene datos personales y hashes de contraseñas).
  Úsala antes de cambios grandes (migraciones, cargas de datos) o cuando quieras una copia propia además de la automática.

.EXAMPLE
  .\scripts\respaldar-aiven.ps1
  .\scripts\respaldar-aiven.ps1 -Base defaultdb -Carpeta D:\respaldos
#>
param(
    [string]$Servidor = "mysql-a2a837-juangabriellondonoarias-b256.k.aivencloud.com",
    [int]$Puerto = 19261,
    [string]$Usuario = "avnadmin",
    [string]$Base = "defaultdb",
    [string]$Carpeta = (Join-Path $PSScriptRoot "..\respaldos")
)

$ErrorActionPreference = "Stop"

# 1) Buscar mysqldump
$mysqldump = (Get-Command mysqldump -ErrorAction SilentlyContinue).Source
if (-not $mysqldump) {
    $mysqldump = Get-ChildItem "C:\Program Files\MySQL", "C:\Program Files (x86)\MySQL" -Recurse -Filter mysqldump.exe -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $mysqldump) { throw "No encontré mysqldump. Instala MySQL Server o Workbench, o agrega su carpeta bin al PATH." }
Write-Host "Usando: $mysqldump"

# 2) Contraseña (solo en memoria, solo para este proceso)
$segura = Read-Host "Contraseña de $Usuario en Aiven" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
try { $env:MYSQL_PWD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }

# 3) Copia
New-Item -ItemType Directory -Force -Path $Carpeta | Out-Null
$sello = Get-Date -Format "yyyyMMdd-HHmm"
$sql = Join-Path $Carpeta "respaldo-aiven-$sello.sql"
$gz = "$sql.gz"

try {
    & $mysqldump --host=$Servidor --port=$Puerto --user=$Usuario --ssl-mode=REQUIRED `
        --single-transaction --routines --triggers --events --set-gtid-purged=OFF --no-tablespaces `
        --result-file=$sql $Base
    if ($LASTEXITCODE -ne 0) { throw "mysqldump terminó con error $LASTEXITCODE" }
}
finally { Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue }

if ((Get-Item $sql).Length -lt 5000) { Remove-Item $sql; throw "La copia salió casi vacía; revisa la conexión y la contraseña." }

# 4) Comprimir
$entrada = [IO.File]::OpenRead($sql)
$salida = [IO.File]::Create($gz)
$zip = New-Object IO.Compression.GZipStream($salida, [IO.Compression.CompressionLevel]::Optimal)
try { $entrada.CopyTo($zip) } finally { $zip.Dispose(); $salida.Dispose(); $entrada.Dispose() }
Remove-Item $sql

Write-Host ""
Write-Host "Listo: $gz ($([math]::Round((Get-Item $gz).Length / 1KB)) KB)" -ForegroundColor Green
Write-Host "Guárdala fuera de este equipo también (disco externo o nube privada). Contiene datos personales." -ForegroundColor Yellow
