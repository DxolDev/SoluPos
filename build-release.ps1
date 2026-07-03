# build-release.ps1
# Genera el AAB firmado de SoluPos para subir a Google Play.
# La contraseña del keystore se pide de forma segura y NO se guarda en ningún lado.
#
# Uso:  desde la carpeta del proyecto, ejecuta:  .\build-release.ps1

$ErrorActionPreference = "Stop"

# --- Rutas ---
$ProjectDir  = $PSScriptRoot
$KeystorePath = "C:\Users\DaveX\solupos-upload.jks"
$KeyAlias     = "solupos"
$JavaHome     = "C:\Program Files\Android\Android Studio\jbr"

if (-not (Test-Path $KeystorePath)) {
    Write-Host "ERROR: no se encontró el keystore en $KeystorePath" -ForegroundColor Red
    exit 1
}

# --- Contraseña (segura, no se muestra ni se guarda) ---
$sec = Read-Host "Contraseña del keystore" -AsSecureString
$plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))

# --- Variables de entorno solo para este proceso ---
$env:JAVA_HOME                 = $JavaHome
$env:SOLUPOS_KEYSTORE_PATH     = $KeystorePath
$env:SOLUPOS_KEY_ALIAS         = $KeyAlias
$env:SOLUPOS_KEYSTORE_PASSWORD = $plain
$env:SOLUPOS_KEY_PASSWORD      = $plain   # misma contraseña (pulsaste Enter al crearlo)

Write-Host "`nCompilando AAB de release firmado..." -ForegroundColor Cyan
& "$ProjectDir\gradlew.bat" -p "$ProjectDir" bundleRelease --console=plain

# --- Limpia la contraseña de memoria ---
$plain = $null
$env:SOLUPOS_KEYSTORE_PASSWORD = $null
$env:SOLUPOS_KEY_PASSWORD      = $null

$aab = "$ProjectDir\app\build\outputs\bundle\release\app-release.aab"
if (Test-Path $aab) {
    Write-Host "`n✅ AAB generado:" -ForegroundColor Green
    Write-Host "   $aab"
} else {
    Write-Host "`n⚠️  No se encontró el AAB. Revisa los errores de arriba." -ForegroundColor Yellow
}
