param(
    [switch]$Build
)

$ROOT = $PSScriptRoot

function Wait-ForPort {
    param([int]$Port, [string]$Name, [int]$TimeoutSec = 60)
    Write-Host "Waiting for $Name (port $Port)..." -ForegroundColor Yellow
    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $tcp.Connect("localhost", $Port)
            $tcp.Close()
            Write-Host "$Name is ready." -ForegroundColor Green
            return $true
        } catch {
            Start-Sleep -Seconds 2
            $elapsed += 2
        }
    }
    Write-Host "$Name did not start within ${TimeoutSec}s." -ForegroundColor Red
    return $false
}

function Start-Service {
    param([string]$Module, [int]$Port)
    Write-Host "Starting $Module..." -ForegroundColor Cyan
    Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/k", "title $Module && cd /d `"$ROOT`" && gradlew.bat :${Module}:bootRun" `
        -WindowStyle Normal
    Start-Sleep -Seconds 3
}

# ──────────────────────────────────────────────
# 1. Infra
# ──────────────────────────────────────────────
Write-Host "`n[1/4] Starting infrastructure..." -ForegroundColor Magenta
Set-Location $ROOT
docker compose up -d
if ($LASTEXITCODE -ne 0) { Write-Host "docker compose failed." -ForegroundColor Red; exit 1 }

Write-Host "Waiting 10s for infra to settle..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# ──────────────────────────────────────────────
# 2. Build (optional)
# ──────────────────────────────────────────────
if ($Build) {
    Write-Host "`n[2/4] Building all services (skipping tests)..." -ForegroundColor Magenta
    & "$ROOT\gradlew.bat" build -x test
    if ($LASTEXITCODE -ne 0) { Write-Host "Build failed." -ForegroundColor Red; exit 1 }
    Write-Host "Build complete." -ForegroundColor Green
} else {
    Write-Host "`n[2/4] Skipping build (pass -Build to rebuild)" -ForegroundColor DarkGray
}

# ──────────────────────────────────────────────
# 3. Core services (ordered)
# ──────────────────────────────────────────────
Write-Host "`n[3/4] Starting core services..." -ForegroundColor Magenta

Start-Service "discovery-service" 8761
Wait-ForPort 8761 "discovery-service" 60

Start-Service "config-service" 8888
Wait-ForPort 8888 "config-service" 60

Start-Service "api-gateway" 9000
Start-Sleep -Seconds 5

# ──────────────────────────────────────────────
# 4. Business services (parallel)
# ──────────────────────────────────────────────
Write-Host "`n[4/4] Starting business services..." -ForegroundColor Magenta

Start-Service "user-service"         8081
Start-Service "product-service"      8082
Start-Service "order-service"        8083
Start-Service "payment-service"      8084
Start-Service "cart-service"         8085
Start-Service "notification-service" 8086

Write-Host "`n============================================" -ForegroundColor Green
Write-Host " All services started!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host " API Gateway    : http://localhost:9000"
Write-Host " Eureka         : http://localhost:8761"
Write-Host " Swagger (user) : http://localhost:8081/swagger-ui.html"
Write-Host " Zipkin         : http://localhost:9411"
Write-Host " Grafana        : http://localhost:3000  (admin / admin123)"
Write-Host ""
Write-Host "Wait ~30s for all services to register with Eureka." -ForegroundColor Yellow
