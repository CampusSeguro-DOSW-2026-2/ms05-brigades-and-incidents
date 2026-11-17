# Cambia el estado de un evento de prueba, como lo haria MS06.
#   .\evento.ps1 -Evento real -Estado FINALIZADO
#   .\evento.ps1 -Evento simulacro -Estado EN_EJECUCION
param(
    [Parameter(Mandatory)][ValidateSet('real', 'simulacro')][string]$Evento,
    [Parameter(Mandatory)][ValidateSet('PROGRAMADO', 'EN_EJECUCION', 'FINALIZADO')][string]$Estado
)
$ErrorActionPreference = 'Stop'
$eventos = Get-Content -Raw -Encoding UTF8 (Join-Path $PSScriptRoot 'sql\eventos-prueba.json') | ConvertFrom-Json
$datos = if ($Evento -eq 'real') { $eventos[0] } else { $eventos[1] }
$datos.estado = $Estado
$json = $datos | ConvertTo-Json -Depth 5 -Compress
Invoke-RestMethod -Method Put -Uri "http://localhost:8085/api/v1/bei/eventos/$($datos.eventoId)/estado" `
    -Headers @{ 'X-Service-Name' = 'ms06' } -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($json)) | Out-Null
Write-Host "Evento '$Evento' ahora esta en $Estado."
