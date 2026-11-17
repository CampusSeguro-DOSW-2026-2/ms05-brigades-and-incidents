# Levanta el entorno local para probar la gestion de brigadistas y la auto asignacion:
#   Docker: bases de datos de MS01 (5442) y MS05 (5435)
#   Maquina: MS01 (8081), API Gateway (8080), MS05 (8085) y frontend Vite (5173)
# Uso:  .\iniciar.ps1             (usa los .jar ya compilados; los compila si no existen)
#       .\iniciar.ps1 -Compilar   (recompila MS01, Gateway y MS05 antes de arrancar)
param([switch]$Compilar)

$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$proyecto = (Resolve-Path (Join-Path $here '..\..')).Path
$logs = Join-Path $here 'logs'
New-Item -ItemType Directory -Force $logs | Out-Null

# Clave de servicio solo para este entorno local (minimo 32 caracteres)
$claveMs05 = 'clave-local-ms05-solo-para-pruebas-0123456789'

$servicios = @(
    @{ Nombre = 'ms01';    Dir = Join-Path $proyecto 'Login\login'; Jar = 'identity-service-0.0.1-SNAPSHOT.jar'; Puerto = 8081 },
    @{ Nombre = 'gateway'; Dir = Join-Path $proyecto 'ApiGateway';  Jar = 'gateway-service-0.0.1-SNAPSHOT.jar';  Puerto = 8080 },
    @{ Nombre = 'ms05';    Dir = Join-Path $proyecto 'Brigadistas'; Jar = 'brigades-and-incidents-0.0.1-SNAPSHOT.jar'; Puerto = 8085 }
)

function Esperar-Salud([string]$nombre, [int]$puerto, [int]$segundos = 150) {
    $limite = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $limite) {
        try {
            $r = Invoke-WebRequest -UseBasicParsing -TimeoutSec 3 "http://localhost:$puerto/actuator/health"
            if ($r.StatusCode -eq 200) { Write-Host "  $nombre listo en :$puerto"; return }
        } catch { }
        Start-Sleep -Seconds 2
    }
    throw "$nombre no respondio en :$puerto. Revisa logs\$nombre.log y logs\$nombre.err.log"
}

function Iniciar-Java([hashtable]$svc, [hashtable]$variables) {
    $jar = Join-Path $svc.Dir "target\$($svc.Jar)"
    foreach ($k in $variables.Keys) { [Environment]::SetEnvironmentVariable($k, $variables[$k], 'Process') }
    $p = Start-Process -FilePath 'java' -ArgumentList @('-jar', "`"$jar`"") -WorkingDirectory $svc.Dir `
        -RedirectStandardOutput (Join-Path $logs "$($svc.Nombre).log") `
        -RedirectStandardError (Join-Path $logs "$($svc.Nombre).err.log") -WindowStyle Hidden -PassThru
    foreach ($k in $variables.Keys) { [Environment]::SetEnvironmentVariable($k, $null, 'Process') }
    return $p
}

Write-Host '1. Bases de datos (Docker)...'
docker compose -f (Join-Path $here 'docker-compose.yml') up -d --wait
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron levantar las bases de datos. Esta Docker Desktop abierto?' }

Write-Host '2. Compilando lo que haga falta...'
foreach ($svc in $servicios) {
    $jar = Join-Path $svc.Dir "target\$($svc.Jar)"
    $desactualizado = $false
    $clases = Join-Path $svc.Dir 'target\classes'
    if ((Test-Path $jar) -and (Test-Path $clases)) {
        # El .jar sale con fecha fija (build reproducible): se compara con la clase compilada mas reciente
        $fechaJar = (Get-ChildItem -Recurse -File $clases | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
        $desactualizado = [bool](Get-ChildItem -Recurse -File (Join-Path $svc.Dir 'src\main'), (Join-Path $svc.Dir 'pom.xml') |
            Where-Object { $_.LastWriteTime -gt $fechaJar } | Select-Object -First 1)
    }
    if ($Compilar -or $desactualizado -or -not (Test-Path $jar)) {
        Write-Host "  Compilando $($svc.Nombre)..."
        Push-Location $svc.Dir
        if (Test-Path '.\mvnw.cmd') { & .\mvnw.cmd -q package -DskipTests } else { & mvn -q package -DskipTests }
        $codigo = $LASTEXITCODE
        Pop-Location
        if ($codigo -ne 0) { throw "Fallo la compilacion de $($svc.Nombre)" }
    }
}

$llavePrivada = Get-Content -Raw (Join-Path $proyecto 'ApiGateway\private.pem')
$llavePublica = Get-Content -Raw (Join-Path $proyecto 'ApiGateway\public.pem')
$pids = @{}

Write-Host '3. Arrancando servicios...'
$pids.ms01 = (Iniciar-Java $servicios[0] @{
    DB_URL = 'jdbc:postgresql://localhost:5442/campus_db'
    DB_USERNAME = 'campus_user'
    DB_PASSWORD = 'campus_password'
    JWT_PRIVATE_KEY = $llavePrivada
}).Id

$pids.gateway = (Iniciar-Java $servicios[1] @{
    SPRING_PROFILES_ACTIVE = 'local'
    JWT_PUBLIC_KEY = $llavePublica
    GW_SERVICE_KEY_MS05 = $claveMs05
}).Id

$pids.ms05 = (Iniciar-Java $servicios[2] @{
    SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5435/brigades_incidents_db'
    SPRING_DATASOURCE_USERNAME = 'campusseguro'
    SPRING_DATASOURCE_PASSWORD = 'ms05_dev_password'
    SPRING_JPA_SHOW_SQL = 'false'
    MS05_GATEWAY_URL = 'http://localhost:8080'
    GW_SERVICE_KEY_MS05 = $claveMs05
    EVIDENCIAS_DIRECTORIO = (Join-Path $logs 'evidencias')
}).Id

$pids | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $logs 'pids.json')

Esperar-Salud 'ms01' 8081
Esperar-Salud 'gateway' 8080
Esperar-Salud 'ms05' 8085

Write-Host '4. Cargando eventos y datos de prueba...'
& (Join-Path $here 'cargar-datos.ps1')

Write-Host '5. Frontend (Vite)...'
$frontend = Join-Path $proyecto 'Frontend'
if (-not (Test-Path (Join-Path $frontend 'node_modules'))) {
    Push-Location $frontend; & npm.cmd install; Pop-Location
}
$env:VITE_API_URL = 'http://localhost:8080'
$env:VITE_USE_MS03 = 'false'
$vite = Start-Process -FilePath 'npm.cmd' -ArgumentList @('run', 'dev', '--', '--port', '5173', '--strictPort') `
    -WorkingDirectory $frontend -RedirectStandardOutput (Join-Path $logs 'frontend.log') `
    -RedirectStandardError (Join-Path $logs 'frontend.err.log') -WindowStyle Hidden -PassThru
Remove-Item Env:VITE_API_URL, Env:VITE_USE_MS03
$pids.frontend = $vite.Id
$pids | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $logs 'pids.json')

Write-Host ''
Write-Host 'Listo. Abre http://localhost:5173 (usuarios y contrasena en README.md).'
Write-Host 'Para apagar todo: .\detener.ps1'
