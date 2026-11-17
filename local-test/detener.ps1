# Apaga el entorno local.
#   .\detener.ps1          detiene los servicios y las bases (conserva los datos)
#   .\detener.ps1 -Borrar  ademas borra las bases de datos (la proxima vez arrancan desde cero)
param([switch]$Borrar)

$here = $PSScriptRoot
$archivo = Join-Path $here 'logs\pids.json'

if (Test-Path $archivo) {
    $pids = Get-Content -Raw $archivo | ConvertFrom-Json
    foreach ($p in $pids.PSObject.Properties) {
        # /T cierra tambien los procesos hijos (npm -> node de Vite)
        & taskkill.exe /PID $p.Value /T /F 2>$null | Out-Null
        Write-Host "  $($p.Name) detenido"
    }
    Remove-Item $archivo
} else {
    Write-Host '  No hay servicios registrados en logs\pids.json'
}

if ($Borrar) {
    docker compose -f (Join-Path $here 'docker-compose.yml') down -v
} else {
    docker compose -f (Join-Path $here 'docker-compose.yml') down
}
