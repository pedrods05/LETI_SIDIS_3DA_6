param(
    [int]$BlueWeight = 100,
    [int]$GreenWeight = 0,
    [switch]$Canary,
    [int]$Step = 10,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Validate-Weights {
    param($Blue, $Green)
    if ($Blue -lt 0 -or $Green -lt 0) { throw "Weights must be >= 0" }
    if ($Blue + $Green -ne 100) { throw "Weights must sum to 100 (got $($Blue + $Green))" }
}

Validate-Weights -Blue $BlueWeight -Green $GreenWeight

$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$nginxConf = Join-Path $repoRoot "hap-appointmentrecords\nginx.conf"
if (-not (Test-Path $nginxConf)) { throw "nginx.conf not found at $nginxConf" }

function Set-Weights {
    param($Blue, $Green)
    Validate-Weights -Blue $Blue -Green $Green
    $content = Get-Content -Raw $nginxConf
    $content = $content -replace 'hap-appointmentrecords-blue:8083 weight=\d+', "hap-appointmentrecords-blue:8083 weight=$Blue"
    $content = $content -replace 'hap-appointmentrecords-green:8090 weight=\d+', "hap-appointmentrecords-green:8090 weight=$Green"
    if (-not $DryRun) { Set-Content -Path $nginxConf -Value $content -NoNewline }
    Write-Host "Weights -> blue: $Blue / green: $Green" -ForegroundColor Cyan
}

if ($Canary) {
    $b = 100; $g = 0
    while ($g -lt $GreenWeight) {
        $g = [Math]::Min($g + $Step, $GreenWeight)
        $b = 100 - $g
        Set-Weights -Blue $b -Green $g
    }
} else {
    Set-Weights -Blue $BlueWeight -Green $GreenWeight
}

if ($DryRun) { Write-Host "Dry run only; not reloading nginx"; return }

try {
    docker compose exec hap-appointmentrecords-proxy nginx -s reload | Out-Null
    Write-Host "Reloaded nginx in hap-appointmentrecords-proxy" -ForegroundColor Green
} catch {
    Write-Warning "Could not reload nginx (is the proxy running?). Start stack, then rerun: docker compose up -d hap-appointmentrecords-proxy"
}
