# Runs the k6 load test against a separate, throwaway copy of the whole stack.
#
#   powershell -ExecutionPolicy Bypass -File load-tests\run-load-test.ps1
#
# 1. Starts project "placementhub-loadtest" (own containers, network and database volume,
#    frontend on port 3100) with random one-off secrets.
# 2. Runs load-tests/k6/placementhub.js in a k6 container on that project's network.
# 3. Deletes the whole test stack, including its database (unless -KeepStack).
# Your normal "placementhub" stack and its data are never touched.

param(
    [int]$BrowseVus = 50,
    [int]$ApplyStudents = 200,
    [int]$SlotStudents = 100,
    [int]$SlotCapacity = 20,
    [int]$PoolSize = 10,
    [switch]$KeepStack
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$project = 'placementhub-loadtest'
$k6Image = 'grafana/k6:1.8.1'

function New-Secret([int]$length) {
    -join ((48..57) + (65..90) + (97..122) | Get-Random -Count $length | ForEach-Object { [char]$_ })
}

$adminPassword = New-Secret 20
$envFile = Join-Path ([System.IO.Path]::GetTempPath()) "placementhub-loadtest.env"
@(
    "MYSQL_DATABASE=placementhub"
    "MYSQL_USER=placementhub"
    "MYSQL_PASSWORD=$(New-Secret 24)"
    "MYSQL_ROOT_PASSWORD=$(New-Secret 24)"
    "MYSQL_HOST_PORT=3308"
    "APP_JWT_SECRET=$(New-Secret 48)"
    "ADMIN_EMAIL=admin@loadtest.local"
    "ADMIN_PASSWORD=$adminPassword"
    "FRONTEND_PORT=3100"
    "HIKARI_POOL_SIZE=$PoolSize"
) | Set-Content -Path $envFile -Encoding ascii

$compose = @('compose', '-p', $project, '-f', (Join-Path $root 'compose.yaml'),
             '-f', (Join-Path $PSScriptRoot 'compose.loadtest.yaml'), '--env-file', $envFile)

try {
    # A stack left over from -KeepStack has a database created with other passwords; start clean.
    # Windows PowerShell treats docker's progress output on stderr as an error, so relax that here.
    $ErrorActionPreference = 'Continue'
    & docker @compose down -v *> $null
    $ErrorActionPreference = 'Stop'
    Write-Host "Starting isolated stack '$project' (first build can take a few minutes)..."
    & docker @compose up --build -d --wait
    if ($LASTEXITCODE -ne 0) { throw "docker compose up failed" }

    Write-Host "Running k6 (DB pool $PoolSize, $BrowseVus browsing users, $ApplyStudents simultaneous applicants, $SlotStudents students for $SlotCapacity seats)..."
    Get-Content (Join-Path $PSScriptRoot 'k6\placementhub.js') -Raw |
        docker run --rm -i --network "${project}_default" `
            -e BASE_URL=http://frontend:8080 -e ADMIN_EMAIL=admin@loadtest.local -e "ADMIN_PASSWORD=$adminPassword" `
            -e "BROWSE_VUS=$BrowseVus" -e "APPLY_STUDENTS=$ApplyStudents" -e "SLOT_STUDENTS=$SlotStudents" -e "SLOT_CAPACITY=$SlotCapacity" `
            $k6Image run -
    $k6Exit = $LASTEXITCODE
    if ($k6Exit -eq 0) { Write-Host "All thresholds passed." -ForegroundColor Green }
    else { Write-Host "k6 reported failed thresholds (exit $k6Exit). See the summary above." -ForegroundColor Red }
}
finally {
    if (-not $KeepStack) {
        Write-Host "Removing the load-test stack and its database..."
        & docker @compose down -v | Out-Null
    }
    Remove-Item $envFile -ErrorAction SilentlyContinue
}
exit $k6Exit
