# Registra los dos eventos de prueba en MS05 (simulando la llamada de MS06) y carga brigadistas, asignaciones e
# incidentes de prueba. Se puede ejecutar cuantas veces se quiera: deja los datos como al inicio.
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot

$eventos = Get-Content -Raw -Encoding UTF8 (Join-Path $here 'sql\eventos-prueba.json') | ConvertFrom-Json
foreach ($evento in $eventos) {
    $json = $evento | ConvertTo-Json -Depth 5 -Compress
    Invoke-RestMethod -Method Put -Uri "http://localhost:8085/api/v1/bei/eventos/$($evento.eventoId)/estado" `
        -Headers @{ 'X-Service-Name' = 'ms06' } -ContentType 'application/json; charset=utf-8' `
        -Body ([Text.Encoding]::UTF8.GetBytes($json)) | Out-Null
    Write-Host "  Evento registrado: $($evento.nombre) ($($evento.estado))"
}

$sql = Join-Path $here 'sql\ms05-datos-prueba.sql'
docker cp $sql local-ms05-db:/tmp/ms05-datos-prueba.sql | Out-Null
docker exec local-ms05-db psql -q -v ON_ERROR_STOP=1 -U campusseguro -d brigades_incidents_db -f /tmp/ms05-datos-prueba.sql | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron cargar los datos de prueba de MS05.' }
Write-Host '  Brigadistas, asignaciones e incidentes de prueba cargados.'
