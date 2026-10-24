# Runs the Playwright end-to-end tests against a separate, throwaway copy of the whole stack.
#
#   powershell -ExecutionPolicy Bypass -File e2e\run-e2e.ps1            # headless
#   powershell -ExecutionPolicy Bypass -File e2e\run-e2e.ps1 -Headed    # watch the browser
#
# 1. Starts project "placementhub-e2e" (own containers, network and database volume,
#    frontend on port 3200) with random one-off secrets and its own admin account.
# 2. Runs the tests in e2e/tests with Chromium.
# 3. Deletes the whole test stack, including its database (unless -KeepStack).
# Your normal "placementhub" stack and its data are never touched.

param(
    [switch]$Headed,
    [switch]$KeepStack
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$project = 'placementhub-e2e'
$port = 3200

function New-Secret([int]$length) {
    -join ((48..57) + (65..90) + (97..122) | Get-Random -Count $length | ForEach-Object { [char]$_ })
}

$adminEmail = 'admin@e2e.test'
$adminPassword = New-Secret 20
$envFile = Join-Path ([System.IO.Path]::GetTempPath()) "placementhub-e2e.env"
@(
    "MYSQL_DATABASE=placementhub"
    "MYSQL_USER=placementhub"
    "MYSQL_PASSWORD=$(New-Secret 24)"
    "MYSQL_ROOT_PASSWORD=$(New-Secret 24)"
    "MYSQL_HOST_PORT=3309"
    "APP_JWT_SECRET=$(New-Secret 48)"
    "ADMIN_EMAIL=$adminEmail"
    "ADMIN_PASSWORD=$adminPassword"
    "FRONTEND_PORT=$port"
) | Set-Content -Path $envFile -Encoding ascii

$compose = @('compose', '-p', $project, '-f', (Join-Path $root 'compose.yaml'),
             '-f', (Join-Path $PSScriptRoot 'compose.e2e.yaml'), '--env-file', $envFile)

$testExit = 1
try {
    Push-Location $PSScriptRoot
    if (-not (Test-Path node_modules)) {
        Write-Host "Installing Playwright (first run only)..."
        npm ci
        if ($LASTEXITCODE -ne 0) { throw "npm ci failed" }
    }
    npx playwright install chromium
    if ($LASTEXITCODE -ne 0) { throw "Playwright browser install failed" }

    # A stack left over from -KeepStack has a database created with other passwords; start clean.
    # Windows PowerShell treats docker's progress output on stderr as an error, so relax that here.
    $ErrorActionPreference = 'Continue'
    & docker @compose down -v *> $null
    $ErrorActionPreference = 'Stop'
    Write-Host "Starting isolated stack '$project' (first build can take a few minutes)..."
    & docker @compose up --build -d --wait
    if ($LASTEXITCODE -ne 0) { throw "docker compose up failed" }

    $env:E2E_BASE_URL = "http://localhost:$port"
    $env:E2E_ADMIN_EMAIL = $adminEmail
    $env:E2E_ADMIN_PASSWORD = $adminPassword
    $playwrightArgs = @('playwright', 'test')
    if ($Headed) { $playwrightArgs += '--headed' }
    npx @playwrightArgs
    $testExit = $LASTEXITCODE
    if ($testExit -eq 0) { Write-Host "All end-to-end tests passed." -ForegroundColor Green }
    else { Write-Host "End-to-end tests failed (exit $testExit). See the output above." -ForegroundColor Red }
}
finally {
    Pop-Location
    Remove-Item Env:E2E_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    if (-not $KeepStack) {
        Write-Host "Removing the end-to-end stack and its database..."
        & docker @compose down -v | Out-Null
    }
    Remove-Item $envFile -ErrorAction SilentlyContinue
}
exit $testExit
